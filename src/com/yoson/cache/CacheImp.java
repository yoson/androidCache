package com.yoson.cache;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import com.alibaba.mtl.appmonitor.AppMonitor;
import com.taobao.statistic.TBS;


/**
 * @description
 * 
 * @author yilong.yyl
 *
 * @date 2014年8月4日 下午2:59:09  
 * 
 */
public class CacheImp {

	public static final int NO_CACHE = 0;
	
	public static final int FIFO_CACHE = 1;
	
	public static final String TAG = "newCache";
	
	public static final String TAOBAODIR = "taobao";
	
	private CacheStorage mCacheStorage;
	
	private String appDataDir;
	
	private ByteBuffer ptr;
	
	
//	private ConfigObject co;
//	private String blockName;
	
	

	private boolean sInited = false;


	public CacheImp(Context mContext,String blockName, String cacheDir,String packageName,ConfigObject co) {
		// TODO Auto-generated constructor stub

    	if(null == mContext){
    		sInited = false;
    		return;
    	}
    	File cacheFile = mContext.getExternalCacheDir();
    	if(null == cacheFile){
    		sInited = false;
    		return;
    	}
    	appDataDir = cacheFile.getAbsolutePath()+File.separator+cacheDir;
    	//Log.e("newCache", "Datadir =" +appDataDir);
		File file =new File(appDataDir);  
		//如果文件夹不存在则创建  
		if  (!file.exists()  && !file.isDirectory())    
		{     
		    file.mkdirs();  
		}

		//TBS.Ext.commitEvent("Page_Store",65101, "create", 0, null, null);
		File flag=new File(file,"flag");
		if(!flag.exists()){
			try {
				flag.createNewFile();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				//TBS.Ext.commitEvent("Page_Store",65101, "create", 1, null, null);
				AppMonitor.Alarm.commitFail("Page_Store", "newCache", "code3", "failJavaExp");
			}
		}
		else{
			Long modifyTime =file.lastModified();
			Long nowTime = System.currentTimeMillis();
			if((nowTime - modifyTime)<24*60*60*1000){
				//TBS.Ext.commitEvent("Page_Store",65101, "create", 2, null, null);
				AppMonitor.Alarm.commitFail("Page_Store", "newCache", "code1", "failIn24");
				sInited = false;
				return;
			}
			else{
				if  (flag.exists()  && !flag.isDirectory())    
				{     
				    flag.delete(); 
				    try {
						flag.createNewFile();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
				AppMonitor.Alarm.commitFail("Page_Store", "newCache", "code2", "failOut24");
			}
				
			
		}
		
		long size = co.blockSize*1024*1024L;
		mCacheStorage = new CacheStorage(blockName,appDataDir, size, FIFO_CACHE, co.isCompress);
		ptr = mCacheStorage.getPtr();


		
		if(ptr == null){
			sInited = false;
			Log.e(TAG, "cache open false");
		}else{
			sInited = true;
		}
		if  (flag.exists()  && !flag.isDirectory())    
		{     
			AppMonitor.Alarm.commitSuccess("Page_Store", "newCache");
		    flag.delete(); 
		}
		
		
	}


	/**
	 * 读取缓存数据
	 */
	public byte[] read(String key){
		byte[] mData = null;
		if(!init())
			return null;
		if(TextUtils.isEmpty(key))
			return null;
//		long startTime = System.nanoTime();
		if(mCacheStorage != null){	
				//Log.w(TAG, "in select");
				try{
					mData = mCacheStorage.select(key.getBytes(),ptr);
				}catch(UnsatisfiedLinkError e){
					Log.e(TAG, "native method not found");
				}
		}
//		long endTime = System.nanoTime();
		
//		if(endTime%1000 == 0){
//			MeasureValueSet mvs;
//			DimensionValueSet dvs = DimensionValueSet.create().setValue("mode", "read").setValue("success", "true");
//			 
//			 
//			if(mData != null)
//				mvs = MeasureValueSet.create().setValue("totalTime", endTime-startTime).setValue("size",mData.length);
//			else
//				mvs = MeasureValueSet.create().setValue("totalTime", endTime-startTime).setValue("size",0d);
//			
//			AppMonitor.Stat.commit("New_Cache", "Cache", dvs,mvs);
//		}


		 

		return mData;
	}

	
	/**
	 * 删除缓存
	 */
	public boolean remove(String key){
		if(!init())
			return false;
		boolean flag = false;
		try{
			if(mCacheStorage != null)
				flag = mCacheStorage.delete(key.getBytes(),ptr);
		}catch(UnsatisfiedLinkError e){
			Log.e(TAG, "native method not found");
		}
		return flag;
	}
	
	
	
	/**
	 * 写入缓存
	 */
	public boolean write(String key,byte[] value,boolean isOverWrite,int compressMode){
		if(!init())
			return false;

//		Log.e(TAG, "isOverWrite = " + isOverWrite);
//		Log.e("TAG", "isRemovable ="+co.isRemovable);
//		Log.e(TAG, "blockname = "+blockName);
//		Log.e(TAG, "key = "+key);
//		Log.e(TAG,"value = "+new String(value));
//		Log.e(TAG, "thread name = "+Thread.currentThread().getName());
//		long startTime = System.nanoTime();
		boolean flag = false;
		try{
			if(mCacheStorage != null)
				flag = mCacheStorage.insert(key.getBytes(), value,isOverWrite, compressMode,ptr);
		}catch(UnsatisfiedLinkError e){
			Log.e(TAG, "native method not found");
		}
//		long endTime = System.nanoTime();
//		if(endTime%1000 == 0){
//			MeasureValueSet mvs = MeasureValueSet.create().setValue("totalTime", endTime-startTime).setValue("size",value.length);
//			DimensionValueSet dvs;
//			if(flag){						
//				 dvs = DimensionValueSet.create().setValue("mode", "write").setValue("success", "true");
//			}
//			else{
//				 dvs = DimensionValueSet.create().setValue("mode", "write").setValue("success", "false");	
//			}
//			AppMonitor.Stat.commit("New_Cache", "Cache", dvs,mvs);
//		}
		return flag;
	}
	
	/**
	 * 清除缓存
	 * 
	 */
	public boolean clear(){
		if(!init())
			return false;
		
		boolean ret = true;
		try{
			if(mCacheStorage != null)
				ret = mCacheStorage.destory(ptr);
		}catch(UnsatisfiedLinkError e){
			Log.e(TAG, "native method not found");
		}
		return ret;
	}
	
	public boolean reSetMaxSize(long size){
		if(!init() || size > 100)
			return false;
		
		boolean ret = true;
		long MtoB = size*1024*1024L;
		
		try{
			if(mCacheStorage != null)
				ret = mCacheStorage.reMaxSize(MtoB,ptr);
		}catch(UnsatisfiedLinkError e){
			Log.e(TAG, "native method not found");
		}
		return ret;
	}
	
	/**
	 * 关闭缓存，将索引写入文件，再次调用缓存操作，将重新打开缓存。建议在退出客户端后调用。
	 */
	public boolean close(){
		sInited = false;
		boolean flag = false;
		try{
			if(mCacheStorage != null)
				flag = mCacheStorage.close(ptr);
		}catch(UnsatisfiedLinkError e){
			Log.e(TAG, "native method not found");
		}
		return flag;
	}
	
	public String[] getAllKey(){
		try{
			if(mCacheStorage != null)
				return mCacheStorage.getAllKey(ptr);
		}catch(UnsatisfiedLinkError e){
			Log.e(TAG, "native method not found");
		}
		return null;
	}
	
	public boolean init(){
		if(sInited)
			return true;    
        return sInited;     
	}

}
