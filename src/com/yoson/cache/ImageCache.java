/*
 * Copyright (C) 2015 alibaba-inc
 *
 * @description 图片cache类，支持图片的二级索引（分辨率）和图片描述信息
 * 
 * @author yilong.yyl
 */

package com.yoson.cache;

import java.nio.ByteBuffer;

import android.util.Log;


/**
 * @description 图片Cache类
 * 
 * @author yilong.yyl
 *
 * @date 2015年1月9日 上午11:52:12  
 * 
 */

public class ImageCache extends Cache {
	
    /** Magic number for current version of cache file format. */
    private static final int CACHE_MAGIC = 0x19891106;
    private static final String LOG = "new-cache";
    
	/**
	 * image中用户的自定义信息
	 */
    
	public byte[] userData;
	
	/**
	 * image 图片分辨率二级索引，目前只支持int型
	 */
	public int index = 0;
	
	public ImageCache(){
		super();
	}
    public ImageCache(String blockName, String key, byte[] value, int index , byte[] userData,
            boolean isOverWrite, int compressMode) {
        super(blockName,key,value,isOverWrite,compressMode);
        this.userData = userData;
        this.index = index;
    }
    
    public ImageCache(String blockName,String key,int index){
    	this(blockName,key,null,index,null,true,-1);
    }
    
    
    /**
     * 将图片对象中的图片和描述信息数据序列化
     */
    public byte[] joinValueUserData(){
    	if(value == null)
    		new NullPointerException("value must be not null!!");
    	int userDataLen = 0;
    	if(userData != null)
    		userDataLen = userData.length;
    	int length = value.length+userDataLen;
    	length += 12;
    	ByteBuffer buffer = ByteBuffer.allocate(length);
    	buffer.putInt(userDataLen);
    	if(userDataLen != 0)
    		buffer.put(userData);
    	buffer.putInt(CACHE_MAGIC);
    	buffer.putInt(value.length);
    	buffer.put(value);
    	return buffer.array();
    }
    
    /**
     * 将value中的数据解析出image数据和userData
     */
    
    public ImageCache parseValueUserData(byte[] buffer){
    	
    	if(buffer == null)
    		return null;
    	

    	ByteBuffer b = ByteBuffer.wrap(buffer);
    	int length = b.getInt();
    	
    	byte[] userData = null;
    	if(length != 0){
    		userData = new byte[length+1];
    		b.get(userData, 0, length);
    	}
    	
    	if(b.getInt() != CACHE_MAGIC)
    		return null;
    	int valueLen = b.getInt();
    	byte[] value = new byte[valueLen];
    	b.get(value);
    	this.userData = userData;
    	this.value = value;
    	return this;
    }





}
