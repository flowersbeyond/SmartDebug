package cn.edu.thu.tsmart.tool.da.instr;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;

public class Messenger {
	private static int port;
	private static Socket socket;
	private static DataOutputStream out;
	public static void init(int portNum){
		try {
			port = portNum;
			socket = new Socket("localhost", port);
			out = new DataOutputStream(socket.getOutputStream());
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	public static void sendMessage(String str){
		try {
			if(!socket.isClosed()){
				out.writeUTF(str);
				out.flush();
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public static void closeSocket(){
		try {
			out.flush();
			Thread.sleep(500);
			socket.close();
			System.out.println("Socket closed");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
