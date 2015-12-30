package server;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class DiscoveryThread implements Runnable {

	DatagramSocket socket;
	String DiscoverRequest 	= "DISCOVER_FUIFSERVER_REQUEST";
	String DiscoverResponse = "DISCOVER_FUIFSERVER_RESPONSE";
	
	@Override
	public void run() {
		try{
			//Keep a socket open to listen to all the UDP trafic that is destined for this port
			socket = new DatagramSocket(8888, InetAddress.getByName("0.0.0.0"));
			socket.setBroadcast(true);
			
			while(true){
				System.out.println(getClass().getName() + ">>>ready to receive broadcast packets!");
				
				//Receive a packet
				byte[] recvBuf = new byte[15000];
				DatagramPacket packet = new DatagramPacket(recvBuf, recvBuf.length);
				socket.receive(packet);
				
				//Packet received
				System.out.println(getClass().getName() + ">>>Discovery packet received from: " + 
				packet.getAddress().getHostAddress());
				
				System.out.println(getClass().getName() + ">>>Packet received; data: " + 
				new String(packet.getData()));
				
				//See if the packet holds the right command (message)
				String message = new String(packet.getData()).trim();
				if(message.equals(DiscoverRequest)){
					byte[] sendData = DiscoverResponse.getBytes();
					
					//Send a response
					DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length,
					packet.getAddress(), packet.getPort());
					
					socket.send(sendPacket);
					
					System.out.println(getClass().getName() + ">>>Sent packet to: " + 
					sendPacket.getAddress().getHostAddress());
					
					System.out.println(getClass().getName() + ">>>Packet sent: data; " + 
					new String(sendPacket.getData(), 0, sendPacket.getLength()));
				}
			}
		}catch(IOException ex){
			System.out.println(ex.getMessage());
		}
	}
	
	public static DiscoveryThread getInstance() {
		return DiscoveryThreadHolder.INSTANCE;
	}

	private static class DiscoveryThreadHolder {
		private static final DiscoveryThread INSTANCE = new DiscoveryThread();
	}
}
