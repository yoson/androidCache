package com.yoson.cache;

import java.nio.ByteBuffer;

import android.util.Log;


/**
 * @description 存储native接口方法调用
 * 
 * @author yilong.yyl
 *
 * @date 2014年8月4日 下午2:59:40  
 * 
 */
public class CacheStorage implements Cache{
	private static boolean mInitSuc = false;
	public static final String TAG = "newCahce";
	static {
		 mInitSuc = LoadSo.initSo( "CacheStorage", 2);
		//System.loadLibrary("CacheStorage");
	}
	
	private ByteBuffer ptr;

	public CacheStorage(String name, String path, long size, int type, boolean compressMode){
		if(mInitSuc){
			try{
				ptr = open(name,path,size,type,compressMode);
			}catch(UnsatisfiedLinkError e){
				Log.e(TAG, "native method not found");
			}
			if(ptr == null){
				Log.e(TAG,"open failed");
			}
		}
		
		else
			Log.e(TAG, "CacheStorage so load failed");
		
	}
	public ByteBuffer getPtr(){
		return ptr;
	}
	public native ByteBuffer open(String name, String path, long size, int type, boolean compressMode) throws UnsatisfiedLinkError;


	public native boolean insert(byte[] key, byte[] value, boolean needCrypt,int iCmp,ByteBuffer ptr) throws UnsatisfiedLinkError;


	public native boolean delete(byte[] key,ByteBuffer ptr) throws UnsatisfiedLinkError;

	public native byte[] select(byte[] key,ByteBuffer ptr) throws UnsatisfiedLinkError;

	public native boolean close(ByteBuffer ptr) throws UnsatisfiedLinkError;

	public native boolean destory(ByteBuffer ptr) throws UnsatisfiedLinkError;
	
	public native String[] getAllKey(ByteBuffer ptr) throws UnsatisfiedLinkError;

	public native boolean reMaxSize(long size,ByteBuffer ptr) throws UnsatisfiedLinkError;

}
