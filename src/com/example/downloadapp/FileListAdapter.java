package com.example.downloadapp;

import java.util.List;

import com.downliad.services.DownLoadService;
import com.download.entites.FileInfo;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
/**
 * 文件列表适配器
 * @author jiangqianghua
 *
 */
public class FileListAdapter extends BaseAdapter {

	private Context mContext = null ; 
	private List<FileInfo> mFileList = null ;
	
	public FileListAdapter(Context context , List<FileInfo> mFileList) {
		this.mContext = context ; 
		this.mFileList = mFileList ;
	}
	
	@Override
	public int getCount() {
		// TODO Auto-generated method stub
		return mFileList.size();
	}

	@Override
	public Object getItem(int position) {
		return mFileList.get(position);
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		ViewHolder holder = null ;
		if(convertView == null)
		{
			convertView = LayoutInflater.from(mContext).inflate(R.layout.listitem, null);
			holder = new ViewHolder();
			holder.tvFile = (TextView) convertView.findViewById(R.id.filename);
			holder.btStart = (Button) convertView.findViewById(R.id.pbStart);
			holder.btStop = (Button) convertView.findViewById(R.id.pbStop);
			holder.pbFile = (ProgressBar) convertView.findViewById(R.id.pbProgressBar);
			convertView.setTag(holder);
		}
		else
		{
			holder = (ViewHolder) convertView.getTag();
			
		}
		// 设置视图控件
		final FileInfo fileInfo = mFileList.get(position);
		holder.tvFile.setText(fileInfo.getFileName());
		holder.pbFile.setMax(100);
		holder.pbFile.setProgress(fileInfo.getFinished());
		// 开始
		holder.btStart.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {

				Intent intent = new Intent(mContext, DownLoadService.class);
				intent.setAction(DownLoadService.ACTION_START);
				intent.putExtra("fileInfo", fileInfo);
				mContext.startService(intent);
			}
		});
		// 停止
		holder.btStop.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {

				Intent intent = new Intent(mContext, DownLoadService.class);
				intent.setAction(DownLoadService.ACTION_STOP);
				intent.putExtra("fileInfo", fileInfo);
				mContext.startService(intent);
			}
		});
		return convertView;
	}

	/**
	 * 更新列表项的进度条
	 */
	public void updateProgress(int id , int progress)
	{
		FileInfo fileInfo = mFileList.get(id);
		fileInfo.setFinished(progress);
		notifyDataSetChanged(); 
	}
	static class ViewHolder{
		TextView tvFile;
		Button btStop , btStart ;
		ProgressBar pbFile ;
	}
}
