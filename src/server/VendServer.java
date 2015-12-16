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
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.stage.Stage;
import util.DatabaseInterface;
import util.Util;

public class VendServer extends Application {
	// Text area for displaying contents
	private TextFlow Log = new TextFlow();
	
	//Setup the connection to the DB
	Connection conn = null;
	
	// Number a client
	private int clientNo = 0;
	
	//global message var
	private String message;
	
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
					Text DateText = new Text("VendServer started at " 
						+ new Date() + '\n');
					Log.getChildren().addAll(DateText);
				});

				DBConnect();
				
				while (true) {
					// Listen for a new connection request
					Socket socket = serverSocket.accept();

					// Increment clientNo
					clientNo++;

					Platform.runLater( () -> {
						// Display the client number
						
						Text NewClientThread = new Text("Client " + clientNo + " connected!" + '\n');
						NewClientThread.setFill(Success);
						Log.getChildren().addAll(NewClientThread);

						// Find the client's host name, and IP address
						InetAddress inetAddress = socket.getInetAddress();

						Text ClientIpText = new Text("Client " + clientNo + "'s IP Address is "
								+ inetAddress.getHostAddress() + "\n");
						Log.getChildren().addAll(ClientIpText);
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
		DataInputStream inputFromClient;
		//ObjectOutputStream outputToClient;
		/** Construct a thread */
		public HandleAClient(Socket socket) {
			this.socket = socket;
		}

		/** Create a new thread to serve a single client */
		public void run() {
			// Create data input and output streams
			try {
				inputFromClient = new DataInputStream(socket.getInputStream());
				//outputToClient = new ObjectOutputStream(socket.getOutputStream());
				
				// Continuously serve the client
				while (shutdown == false) {
						// Receive message from the client
						ListenForClient(inputFromClient);
	
						// Format message to SQL statement
						SQLQuery = Util.toSQL(message, conn);
						
						HandleProtocol(Protocol, shutdown, inputFromClient);
						
						if(SQLQuery != null){
							// Grabs the unsafe SQL in plain text
							PlainTextSQL = Util.toSQL(message);
						
							// Run against DB
							queryServer(SQLQuery, PlainTextSQL);
						}
				}	
			}catch (SQLException sqlE){
				//Recoverable error
				Platform.runLater( () -> {
					Text sqlEError = new Text(sqlE.getMessage());
					sqlEError.setFill(Error);
					Log.getChildren().addAll(sqlEError);
				});
			}catch(NullPointerException | IOException ConnectionEx){
				//Recoverable error
				Platform.runLater( () -> {
					Text ConnectionExError = new Text(ConnectionEx.getMessage());
					ConnectionExError.setFill(Error);
					Log.getChildren().addAll(ConnectionExError);
				
					closeClientThread(shutdown);
				});
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
	public void ListenForClient(DataInputStream inputFromClient)throws NullPointerException{
		try {
			message = inputFromClient.readUTF();

			parts = message.split(":");
			
			//For readability
			Protocol = parts[0];
			MacAddress = parts[1];
			ItemSlot = parts[2];
			Platform.runLater( () -> {
				Text MessageReceivedText = new Text("Message received from " + Protocol + ":" + MacAddress +  ": " +ItemSlot + "\n");
				Log.getChildren().addAll(MessageReceivedText);
			});
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
			Platform.runLater( () -> {
				Text ConnectionSuccess = new Text("Successfully Connected to DataBase!" + "\n\n");
				ConnectionSuccess.setFill(Success);
				Log.getChildren().add(ConnectionSuccess);
			});
		}else{
			Platform.runLater( () -> {
				Text ConnectionError = new Text("ERROR: Connection to the db timed out. Please check your connection." + "\n");
				ConnectionError.setFill(Error);
				Log.getChildren().addAll(ConnectionError);
			});
		}
	}
	
	/**
	 * Requires a connection to a database.
	 * Runs the provided Query against the database that is currently connected.
	 * @param SQLQuery
	 */
	public void queryServer(PreparedStatement SQLQuery, String SQLClone){
		try {
			Platform.runLater( () -> {
				Text SQLAttempt = new Text("Attempting to run the following SQL against the connected DB... \n '" + SQLClone + "'\n");
				Log.getChildren().addAll(SQLAttempt);
			});
			//If it runs and there is a result
			if(SQLQuery.executeUpdate() > 0){
				Platform.runLater( () -> {
					Text SQLSuccess = new Text("Success!" + "\n\n");
					SQLSuccess.setFill(Success);
					Log.getChildren().addAll(SQLSuccess);
				});
			}else{
				Platform.runLater( () -> {
				//Runs but no rows affected
					Text SQLKindaSuccess = new Text("WARNING: The SQL ran succesfully but there were no rows affected!" + "\n\n");
					SQLKindaSuccess.setFill(Warning);
					Log.getChildren().addAll(SQLKindaSuccess);
				});
			}
			//Cannot have a negative quantity, EX Selling a product when there is zero in stock
		} catch (SQLException e) {
			Platform.runLater( () -> {
				Text SQLError = new Text("ERROR: Quantity cannot be negative!" + "\n\n");
				SQLError.setFill(Error);
				Log.getChildren().addAll(SQLError);
			});
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
		
		Platform.runLater( () -> {
			Text ClosingThread = new Text("Closing thread for this client..." + "\n");
			Log.getChildren().addAll(ClosingThread);
		});
		
		//Decrement the amount of clients connected
		clientNo--;
		
		Platform.runLater( () -> {
			Text ClientsLeft = new Text(clientNo + " client(s) connected" + "\n\n");
			Log.getChildren().addAll(ClientsLeft);
		});
	}
	
	/**
	 * Gives different messages depending on the different protocols. 2 returns whether or not the vending machine exists.
	 * 3 closes the clients thread and logs it to the server log.
	 * 
	 * @param Protocol The protocol code sent at the beginning of the message sent from a client
	 * @param shutdown Flag to close thread handling client
	 * @param inputFromClient InputStream from client
	 */
	private void HandleProtocol(String Protocol, boolean shutdown, DataInputStream inputFromClient){
		
		char charProto = Protocol.charAt(0);
		
		switch(charProto){
			case '2': // New vend machine protocol
				//Because SQLQuery will be null if the machine was found, see Util.toSQL
				if(SQLQuery != null){
					Platform.runLater( () -> {
						Text SQLWarning = new Text("WARNING: Fill the new Vending Machine!" + "\n\n");
						SQLWarning.setFill(Warning);
						Log.getChildren().addAll(SQLWarning);
					});
				}else{
					Platform.runLater( () -> {
						Text SQLSuccess = new Text("Found Vending Machine in Database!" + "\n\n");
						SQLSuccess.setFill(Success);
						Log.getChildren().addAll(SQLSuccess);
					});
				}
				break;
			case '3': // Client leaving protocol
				Platform.runLater( () -> {
					Text ConnectionClosed = new Text("Connection closed by the client" + "\n");
					Log.getChildren().addAll(ConnectionClosed);
				});
				closeClientThread(shutdown);
				break;
		}
		
			
	}
}