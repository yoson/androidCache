/**
 * @description
 * 
 * @author yilong.yyl
 *
 * @date 2015年3月26日 下午3:32:08  
 * 
 */
package com.yoson.cache;

import java.io.File;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import com.alibaba.mtl.appmonitor.AppMonitor;

/**
 * @description
 * 
 * @author yilong.yyl
 *
 * @date 2015年3月26日 下午3:32:08  
 * 
 */
public class CacheRebootReceiver extends BroadcastReceiver {

	/* (non-Javadoc)
	 * @see android.content.BroadcastReceiver#onReceive(android.content.Context, android.content.Intent)
	 */
	@Override
	public void onReceive(Context context, Intent intent) {
		// TODO Auto-generated method stub
		try{
			File cacheFile = context.getExternalCacheDir();
			String appDataDir = cacheFile.getAbsolutePath()+File.separator+"apicache";
			File file =new File(appDataDir);  
			if  (file.exists()  && file.isDirectory())    
			{ 
				File flag=new File(file,"flag");
				if(flag.exists() && !flag.isDirectory()){
					flag.delete();
					//TBS.Ext.commitEvent("Page_Store",65101, "create", 4, null, null);
					AppMonitor.Alarm.commitFail("Page_Store", "newCache", "code4", "failReboot");
				}
			}
		}catch(Exception e){
			Log.e("newCache","rebootReceiver failed!");
		}
	}

}
