package com.yoson.cache;

import android.os.Parcel;
import android.os.Parcelable;


/**
 * @description  配置object定义
 * 
 * @author yilong.yyl
 *
 * @date 2014年8月4日 下午2:59:59  
 * 
 */
public class ConfigObject implements Parcelable {
	
	public int blockSize;
	public boolean isCompress;
	public boolean isEncrypt;
	public boolean isRemovable;
	
	public ConfigObject(){
		blockSize = 2;     //默认初始化为2M
		isCompress = true;
		isEncrypt = true;
		isRemovable =true;
	}
	public ConfigObject(Parcel parcel) {
		blockSize = parcel.readInt();
		isCompress = parcel.readByte()!=0;
		isEncrypt = parcel.readByte()!=0;
		isRemovable = parcel.readByte()!=0;
	}
	
	@Override
	public int describeContents() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		// TODO Auto-generated method stub
		dest.writeInt(blockSize);
		dest.writeByte((byte)(isCompress ?1:0));
		dest.writeByte((byte)(isEncrypt ?1:0));
		dest.writeByte((byte)(isRemovable ?1:0));

	}
	public static final Parcelable.Creator<ConfigObject> CREATOR = new Parcelable.Creator<ConfigObject>(){   
		@Override   
		public ConfigObject createFromParcel(Parcel parcel) {//从Parcel中读取数据，返回Schedule对象   
		return new ConfigObject(parcel);   
		}
		@Override   
		public ConfigObject[] newArray(int size) {   
		return new ConfigObject[size];   
		} 
	};
}
