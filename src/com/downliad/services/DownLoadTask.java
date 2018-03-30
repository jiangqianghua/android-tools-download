package com.downliad.services;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.RandomAccess;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.http.HttpStatus;

import com.download.db.ThreadDAO;
import com.download.db.ThreadDAOImpl;
import com.download.entites.FileInfo;
import com.download.entites.ThreadInfo;

import android.content.Context;
import android.content.Intent;

/**
 * 下载任务类
 * @author jiangqianghua
 *
 */
public class DownLoadTask {

	private Context mContext = null ;
	private FileInfo mFileInfo  = null;
	private ThreadDAO mDao = null ;
	private int mFinished = 0 ;
	public boolean isPause = false ;
	// 多线程下载一个文件，多线程数量
	private int mThreadCount = 1 ; 
	
	private List<DownLoadThread> mThreadList = null ;
	/**
	 * 线程池
	 */
	public static ExecutorService sExecutorService = 
			Executors.newCachedThreadPool() ;
	public DownLoadTask(Context mContext , FileInfo mFileInfo , int mThreadCount )
	{
		this.mContext = mContext ; 
		this.mFileInfo = mFileInfo ;
		mDao = new ThreadDAOImpl(mContext);
		this.mThreadCount = mThreadCount ;
	}
	
	public void download(){
		// 读取数据库线程信息
		List<ThreadInfo> threadInfos = mDao.getThreads(mFileInfo.getUrl());
	//	ThreadInfo threadInfo = null ;
		if(threadInfos.size() == 0)
		{
			int length = mFileInfo.getLength() / mThreadCount ;
			for(int i = 0 ; i < mThreadCount ; i++)
			{
				ThreadInfo threadInfo = new ThreadInfo(i, mFileInfo.getUrl(), length*i, (i+1)*length - 1, 0);
				if(i == mThreadCount -1) // 防止除不尽的情况
				{
					threadInfo.setEnd(mFileInfo.getLength());
				}
				threadInfos.add(threadInfo);
			}

		}else{


		}
		mThreadList = new ArrayList<DownLoadThread>() ;
		// 启动多个线程进行下载
		for(ThreadInfo info:threadInfos)
		{
			DownLoadThread thread = new DownLoadThread(info);
			//thread.start(); 
			DownLoadTask.sExecutorService.execute(thread);
			mThreadList.add(thread);
		}
		
	}
	/**
	 * 判断所有线程是否都执行完毕
	 */
	private synchronized void checkAllThreadFinished(){
		boolean allFinished = true ;
		for(DownLoadThread thread:mThreadList)
		{
			if(!thread.isFinished)
			{
				allFinished = false ;
				break;
			}
		}
		if(allFinished)
		{
			// 通知ui，下载结束
			Intent intent = new Intent(DownLoadService.ACTION_FINISH);
			intent.putExtra("fileInfo", mFileInfo);
			mContext.sendBroadcast(intent);
		}
		
	}
	class DownLoadThread extends Thread
	{
		private  ThreadInfo mThreadInfo  = null ;
		private boolean isFinished = false ; 
		public  DownLoadThread(ThreadInfo mThreadInfo) {
			this.mThreadInfo = mThreadInfo ;
		}
		
		@Override
		public void run() {

			// 向数据库插入线程信息
			if(!mDao.isExists(mThreadInfo.getUrl(), mThreadInfo.getId()))
			{
				mDao.insertThread(mThreadInfo);
			}
			HttpURLConnection conn = null ;
			RandomAccessFile raf  = null ;
			InputStream input = null ;
			try {
				URL url = new URL(mThreadInfo.getUrl());
				conn = (HttpURLConnection) url.openConnection() ;
				conn.setConnectTimeout(3000);
				// 设置下载位置
				conn.setRequestMethod("GET");
				int start = mThreadInfo.getStart() + mThreadInfo.getFinished() ;
				conn.setRequestProperty("Range", "bytes="+start+"-"+mThreadInfo.getEnd());
				// 设置文件写入文件
				File file = new File(DownLoadService.DOWNLOAD_PATH , mFileInfo.getFileName());
				raf = new RandomAccessFile(file, "rwd");
				raf.seek(start);
				Intent intent = new Intent(DownLoadService.ACTION_UPDATE);
				mFinished += mThreadInfo.getFinished() ;
				// 开始下载
				if(conn.getResponseCode() == HttpStatus.SC_PARTIAL_CONTENT)
				{
					// 读取数据
					input = conn.getInputStream();
					byte[] buffer = new byte[1024*4];
					int len = -1 ; 
					long time = System.currentTimeMillis();
					while((len = input.read(buffer)) != -1)
					{
						// 写入文件
						raf.write(buffer,0,len);
						// 把下载进度发送广播给activity
						mFinished += len ; // 所有的进度
						// 每一个线程的进度
						mThreadInfo.setFinished(mThreadInfo.getFinished() + len );
						if(System.currentTimeMillis() - time > 500)
						{
							time = System.currentTimeMillis() ;
							intent.putExtra("finished", mFinished*100/mFileInfo.getLength());
							intent.putExtra("id", mFileInfo.getId());
							mContext.sendBroadcast(intent);
						}
						//在下载暂停，保存下载进度
						if(isPause)
						{
							mDao.updateThread(mThreadInfo.getUrl(), mThreadInfo.getId(), mThreadInfo.getFinished());
							return ;
						}
					}
					isFinished = true ;
					// 删除线程信息
					mDao.deleteThread(mThreadInfo.getUrl(), mThreadInfo.getId());
					checkAllThreadFinished();
				}

			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}finally{
				conn.disconnect(); 
				try {
					raf.close();
					input.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

			}

		}
	}
}
