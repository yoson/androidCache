package com.yoson.cache;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import android.content.Context;
import android.os.RemoteException;
import android.text.TextUtils;
import android.util.Log;


//import com.alibaba.mtl.appmonitor.AppMonitor;
//import com.alibaba.mtl.appmonitor.model.DimensionSet;
//import com.alibaba.mtl.appmonitor.model.MeasureSet;
import com.taobao.android.service.Services;
import com.taobao.nbcache.IMultiNBCacheService;

/**
 * @description 管理类，请业务方调用直接调用该类
 * 
 * @author yilong.yyl
 *
 * @date 2014年8月4日 下午3:00:08  
 * 
 */
public class MultiNBCache {

    private static final String TAG = "newCache";
    
    private static Context mContext;
    private static String mCacheDir = "apicache";
    private static final String mBlockName = "defaultBlock";
    private static boolean downGradeFlag = true;
    private static IMultiNBCacheService mService = null;
	
    /**
     * 初始化cache，耗时操作，请勿在main线程调用，建议使用AsyncTask进行初始化
     * 
     */
	public static synchronized boolean init(String cacheDir,Context context) {
		if(!downGradeFlag){
			if(mService != null)
				return true;
			if(mContext == null && context == null){
				//throw new RuntimeException("The context is null, you have to set the context !");
				return false;
			}
			if(TextUtils.isEmpty(cacheDir) || cacheDir.length()>20){
				Log.e(TAG, "cacheDir is false!");
				return false;
			}
			String state = android.os.Environment.getExternalStorageState();
	        if (state != null && state.equals(android.os.Environment.MEDIA_MOUNTED)){
	        	mContext = context.getApplicationContext();
	    		//mCacheDir = cacheDir;
	    		mService = Services.get(mContext, IMultiNBCacheService.class);
	    		if(mService != null) {
	    			Log.i(TAG, "bindService is successed!");
	    			try {
	    				mService.init(mCacheDir);
	    			} catch (RemoteException e1) {
	    				e1.printStackTrace();
	    			}
	    			return true;
	    		}
	    		else {
	    			Log.e(TAG, "bindService is failed!");
	    			return false;
	    		}
	        }else{
	        	Log.e(TAG, "ExternalStorage is not aviable! all cache is failed");
	        	return false;
	        }
		}else
			return false;
	}
	
	public static boolean isInited() {
		return mService != null;
	}
	/**
	 * MD5 key默认加密。32位
	 * 
	 * @param inStr
	 * @return
	 */
	private static String MD5(String input) {
		try {
			MessageDigest messageDigest = MessageDigest.getInstance("MD5");
			byte[] inputByteArray = input.getBytes();
			messageDigest.update(inputByteArray);
			byte[] resultByteArray = messageDigest.digest();
			return byteArrayToHex(resultByteArray);
		} catch (NoSuchAlgorithmException e) {
			return null;
		}
	}
	
	private static String byteArrayToHex(byte[] byteArray) {
		char[] hexDigits = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
				'A', 'B', 'C', 'D', 'E', 'F' };
		char[] resultCharArray = new char[byteArray.length * 2];
		int index = 0;
		for (byte b : byteArray) {
			resultCharArray[index++] = hexDigits[b >>> 4 & 0xf];
			resultCharArray[index++] = hexDigits[b & 0xf];

		}
		return new String(resultCharArray);
	}
    /**
     * 读取加密key的缓存数据
     * 
     */
    public static byte[] read(String blockName, String key){
    	if(TextUtils.isEmpty(key)){
    		Log.w(TAG, "the key is null");
    		return null;
    	}
    	else
    		return readWithNoEncrypt(blockName,MD5(key));
    }
    
    /**
     * 获取图片的二级缓存索引
     * 
     */
    public static int[] getCatalog(String blockName, String key){
    	
    	if(blockName == null || TextUtils.isEmpty(blockName))
    		blockName = mBlockName;
		if(key == null || blockName.length()>20) {
			Log.w(TAG, "the key is null");
			return null;
		}
    	if(mService == null )
    		//if(!init(mCacheDir,mContext))
    		return null;
    	
    	int[] indexs = null;
    	if(mService != null)
    		try {
    			indexs = mService.getCatalog(blockName, key);
    		} catch (RemoteException e) {
    			e.printStackTrace();
    		}
    	return indexs;
    }

    /**
     * 读取未加密key的缓存数据，仅限离线操作方使用，其他业务建议使用加密
     * 
     */
	public static byte[] readWithNoEncrypt(String blockName, String key){
	    	
	    	if(blockName == null || TextUtils.isEmpty(blockName))
	    		blockName = mBlockName;
			if(key == null || blockName.length()>20) {
				Log.w(TAG, "the key is null");
				return null;
			}
	    	if(mService == null )
	    		//if(!init(mCacheDir,mContext))
	    		return null;
	    	
	    	byte[] data = null;
	    	if(mService != null)
	    		try {
	    			data = mService.read(blockName, key);
	    		} catch (RemoteException e) {
	    			e.printStackTrace();
	    		}
	    	return data;
	    }

    /**
     * 删除某个块内的某条缓存记录
     * 
     */
	public static boolean remove(String blockName, String key){
    	if(TextUtils.isEmpty(key)){
    		Log.w(TAG, "the key is null");
    		return false;
    	}
    	else
    		return removeWithNoEncrypt(blockName,MD5(key));
	}
    public static boolean removeWithNoEncrypt(String blockName, String key){
    	
    	boolean ret = false;
    	if(blockName == null || TextUtils.isEmpty(blockName))
    		blockName = mBlockName;
    	if(key == null || blockName.length()>20) {
			Log.w(TAG, "the key is null || blockName length is [0,20]");
			return false;
		}
    	if(mService == null )
    		//if(!init(mCacheDir,mContext))
    		return false;
    	if(mService != null)
    		try {
    			ret = mService.remove(blockName,key);
    		} catch (RemoteException e) {
    			e.printStackTrace();
    		}
    	return ret;
    	
    }

    /**
     * key value 全部加密方式写入缓存，其中key为md5加密，无法解密回原key
     * isOverWrite 是否覆盖写
     * compressMode -1代表默认整个block，0代表不压缩，1代表gzip压缩
     */
    public static boolean write(String blockName, String key,byte[] value ,boolean isOverWrite,int compressMode){
    	if(TextUtils.isEmpty(key)){
    		Log.w(TAG, "the key is null");
    		return false;
    	}
    	else
    		return writeWithNoEncrypt(blockName, MD5(key),value ,isOverWrite,compressMode);
    }
    
    
    /**
     * <br>写入图片缓存</br>
     * <br>parameter:ImageCache<br>
     * return:boolean
     */
    public static boolean writeImage(ImageCache ic){
    	if(ic.blockName == null || TextUtils.isEmpty(ic.blockName))
    		ic.blockName = mBlockName;
    	if(ic.key == null || ic.blockName.length()>20 || ic.value == null) {
    		throw new NullPointerException("key and value must be not null.");
		}
    	if(mService == null)
    		//if(!init(mCacheDir,mContext))
    		return false;
    	boolean ret = false;
    	if(mService != null)
    		try {
    			ret = mService.writeCatalog(ic.blockName, ic.key, ic.index, ic.joinValueUserData(), ic.isOverWrite, ic.compressMode);
    		} catch (RemoteException e) {
    			e.printStackTrace();
    		}
    	return ret;
    }
    
    /**
     * <br>读取图片缓存</br>
     * <br>parameter:ImageCache<br>
     * return:ImageCache
     */
    public static ImageCache readImage(ImageCache ic){
    	
    	if(ic.blockName == null || TextUtils.isEmpty(ic.blockName))
    		ic.blockName = mBlockName;
		if(ic.key == null || ic.blockName.length()>20) {
			throw new NullPointerException("key must be not null~");
		}
    	if(mService == null )
    		//if(!init(mCacheDir,mContext))
    		return null;
    	
    	if(mService != null)
    		try {
    			byte[] data = mService.read(ic.blockName, ic.key+ic.index);
    			ic.parseValueUserData(data);
    		} catch (RemoteException e) {
    			e.printStackTrace();
    		}
    	return ic;
    }

    
    /**
     * 写入图片缓存，其中key为md5加密，无法解密回原key
     * isOverWrite 是否覆盖写
     * compressMode -1代表默认整个block，0代表不压缩，1代表gzip压缩
     */
    
    public static boolean writeCatalog(String blockName, String key,int catalog,byte[] value ,boolean isOverWrite,int compressMode){
    	if(blockName == null || TextUtils.isEmpty(blockName))
    		blockName = mBlockName;
    	if(key == null || blockName.length()>20 || value == null) {
			Log.w(TAG, "the key is null || blockName is over length,must be [0,20]");
			return false;
		}
    	if(mService == null)
    		//if(!init(mCacheDir,mContext))
    		return false;
    	boolean ret = false;
    	if(mService != null)
    		try {
    			ret = mService.writeCatalog(blockName, key, catalog, value, isOverWrite, compressMode);
    		} catch (RemoteException e) {
    			e.printStackTrace();
    		}
    	return ret;
    }
    
    public static boolean removeCatalog(String blockName, String key,int catalog){
    	if(blockName == null || TextUtils.isEmpty(blockName))
    		blockName = mBlockName;
    	if(key == null || blockName.length()>20) {
			Log.w(TAG, "the key is null || blockName is over length,must be [0,20]");
			return false;
		}
    	if(mService == null)
    		//if(!init(mCacheDir,mContext))
    		return false;
    	boolean ret = false;
    	if(mService != null)
    		try {
    			mService.removeCatalog(blockName, key, catalog);
    		} catch (RemoteException e) {
    			e.printStackTrace();
    		}
    	return ret;
    }
    /**
     * key不加密，value加密方式写入缓存，仅限离线操作使用
     * isOverWrite 是否覆盖写
     * compressMode -1代表默认整个block，0代表不压缩，1代表gzip压缩
     */
    public static boolean writeWithNoEncrypt(String blockName, String key,byte[] value ,boolean isOverWrite,int compressMode){
    	if(blockName == null || TextUtils.isEmpty(blockName))
    		blockName = mBlockName;
    	if(key == null || blockName.length()>20 || value == null) {
			Log.w(TAG, "the key is null || blockName is over length,must be [0,20]");
			return false;
		}
    	if(mService == null)
    		//if(!init(mCacheDir,mContext))
    		return false;
    	boolean ret = false;
    	if(mService != null)
    		try {
    			ret = mService.write(blockName,key,value,isOverWrite,compressMode);
    		} catch (RemoteException e) {
    			e.printStackTrace();
    		}
    	return ret;
    }

    
    /**
     * 初始化block块，包括设置缓存block的大小、压缩、加密、是否可以被移除信息
     * 不建议将setBlockConfig跟read write混在一起进行操作，因为set可能耗时较长，这时候就会阻塞读写操作，导致性能问题，最好的方式就是在初始化是set掉，之后读写时不再重写设置
     *
     */
   public static boolean setBlockConfig(String blockName,ConfigObject co){
   	   if(blockName == null || TextUtils.isEmpty(blockName))
   			blockName = mBlockName;
	   if(blockName.length()>20) {
			Log.w(TAG, "the blockName over length must be [0,20]");
			return false;
		}
	   if(null == co || co.blockSize > 100 || co.blockSize < 2){
		   Log.e(TAG, "blockSize must be in [2,20].");
		   return false;
	   }
	   else if(mService == null )
	   		//if(!init(mCacheDir,mContext))
	   		return false;
	   	boolean ret = false;
	   	if(mService != null)
	   		try {
	   			ret = mService.setBlockConfig(blockName, co);
	   		} catch (RemoteException e) {
	   			e.printStackTrace();
	   		}
	   	return ret;
   }

    /**
     * 清除整个block缓存的所有记录，但并不会删除文件和索引
     *
     */
    public static boolean removeBlock(String blockName){
       if(blockName == null || TextUtils.isEmpty(blockName))
    		blockName = mBlockName;
 	   if(blockName.length()>20) {
 			Log.w(TAG, "the blockName is over length");
 			return false;
 		}
	   if(mService == null )
		   //if(!init(mCacheDir,mContext))
	   		return false;
    	boolean ret = false;
    	if(mService != null)
    		try {
    			ret = mService.removeBlock(blockName);
    		} catch (RemoteException e) {
    			e.printStackTrace();
    		}
    	return ret;
    }
    
    /**
     * 得到整个block所有的key，遍历得到，不保证按照写入顺序
     *
     */
    public static String[] getAllKey(String blockName){	
    	if(blockName == null || TextUtils.isEmpty(blockName))
    		blockName = mBlockName;
    	if(mService == null )
    		//if(!init(mCacheDir,mContext))
    		return null;
    	
    	String[] data = null;
    	if(mService != null)
    		try {
    			data = mService.getAllKey(blockName);
    		} catch (RemoteException e) {
    			e.printStackTrace();
    		}
    	return data;
    }
    /**
     * 清除整个block缓存的所有记录，但并不会删除文件和索引
     *
     */
    public static boolean closeBlock(String blockName){
       if(blockName == null || TextUtils.isEmpty(blockName))
    		blockName = mBlockName;
 	   if(blockName.length()>20) {
 			Log.w(TAG, "the blockName is over length");
 			return false;
 		}
	   else if(mService == null )
		   //if(!init(mCacheDir,mContext))
	   		return false;
    	boolean ret = false;
    	if(mService != null)
    		try {
    			ret = mService.closeBlock(blockName);
    		} catch (RemoteException e) {
    			e.printStackTrace();
    		}
    	return ret;
    }
}
