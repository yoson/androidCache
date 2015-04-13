package com.yoson.cache;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.os.Build;
import java.io.IOException;
import android.util.Log;

import com.taobao.statistic.TBS;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;


/**
 * 
 * 
 * 这个类负责主客户端用到的SO库管理，包括
 * a.加载
 * 从包大小和加载时间两方面考虑，arm架构的so文件在打包时仅在armeabi下提供一份。首先尝试用System.LoadLibray函数加载这个so库，对于这种情况库文件位置和版本都由系统帮你管理了。在某些armebi-v7a架构的手机，loadlibrary会因为不匹配arameabi而加载失败。
 * 这时尝试从程序的apk包中提取前述armeabi下的so文件，复制到程序的data目录下，尝试用System.Load方式加载，对于这种情况，需要做好管理版本。
 *
 * b.版本管理
 * version为正整数，当版本不兼容时，增加version的值。
 * 
 * @author xiaolan.wdj@taobao.com
 */

public class LoadSo {

	//below is the CPU string types
	private final static String ARMEABI = "armeabi"; //default
	private final static String X86 = "x86";
	private final static String MIPS = "mips";
	//private final static String ARMV7A = "armeabi-v7a";
	
	static Context mContext = null;
	 
	  
	private final static int EventID_SO_INIT = 21033;  //public static int EventID_SO_INIT = 21033; //SO加载的埋点
	final static String LOGTAG = "INIT_SO";
	
	
	public static void init(Context c)
	{
		mContext = c;
	}
	/**
	 * 完成加载so库的操作，先尝试用loadlibrary加载，如果失败则从apk中提取出armebi下的so,再加载。
	 * @param libname 库名字，例如webp，不必为libwep.so
	 * @param version 版本号，正整数，新的版本>老的版本
	 * */
	public static boolean initSo(String libName, int version) 
	{
		
		
		boolean InitSuc = false;
			
		//首先，通过System.loadLibrary方法，从libs下库加载
		try
		{
			
			
			Log.i(LOGTAG, "init libray: " + libName );
			System.loadLibrary(libName);
			Log.i(LOGTAG, "init libray sucess:" + libName);
			
			InitSuc = true;
		}
		catch(Exception e)
		{
			InitSuc = false;
			Log.e(LOGTAG, "init libray failed:" + e.getMessage());
			e.printStackTrace();
		}
		catch(java.lang.UnsatisfiedLinkError e2)
		{
			InitSuc = false;
			Log.e(LOGTAG, "init library failed(UnsatisfiedLinkError):" + e2.getMessage());
			e2.printStackTrace();
			
		}
		catch(java.lang.Error e3)
		{
			InitSuc = false;
			e3.printStackTrace();
			Log.e(LOGTAG, "init library failed(Error):" + e3.getMessage());
			
		}
		
		try
		{
		
			if(!InitSuc)
			{
				
				//从apk中提取的文件已经存在
				if(isExist(libName, version))
				{
					boolean res =  _loadUnzipSo(libName, version);
					if( res )
						return res;
					else
					{
						removeSoIfExit(libName,version);
						//以有的SO有问题，删除然后尝试再解
						TBS.Ext.commitEvent(EventID_SO_INIT, "the exist target so is bad lib:" + libName  );
					}
				}
				
				//从libs下加载失败,则从apk中读出SO，加载		
				String cpuType = _cpuType();
				if( cpuType.equalsIgnoreCase(MIPS) || cpuType.equalsIgnoreCase(X86))
				{
					Log.w(LOGTAG, "cpu type" + cpuType + " no so in libs");
					TBS.Ext.commitEvent(EventID_SO_INIT, "no so in libs for cpu:" + cpuType + " ,lib:" + libName );
					
				}
				else
				{
					try
					{
						InitSuc =  unZipSelectedFiles(libName, version);
					}
					catch( ZipException e )
					{
						e.printStackTrace();
						TBS.Ext.commitEvent(EventID_SO_INIT, "no so in libs for cpu:" + cpuType + " ,lib:" + libName );
					}
					catch( IOException e2)
					{
						e2.printStackTrace();
						TBS.Ext.commitEvent(EventID_SO_INIT, "no so in libs for cpu:" + cpuType + " ,lib:" + libName );
					}
				}
				
			}
		}
		catch(Exception e)
		{
			InitSuc = false;
			Log.e(LOGTAG, "init libray failed:" + e.getMessage());
			e.printStackTrace();
			TBS.Ext.commitEvent(EventID_SO_INIT, "unzip andload fail lib:" + libName + ",e=" + e.getMessage());
		}
		catch(java.lang.UnsatisfiedLinkError e2)
		{
			InitSuc = false;
			Log.e(LOGTAG, "init library failed(UnsatisfiedLinkError):" + e2.getMessage());
			e2.printStackTrace();
			TBS.Ext.commitEvent(EventID_SO_INIT, "unzip andload failed(UnsatisfiedLinkError) lib:" + libName + ",e=" + e2.getMessage());
			
		}
		catch(java.lang.Error e3)
		{
			InitSuc = false;
			Log.e(LOGTAG, "init library failed(Error):" + e3.getMessage());
			e3.printStackTrace();
			TBS.Ext.commitEvent(EventID_SO_INIT, "unzip andload failed(Error) lib:" + libName + ",e=" + e3.getMessage());
		}
		
		if(!InitSuc)
		{
			Log.e(LOGTAG, "initSo return false lib: " + libName);
		    TBS.Ext.commitEvent(EventID_SO_INIT, "initSo return false lib: " + libName + ",cputype:" +  _cpuType());
		}
		return InitSuc;
		
	}
	
	private static String _getFieldReflectively(Build build, String fieldName) {
		try {
			final Field field = Build.class.getField(fieldName);
			return field.get(build).toString();
		} catch (Exception ex) {
			return "Unknown";
		}
	}
	private static String _cpuType() {
		
		String abi = _getFieldReflectively(new Build(), "CPU_ABI");
		if (abi==null || abi.length()==0 || abi.equals("Unknown")) {
			abi = ARMEABI;
		}
		abi = abi.toLowerCase();
		return abi;
	}
	
	
	//拼装提取的so文件全名
	static String _targetSoFile(String libname, int version)
	{
		Context context = mContext;
		if(null==context)
			return "";
		
		String path = File.separator+"data"+File.separator+"data"+File.separator + context.getPackageName() + File.separator+"files";
		
		File f  = context.getFilesDir();
		if( f != null)
		{
			path = f.getPath();
		}
		return  path + File.separator+"lib" + libname +"bk"+ version +".so";
		
	}
	
	//检查这个so,是否已经获得,有则删除
	static void removeSoIfExit(String libname,int version)
	{
		
		String  file = _targetSoFile(libname, version);
		File a = new File(file);
		if( a.exists() )
		{
			Log.w(LOGTAG,"so already there:" + libname);
			a.delete();
		}
		
	}

	//是否SO已经提取
	static boolean isExist(String libname, int version)
	{
		
		String  file = _targetSoFile(libname, version);
		File a = new File(file);
		return a.exists();
				
	}
	
	
	//加载提取出来的SO文件
	static boolean _loadUnzipSo(String libname, int version)
	{
		boolean initSuc = false;
		try
		{
						
			//尝试libwebp.so
			Log.w(LOGTAG, "init my so libray:" + libname);
			if( isExist(libname,version))
			{
				System.load(_targetSoFile(libname,version));
			}
			else
			{
				Log.e(LOGTAG, "so not in fetched place:" + libname);
				TBS.Ext.commitEvent(EventID_SO_INIT, "so not in fetched place:" + libname);
			}
			initSuc = true;
		}
		catch(Exception e)
		{
			initSuc = false;
			Log.e(LOGTAG, "init so failed library:" + libname + ",e=" + e.getMessage());
			e.printStackTrace();
			TBS.Ext.commitEvent(EventID_SO_INIT, "so not in fetched place:" + libname + ",e=" + e.getMessage());
		}
		catch(java.lang.UnsatisfiedLinkError e2)
		{
			initSuc = false;
			Log.e(LOGTAG, "init so failed failed(UnsatisfiedLinkError):" + e2.getMessage());
			e2.printStackTrace();
			TBS.Ext.commitEvent(EventID_SO_INIT, "so not in fetched place:" + libname + ",e=" + e2.getMessage());
			
		}
		catch(java.lang.Error e3)
		{
			initSuc = false;
			e3.printStackTrace();
			TBS.Ext.commitEvent(EventID_SO_INIT, "so not in fetched place:" + libname + ",e=" + e3.getMessage());
		}
		return initSuc;
	}
					
	static boolean unZipSelectedFiles(String libname, int version ) throws ZipException, IOException 
    {
				
		String sourcePath = "lib/armeabi/lib"+ libname +".so";
		try 
		{
			String zipPath = "";
			Context context = mContext;
			if( context == null)
			{
				TBS.Ext.commitEvent(EventID_SO_INIT, "TaoApplication.context is null lib:" + libname );
				return false;
			}
					
			ApplicationInfo aInfo = context.getApplicationInfo();
			if( null != aInfo)
			{
				zipPath = aInfo.sourceDir;
			}
				
			
			
			ZipFile zf;
			zf = new ZipFile(zipPath);
		
			for (Enumeration<?> entries = zf.entries(); entries.hasMoreElements();) 
			{
				ZipEntry entry = ((ZipEntry) entries.nextElement());
				if (entry.getName().startsWith(sourcePath)) 
				{
		
					InputStream in = null;
					FileOutputStream os = null;
                    FileChannel channel = null;
                    int total = 0;
					try 
					{
						
						//确保老的库被删除，这代码应该是不跑到的，因为最开始需要检查只有在不存在的情况下，才提取
						removeSoIfExit(libname, version);
						
						// copy 文件
						in = zf.getInputStream(entry);
						os =  context.openFileOutput("lib" + libname + "bk"+version+ ".so",
								Context.MODE_PRIVATE);						
						channel = os.getChannel();
						
						byte[] buffers = new byte[1024];
						int realLength;
						
						while ((realLength = in.read(buffers)) > 0) {
							//os.write(buffers);
							channel.write(ByteBuffer.wrap(buffers, 0, realLength));
							total += realLength;
							
						}
						Log.i(LOGTAG, "so filesize:" + total);
					} 
					finally 
					{
						if (in != null) 
						{
							try 
							{						
								in.close();
							} 
							catch (Exception e) {
								e.printStackTrace();
							}
						}
						
						
						if (channel != null) try {
							channel.close();
							} catch (Exception e) {
							e.printStackTrace();
						}
						
						
						if (os != null) try {
							os.close();
						} catch (Exception e) {
							e.printStackTrace();
						}
						
						if(zf != null)
						{
							zf.close();
							zf = null;
						}
					}
					
					if( total > 0)
					{
						return _loadUnzipSo(libname, version);
					}
					else
					{
						TBS.Ext.commitEvent(EventID_SO_INIT, "unzip fail:" + libname );
						return false;
					}
				}
			}
		} catch (java.io.IOException e) {
			e.printStackTrace();
			TBS.Ext.commitEvent(EventID_SO_INIT, "unzip fail:" + libname + ",e=" + e.getMessage());
			
		}
		return false;
    }
  	
}
