import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketAddress;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;

public class TftpClient {

	public static void main(String[] args) {
	
		try 
		{
			//check the correct args were entered
		    if (args.length != 3) 
		    {
		         System.out.println("Usage: <hostname or ip> <port> <filename>");
		         return;
		    }	 
		    //parse args
		    String hostname = args[0];
		    int port = Integer.parseInt(args[1]);
		    String filename = args[2];
		    
		    //create inet address and datagram socket
		    InetAddress address = InetAddress.getByName(args[0]);
		    DatagramSocket socket = new DatagramSocket();
		    
		    //pack and send rrq
			byte[] buf = filename.getBytes();	
			DatagramPacket packetRRQ = packRRQ(buf, buf.length, hostname, port);
		    System.out.println("sending packet");	    
		    socket.send(packetRRQ);  
		    
		    String recivedData = "";
			byte ackNum = 0;
			byte currentBlock = 1;
			byte lastBlock = 0;
			
			//create file and file outputstream
			FileOutputStream fos = new FileOutputStream(filename);    
		    File file = new File(filename);
			try 
			{
				//loops till end of file 
				while(true)
				{
					byte[] empty = new byte[600];
					
				 	//blocks untill a packet is recived
				 	DatagramPacket recivedPacket = new DatagramPacket(empty, empty.length);			 
				 	socket.receive(recivedPacket);
				 	
				 	//sets the acknum to the current packet
				 	ackNum = TftpUtility.extractBlockSeq(recivedPacket);
				 	//checks that is was a data packet if it wasent break and print error  
					if(TftpUtility.checkPacketType(recivedPacket) != TftpUtility.DATA)
					{
						System.out.println("FILE ERROR");
						file.delete();
						break;
					}
					 	
					//if the packet is the correct numbered packet
					if(ackNum == currentBlock)
					{
						lastBlock = ackNum;
						currentBlock++;
		 		
					 
						System.out.println(recivedPacket.getPort());		
					//pack an ack and send it to the server
		 		    System.out.println("sending ack num " + ackNum);
			 		DatagramPacket packet2 = packACK(ackNum, 2, hostname, recivedPacket.getPort());   
						socket.send(packet2); 
						//write to the file
						fos.write(recivedPacket.getData(), 2, recivedPacket.getLength()-2);
						//check it is a full packet if it isant handle end of file
				 		if(recivedPacket.getLength() < TftpUtility.PACKET_BUFFER_SIZE)
				 		{
							System.out.println("short packet");
				 			break;
				 		}	
					}
					else
					{
						//acks the laste correctly recived block
			 		    System.out.println("packet out of order. sending ack num " + lastBlock); 
					 	DatagramPacket packet2 = packACK(lastBlock, 2, hostname, recivedPacket.getPort());
						socket.send(packet2);
					} 	
				}
				socket.close(); 
			} 			
			catch (IOException e) 
			{
				System.err.println("Exception: " + e);
			}			
		}
		catch(Exception e) 
		{
			System.err.println("Exception: " + e);
		}
	}
	
	//packs a read request for the specified file and host
	private static DatagramPacket packRRQ(byte[] fileName, int length, String hostname, int port){	
		int packetLength = 1 + length;//type (1) + block seq (1) + data length
		ByteBuffer byteBuffer = ByteBuffer.allocate(packetLength); 
		byteBuffer.put(TftpUtility.RRQ);//type
		byteBuffer.put(fileName);//data	 
		DatagramPacket dataPacket = new DatagramPacket(byteBuffer.array(), packetLength);
		SocketAddress serverAddress = new InetSocketAddress(hostname, port);
		dataPacket.setSocketAddress(serverAddress);
		return  dataPacket;
	}

	//packs an ack for the specified block
	private static DatagramPacket packACK(byte ackNum, int length, String hostname, int port){
		int packetLength = 1 + length;//type (1) + block seq (1) + data length
		ByteBuffer byteBuffer = ByteBuffer.allocate(packetLength); 
		byteBuffer.put(TftpUtility.ACK);//type
		byteBuffer.put(ackNum);//data	 
		DatagramPacket dataPacket = new DatagramPacket(byteBuffer.array(), packetLength);
		SocketAddress serverAddress = new InetSocketAddress(hostname, port);
		dataPacket.setSocketAddress(serverAddress);
		return  dataPacket;
	}
}














