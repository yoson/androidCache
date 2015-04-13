package com.taobao.nbcache;

import com.taobao.nbcache.ConfigObject;

interface IMultiNBCacheService{
	boolean init(String dir);
	byte[] read(String blockName,String key);
	String[] getAllKey(String blockName);
	boolean write(String blockName, String key, in byte[] value,boolean isOverWrite, int compressMode);
	boolean remove(String blockName,String key);
	boolean removeBlock(String blockName);
	boolean setBlockConfig(String blockName,in ConfigObject co);
	int[] getCatalog(String blockName, String key);
	boolean writeCatalog(String blockName,String key,int catalog,in byte[] value,boolean isOverWrite, int compressMode);
	boolean removeCatalog(String blockName,String key,int catalog);
	boolean closeBlock(String blockName);
}