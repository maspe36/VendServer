package client;

import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.Socket;
import java.util.Enumeration;
import java.util.Scanner;

import util.Util;


public class VendingMachine{

	OutputStream toServer;

	String chat;
	
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
			Socket connectToServer = new Socket(getServerIp(), 8000);

			toServer = connectToServer.getOutputStream();
			toServer.flush();
			//isFromServer = new ObjectInputStream(connectToServer.getInputStream());
			System.out.println("Connected!");
			startUpMessage();
		}
		catch(IOException ex){ 
			System.err.println("Connection refused!");
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

								writeToServer(message, toServer);
								toServer.flush(); // clear path
								System.exit(0); //close program if user typed bye
							}
							
							//grab message to send and physical address of the client
							String message =(Protocol + ":" + MacAddress + ":" + ItemSold);

							writeToServer(message, toServer);
							toServer.flush(); // clear path
						}else{
							//TO-DO throw an error to server saying the entered ItemSold was null and it cannot be null
							ItemSold = "empty";
						}
					}
					catch(IOException ex){
						System.err.println(ex.getMessage());
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
			writeToServer(Protocol + ":" + MacAddress + ":" + ItemSold, toServer);
			toServer.flush();
			Protocol = "1";
		}catch (IOException e) {
			System.err.println(e.getMessage());
		}
	}
	
	/**
	 * 
	 * @param msg
	 * @param toServer
	 */
	public void writeToServer(String msg, OutputStream toServer){
		try {
			byte[] toSendBytes = msg.getBytes();
			int toSendLen = toSendBytes.length;
			byte[] toSendLenBytes = new byte[4];
			toSendLenBytes[0] = (byte)(toSendLen & 0xff);
			toSendLenBytes[1] = (byte)((toSendLen >> 8) & 0xff);
			toSendLenBytes[2] = (byte)((toSendLen >> 16) & 0xff);
			toSendLenBytes[3] = (byte)((toSendLen >> 24) & 0xff);
			toServer.write(toSendLenBytes);
			toServer.write(toSendBytes);
		} catch (IOException e) {
			System.err.println(e.getMessage());
		}
	}
	
	/**
	 * Sends a broadcast message on port 8888 and looks for
	 * DISCOVER_FUIFSERVER_RESPONSE.
	 * @return InetAddress
	 */
	public InetAddress getServerIp(){
		
		InetAddress IP = null;
		
		// Find the server using UDP broadcast
		try {
			//Open a random port to send the package
			DatagramSocket c = new DatagramSocket();
			c.setBroadcast(true);

			byte[] sendData = "DISCOVER_FUIFSERVER_REQUEST".getBytes();

			//Try the 255.255.255.255 first
			try {
				DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, InetAddress.getByName("255.255.255.255"), 8888);
				c.send(sendPacket);
				System.out.println(getClass().getName() + ">>> Request packet sent to: 255.255.255.255 (DEFAULT)");
			} catch (Exception e) {
				System.err.println(getClass().getName() + ">>> Request packet not sent!");
			}

			// Broadcast the message over all the network interfaces
			Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
			while (interfaces.hasMoreElements()) {
				NetworkInterface networkInterface = interfaces.nextElement();

				if (networkInterface.isLoopback() || !networkInterface.isUp()) {
					continue; // Don't want to broadcast to the loopback interface
				}

				for (InterfaceAddress interfaceAddress : networkInterface.getInterfaceAddresses()) {
					InetAddress broadcast = interfaceAddress.getBroadcast();
					if (broadcast == null) {
						continue;
					}

					// Send the broadcast package!
					try {
						DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, broadcast, 8888);
						c.send(sendPacket);
					} catch (Exception e) {
						System.err.println(e.getMessage());
					}

					System.out.println(getClass().getName() + ">>> Request packet sent to: " + broadcast.getHostAddress() + "; Interface: " + networkInterface.getDisplayName());
				}
			}

			System.out.println(getClass().getName() + ">>> Done looping over all network interfaces. Now waiting for a reply!");

			//Wait for a response
			byte[] recvBuf = new byte[15000];
			DatagramPacket receivePacket = new DatagramPacket(recvBuf, recvBuf.length);
			c.receive(receivePacket);

			//We have a response
			System.out.println(getClass().getName() + ">>> Broadcast response from server: " + receivePacket.getAddress().getHostAddress());

			//Check if the message is correct
			String message = new String(receivePacket.getData()).trim();
			if (message.equals("DISCOVER_FUIFSERVER_RESPONSE")) {
				//DO SOMETHING WITH THE SERVER'S IP (for example, store it in your controller)
				IP = (receivePacket.getAddress());
			}

			//Close the port!
			c.close();
		} catch (IOException ex) {
			System.err.println(ex.getMessage());
		}
		return IP;
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
	
