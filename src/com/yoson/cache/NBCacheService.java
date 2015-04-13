package com.yoson.cache;

import java.io.UnsupportedEncodingException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.IBinder;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;

import com.alibaba.fastjson.JSON;
import com.taobao.nbcache.IMultiNBCacheService;

/**
 * @description
 * 
 * @author yilong.yyl
 *
 * @date 2014年8月4日 下午2:59:27  
 * 
 */
public class NBCacheService extends Service{
	private static String mCacheDir = "apicache";
	private static NBCacheBinder mNBCacheBinder;
	private static SharedPreferences preferences = null;
	private static Editor editor = null;
	//private CacheStorageCache mCacheImps;
	private ConcurrentHashMap<String,CacheImp> chMap = new ConcurrentHashMap<String, CacheImp>();
	private ConfigObject co = null;
	public static final String tag = "newCache";
	public static boolean exFlag = false;
	
	@Override
	public void onCreate() {
		preferences = PreferenceManager.getDefaultSharedPreferences(this.getApplicationContext());
		editor=preferences.edit();
		//mCacheImps = CacheStorageCache.getInstance();
		co = new ConfigObject();
		mNBCacheBinder = new NBCacheBinder(this);
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		return super.onStartCommand(intent, flags, startId);
	}
	
	@Override
	public void onDestroy(){

		for(Map.Entry<String,CacheImp> cim: chMap.entrySet() ){
	        cim.getValue().close();
		}
		chMap.clear();
	}


	@Override
	public IBinder onBind(Intent intent) {
		return mNBCacheBinder;
	}
	
	
	
	@Override
	public boolean onUnbind(Intent intent) {
		// TODO Auto-generated method stub
		Log.e("newCache", "service is unbind");
		return super.onUnbind(intent);
	}

	/**
	 * 在Service中维护一个CacheImp对象的HashMap，每次不用重复new。
	 * 如果存在，则从HashMap中根据id字段进行返回，
	 * 如果不存在的，根据id字段进行创建
	 * @param id
	 * @return CacheImp
	 */
	private CacheImp getCacheImp(String blockName) {
		
		String configStr = preferences.getString(blockName, null);
		if(configStr != null){
			co = JSON.parseObject(configStr, ConfigObject.class);
		}
		else{
			editor.putString(blockName, JSON.toJSONString(co));
			editor.commit();
		}
		
		if(chMap.containsKey(blockName))
			return chMap.get(blockName);
		else{
			if(chMap.size()>10){
				for(Map.Entry<String,CacheImp> cim: chMap.entrySet() ){
					Log.e(tag, "instance > 10,has been GC");
			        cim.getValue().close();
				}
				chMap.clear();
			}
			if(exFlag)
				return null;
			CacheImp ciImp = new CacheImp(this.getApplicationContext(),blockName, mCacheDir, getPackageName(), co);
			if(ciImp.init()){
				chMap.put(blockName, ciImp);
				return ciImp;
			}else{
				exFlag = true; 
				return null;
			}
		}
	}

	
	class NBCacheBinder extends IMultiNBCacheService.Stub {
		NBCacheService  mService;
		
		public NBCacheBinder(NBCacheService service) {
			mService = service;
		}


		@Override
		public boolean init(String dir) throws RemoteException {
			mCacheDir = dir;
			return true;
		}

		@Override
		public synchronized byte[] read(String blockName, String key)
				throws RemoteException {
			if(mService != null){
				CacheImp cimp = mService.getCacheImp(blockName);
				if(cimp != null)
					return cimp.read(key);
			}
			return null;
		}
		
		@Override
		public synchronized String[] getAllKey(String blockName)
				throws RemoteException {
			if(mService != null){
				CacheImp cimp = mService.getCacheImp(blockName);
				if(cimp != null)
					return cimp.getAllKey();
			}
			return null;
		}

		@Override
		public synchronized boolean write(String blockName, String key, byte[] value, boolean isOverWrite,int compressMode) throws RemoteException {
			// TODO Auto-generated method stub
			boolean flag = false;
			if(mService!= null && key != null) {
				CacheImp cimp = mService.getCacheImp(blockName);
				if(cimp != null)
					flag = cimp.write(key, value, isOverWrite,compressMode);
//				if(!flag){
//					mService.getCacheImp(blockName).close();
//					chMap.remove(blockName);
//					//写入失败则尝试第二次写入，如果再次失败，销毁缓存，重建cache
//					flag = mService.getCacheImp(blockName).write(key, value, isOverWrite,compressMode);
//					if(!flag){
//						mService.getCacheImp(blockName).clear();
//						mService.getCacheImp(blockName).close();
//						chMap.remove(blockName);
//						flag = mService.getCacheImp(blockName).write(key, value, isOverWrite,compressMode);
//					}
//				}
//				if(TextUtils.equals(blockName, "carts")){
//					 //FileOutputStream out = null;
//					 FileWriter writer = null;
//					 try{
//						File paFile = Environment.getExternalStorageDirectory();
//						String pathString = paFile.getAbsolutePath()+File.separator+"taobao"+File.separator +"apiCache"+File.separator+getPackageName()+File.separator+"carts_java.log";
//						
//						writer = new FileWriter(pathString, true);
//				        writer.write(key+"\n");
//
//				        writer.write(new String(value)+"\n");
//					 }catch (Exception e) {
//						// TODO: handle exception
//						 e.printStackTrace();
//					}finally{
//						 try {
//							 writer.close();
//						} catch (IOException e) {
//							// TODO Auto-generated catch block
//							e.printStackTrace();
//						}   
//					}
//    			}
			}
			return flag;
		}

		@Override
		public synchronized boolean remove(String blockName, String key)
				throws RemoteException {
			// TODO Auto-generated method stub
			String configStr = preferences.getString(blockName, null);
			
			if(configStr != null){
				co = JSON.parseObject(configStr, ConfigObject.class);
			}
			if(mService != null && co.isRemovable){
				CacheImp cimp = mService.getCacheImp(blockName);
				if(cimp != null)
					return cimp.remove(key);
			}
			return false;
		}

		@Override
		public synchronized boolean removeBlock(String blockName) throws RemoteException {
			// TODO Auto-generated method stub
			boolean flagC = false;
			String configStr = preferences.getString(blockName, null);
			if(configStr != null){
				co = JSON.parseObject(configStr, ConfigObject.class);
			}
			if(mService != null && co.isRemovable){
				CacheImp cimp = mService.getCacheImp(blockName);
				if(cimp != null){
					 flagC = cimp.clear();
				}
			}
			return flagC;
		}

		@Override
		public synchronized boolean setBlockConfig(String blockName, ConfigObject co)
				throws RemoteException {
			// TODO Auto-generated method stub
			
			editor.putString(blockName, JSON.toJSONString(co));
			editor.commit();
			CacheImp cimp = mService.getCacheImp(blockName);
			if(cimp != null)
				return cimp.reSetMaxSize(co.blockSize);
			return false;
		}

		@Override
		public synchronized boolean closeBlock(String blockName){
			if(mService != null){
				CacheImp cimp = mService.getCacheImp(blockName);
				if(cimp != null)
					return cimp.close();
			}
			return false;
		}


		/* (non-Javadoc)
		 * @see com.taobao.nbcache.IMultiNBCacheService#getCatalog(java.lang.String, java.lang.String)
		 */		
		@Override
		public synchronized int[] getCatalog(String blockName, String key)
				throws RemoteException {
			// TODO Auto-generated method stub
			byte[] bs = read(blockName,key);
			int[] is = null;
			try {
				if(bs != null){
					String str = new String(bs,"utf-8");
					//Log.e(tag, "strGetCatalog ="+str);
					if(!TextUtils.isEmpty(str)){
						String[] strArray = str.split("\\|");
						is = new int[strArray.length];
						for(int i=0;i<strArray.length;i++){
							//Log.e(tag, "strArray = "+strArray[i]);
							is[i] = Integer.valueOf(strArray[i]);
							//Log.e(tag, "index ="+is[i]);
						}
					}
				}
			} catch (UnsupportedEncodingException e) {
				// TODO Auto-generated catch block
				remove(blockName,key);
				Log.e(tag, "catalog is UnsupportedEncodingException,has been removed !");
			} catch(NumberFormatException e){
				// TODO Auto-generated catch block
				remove(blockName,key);
				Log.e(tag, "catalog is NumberFormatException,has been removed !");
			}
			return is;
		}


		/* (non-Javadoc)
		 * @see com.taobao.nbcache.IMultiNBCacheService#writeCatalog(java.lang.String, java.lang.String, int, byte[], boolean, int)
		 */
		@Override
		public synchronized boolean writeCatalog(String blockName, String key, int catalog,
				byte[] value, boolean isOverWrite, int compressMode)
				throws RemoteException {
			// TODO Auto-generated method stub
			boolean retA = false,retB = true;
			
			//Log.e(tag, "blockName ="+blockName+",key="+key+",catalog="+catalog);
			retA = write(blockName,key+catalog,value,isOverWrite,compressMode);
			if(retA){
				byte[] bs = read(blockName+"Index",key);
				String str = "";
				boolean existFlag = false;
				if(bs != null){
					try {
						str = new String(bs,"utf-8");
						//Log.e(tag, "writeStr="+str);
						if(!TextUtils.isEmpty(str)){
							String[] strArray = str.split("\\|");
							for(String str1 : strArray){
								if(str1.equalsIgnoreCase(String.valueOf(catalog)))
									existFlag = true;
							}
						}
					} catch (UnsupportedEncodingException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					str += "|";
				}
				
				try {
					if(!existFlag){
						str += catalog;
						//Log.e(tag, "writeStr+ ="+str);
						retB = write(blockName+"Index", key, str.getBytes("utf-8"), true, -1);
//						if(!retB){
//							Log.e(tag, key +"index write failed");
//						}
					}
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			return retA && retB;
		}


		/* (non-Javadoc)
		 * @see com.taobao.nbcache.IMultiNBCacheService#removeCatalog(java.lang.String, java.lang.String)
		 */
		@Override
		public synchronized boolean removeCatalog(String blockName, String key, int catalog)
				throws RemoteException {
			// TODO Auto-generated method stub
			boolean retA = true,retB = true;
			
			byte[] bs = read(blockName+"Index",key);
			String str = "";
			boolean existFlag = false;
			StringBuilder sb = new StringBuilder();
			if(bs != null){
				try {
					str = new String(bs,"utf-8");
					//Log.e(tag, "removeCataStr = "+str);
					if(!TextUtils.isEmpty(str)){
						String[] strArray = str.split("\\|");
						for(String str1 : strArray){
							if(str1.equalsIgnoreCase(String.valueOf(catalog)))
								existFlag = true;
							else{
								sb.append(str1);
								sb.append("|");
							}
						}
					}
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			try {
				//Log.e(tag, sb.toString());
				if(sb.length()>0){
					str = sb.substring(0, sb.length()-2);
					//Log.e(tag, str);
					if(existFlag)
						retA = write(blockName+"Index", key, str.getBytes("utf-8"), true, -1);
				}else
					remove(blockName+"Index", key);
			} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
			}
			
			retB = remove(blockName,key+catalog);
			return retA && retB;
		}
		
	}
	
}
