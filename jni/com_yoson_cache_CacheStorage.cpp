#include <stdio.h>
#include <stdlib.h>

#include <jni.h>
#include <android/log.h>
#include "com_yoson_cache_CacheStorage.h"
#include "mStore.h"

/*
 * Class:     com_yoson_cache_CacheStorage
 * Method:    open
 * Signature: (Ljava/lang/String;Ljava/lang/String;III)Z
 */
JNIEXPORT jobject JNICALL Java_com_yoson_cache_CacheStorage_open
  (JNIEnv *env, jobject object, jstring name, jstring path, jlong size, jint type, jboolean compressMode){

	    const char *nameStr = env->GetStringUTFChars(name,0);
		const char *pathStr = env->GetStringUTFChars(path,0);
		cache_type ct;
		if(type == 0){
			ct = CACHE_NOTING;
		}else{
			ct = CACHE_FIFO;
		}
		mStore *store = new mStore(nameStr,pathStr,size,ct,compressMode);
		jobject ptr = env->NewDirectByteBuffer(store, 4);
		env->ReleaseStringUTFChars(name,nameStr);
		env->ReleaseStringUTFChars(path,pathStr);
		return ptr;

}

/*
 * Class:     com_yoson_cache_CacheStorage
 * Method:    insert
 * Signature: (Ljava/lang/String;ILjava/lang/String;IJZI)Z
 */
JNIEXPORT jboolean JNICALL Java_com_yoson_cache_CacheStorage_insert
  (JNIEnv *env, jobject object, jbyteArray key, jbyteArray value, jboolean isoverwr, jint iscmp, jobject ptr){
	//__android_log_print(ANDROID_LOG_INFO, "newCache", "in insert");
	void * keyPtr = env->GetByteArrayElements(key, 0);
	void * valuePtr = env->GetByteArrayElements(value,0);
	//__android_log_print(ANDROID_LOG_INFO, "newCache", "insert key");
	jint keyLen = env->GetArrayLength(key);
	jint valueLen = env->GetArrayLength(value);

	//__android_log_print(ANDROID_LOG_INFO, "newCache", "insert value");
	mStore *store = (mStore *) env->GetDirectBufferAddress(ptr);
	//__android_log_print(ANDROID_LOG_INFO, "newCache", " insert new store");
	jboolean flag = store->insert(keyPtr,keyLen,valuePtr,valueLen,isoverwr,iscmp);
	if(!flag){
		flag = store->insert(keyPtr,keyLen,valuePtr,valueLen,isoverwr,iscmp);
		//__android_log_print(ANDROID_LOG_INFO, "JNIMsg", "write failed,try again!");
	}
	//__android_log_print(ANDROID_LOG_INFO, "newCache", "in insert succe");
	env->ReleaseByteArrayElements(key,(jbyte *)keyPtr,0);
	env->ReleaseByteArrayElements(value,(jbyte *)valuePtr,0);
	//__android_log_print(ANDROID_LOG_INFO, "newCache", "in insert final");
	return flag;
}

/*
 * Class:     com_yoson_cache_CacheStorage
 * Method:    delete
 * Signature: (Ljava/lang/String;I)Z
 */
JNIEXPORT jboolean JNICALL Java_com_yoson_cache_CacheStorage_delete
  (JNIEnv *env, jobject object, jbyteArray key, jobject ptr){
	void * keyPtr = env->GetByteArrayElements(key, 0);
	jint keyLen = env->GetArrayLength(key);
	mStore *store = (mStore *) env->GetDirectBufferAddress(ptr);
	jboolean flag = store->remove(keyPtr,keyLen);
	env->ReleaseByteArrayElements(key,(jbyte *)keyPtr,0);
	return flag;
}

/*
 * Class:     com_yoson_cache_CacheStorage
 * Method:    select
 * Signature: (Ljava/lang/String;I)Ljava/lang/String;
 */
JNIEXPORT jbyteArray JNICALL Java_com_yoson_cache_CacheStorage_select
  (JNIEnv *env, jobject object, jbyteArray key, jobject ptr){
	//__android_log_print(ANDROID_LOG_INFO, "newCache", "read in");
	void * keyPtr = env->GetByteArrayElements(key, 0);
	jint keyLen = env->GetArrayLength(key);
	//__android_log_print(ANDROID_LOG_INFO, "newCache", "get key");
	void * value = NULL;
	//__android_log_print(ANDROID_LOG_INFO, "newCache", "void *");
	void ** bPtr = &value;
	//__android_log_print(ANDROID_LOG_INFO, "newCache", "void **");
	mStore *store = (mStore *) env->GetDirectBufferAddress(ptr);
	//__android_log_print(ANDROID_LOG_INFO, "newCache", "ptr");
	jint size = store->get(keyPtr,keyLen,bPtr);
//	char tempChar[10];
//	sprintf(tempChar, "%d", size);
//	const char* a= tempChar;
//	__android_log_print(ANDROID_LOG_INFO, "newCache", a);
	env->ReleaseByteArrayElements(key,(jbyte *) keyPtr,0);
	//__android_log_print(ANDROID_LOG_INFO, "newCache", "free keyPtr");
	//__android_log_print(ANDROID_LOG_INFO, "newCache", (const char *)*bPtr);

	if(size>0){

		if(bPtr == NULL || *bPtr == NULL){
			__android_log_print(ANDROID_LOG_INFO, "newCache", "uncompress failed!");
			return NULL;
		}
		//__android_log_print(ANDROID_LOG_INFO, "newCache", "in size >0");
		jbyteArray array = env->NewByteArray(size);
		//__android_log_print(ANDROID_LOG_INFO, "newCache", "new ByteArray");

		jbyte * body = (jbyte *)*bPtr;
		//__android_log_print(ANDROID_LOG_INFO, "newCache", "new body");
		env->SetByteArrayRegion(array,0,size,body);
		//__android_log_print(ANDROID_LOG_INFO, "newCache", "new ByteArray");
		free(body);
		//__android_log_print(ANDROID_LOG_INFO, "newCache", "free body");
		//env->ReleaseByteArrayElements(array,body,0);
		//__android_log_print(ANDROID_LOG_INFO, "newCache", "read succ");
		return array;

	}
	else{
//		__android_log_print(ANDROID_LOG_INFO, "newCache", "size = -1");
//		__android_log_print(ANDROID_LOG_INFO, "newCache", "size = -1,free");
		return NULL;
	}


}

/*
 * Class:     com_yoson_cache_CacheStorage
 * Method:    close
 * Signature: ()Z
 */
JNIEXPORT jboolean JNICALL Java_com_yoson_cache_CacheStorage_close
  (JNIEnv *env, jobject object, jobject ptr){
	mStore *store = (mStore *) env->GetDirectBufferAddress(ptr);
	if(store == NULL)
		return true;
	jboolean flag = store->close();
	//__android_log_print(ANDROID_LOG_INFO, "newCache", "close");
	if(store != NULL)
		free(store);
	return true;
}

/*
 * Class:     com_yoson_cache_CacheStorage
 * Method:    destory
 * Signature: ()Z
 */
JNIEXPORT jboolean JNICALL Java_com_yoson_cache_CacheStorage_destory
  (JNIEnv *env, jobject object, jobject ptr){
	mStore *store = (mStore *) env->GetDirectBufferAddress(ptr);
	//jboolean flagA = store->close();
	jboolean flag = store->clearAll();
//	if(store != NULL)
//		free(store);
	//__android_log_print(ANDROID_LOG_INFO, "newCache", "clear all key");
	return flag;
}

/*
 * Class:     com_yoson_cache_CacheStorage
 * Method:    getAllKey
 * Signature: (Ljava/nio/ByteBuffer;)[Ljava/lang/String;
 */
JNIEXPORT jobjectArray JNICALL Java_com_yoson_cache_CacheStorage_getAllKey
  (JNIEnv *env, jobject object, jobject ptr){
	mStore *store = (mStore *) env->GetDirectBufferAddress(ptr);
	jint size = store->getNum();
	jclass stringClass = env->FindClass("java/lang/String");
	jobjectArray array = env->NewObjectArray(size,stringClass,NULL);

	jstring tempFirst = env->NewStringUTF(store->getFirstKey());
	if(tempFirst == NULL){
		return NULL;
	}
	else{
		if(0 != array){
			env->SetObjectArrayElement(array,0,tempFirst);
			jstring temp = env->NewStringUTF(store->getNextKey());
			for(int i = 1;temp!=NULL;i++){
				env->SetObjectArrayElement(array,i,temp);
				temp = env->NewStringUTF(store->getNextKey());
			}
			env->DeleteLocalRef(temp);
		}
	}
	env->DeleteLocalRef(stringClass);
	env->DeleteLocalRef(tempFirst);

	return array;
}

/*
 * Class:     com_yoson_cache_CacheStorage
 * Method:    reMaxSize
 * Signature: (JLjava/nio/ByteBuffer;)Z
 */
JNIEXPORT jboolean JNICALL Java_com_yoson_cache_CacheStorage_reMaxSize
  (JNIEnv *env, jobject object, jlong size, jobject ptr){
	mStore *store = (mStore *) env->GetDirectBufferAddress(ptr);
	return store->reMaxSize(size);
}


