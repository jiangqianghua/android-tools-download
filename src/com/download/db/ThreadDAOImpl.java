package com.download.db;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.download.entites.ThreadInfo;

/**
 * 数据访问接口实现
 * @author jiangqianghua
 *
 */
public class ThreadDAOImpl implements ThreadDAO {

	private DBHelper mHelper = null ;
	
	public ThreadDAOImpl(Context context) {
		// TODO Auto-generated constructor stub
		mHelper =  DBHelper.getInstance(context);
	}
	
	@Override
	public synchronized void insertThread(ThreadInfo threadInfo) {
		SQLiteDatabase db = mHelper.getWritableDatabase() ;
		db.execSQL(
				"insert into thread_info(thread_id , url , start , end , finished) values(?,?,?,?,?)"
				, new Object[]{threadInfo.getId() , threadInfo.getUrl() , threadInfo.getStart() , 
						threadInfo.getEnd() , threadInfo.getFinished()});
		db.close(); 
		
	}

	@Override
	public synchronized void deleteThread(String url, int thread_id) {
		
		SQLiteDatabase db = mHelper.getWritableDatabase() ;
		db.execSQL(
				"delete from thread_info where url = ? and thread_id = ?"
				, new Object[]{url , thread_id});
		db.close(); 

	}

	@Override
	public synchronized void updateThread(String url, int thread_id, int finshed) {

		SQLiteDatabase db = mHelper.getWritableDatabase() ;
		db.execSQL(
				"update thread_info set finished = ? where url = ? and thread_id = ?"
				, new Object[]{finshed ,url , thread_id});
		db.close(); 
	}

	@Override
	public synchronized List<ThreadInfo> getThreads(String url) {
		SQLiteDatabase db = mHelper.getReadableDatabase() ;
		List<ThreadInfo> list = new ArrayList<ThreadInfo>();
		Cursor cursor = db.rawQuery(
				"select * from thread_info where url = ?"
				, new String[]{url});
		while(cursor.moveToNext()){
			ThreadInfo thread = new ThreadInfo();
			thread.setId(cursor.getInt(cursor.getColumnIndex("thread_id")));
			thread.setUrl(cursor.getString(cursor.getColumnIndex("url")));
			thread.setStart(cursor.getInt(cursor.getColumnIndex("start")));
			thread.setEnd(cursor.getInt(cursor.getColumnIndex("end")));
			thread.setFinished(cursor.getInt(cursor.getColumnIndex("finished")));
			list.add(thread);
		}
		db.close(); 
		cursor.close(); 
		return list;
	}

	@Override
	public synchronized boolean isExists(String url, int hread_id) {

		SQLiteDatabase db = mHelper.getReadableDatabase() ;
		Cursor cursor = db.rawQuery(
				"select * from thread_info where url = ? and thread_id = ?"
				, new String[]{url , hread_id+""});
		boolean exists = cursor.moveToNext() ;
		cursor.close(); 
		db.close(); 
		return exists;
	}

}
