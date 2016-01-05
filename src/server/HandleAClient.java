/**
 * 
 */
package server;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import util.Util;

//TO-DO: 
//1. Add a way to specify which client is gone
//2. Restructure some of the queryServer method


/**
 * The constructor creates the relationship between a client thread 
 * and a new socket to connect them to the server. Contains methods to handle interactions
 * with the client.
 * @author Sam Privett
 *
 */
public class HandleAClient implements Runnable{
	
	private boolean shutdown = false;
	private String received;
	
	private PreparedStatement SQLQuery;
	private String PlainTextSQL;
	
	//Global parts of message 
	//Current format is P:MM-MM-MM-MM-MM-MM:XX 
	//P = Protocol, X = ItemSlot(A1, A2...), and M = MacAddress
	String[] parts;
	
	String Protocol;
	String MacAddress;
	String ItemSlot;
	
	private Socket socket; // A connected socket
	InputStream inputFromClient;
	
	/** Construct a thread 
	 * @param [] */
	public HandleAClient(Socket socket) {
		this.socket = socket;
	}

	/** Create a new thread to serve a single client */
	public void run() {
		// Create data input and output streams
		try {
			inputFromClient = socket.getInputStream();
			
			// Continuously serve the client
			while (shutdown == false) {
					// Receive message from the client
					ListenForClient(inputFromClient);

					// Format message to SQL statement
					SQLQuery = Util.toSQL(received, VendServer.getConnection());
					
					HandleProtocol(Protocol, shutdown, inputFromClient);
					
					if(SQLQuery != null){
						// Grabs the unsafe SQL in plain text
						PlainTextSQL = Util.toSQL(received);
					
						// Run against DB
						queryServer(SQLQuery, PlainTextSQL);
					}
			}
		}
		catch(NullPointerException | IOException ConnectionEx){
			//Recoverable error
			System.err.println(ConnectionEx.getMessage());
			closeClientThread(shutdown);
		}
	}


	/**
 	* Listen for messages from the client and write if it was a success or failure to the Server log.
 	* @param inputFromClient ObjectInputStream
 	* @throws IOException Connection has been lost to ObjectInputStream
 	*/
	public void ListenForClient(InputStream inputFromClient)throws NullPointerException{
		try {
			// Receiving
        	byte[] lenBytes = new byte[4];
        	inputFromClient.read(lenBytes, 0, 4);
        	int len = (((lenBytes[3] & 0xff) << 24) | ((lenBytes[2] & 0xff) << 16) |
                  	  ((lenBytes[1] & 0xff) << 8) | (lenBytes[0] & 0xff));
        	byte[] receivedBytes = new byte[len];
        	inputFromClient.read(receivedBytes, 0, len);
        	received = new String(receivedBytes, 0, len);
        
        	parts = received.split(":");
        		
			//For readability
			Protocol = parts[0];
			MacAddress = parts[1];
			ItemSlot = parts[2];
		
			System.out.println("Message received from " + Protocol + ":" + MacAddress +  ": " +ItemSlot);
		}catch (NullPointerException NPE){
			throw new NullPointerException("ERROR: Connection to the client was lost!" + "\n");
		}
		catch (IOException e) {
			//TO-DO 1
			throw new NullPointerException("ERROR: Connection to the client was lost!" + "\n");
		}
	}
	
	/**
	 * Requires a connection to a database.
	 * Runs the provided Query against the database that is currently connected.
	 * @param SQLQuery
	 */
	public void queryServer(PreparedStatement SQLQuery, String SQLClone){
		//TO-DO 2
		try {
			System.out.println("Attempting to run the following SQL against the connected DB... \n '" + SQLClone);
			//If it runs and there is a result
			if(SQLQuery.executeUpdate() > 0){
				System.out.println("Success!");
			}else{
				//Runs but no rows affected
				System.out.println("WARNING: The SQL ran succesfully but there were no rows affected!");
			}
			//Cannot have a negative quantity, EX Selling a product when there is zero in stock
		} catch (SQLException e) {
			System.err.println(e.getMessage());
		}
	}
	
	/**
	 * Writes to the server log and changes a flag to close the thread handling this client. 
	 * Then decrements the number of clients connected and prints the amount connected currently.
	 * 
	 * @param shutdown Flags the thread to close on true
	 */
	private void closeClientThread(boolean shutdown){
		//Close this thread if the connection to the client has been closed.
		this.shutdown = true; //Pass by reference with this.shutdown otherwise the value will not change
		System.out.println("Closing thread for machine with id " + getMacAddress() + "...");
		
		//Decrement the amount of clients connected
		VendServer.setClientNo(VendServer.getClientNo() - 1);
		System.out.println(VendServer.getClientNo() + " client(s) connected");
	}
	
	/**
	 * Gives different messages depending on the different protocols. 2 returns whether or not the vending machine exists.
	 * 3 closes the clients thread and logs it to the server log.
	 * 
	 * @param Protocol The protocol code sent at the beginning of the message sent from a client
	 * @param shutdown Flag to close thread handling client
	 * @param inputFromClient InputStream from client
	 */
	private void HandleProtocol(String Protocol, boolean shutdown, InputStream inputFromClient){
		
		char charProto = Protocol.charAt(0);
		
		switch(charProto){
			case '2': // New vend machine protocol
				//Because SQLQuery will be null if the machine was found, see Util.toSQL
				if(SQLQuery != null){
					System.out.println("Added Vending Machine to the Database");
					System.out.println("WARNING: Fill the new Vending Machine!");
				}else{
					System.out.println("Found Vending Machine in Database!");
				}
				break;
			case '3': // Client leaving protocol
				System.out.println("Connection closed by the client");
				closeClientThread(shutdown);
				break;
		}
	}

	/**
	 * Gets the current thread's MacAddress
	 * @return MacAddress
	 */
	private String getMacAddress(){
		return MacAddress;
	}
}