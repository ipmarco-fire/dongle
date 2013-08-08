package com.ipmacro.dongle.upgrade;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import com.ipmacro.server.WebServerService;

public class ServerActivity extends Activity {
	Context context;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.server);
		context = this;
	}

	public void start(View v) {
		Intent webServerService = new Intent(context, WebServerService.class);
		startService(webServerService);
	}

	public void stop(View v) {
		Intent webServerService = new Intent(context, WebServerService.class);
		stopService(webServerService);
	}
}
