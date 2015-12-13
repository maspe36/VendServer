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
	private String message;
	
	private PreparedStatement SQLQuery;
	private String PlainTextSQL;
	
	//Global parts of message 
	//Current format is MM-MM-MM-MM-MM-MM:XX 
	//X = ItemSlot(A1, A2...), and M = MacAddress
	String[] parts;
	
	String MacAddress;
	String ItemSlot;
	
	//List of all clients threads that are handled by the server, unused currently
	ArrayList<HandleAClient> list = new ArrayList<HandleAClient>();

	@Override // Override the start method in the Application class
	public void start(Stage primaryStage) {
		// Create a scene and place it in the stage
		Scene scene = new Scene(Log);
		primaryStage.setTitle("VendServer"); // Set the stage title
		primaryStage.setScene(scene); // Place the scene in the stage
		primaryStage.show(); // Display the stage
		Log.setWrapText(true); //WordWrap
		
		//Dynamically size the Textarea to the size of the window
		Log.prefWidthProperty().bind(scene.widthProperty());
		Log.prefHeightProperty().bind(scene.heightProperty());
		
		//New server thread
		clientThread();
	}
	
	@SuppressWarnings("resource")
	private void clientThread(){
		new Thread( () -> {
			try {
				// Create a server socket
				ServerSocket serverSocket = new ServerSocket(8000);
				Platform.runLater( () -> {Log.appendText("VendServer started at " 
						+ new Date() + '\n');});

				DBConnect();
				
				while (true) {
					// Listen for a new connection request
					Socket socket = serverSocket.accept();

					// Increment clientNo
					clientNo++;

					Platform.runLater( () -> {
						// Display the client number
						Log.appendText("Starting thread for client " + clientNo +
								" at " + new Date() + '\n');

						// Find the client's host name, and IP address
						InetAddress inetAddress = socket.getInetAddress();
						Log.appendText("Client " + clientNo + "'s host name is "
								+ inetAddress.getHostName() + "\n");
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
		ObjectInputStream inputFromClient;
		//ObjectOutputStream outputToClient;
		/** Construct a thread */
		public HandleAClient(Socket socket) {
			this.socket = socket;
		}

		/** Create a new thread to serve a single client */
		public void run() {
			
			boolean shutdown;
			
			// Create data input and output streams
			try {
				inputFromClient = new ObjectInputStream(socket.getInputStream());
				//outputToClient = new ObjectOutputStream(socket.getOutputStream());

				// set this to true to stop the thread
				shutdown = false;

				
				// Continuously serve the client
				while (!shutdown) {
						// Receive message from the client
						ListenForClient(inputFromClient);
						
						// Format message to SQL statement
						SQLQuery = Util.toSQL(message, conn);
					
						// Grabs the unsafe SQL in plain text
						PlainTextSQL = Util.toSQL(message);
				
						// Run against DB
						queryServer(SQLQuery, PlainTextSQL);
				}	
			}catch (SQLException sqlE){
				//Recoverable error
				Log.appendText(sqlE.getMessage() + "\n");
			}catch(NullPointerException | IOException ConnectionEx){
				//Recoverable error
				Log.appendText(ConnectionEx.getMessage() + "\n");
				Log.appendText("Closing thread for this client...");
				//Close this thread if the connection to the client has been closed.
				shutdown = true;
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
	public void ListenForClient(ObjectInputStream inputFromClient)throws NullPointerException{
		try {
			message = inputFromClient.readUTF();

			parts = message.split(":");
			
			//For readability
			MacAddress = parts[0];
			ItemSlot = parts[1];
		
			Log.appendText("Message received from " + MacAddress +  ": " +ItemSlot + "\n");	
		}catch (NullPointerException NPE){
			throw new NullPointerException("ERROR: Connection to the client was lost!");
		}
		catch (IOException e) {
			//TO-DO: Add a way to specify which client is gone
			throw new NullPointerException("ERROR: Connection to the client was lost!");
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
			Log.appendText("Successfully Connected to DataBase!" + "\n");
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
				Log.appendText("Success!" + "\n");
			}else{
				//Runs but no rows affected
				Log.appendText("The SQL ran succesfully but there were no rows affected!" + "\n");
			}
			//Cannot have a negative quantity, EX Selling a product when there is zero in stock
		} catch (SQLException e) {
			Log.appendText("ERROR: Quantity cannot be negative!" + "\n");
		}
	}
}