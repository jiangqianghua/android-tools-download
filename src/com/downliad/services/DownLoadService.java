package com.downliad.services;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.RandomAccess;

import org.apache.http.HttpStatus;

import com.download.entites.FileInfo;

import android.app.Service;
import android.content.Intent;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

public class DownLoadService extends Service {

	public static final String DOWNLOAD_PATH = Environment.getExternalStorageDirectory().getAbsolutePath()+"/downloads/"; 
	public static final String ACTION_START = "ACTION_START";
	public static final String ACTION_STOP = "ACTION_STOP";
	public static final String ACTION_UPDATE = "ACTION_UPDATE";
	public static final String ACTION_FINISH = "ACTION_FINISH";
	public static final int MSG_INIT = 0 ;
	//private DownLoadTask mTask ; 
	// 下载任务集合
	private Map<Integer, DownLoadTask> mTasks = new LinkedHashMap<Integer, DownLoadTask>();
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {

		if(ACTION_START.equals(intent.getAction()))
		{
			FileInfo fileInfo = (FileInfo) intent.getSerializableExtra("fileInfo");
			Log.i("test","start:"+fileInfo.toString());
			
			// run thread
			//new InitThread(fileInfo).start(); 
			DownLoadTask.sExecutorService.execute(new InitThread(fileInfo));
		}
		else if(ACTION_STOP.equals(intent.getAction()))
		{
			FileInfo fileInfo = (FileInfo) intent.getSerializableExtra("fileInfo");
			Log.i("test","stop:"+fileInfo.toString());

			DownLoadTask task = mTasks.get(fileInfo.getId());
			if(task != null)
			{
				task.isPause = true ;
			}
			
			
		}
		
		return super.onStartCommand(intent, flags, startId);
	}
	
	@Override
	public IBinder onBind(Intent intent) {

		return null;
	}
	
	Handler mHandler = new Handler()
	{
		public void handleMessage(android.os.Message msg) 
		{
			switch (msg.what) {
			case MSG_INIT:
				FileInfo fileInfo = (FileInfo) msg.obj ;
				Log.i("test", 	"Init "+fileInfo);
				// 启动下载任务
				DownLoadTask mTask = new DownLoadTask(DownLoadService.this, fileInfo,3);
				mTask.download();
				mTasks.put(fileInfo.getId(), mTask);
				break;

			default:
				break;
			}
		};
	};
	// 初始化子线程
	class InitThread extends Thread
	{
		private FileInfo mFileInfo = null ;
		public InitThread(FileInfo mFileInfo)
		{
			this.mFileInfo = mFileInfo ;
		}
		
		@Override
		public void run() {
			HttpURLConnection conn = null ;
			RandomAccessFile raf = null ;
			try{
				// 连接网络文件
				URL url = new URL(mFileInfo.getUrl());
				conn = (HttpURLConnection) url.openConnection() ;
				conn.setConnectTimeout(3000);
				conn.setRequestMethod("GET");
				int length = -1 ;
				if(conn.getResponseCode() == HttpStatus.SC_OK)
				{
					//获取文件长度
					length = conn.getContentLength() ;
				}
				if(length <= 0)
				{
					return ;
				}
				
				File dir = new File(DOWNLOAD_PATH);
				if(!dir.exists())
				{
					dir.mkdir();
				}
				//在本地创建文件
				File file = new File(dir , mFileInfo.getFileName());
				raf = new RandomAccessFile(file , "rwd") ;
				
				//设置文件长度
				raf.setLength(length);
				mFileInfo.setLength(length);
				mHandler.obtainMessage(MSG_INIT,mFileInfo).sendToTarget();
			}catch(Exception e)
			{
				
			}
			finally{
				conn.disconnect(); 
				try {
					raf.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} 
			}
			super.run();
		}
	}

}
