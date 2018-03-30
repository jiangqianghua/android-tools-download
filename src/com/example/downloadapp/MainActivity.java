package com.example.downloadapp;

import java.util.ArrayList;
import java.util.List;

import com.downliad.services.DownLoadService;
import com.download.entites.FileInfo;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {


	private ListView mLvFile ;
	private List<FileInfo> mFileList = null ; 
	private FileListAdapter mAdapter = null ;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		mLvFile = (ListView) findViewById(R.id.lvfile);

		mFileList = new ArrayList<FileInfo>();
		
		 FileInfo fileInfo = new FileInfo(0, "http://www.imooc.com/mobile/mukewang.apk", "mukewang.apk", 0, 0);
		
		 FileInfo fileInfo1 = new FileInfo(1, "http://www.imooc.com/mobile/mukewang.apk", "mukewang1.apk", 0, 0);
		
		 FileInfo fileInfo2 = new FileInfo(2, "http://www.imooc.com/mobile/mukewang.apk", "mukewang2.apk", 0, 0);
		
		 FileInfo fileInfo3 = new FileInfo(3, "http://www.imooc.com/mobile/mukewang.apk", "mukewang3.apk", 0, 0);
		
		mFileList.add(fileInfo);
		mFileList.add(fileInfo1);
		mFileList.add(fileInfo2);
		mFileList.add(fileInfo3);
		mAdapter = new FileListAdapter(this, mFileList);
		mLvFile.setAdapter(mAdapter);
		// 注册广播接收器
		IntentFilter filter = new IntentFilter() ;
		filter.addAction(DownLoadService.ACTION_UPDATE);
		filter.addAction(DownLoadService.ACTION_FINISH);
		registerReceiver(mReceiver, filter);
		
	}
	
	
	protected void onDestroy() {
		unregisterReceiver(mReceiver);
		super.onDestroy(); 

	};
	/**
	 * 更新ui的广播接收器
	 */
	BroadcastReceiver mReceiver = new BroadcastReceiver(){

		@Override
		public void onReceive(Context context, Intent intent) {
			// TODO Auto-generated method stub
			if(DownLoadService.ACTION_UPDATE.equals(intent.getAction())){
				int finished = intent.getIntExtra("finished", 0);
				int id = intent.getIntExtra("id", 0);
				//mPbProgressBar.setProgress(finished);
				mAdapter.updateProgress(id, finished);
			}
			else if(DownLoadService.ACTION_FINISH.equals(intent.getAction())){
				// 更新进度为0
				FileInfo fileInfo = (FileInfo) intent.getSerializableExtra("fileInfo");
				mAdapter.updateProgress(fileInfo.getId(), 0);
				Toast.makeText(MainActivity.this, fileInfo.getFileName(), Toast.LENGTH_SHORT).show();
			}
		}
		
	};
}
