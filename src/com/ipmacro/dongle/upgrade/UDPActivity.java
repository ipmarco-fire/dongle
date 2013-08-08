package com.ipmacro.dongle.upgrade;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;


import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

public class UDPActivity extends Activity{
	private static final int MSG_START_SERVER_FALI = 1;
	public static final String LOCALHOST = "10.1.0.1";
	public static final int PORT = 10101;
	
	boolean hasStart = false;
	Context mContext;
	Handler mhandler = new Handler(){
		@Override
		public void handleMessage(Message msg) {
			switch(msg.what){
			case MSG_START_SERVER_FALI:
				int pv = 11;int sv = 12;String path = "xxx";
				String msg2 = "{Cmd:1,Version:"+sv+",ProductID:12,Url:'"+path+"'}";
				Toast.makeText(mContext, "Send:"+msg2,3000).show();
				break;
			}
		}
	};
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.udp);
		
		mContext = this;
	}
	
	public void start(View v){
		if(hasStart){
			return ;
		}
		Thread thread = new Thread() {
			public void run() {
				try {
					Integer port = PORT;  
			        // 接收的字节大小，客户端发送的数据不能超过这个大小  
			        byte[] message = new byte[1024];  
			        try {  
			            // 建立Socket连接  
			            DatagramSocket datagramSocket = new DatagramSocket(port);  
			            DatagramPacket datagramPacket = new DatagramPacket(message,  
			                    message.length);  
			            try {  
			                while (hasStart) {  
			                    // 准备接收数据  
			                    datagramSocket.receive(datagramPacket);  
			                    Log.d("UDP Demo", datagramPacket.getAddress()  
			                            .getHostAddress().toString()  
			                            + ":" + new String(datagramPacket.getData()));  
			                    
			                    
			                }  
			            } catch (IOException e) {  
			                e.printStackTrace();  
			            }  
			        } catch (SocketException e) {  
			            e.printStackTrace();  
			        } 
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		};
		thread.start();
		hasStart = true;
		Toast.makeText(this, "服务器开启", Toast.LENGTH_LONG).show();
	}
	
	public void send(View v){
		Thread thread = new Thread() {
			public void run() {
				
				int pv = 11;int sv = 12;String path = "xxx";
				
				String msg = "{Cmd:1,Version:"+sv+",ProductID:12,Url:'"+path+"'}";
				Log.w("startServer",msg);
				mhandler.sendEmptyMessage(MSG_START_SERVER_FALI);
				send(msg);
				
			}
		};
		thread.start();
	}
	
	@Override
	protected void onPause() {
		super.onPause();
		hasStart = false;
	}
	
	public void send(String message) {  
        message = (message == null ? "Hello IdeasAndroid!" : message);  
        int server_port = PORT;  
        DatagramSocket s = null;  
        try {  
            s = new DatagramSocket();  
        } catch (SocketException e) {  
            e.printStackTrace();  
        }  
        InetAddress local = null;  
        try {  
            // 换成服务器端IP  
            local = InetAddress.getByName(LOCALHOST);  
        } catch (UnknownHostException e) {  
            e.printStackTrace();  
        }  
        int msg_length = message.length();  
        byte[]  messageByte = message.getBytes();  
        DatagramPacket p = new DatagramPacket(messageByte, msg_length, local,  
                server_port);  
        try {  
            s.send(p);  
        } catch (IOException e) {  
            e.printStackTrace();  
        }  
    } 
}
