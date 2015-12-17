package server;

import java.io.*;
import java.net.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.TextArea;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import util.DatabaseInterface;
import util.Util;

public class VendServer extends Application {
	// Text area for displaying contents
	private TextArea Log = new TextArea();
	
	//Setup the connection to the DB
	Connection conn = null;
	
	// Number a client
	private int clientNo = 0;
	
	//global message var
	//private String message;
	private String received;
	
	private PreparedStatement SQLQuery;
	private String PlainTextSQL;
	
	//message colors
	Color Success = Color.GREEN;
	Color Warning = Color.YELLOW;
	Color Error = Color.RED;
	
	//Global parts of message 
	//Current format is P:MM-MM-MM-MM-MM-MM:XX 
	//P = Protocol, X = ItemSlot(A1, A2...), and M = MacAddress
	String[] parts;
	
	String Protocol;
	String MacAddress;
	String ItemSlot;
	
	// To change the UI outside of the application thread use the new java 8 syntax
	// Platform.runLater( () -> { 'Your UI changes here' });
	// We use this to add children to our TextFlow (Log);
	
	//List of all clients threads that are handled by the server, unused currently
	ArrayList<HandleAClient> list = new ArrayList<HandleAClient>();

	private boolean shutdown = false;

	@Override // Override the start method in the Application class
	public void start(Stage primaryStage) {
		// Create a scene and place it in the stage
		Scene scene = new Scene(Log);
		primaryStage.setTitle("VendServer"); // Set the stage title
		primaryStage.setScene(scene); // Place the scene in the stage
		primaryStage.show(); // Display the stage
		
		//Dynamically size the Textarea to the size of the window
		Log.prefWidthProperty().bind(scene.widthProperty());
		Log.prefHeightProperty().bind(scene.heightProperty());
		
		Log.setWrapText(true);
		
		//New server thread
		clientThread();
	}
	
	@SuppressWarnings("resource")
	private void clientThread(){
		new Thread( () -> {
			try {
				// Create a server socket
				ServerSocket serverSocket = new ServerSocket(8000);
				Platform.runLater( () -> {
					Log.appendText("VendServer started at " 
							+ new Date() + '\n');
				});

				DBConnect();
				
				while (true) {
					// Listen for a new connection request
					Socket socket = serverSocket.accept();

					// Increment clientNo
					clientNo++;

					Platform.runLater( () -> {
						// Display the client number
						Log.appendText("Client " + clientNo + " connected!" + '\n');

						// Find the client's host name, and IP address
						InetAddress inetAddress = socket.getInetAddress();

						Log.appendText("Client " + clientNo + "'s IP Address is "
								+ inetAddress.getHostAddress() + "\n");
					});

					// Create and start a new thread for the connection
					HandleAClient foo = new HandleAClient(socket);
					new Thread(foo).start();
					list.add(foo);
				}
			}
			catch(IOException ex) {
				System.err.println(ex);
			}
		}).start();
	}

	/**
	 * The constructor creates the relationship between a client thread 
	 * and a new socket to connect them to the server. Contains methods to handle interactions
	 * with the client.
	 * @author Sam Privett
	 *
	 */
	class HandleAClient implements Runnable {
		private Socket socket; // A connected socket
		InputStream inputFromClient;
		//DataInputStream inputFromClient;
		//ObjectOutputStream outputToClient;
		/** Construct a thread */
		public HandleAClient(Socket socket) {
			this.socket = socket;
		}

		/** Create a new thread to serve a single client */
		public void run() {
			// Create data input and output streams
			try {
				inputFromClient = socket.getInputStream();
				//outputToClient = new ObjectOutputStream(socket.getOutputStream());
				
				// Continuously serve the client
				while (shutdown == false) {
						// Receive message from the client
						ListenForClient(inputFromClient);
	
						// Format message to SQL statement
						SQLQuery = Util.toSQL(received, conn);
						
						HandleProtocol(Protocol, shutdown, inputFromClient);
						
						if(SQLQuery != null){
							// Grabs the unsafe SQL in plain text
							PlainTextSQL = Util.toSQL(received);
						
							// Run against DB
							queryServer(SQLQuery, PlainTextSQL);
						}
				}	
			}catch (SQLException sqlE){
				//Recoverable error
				Log.appendText(sqlE.getMessage());
			}catch(NullPointerException | IOException ConnectionEx){
				//Recoverable error
				Log.appendText(ConnectionEx.getMessage());
				closeClientThread(shutdown);
			}
		}
	}

	public static void main(String[] args) {
		launch(args);
	}
	
	/**
	 * Listen for messages from the client and write if it was a success or failure to the Server log.
	 * @param inputFromClient ObjectInputStream
	 * @throws IOException Connection has been lost to ObjectInputStream
	 */
	public void ListenForClient(InputStream inputFromClient)throws NullPointerException{
		try {
			//message = inputFromClient.readUTF();
			// Receiving
	        byte[] lenBytes = new byte[4];
	        inputFromClient.read(lenBytes, 0, 4);
	        int len = (((lenBytes[3] & 0xff) << 24) | ((lenBytes[2] & 0xff) << 16) |
	                  ((lenBytes[1] & 0xff) << 8) | (lenBytes[0] & 0xff));
	        byte[] receivedBytes = new byte[len];
	        inputFromClient.read(receivedBytes, 0, len);
	        received = new String(receivedBytes, 0, len);

	        parts = received.split(":");
	        
			//parts = message.split(":");
			
			//For readability
			Protocol = parts[0];
			MacAddress = parts[1];
			ItemSlot = parts[2];
			
			Log.appendText("Message received from " + Protocol + ":" + MacAddress +  ": " +ItemSlot + "\n");
		}catch (NullPointerException NPE){
			throw new NullPointerException("ERROR: Connection to the client was lost!" + "\n");
		}
		catch (IOException e) {
			//TO-DO: Add a way to specify which client is gone
			throw new NullPointerException("ERROR: Connection to the client was lost!" + "\n");
		}
	}
	
	/**
	 * Connect to the database at il-server-001.uccc.uc.edu with the privetsl credentials.
	 * Writes to server log whether it was a success or failure
	 */
	public void DBConnect(){				
		DatabaseInterface db = new DatabaseInterface();
		System.out.println("Driver Loaded");
		
		// address to connect to my database
		conn = db.Connect("il-server-001.uccc.uc.edu\\mssqlserver2012",
				"CandyServerLogin", 
				"Maspe36Miami");
		
		//Connect method returns a null connection if it was not a successful connection
		if(conn != null){
			Log.appendText("Successfully Connected to DataBase!" + "\n\n");
		}else{
			Log.appendText("ERROR: Connection to the db timed out. Please check your connection." + "\n");
		}
	}
	
	/**
	 * Requires a connection to a database.
	 * Runs the provided Query against the database that is currently connected.
	 * @param SQLQuery
	 */
	public void queryServer(PreparedStatement SQLQuery, String SQLClone){
		try {
			Log.appendText("Attempting to run the following SQL against the connected DB... \n '" + SQLClone + "'\n");
			//If it runs and there is a result
			if(SQLQuery.executeUpdate() > 0){
				Log.appendText("Success!" + "\n\n");
			}else{
				//Runs but no rows affected
				Log.appendText("WARNING: The SQL ran succesfully but there were no rows affected!" + "\n\n");
			}
			//Cannot have a negative quantity, EX Selling a product when there is zero in stock
		} catch (SQLException e) {
			Log.appendText("ERROR: Quantity cannot be negative!" + "\n\n");
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

		Log.appendText("Closing thread for this client..." + "\n");
		
		//Decrement the amount of clients connected
		clientNo--;
		
		Log.appendText(clientNo + " client(s) connected" + "\n\n");
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
					Log.appendText("WARNING: Fill the new Vending Machine!" + "\n\n");
				}else{
					Log.appendText("Found Vending Machine in Database!" + "\n\n");
				}
				break;
			case '3': // Client leaving protocol
				Log.appendText("Connection closed by the client" + "\n");
				closeClientThread(shutdown);
				break;
		}
		
			
	}
}