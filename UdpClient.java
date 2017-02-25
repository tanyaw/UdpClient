import java.net.Socket;
import java.io.OutputStream;
import java.io.InputStream;
import java.util.Random;

public final class UdpClient{ 
    public static void main(String[] args) throws Exception {
        // Create Socket connection 
        try (Socket socket = new Socket("codebank.xyz", 38005)) {

        	// Get address 
			System.out.println("Connected to server.");  
			String address = socket.getInetAddress().getHostAddress();
			String[] split = address.split("\\.");

			// Create input stream and input stream reader
	        InputStream is = socket.getInputStream();

			// Create output stream
			OutputStream os = socket.getOutputStream();

			// Initial size of packets
			int size = 2;
			int udpSize = 2;
			int udpLength = 0;

			//int udpPort = 0;
			long udpPort = 0;;

			// Needed to get randomized data for UDP source port field
   			Random ran = new Random();
   			byte[] sourcePort = new byte[2];

			for (int i = 0; i < 13; i++){
				byte[] udpHeader = new byte[8];
				byte[] udpData = new byte[udpSize];

				// Handshaking step when i = 0
				if (i == 0)
					size = 4;
 

				byte[] header = new byte[20];
				int length = size + 20;

				// Set header and version field
				header[0] = 0b01000101;

				// Set TOS
				header[1] = 0;

				// Set length
				header[3] = (byte)(length & 0xFF);  
				header[2] = (byte)((length >> 8) & 0xFF); 

				// Ident is 32-47
				header[4] = 0;
				header[5] = 0;

				// Flags is 48-50 and set as 010
				header[6] = 0b01000000;

				// Offset 51-63 is all 0
				header[7] = 0;

				// TTL of 50 from 64-71
				header[8] = 0b110010;

				// Protocol is UDP from 72 to 79
				header[9] = 0b00010001;

				// Checksum is 80-95
				header[10] = 0;
				header[11] = 0;

				// Source address
				header[12] = 0;
				header[13] = 0;
				header[14] = 0;
				header[15] = 0;

				// Destination IP address is server's IP address from 128-159
				for (int j = 0; j < 4; j++){
					header[j+16]  = (byte)Integer.parseInt(split[j]);
				}

				// Send to checksum function
				short checksum = checksum(header);

				header[11] = (byte)((int)checksum & 0xFF);  
				header[10] = (byte)((int)(checksum >> 8) & 0xFF); 

				// Implement data 
				byte[] data = new byte[size];

				if (i == 0){
					data[0] = (byte)0b11011110;
					data[1] = (byte)0b10101101;
					data[2] = (byte)0b10111110;
					data[3] = (byte)0b11101111;

					size = 1;
				}

				// Construct UDP packets for data
				else{
					//byte[] udpHeader = new byte[8];
					//byte[] udpData = new byte[size];
					ran.nextBytes(udpData);
					udpLength = udpSize + 8;
					byte[] pseudoheader = new byte[20];

					/* Source port already initialized to 0 */

					// Destination port obtained from handshake step
					udpHeader[2] = (byte)((udpPort >> 8) & 0xFF); 
					udpHeader[3] = (byte)(udpPort & 0xFF);  
					
					// Length (UDP header + UDP data)
					udpHeader[4] = (byte)((udpLength >> 8) & 0xFF); 
					udpHeader[5] = (byte)(udpLength & 0xFF);  
					System.out.println("udpheader[4] = " + udpHeader[4]);
					System.out.println("udpheader[5] = " + udpHeader[5]);

					// Checksum to be initialized later
					udpHeader[6] = 0;
					udpHeader[7] = 0;

					// Create IPv4 pseudoheader for checksum
					/* [0-3] Source address is already initialized to 0*/

					// [4]-[7] Destination Address
					pseudoheader[4] = header[16];
					pseudoheader[5] = header[17];
					pseudoheader[6] = header[18];
					pseudoheader[7] = header[19];

					/* [8] Already initialized to 0 */

					// Protocol (set to UDP)
					pseudoheader[9] = 0b00010001;

					// UDP length
					pseudoheader[10] = (byte)((udpLength >> 8) & 0xFF); 
					pseudoheader[11] = (byte)(udpLength & 0xFF);  

					/* Source port already initialized to 0 */

					// Destination port
					pseudoheader[13] = udpHeader[2];
	
					// Length
					pseudoheader[14] = udpHeader[4];

					// Checksum for UDP packet and pseudoheader 
					short udpChecksum = udpChecksum(udpHeader, pseudoheader, udpData);
					udpHeader[6] = (byte)((int)(udpChecksum >> 8) & 0xFF); 
					udpHeader[7] = (byte)((int)udpChecksum & 0xFF); 

					pseudoheader[15] = (byte)((int)(udpChecksum >> 8) & 0xFF);

					// Pseudo header [16] and after is UDP header + UDP data
					/*for (int j = 0; j < 8; j++){
						pseudoheader[16 + j] = udpHeader[j]; 
					}

					for (int j = 0; j < udpLength; j++){
						pseudoheader[24+j] = udpData[j];
					}*/

					// Do I compute udpchecksum here??
					udpSize *= 2;
				}

				// Send packets to the server with each packet number
				os.write(header);

				if (i == 0){
					os.write(data);
					System.out.print("Handshake response: 0x");
				}
				else{
					os.write(udpHeader);
					os.write(udpData);
					System.out.println("udp data size = " + udpData.length);
					System.out.println("Sending packet with " + size + " bytes of data");
					System.out.print("Response: 0x");
				}

				// Get server reply
				for (int j = 0; j < 4; j++)
					System.out.printf("%02X", is.read());
	
				System.out.println("\n");
				
				int b1;
				int b2;


				// Get port number
				if (i == 0){
					b1 = is.read();

	        		// Read from the input stream a second time
	        		b2 = is.read();

	      			udpPort += (long)(((b1 << 8) & 0xFF00) | (b2 & 0xFF));  
	    
	        		// Concatenate 
	        		System.out.println("Port number received: " + udpPort);
					//String s2 = String.format("%8s", Integer.toBinaryString(portNum[1] & 0xFF)).replace(' ', '0');
					//System.out.println(s2);

				}

				/*//Print binary output of IPv4 pakcet headers
				System.out.println("0        8        16       24");
				int counter=1;
				for(byte b: udpHeader) {
				    System.out.print(Integer.toBinaryString(b & 255 | 256).substring(1) + " ");
				    if(counter%4 ==0) {
				    	System.out.println();
				    }
				    counter++;
				}*/


				// Change size of data array
				size *= 2;
			}
        }							
    }

  	public static short checksum(byte[] b){
    	// The 32 bit number sum
    	long sum = 0;

	    //Go through the byte array two bytes at a time
	    for (int i = 0; i < b.length-1; i+=2){
	      // Form a 16-bit number out of the two bytes and add to the sum
	      sum += (long)(((b[i] << 8) & 0xFF00) | (b[i+1] & 0xFF));  
	    
	    // Check for overflow     
	      if ((sum & 0xFFFF0000) > 0){    
	        // Overflow occured so clear it and add it back to the sum
	        sum &= 0xFFFF;
	        sum++;
	      }
	    }

	    // If the length is odd
	    if (b.length % 2 != 0){
	      sum += (long)(((b[b.length-1] << 8) & 0xFF00));  
	      
	      // Check for overflow     
	      if ((sum & 0xFFFF0000) > 0){    
	        // Overflow occured so clear it and add it back to the sum
	        sum &= 0xFFFF;
	        sum++;
	      }
	    }
	    // Perform ones' complement and return the rightmost 16 bits of the sum
	    return (short)~(sum & 0xFFFF);
  	}

   	public static short udpChecksum(byte[] pseudoheader, byte[] udpHeader, byte[] data){
    	// The 16 bit number sum
    	int sum = 0;

    	sum = sum(pseudoheader) + sum(udpHeader) + sum(data);
	    
	    // Perform ones' complement and return the rightmost 16 bits of the sum
	    return (short)~(sum & 0xFFFF);
  	} 	

  	public static int sum(byte[] b){
  		int sum = 0;

  		for (int i = 0; i < b.length-1; i+=2){
	     // Form a 16-bit number out of the two bytes and add to the sum
	     sum += (long)(((b[i] << 8) & 0xFF00) | (b[i+1] & 0xFF));  
	    
	    // Check for overflow     
	     if ((sum & 0xFFFF0000) > 0){    
	        // Overflow occured so clear it and add it back to the sum
	        sum &= 0xFFFF;
	        sum++;
	      }
	    }

	    // If the length is odd
	    if (b.length % 2 != 0){
	      sum += (long)(((b[b.length-1] << 8) & 0xFF00));  
	      
	      // Check for overflow     
	     if ((sum & 0xFFFF0000) > 0){    
	     	// Overflow occured so clear it and add it back to the sum
	        sum &= 0xFFFF;
	        sum++;
	      }
	    }

	    return sum;
  	}
}