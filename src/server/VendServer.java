package server;

import java.io.*;
import java.net.*;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.Date;

import util.DatabaseInterface;

public class VendServer {
	//Setup the connection to the DB
	static Connection conn = null;
	
	// Number a client
	private static int clientNo = 0;

	//Thread to respond to any network broadcast messages intended to find the server
	static DiscoveryThread discover = new DiscoveryThread();
	
	//List of all clients threads that are handled by the server, unused currently
	static ArrayList<HandleAClient> list = new ArrayList<HandleAClient>();

	public static void main(String[] args) {
		//New server thread
		ServerThread();
		
		//Listen and serve broadcast messages
		DiscoveryThread foo = new DiscoveryThread();
		new Thread(foo).start();
	}
	
	@SuppressWarnings("resource")
	private static void ServerThread(){
		new Thread( () -> {
			try {
				// Create a server socket
				ServerSocket serverSocket = new ServerSocket(8000);
				System.out.println("VendServer started at " + new Date());
				DBConnect();
				
				while (true) {
					// Listen for a new connection request
					Socket socket = serverSocket.accept();

					// Increment clientNo
					clientNo++;
					
					// Display the client number
					System.out.println("Client " + clientNo + " connected!");

					// Find the client's host name, and IP address
					InetAddress inetAddress = socket.getInetAddress();

					System.out.println("Client " + clientNo + "'s IP Address is " + inetAddress.getHostAddress());

					// Create and start a new thread for the connection
					HandleAClient foo = new HandleAClient(socket);
					new Thread(foo).start();
					list.add(foo);
				}
			}
			catch(IOException ex) {
				//Should only catch IOExceptions from ServerSocket
				System.err.println(ex.getMessage());
			}catch(Exception e){
				//Generic exception
				//If we get here something is seriously messed up.
				System.err.println(e.getMessage());
			}
		}).start();
	}
	
	/**
	 * Connect to the database at il-server-001.uccc.uc.edu with the privetsl credentials.
	 * Writes to server log whether it was a success or failure
	 */
	public static void DBConnect(){				
		DatabaseInterface db = new DatabaseInterface();
		System.out.println("Driver Loaded");
		
		// address to connect to my database
		conn = db.Connect("45.63.17.32",
				"VendDB",
		//conn = db.Connect("10.230.22.56\\DEVICESQLSERVER2",
				"VendServer", 
				"Maspe36Miami");
		
		//Connect method returns a null connection if it was not a successful connection
		if(conn != null){
			System.out.println("Successfully Connected to DataBase!");
		}else{
			System.err.println("ERROR: Connection to the db timed out. Please check your connection.");
		}
	}
	
	/**
	 * Returns the instance of the database connection
	 * @return Connection 
	 */
	public static Connection getConnection(){
		return conn;
	}
	
	/**
	 * Returns the instance of the number of clients
	 * @return clientNo
	 */
	public static int getClientNo(){
		return clientNo;
	}
	
	/**
	 * Sets the static var clientNo to a new int
	 * @param NewNumOfClients
	 */
	public static void setClientNo(int NewNumOfClients){
		clientNo = NewNumOfClients;
	}
}