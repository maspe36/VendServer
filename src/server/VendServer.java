package server;

import java.io.*;
import java.net.*;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Date;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.stage.Stage;

public class VendServer extends Application {
	// Text area for displaying contents
	private TextArea ta = new TextArea();

	//Setup the connection to the DB
	Connection conn = null;
	
	// Number a client
	private int clientNo = 0;
	
	//global message var
	private String message;
	
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
		Scene scene = new Scene(new ScrollPane(ta), 450, 200);
		primaryStage.setTitle("VendServer"); // Set the stage title
		primaryStage.setScene(scene); // Place the scene in the stage
		primaryStage.show(); // Display the stage

		//New server thread
		clientThread();
	}
	
	@SuppressWarnings("resource")
	private void clientThread(){
		new Thread( () -> {
			try {
				// Create a server socket
				ServerSocket serverSocket = new ServerSocket(8000);
				Platform.runLater( () -> {ta.appendText("VendServer started at " 
						+ new Date() + '\n');});

				DBConnect();
				
				while (true) {
					// Listen for a new connection request
					Socket socket = serverSocket.accept();

					// Increment clientNo
					clientNo++;

					Platform.runLater( () -> {
						// Display the client number
						ta.appendText("Starting thread for client " + clientNo +
								" at " + new Date() + '\n');

						// Find the client's host name, and IP address
						InetAddress inetAddress = socket.getInetAddress();
						ta.appendText("Client " + clientNo + "'s host name is "
								+ inetAddress.getHostName() + "\n");
						ta.appendText("Client " + clientNo + "'s IP Address is "
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
			try {
				// Create data input and output streams
				inputFromClient = new ObjectInputStream(socket.getInputStream());
				//outputToClient = new ObjectOutputStream(socket.getOutputStream());

				// Continuously serve the client
				while (true) {
					// Receive message from the client
					ListenForClient(inputFromClient);
					
					// Format message to SQL statement
					message = Util.toSQL(message);
						
					// Run against DB
					queryServer(message);
				}
			}
			catch(IOException ex) {
				ex.printStackTrace();
			}    
		}
	}

	public static void main(String[] args) {
		launch(args);
	}
	
	/**
	 * Listen for messages from the client and write if it was a success or failure to the Server log.
	 * @param inputFromClient
	 */
	public void ListenForClient(ObjectInputStream inputFromClient){
		try {
			message = inputFromClient.readUTF();

			parts = message.split(":");
			
			//For readability
			MacAddress = parts[0];
			ItemSlot = parts[1];
			
			Platform.runLater(() -> {
				ta.appendText("Message received from " + MacAddress +  ": " +ItemSlot + "\n");
			});	
		} catch (IOException e) {
			ta.appendText("Error recieving messages from Client!" + "\n");
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
				"privetsl", 
				"Maspe36Miami");
		
		//Connect method returns a null connection if it was not a successful connection
		if(conn != null){
			ta.appendText("Successfully Connected to DataBase!" + "\n");
		}else{
			ta.appendText("Connection to the db timed out. Please check your connection." + "\n");
		}
	}
	
	/**
	 * Requires a connection to a database.
	 * Runs the provided Query against the database that is currently connected.
	 * @param SQLQuery
	 */
	public void queryServer(String SQLQuery){
		try {
			Statement stmt = conn.createStatement();
			stmt.execute(SQLQuery);
			ta.appendText("Query successfully run against Database!" + "\n");
		} catch (SQLException e) {
			System.out.println(e.getMessage());
			ta.appendText("Failed running the SQL against the database!" + "\n");
		}
	}
}