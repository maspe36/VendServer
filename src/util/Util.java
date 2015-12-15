package util;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Enumeration;

public class Util {
	/**
	 * Takes in a string and a connection to create a PreparedStatement
	 * 
	 * @param msg Message from client to be formatted
	 * @param conn Conenction to the DB
	 * @return PreparedStatement of the SQL to be run
	 */
	public static PreparedStatement toSQL(String msg, Connection conn)throws SQLException{
		//Current format is MM-MM-MM-MM-MM-MM:XX 
		//X = ItemSlot(A1, A2...), and M = MacAddress
		String[] parts = msg.split(":");
		
		//Convert the protocl to a char so we can change what commands are run
		char Protocol = parts[0].toCharArray()[0];
		String MacAddress = parts[1];
		String ItemSlot = parts[2];
		
		PreparedStatement pstmt = null;
		
		//Change the PreparedStatement to the correct SQL. 
		//If it was 3 it was closed in the VendServer HandleAClient Run while loop.
		switch(Protocol){
			case '1':
				try {
					pstmt = conn.prepareStatement
						   ("UPDATE tVendingMachine_Product " +
							"SET Quantity = (Quantity - 1) " +
							"FROM dbo.tVendingMachine " + 
							"INNER JOIN dbo.tVendingMachine_Product " +
							"ON dbo.tVendingMachine.VendingMachineID = dbo.tVendingMachine_Product.VendingMachineID " +
							"WHERE MacAddress = ? AND ItemSlot = ?;");
					
					pstmt.setString(1, MacAddress);
					pstmt.setString(2, ItemSlot);
					
				} catch (SQLException e) {
					throw new SQLException("ERROR: Could not run the ItemSale SQL Statement!");
				}
				break;
			case '2':
				try {
					
					int LocationID = 1;
					
					PreparedStatement checkExistingMachine = conn.prepareStatement
						   ("SELECT MacAddress " +
							"FROM dbo.tVendingMachine " +
						    "WHERE (MacAddress = ?)");
					
					checkExistingMachine.setString(1, MacAddress);
					
					ResultSet rs = checkExistingMachine.executeQuery();
					
					if(!rs.next()){
						pstmt = conn.prepareStatement
							   ("INSERT INTO tVendingMachine (MacAddress, LocationID) VALUES (?, ?)");
						
						pstmt.setString(1, MacAddress);
						pstmt.setInt(2, LocationID);
					}
				} catch (SQLException e) {
					System.out.println(e.getMessage());
					throw new SQLException("ERROR: Could not run the NewVendingMachine SQL Statement!");
				}
				break;
		}
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
		
		//Current format is P:MM-MM-MM-MM-MM-MM:XX 
		//P = Protocol, X = ItemSlot(A1, A2...), and M = MacAddress
		char Protocol = parts[0].toCharArray()[0];
		String MacAddress = parts[1];
		String ItemSlot = parts[2];
		
		int LocationID = 1;
		String SQLQuery = "";
		
		switch(Protocol){
			case '1': //ItemSold case
				SQLQuery = 
				"UPDATE tVendingMachine_Product " +
				"SET Quantity = (Quantity - 1) " +
				"FROM dbo.tVendingMachine " + 
				"INNER JOIN dbo.tVendingMachine_Product " +
				"ON dbo.tVendingMachine.VendingMachineID = dbo.tVendingMachine_Product.VendingMachineID " +
				"WHERE MacAddress = '" + MacAddress + "' AND ItemSlot = '" + ItemSlot + "';";
				break;
			case '2': //New VendingMachine case
				SQLQuery = 
				"INSERT INTO tVendingMachine (MacAddress, LocationID) " + 
				"VALUES ('" + MacAddress + "', " + LocationID + " )";
				break;
		}
		
		
		
		return SQLQuery;
	}
	
	/**
	 * Finds the first MacAddress that is not empty, and has an IP address. 
	 * Assumption is that the first connected MacAddress that isn't empty and has an IP
	 * is the MacAddress currently being used in the connection.
	 * 
	 * Assumptions are dangerous.
	 * 
	 * @return MacAddress
	 * @author Bill Nicholson
	 */
	public static String getMacAddress(){
		
		//No magic numbers young padawan privett
		int LengthOfMacAddress = 17;
		
		try {
			for (Enumeration<NetworkInterface> e = NetworkInterface.getNetworkInterfaces(); e.hasMoreElements();) {
				NetworkInterface n = e.nextElement();
			    byte[] mac = n.getHardwareAddress();
			    // Convert bytes to printable hex digits
			    if (mac != null) {
			    	Enumeration<InetAddress> myInetAddress = n.getInetAddresses();
			    	// Is there an IP address associated with this network interface?
			    	if (myInetAddress.hasMoreElements()) {
				    	StringBuilder sb = new StringBuilder();
				    	//Concat byte array with the following format mm-mm-mm-mm-mm-mm
				    	for(int i = 0; i < mac.length; i++) {
				    		sb.append(String.format("%02X%s", mac[i], (i < mac.length - 1) ? "-" : ""));
				    	}
				    	// MacAddress that appear first and aren't used appear to have more chars? 
				    	// Extra byte bad guys									 00-00-00-00-00-00-0E
				    	// 																  VS
				    	// The good guys										 00-00-00-00-00-00
				    	if(sb.length() == LengthOfMacAddress){
				    		return sb.toString();
				    	}
				    }
			    }
			}
		} catch (SocketException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}
}
