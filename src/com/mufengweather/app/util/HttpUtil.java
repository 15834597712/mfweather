package com.mufengweather.app.util;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import android.util.Log;

/**
 * 网络请求封装 sendHttpRequest(final String address,final HttpCallbackListener listener)
 * 为什么用HttpCallbackListener 因为服务器响应是需要时间的所以有可能引起主线程的阻塞 所以在send方法中开了一个子线程 由于耗时逻辑都是
 * 在子线程中处理的 所以send方法在服务器还没来的及响应的时就结束了那么服务器响应的数据就没办法接收了 。 为了解决这个问题用了一个Java的回调机制
 * 就是HttpCallbackListener。
 */
public class HttpUtil {
	public static void sendHttpRequest(final String address,
			final HttpCallbackListener listener) {
		new Thread(new Runnable() {
			@Override
			public void run(){
				HttpURLConnection connection = null;
				try {
					URL url = new URL(address);
					connection=(HttpURLConnection) url.openConnection();
					connection.setRequestMethod("GET");
					connection.setConnectTimeout(10000);
					connection.setReadTimeout(10000);
					InputStream in = connection.getInputStream();
					BufferedReader reader = new BufferedReader(new InputStreamReader(in));
					StringBuilder response = new StringBuilder();
					String line;
					while((line = reader.readLine()) != null ){
						response.append(line);
						
					}
					if(listener != null){
						//回调onFinish方法
						listener.onFinish(response.toString());
					}
				} catch (Exception e) {
					if(listener != null){
						//回调onError方法
						listener.onError(e);
						
					} 
			   }finally{
				   if(connection != null){
					   connection.disconnect();
				   }
			   }
				
			}
		}).start();
	
	}
}