package com.ipmacro.server;

import java.io.File;
import java.io.IOException;

import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.entity.FileEntity;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpRequestHandler;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.ipmacro.app.Config;

public class FileCommandHandler implements HttpRequestHandler {
	private static final String TAG = "TAG";
	private Context context = null;

	public FileCommandHandler(Context context) {
		this.context = context;
	}

	@Override
	public void handle(HttpRequest request, HttpResponse response,
			HttpContext context) throws HttpException, IOException {

		SharedPreferences sp = this.context.getSharedPreferences("SP",
				Context.MODE_PRIVATE);
		String file_path = sp.getString(Config.IMG_PATH, "path");
		
		Log.e("xxx",file_path);
		try {
			File file = new File(file_path);
			FileEntity body = new FileEntity(file, "audio/mpeg");
			response.setHeader("Content-Type", "application/force-download");
			response.setHeader("Content-Disposition",
					"attachment; filename=img.so");
			response.setEntity(body);

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
