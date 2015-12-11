package server;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.sql.Connection;
import java.sql.PreparedStatement;

public class Util {
	/**
	 * Takes in a string and a connection to create a PreparedStatement
	 * 
	 * @param msg Message from client to be formatted
	 * @param conn Conenction to the DB
	 * @return PreparedStatement of the SQL to be run
	 */
	public static PreparedStatement toSQL(String msg, Connection conn) throws Exception{
		//Current format is MM-MM-MM-MM-MM-MM:XX 
		//X = ItemSlot(A1, A2...), and M = MacAddress
		String[] parts = msg.split(":");
		
		String MacAddress = parts[0];
		String ItemSlot = parts[1];
		
		PreparedStatement pstmt = null;
		
		pstmt = conn.prepareStatement
			   ("UPDATE tVendingMachine_Product " +
				"SET Quantity = (Quantity - 1) " +
				"FROM dbo.tVendingMachine " + 
				"INNER JOIN dbo.tVendingMachine_Product " +
				"ON dbo.tVendingMachine.VendingMachineID = dbo.tVendingMachine_Product.VendingMachineID " +
				"WHERE MacAddress = ? AND ItemSlot = ?;");
			
		pstmt.setString(1, MacAddress);
		pstmt.setString(2, ItemSlot);
		
		return pstmt;
	}
	
	/**
	 * 
	 * Takes a String and returns a String formatted to a SQL statement. 
	 * 
	 * WARNING:
	 * Do not run the returned string against the DB, 
	 * this format is susceptible to a SQL Injection attack.
	 * 
	 * @param msg Message from client to be formatted
	 * @return SQL in a string format
	 */
	public static String toSQL(String msg){
		String[] parts = msg.split(":");
		
		String MacAddress = parts[0];
		String ItemSlot = parts[1];
		
		String SQLQuery = 
				"UPDATE tVendingMachine_Product " +
				"SET Quantity = (Quantity - 1) " +
				"FROM dbo.tVendingMachine " + 
				"INNER JOIN dbo.tVendingMachine_Product " +
				"ON dbo.tVendingMachine.VendingMachineID = dbo.tVendingMachine_Product.VendingMachineID " +
				"WHERE MacAddress = \"" + MacAddress + "\" AND ItemSlot = \"" + ItemSlot + "\";";
		
		return SQLQuery;
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
