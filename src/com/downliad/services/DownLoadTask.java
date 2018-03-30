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
 * ����������
 * @author jiangqianghua
 *
 */
public class DownLoadTask {

	private Context mContext = null ;
	private FileInfo mFileInfo  = null;
	private ThreadDAO mDao = null ;
	private int mFinished = 0 ;
	public boolean isPause = false ;
	// ���߳�����һ���ļ������߳�����
	private int mThreadCount = 1 ; 
	
	private List<DownLoadThread> mThreadList = null ;
	/**
	 * �̳߳�
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
		// ��ȡ���ݿ��߳���Ϣ
		List<ThreadInfo> threadInfos = mDao.getThreads(mFileInfo.getUrl());
	//	ThreadInfo threadInfo = null ;
		if(threadInfos.size() == 0)
		{
			int length = mFileInfo.getLength() / mThreadCount ;
			for(int i = 0 ; i < mThreadCount ; i++)
			{
				ThreadInfo threadInfo = new ThreadInfo(i, mFileInfo.getUrl(), length*i, (i+1)*length - 1, 0);
				if(i == mThreadCount -1) // ��ֹ�����������
				{
					threadInfo.setEnd(mFileInfo.getLength());
				}
				threadInfos.add(threadInfo);
			}

		}else{


		}
		mThreadList = new ArrayList<DownLoadThread>() ;
		// ��������߳̽�������
		for(ThreadInfo info:threadInfos)
		{
			DownLoadThread thread = new DownLoadThread(info);
			//thread.start(); 
			DownLoadTask.sExecutorService.execute(thread);
			mThreadList.add(thread);
		}
		
	}
	/**
	 * �ж������߳��Ƿ�ִ�����
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
			// ֪ͨui�����ؽ���
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

			// �����ݿ�����߳���Ϣ
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
				// ��������λ��
				conn.setRequestMethod("GET");
				int start = mThreadInfo.getStart() + mThreadInfo.getFinished() ;
				conn.setRequestProperty("Range", "bytes="+start+"-"+mThreadInfo.getEnd());
				// �����ļ�д���ļ�
				File file = new File(DownLoadService.DOWNLOAD_PATH , mFileInfo.getFileName());
				raf = new RandomAccessFile(file, "rwd");
				raf.seek(start);
				Intent intent = new Intent(DownLoadService.ACTION_UPDATE);
				mFinished += mThreadInfo.getFinished() ;
				// ��ʼ����
				if(conn.getResponseCode() == HttpStatus.SC_PARTIAL_CONTENT)
				{
					// ��ȡ����
					input = conn.getInputStream();
					byte[] buffer = new byte[1024*4];
					int len = -1 ; 
					long time = System.currentTimeMillis();
					while((len = input.read(buffer)) != -1)
					{
						// д���ļ�
						raf.write(buffer,0,len);
						// �����ؽ��ȷ��͹㲥��activity
						mFinished += len ; // ���еĽ���
						// ÿһ���̵߳Ľ���
						mThreadInfo.setFinished(mThreadInfo.getFinished() + len );
						if(System.currentTimeMillis() - time > 500)
						{
							time = System.currentTimeMillis() ;
							intent.putExtra("finished", mFinished*100/mFileInfo.getLength());
							intent.putExtra("id", mFileInfo.getId());
							mContext.sendBroadcast(intent);
						}
						//��������ͣ���������ؽ���
						if(isPause)
						{
							mDao.updateThread(mThreadInfo.getUrl(), mThreadInfo.getId(), mThreadInfo.getFinished());
							return ;
						}
					}
					isFinished = true ;
					// ɾ���߳���Ϣ
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
