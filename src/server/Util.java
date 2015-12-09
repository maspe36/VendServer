package server;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;

public class Util {
	/*
	 * SUSCEPTIBLE TO A SQL INJECTION
	 */
	
	// TO-DO change to a prepared statement
	public static String toSQL(String msg){
		//Current format is XX:MM-MM-MM-MM-MM-MM 
		//X = ItemSlot(A1, A2...), and M = MacAddress
		String[] parts = msg.split(":");
		String sellSqlStatement = "INSERT INTO tVendLog (MacAddress, ItemLocation) "
								+ "VALUES('" + parts[0] + "', '" + parts[1] + "')";
		return sellSqlStatement;
	}
	
	public static String getMacAddress(){
		InetAddress ip;
		
		try {
			ip = InetAddress.getLocalHost();
			System.out.println("Current IP address : " + ip.getHostAddress());
			
			NetworkInterface network = NetworkInterface.getByInetAddress(ip);
			byte[] mac = network.getHardwareAddress();	
			
			System.out.print("Current MAC address : ");			
			StringBuilder sb = new StringBuilder();
			
			for (int i = 0; i < mac.length; i++) {
				sb.append(String.format("%02X%s", mac[i], (i < mac.length - 1) ? "-" : ""));		
			}
			System.out.println(sb.toString());
			
			return sb.toString();
			
		} catch (UnknownHostException e) {	
			e.printStackTrace();	
		} catch (SocketException e){		
			e.printStackTrace();
		}
		
		return null;
		
	}
}
