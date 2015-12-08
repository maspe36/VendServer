package client;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.Scanner;

import server.Util;


public class VendingMachine{

	ObjectOutputStream toServer;
	ObjectInputStream isFromServer;

	String chat;
	static String ip = "localhost";

	String MacAddress = Util.getMacAddress();
	
	Scanner sys = new Scanner(System.in);

	//Instantiate and start the client
	public static void main(String[] args){
		VendingMachine client = new VendingMachine();
		client.initialize();
	}
	
	public void initialize() {
		try{
			//create a socket to connect to server
			@SuppressWarnings("resource")
			Socket connectToServer = new Socket(ip, 8000);

			toServer = new ObjectOutputStream(connectToServer.getOutputStream());
			toServer.flush();
			isFromServer = new ObjectInputStream(connectToServer.getInputStream());
			System.out.println("Connected!");
		}
		catch(IOException ex){ 
			System.out.println("Connection refused!");
		}
		//constantly be ready to send messages
		writeToServer();
		//constantly search for messages
		HandleAMessage();
	}

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
}
	
