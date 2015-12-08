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
	String[] parts;
	//private String backup;
	ArrayList<HandleAClient> list = new ArrayList<HandleAClient>();

	@SuppressWarnings("resource")
	@Override // Override the start method in the Application class
	public void start(Stage primaryStage) {
		// Create a scene and place it in the stage
		Scene scene = new Scene(new ScrollPane(ta), 450, 200);
		primaryStage.setTitle("VendServer"); // Set the stage title
		primaryStage.setScene(scene); // Place the scene in the stage
		primaryStage.show(); // Display the stage

		//New server thread
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

	// Define the thread class for handling new connection
	class HandleAClient implements Runnable {
		private Socket socket; // A connected socket
		ObjectInputStream inputFromClient;
		ObjectOutputStream outputToClient;
		/** Construct a thread */
		public HandleAClient(Socket socket) {
			this.socket = socket;
		}

		/** Run a thread */
		public void run() {
			try {
				// Create data input and output streams
				inputFromClient = new ObjectInputStream(socket.getInputStream());
				outputToClient = new ObjectOutputStream(socket.getOutputStream());

				// Continuously serve the client
				while (true) {
					// Receive message from the client
					ListenForClient(inputFromClient);
					// Format message to SQL statement
					message = Util.toSQL(message);
					// Run against DB
					queryServer(message);
					// Send success or failure
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
	
	//Query the DB
	public void queryServer(String msg){
		try {
			Statement stmt = conn.createStatement();
			stmt.execute(msg);
			ta.appendText("Recorded transaction in tVendLog" + "\n");
		} catch (SQLException e) {
			System.out.println(e.getMessage());
			ta.appendText("Failed running the SQL against the database!" + "\n");
		}
	}
	
	//Continuously listen for a message from the client
	public void ListenForClient(ObjectInputStream inputFromClient){
		try {
			message = inputFromClient.readUTF();

			parts = message.split(":");
			
			Platform.runLater(() -> {
				ta.appendText("Message received from " + parts[0] +  " : " + parts[1] + "\n");
			});
		} catch (IOException e) {
			ta.appendText("Error while listening for client messages!" + "\n");
		}
	}
	
	//Connect to the database
	public void DBConnect(){				
		DatabaseInterface db = new DatabaseInterface();
		System.out.println("Driver Loaded");
		
		conn = db.Connect("il-server-001.uccc.uc.edu\\mssqlserver2012",
				"privetsl", 
				"Maspe36Miami");
		
		if(conn != null){
			// address to connect to my database
			ta.appendText("Successfully Connected to DataBase!");
		}else{
			ta.appendText("Connection to the db timed out. Please check your connection.");
		}
	}
}