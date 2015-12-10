package client;

import java.io.IOException;
import java.io.*;
import java.net.Socket;
import java.util.Scanner;

import server.Util;


public class VendingMachine{

	ObjectOutputStream toServer;
	//ObjectInputStream isFromServer;

	String chat;
	static String ip = "localhost";
	String MacAddress = Util.getMacAddress();
	
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
						//grab message to send and physical address of the client
						String message =(MacAddress + ":" + sys.nextLine());

						toServer.writeUTF(message);
						toServer.flush(); // clear path
					}
					catch(IOException ex){
						System.err.println(ex);
					}
				}
			}
		};
		writingMessage.start();
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
	
