package client;

import java.io.IOException;
import java.io.*;
import java.net.Socket;
import java.util.Scanner;

import util.Util;


public class VendingMachine{

	ObjectOutputStream toServer;
	//ObjectInputStream isFromServer;

	String chat;
	static String ip = "localhost";
	
	//Current format is P:MM-MM-MM-MM-MM-MM:XX 
	//P = Protocol, X = ItemSlot(A1, A2...), and M = MacAddress
	
	String Protocol = "2";
	String MacAddress = Util.getMacAddress();
	String ItemSold = "StartUpMessage"; //Needs to be initialized for the startup message
	
	Scanner sys = new Scanner(System.in);

	//Instantiate and start the client
	public static void main(String[] args){
		VendingMachine client = new VendingMachine();
		client.initialize();
	}
	
	/**
	 * Open the connection to the server at localhost on port 8000
	 */
	public void initialize() {
		try{
			//create a socket to connect to server
			@SuppressWarnings("resource")
			Socket connectToServer = new Socket(ip, 8000);

			toServer = new ObjectOutputStream(connectToServer.getOutputStream());
			toServer.flush();
			//isFromServer = new ObjectInputStream(connectToServer.getInputStream());
			System.out.println("Connected!");
			startUpMessage();
		}
		catch(IOException ex){ 
			System.out.println("Connection refused!");
		}
		//constantly be ready to send messages
		writeToServer();
		
		/*Removing ability to receive messages for now as Vending Machines don't need to do this.
		//constantly search for messages
		HandleAMessage();
		*/
	}

	/**
	 * Writes to the Server in plain text on nextLine(). Sends with the format of MacAddress:ItemSlot
	 */
	protected void writeToServer(){
		Thread writingMessage = new Thread() {
			public void run(){
				while(true){
					try{
						//Grab the Vending Machine input
						ItemSold = sys.nextLine();
						
						ItemSold = ItemSold.toUpperCase();
						
						//If ItemSold is not equal
						if(!ItemSold.equals("")){
							//Change the protocol depending on what the user enters
							if(SetProtocol()){
								//grab message to send and physical address of the client
								String message =(Protocol + ":" + MacAddress + ":" + ItemSold);

								toServer.writeUTF(message);
								toServer.flush(); // clear path
								System.exit(0); //close program if user typed bye
							}
							
							//grab message to send and physical address of the client
							String message =(Protocol + ":" + MacAddress + ":" + ItemSold);

							toServer.writeUTF(message);
							toServer.flush(); // clear path
						}else{
							//TO-DO throw an error to server saying the entered ItemSold was null and it cannot be null
							ItemSold = "empty";
						}
					}
					catch(IOException ex){
						System.err.println(ex);
					}
				}
			}
		};
		writingMessage.start();
	}
	
	/**
	 * 
	 * Changes the protocol depending on what the client has input.
	 * 
	 * @return True if protocol has been changed from default
	 */
	
	private boolean SetProtocol(){
		if(ItemSold.equals("BYE")){
			Protocol = "3";
			return true;
		}
		return false;
	}
	
	/**
	 * Sends a message with the startup protocol (2 at the moment) as soon as 
	 * the Vending Machine has found and connected to the VendServer.
	 */
	
	private void startUpMessage(){
		try {
			toServer.writeUTF(Protocol + ":" + MacAddress + ":" + ItemSold);
			toServer.flush();
			Protocol = "1";
		}catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/* Removing the ability to receive messages for the time being as Vending Machines don't need it.
	protected void HandleAMessage(){
		Thread messageHandling = new Thread() {
			public void run(){
				while(true){
					try {
						chat = (String) isFromServer.readUTF();
						System.out.println(chat);	
					}
					catch (IOException ex) {
						System.err.println(ex);
					}
				}
			}
		};
		messageHandling.start();
	}
	*/
}
	
