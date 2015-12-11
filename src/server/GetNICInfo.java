// Example codes to read network adapters and see if one has an IP address assigned to it.
// Bill Nicholson
// nicholdw@ucmail.uc.edu

package nicholdw;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;

public class GetNICInfo{

	public static void main(String[] args) {
		
		try {
			Enumeration<NetworkInterface> foo = NetworkInterface.getNetworkInterfaces();
			
			for (Enumeration<NetworkInterface> e = NetworkInterface.getNetworkInterfaces(); e.hasMoreElements();) {
				NetworkInterface n = e.nextElement();
			    System.out.println(n.getDisplayName());
			    byte[] mac = n.getHardwareAddress();
			    // Convert bytes to printable hex digits
			    if (mac != null) {
			    	Enumeration<InetAddress> myInetAddress = n.getInetAddresses();
			    	// Is there an IP address associated with this network interface?
			    	if (myInetAddress.hasMoreElements()) {
				    	System.out.println(myInetAddress.nextElement().toString());
				    	for(int i = 0; i < mac.length; i++) {
				    		// If we get here, we have a network interface that has a MAC addr AND has an IP address
				    		System.out.print(String.format("%02X ", mac[i]));
				    	}
				    }
			    }
			    //System.out.println(n.getHardwareAddress());
			}
			System.out.println("hi");
			
			
			//for (NetworkInterface x : foo) {
				
			//}
			
		} catch (SocketException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
		

	}

}
