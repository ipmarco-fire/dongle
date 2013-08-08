package com.ipmacro.dongle.upgrade;

import java.io.File;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.ipmacro.app.Config;
import com.ipmacro.app.MyApplication;
import com.ipmacro.app.MyApplication.URLCallBack;
import com.ipmacro.server.WebServer;
import com.ipmacro.server.WebServerService;
import com.ipmacro.utils.DownloadUtil;
import com.ipmacro.utils.Tools;

public class MainActivity extends Activity {

	private static final int MSG_START_SERVER_FALI = 1;
	private static final int MSG_SEND_CMD = 2;
	
	MyApplication mApp;
	Context mContext;
	BroadcastReceiver receiver;

	LinearLayout layoutProgress;
	TextView txtInfo;
	Button btnDownload;
	ProgressBar progressBar;
	TextView txtProgress;

	int mainSoftVersion, mainPageVersion,productId;
	String httpUrl = null;
	DownloadUtil downloadUtil;
	int cmd= 0;
	
	Handler mhandler = new Handler(){
		@Override
		public void handleMessage(Message msg) {
			switch(msg.what){
			case MSG_START_SERVER_FALI:
				Toast.makeText(mContext, "没用升级文件,请先下载", Toast.LENGTH_LONG).show();
				break;
			case MSG_SEND_CMD:
				Thread thread = new Thread(runnable);
				thread.start();
				mhandler.sendEmptyMessageDelayed(MSG_SEND_CMD, 2000);
			}
		}
	};
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		mApp = (MyApplication) getApplication();
		mContext = this;
		downloadUtil = new DownloadUtil(this);

		receiver = new DownloadRecordReceiver();
		IntentFilter filter = new IntentFilter();
		filter.addAction(DownloadUtil.ACTION_DOWNLOAD_RECEIVER);
		registerReceiver(receiver, filter);

		initView();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		unregisterReceiver(receiver);
		
		Intent webServerService = new Intent(this, WebServerService.class);
		stopService(webServerService);
		mhandler.removeMessages(MSG_SEND_CMD);
	}

	private void initView() {
		txtInfo = (TextView) findViewById(R.id.txtInfo);
		btnDownload = (Button) findViewById(R.id.btnDownload);

		layoutProgress = (LinearLayout) findViewById(R.id.layoutProgress);
		progressBar = (ProgressBar) findViewById(R.id.progressBar);
		txtProgress = (TextView) findViewById(R.id.txtProgress);
	}

	public void checkUpgrade(View v) {
		mApp.loadURL(Config.UPGRADE_URL, new URLCallBack() {
			@Override
			public void loadHtml(String con) {
				try {
					parseUpgrade(con);
					
				} catch (JSONException e) {
					e.printStackTrace();
					Toast.makeText(mContext, "解析升级JSON失败", Toast.LENGTH_LONG)
							.show();
				}
			}
		});
	}

	public void parseUpgrade(String con) throws JSONException {
		JSONObject root = new JSONObject(con);
		if (con == null) {
			Toast.makeText(mContext, "下载升级JSON失败", Toast.LENGTH_LONG).show();
			return;
		}
		mainSoftVersion = root.getInt("mainSoftVersion");
		mainPageVersion = root.getInt("mainPageVersion");
		productId = root.getInt("productId");
		httpUrl = root.getString("httpUrl");
		// httpUrl = "http://192.168.1.195/system_new.img.ori.v2";

		String text = "版本信息:Soft:" + mainSoftVersion;
		//+ mainSoftVersion + ",productId:"+ productId + ",升级地址:" + httpUrl;
		txtInfo.setText(text);
		
		btnDownload.setVisibility(View.VISIBLE);
		
		SharedPreferences sp = getSharedPreferences("SP", MODE_PRIVATE);
		int sv = sp.getInt(Config.MAIN_SOFT_VERSION, 0);
		if(mainSoftVersion > sv){
			createDialog("服务器有新的版本,请按下载按钮下载升级文件");
		}else{
			String path = sp.getString(Config.IMG_PATH, "");
			File file = new File(path);
			if(!file.exists()){
				createDialog("本地升级文件不存在,请按下载按钮下载升级文件");
			}else{
				createDialog("升级文件已存在,如果升级失败可以按下载按钮重新下载升级文件");
			}
		}
	}
		
	private void createDialog(String msg) {
		AlertDialog.Builder builder = new Builder(this);
		builder.setMessage(msg);
		builder.setPositiveButton(getResources().getString(R.string.ok),
				new DialogInterface.OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {
					}
				});
		builder.create().show();
	}
	
	public void afterDownload() {
		String fileName = "dongle_" + mainSoftVersion + "_" + mainPageVersion
				+ "_bak.img";
		String newName = "dongle_" + mainSoftVersion + "_" + mainPageVersion
				+ ".img";
		downloadUtil.rename(fileName, newName);

		SharedPreferences sp = getSharedPreferences("SP", MODE_PRIVATE);
		Editor editor = sp.edit();
		editor.putString(Config.IMG_PATH, downloadUtil.getFolder()+"/"+newName);
		editor.putInt(Config.MAIN_PAGE_VERSION, mainPageVersion);
		editor.putInt(Config.MAIN_SOFT_VERSION, mainSoftVersion);
		editor.putInt(Config.PRODUCTID, productId);
		editor.commit();
	}

	public void startServer(View v){
		SharedPreferences sp = getSharedPreferences("SP", MODE_PRIVATE);
		String path = sp.getString(Config.IMG_PATH, "");
		if(path.equals("")){
			Toast.makeText(mContext, "没用升级文件,请先下载", Toast.LENGTH_LONG).show();
			return ;
		}
		File file = new File(path);
		if(!file.exists()){
			Toast.makeText(mContext, "没用升级文件,请先下载", Toast.LENGTH_LONG).show();
			return ;
		}
		
		Intent webServerService = new Intent(this, WebServerService.class);
		startService(webServerService);
		cmd = Integer.parseInt((String) v.getTag());
		
		mhandler.removeMessages(MSG_SEND_CMD);
		mhandler.sendEmptyMessage(MSG_SEND_CMD);
		
		/*
		Thread thread = new Thread() {
			public void run() {
			}
		};
		thread.start();
		*/
	}
	
	Runnable runnable = new Runnable() {
		
		@Override
		public void run() {
			SharedPreferences sp = getSharedPreferences("SP", MODE_PRIVATE);
			int pv = sp.getInt(Config.MAIN_PAGE_VERSION, 0);
			int sv = sp.getInt(Config.MAIN_SOFT_VERSION, 0);
			int productId = sp.getInt(Config.PRODUCTID, 0);
			if(pv==0 || sv==0 ){
				mhandler.sendEmptyMessage(MSG_START_SERVER_FALI);
				return;
			}
			
			String ip = Tools.getLocalIpAddress( mContext);
			String msg = cmd+";"+sv+";"+productId+";"+"http://"+ip+":8080"+WebServer.IMG_PATTERN+";";
			Log.w("msg",msg);
			send(msg);
		}
	};
	
	public void download(View v) {
		if (httpUrl == null || httpUrl.equals("")) {
			Toast.makeText(mContext, "请先获取升级信息", Toast.LENGTH_LONG).show();
			return;
		}
		if (downloadUtil.isRunning()) {
			downloadUtil.cancel();
			btnDownload.setText(R.string.download);
			layoutProgress.setVisibility(View.GONE);
			return;
		}

		btnDownload.setText(R.string.cancel);
		layoutProgress.setVisibility(View.VISIBLE);
		Thread thread = new Thread() {
			public void run() {
				try {
					String fileName = "dongle_" + mainSoftVersion + "_"
							+ mainPageVersion + "_bak.img";
					String apkPath = downloadUtil.download(httpUrl, fileName);
					Log.e("donwload", apkPath);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		};
		thread.start();
	}

	public class DownloadRecordReceiver extends BroadcastReceiver {
		@Override
		public void onReceive(Context context, Intent intent) {
			Bundle bundle = intent.getExtras();
			int state = bundle.getInt(DownloadUtil.DOWNLOAD_STATE, 0); // 下载状态
			String url = bundle.getString(DownloadUtil.DOWNLOAD_URL); // 下载链接
			String path = bundle.getString(DownloadUtil.DOWNLOAD_PATH); // 下载保存路径
			int length = bundle.getInt(DownloadUtil.DOWNLOAD_LENGTH, 0)/1024; // 下载长度
			int size = bundle.getInt(DownloadUtil.DOWNLOAD_SIZE, 0)/1024; // 下载长度大小
			
			if(size == 0){
				size = 1;
			}
		
			
			switch (state) {
			case DownloadUtil.DOWNLOAD_STATIC_INIT:
				progressBar.setMax(size);
				progressBar.setProgress(0);
				txtProgress.setText((length * 100) / size + "%");
				break;
			case DownloadUtil.DOWNLOAD_STATIC_DOWNLOADING:
				progressBar.setProgress(length);
				txtProgress.setText((length * 100 )/ size + "%");
				break;
			case DownloadUtil.DOWNLOAD_STATIC_FINISH:
				btnDownload.setText(R.string.download);
				progressBar.setProgress(length);
				txtProgress.setText((length * 100) / size + "%");
				Toast.makeText(context, R.string.download_success, 5000).show();
				afterDownload();
				break;
			case DownloadUtil.DOWNLOAD_STATIC_FIAL:
				btnDownload.setText(R.string.download);
				Toast.makeText(context, R.string.download_fail, 5000).show();
				break;
			case DownloadUtil.DOWNLOAD_STATIC_CANCEL:
				btnDownload.setText(R.string.download);
				break;
			default:
				break;
			}
		}
	}
	
	public void send(String message) {  
        message = (message == null ? "Hello IdeasAndroid!" : message);  
        int server_port = 10101;  
        DatagramSocket s = null;  
        try {  
            s = new DatagramSocket();  
        } catch (SocketException e) {  
            e.printStackTrace();  
        }  
        InetAddress local = null;  
        try {  
            // 换成服务器端IP  
            local = InetAddress.getByName("10.1.0.1");  
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
