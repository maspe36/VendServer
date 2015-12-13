package util;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.sql.Connection;
import java.sql.PreparedStatement;
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
		
		String MacAddress = parts[0];
		String ItemSlot = parts[1];
		
		PreparedStatement pstmt = null;
		
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
		try {
			for (Enumeration<NetworkInterface> e = NetworkInterface.getNetworkInterfaces(); e.hasMoreElements();) {
				NetworkInterface n = e.nextElement();
			    //System.out.println(n.getDisplayName());
			    byte[] mac = n.getHardwareAddress();
			    // Convert bytes to printable hex digits
			    if (mac != null) {
			    	Enumeration<InetAddress> myInetAddress = n.getInetAddresses();
			    	// Is there an IP address associated with this network interface?
			    	if (myInetAddress.hasMoreElements()) {
				    	//System.out.println(myInetAddress.nextElement().toString());
				    	StringBuilder sb = new StringBuilder();
				    	for(int i = 0; i < mac.length; i++) {
				    		sb.append(String.format("%02X%s", mac[i], (i < mac.length - 1) ? "-" : ""));
				    	}
				    	if(sb.length() == 17){
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
