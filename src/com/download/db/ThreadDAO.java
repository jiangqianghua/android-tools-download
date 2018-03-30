package com.download.db;

import java.util.List;

import com.download.entites.ThreadInfo;

/**
 * 数据库访问接口
 * @author jiangqianghua
 *
 */
public interface ThreadDAO {

	public void insertThread(ThreadInfo threadInfo);
	
	public void deleteThread(String url , int thread_id);
	
	public void updateThread(String url , int thread_id , int finshed);
	
	public List<ThreadInfo> getThreads(String url);
	
	public boolean isExists(String url , int hread_id);
}
