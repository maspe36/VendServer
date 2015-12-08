package server;

import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseInterface {
	public Connection Connect(String serverName, String user, String password) {
		Connection con = null;
		// Load Microsoft JDBC Driver 1.0
		System.out.println("Callng Class.forName()...");
		try {
			//Driver d = (Driver)Class.forName("com.microsoft.jdbc.sqlserver.SQLServerDriver").newInstance();
   			Driver d = (Driver)Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver").newInstance();

		} catch (java.lang.ClassNotFoundException e) {
			System.err.println("ClassNotFoundException: " + e.getMessage());
		} catch (InstantiationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		//Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
		System.out.println("Class.forName() completed");
		try {
			// Obtaining a connection to SQL Server
			con = DriverManager.getConnection("jdbc:sqlserver://"+serverName + ";"
			+ "user=" + user + ";password=" + password );
//			con = DriverManager.getConnection("jdbc:sqlserver://Win2008Server;"
//					+ "user=JavaDude;password=JavaDude01");
			System.out.println("DriverManager.getConnection() completed");
		}
		catch (SQLException sqle) {
			System.err.println("SQLException: " + sqle.getMessage());
			con = null;
		} catch(Exception ex){
			System.err.println("Exception: " + ex.getMessage());
			con = null;
		} 
		return con;
	}
}
