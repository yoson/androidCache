#ifndef _mStore_H
#define _mStore_H
//mStore.h
//CacheTest
//
//Created by 容隽 on 14-7-7.
//Copyright (c) 2014年 容隽. All rights reserved.
//
// 缓存规则
extern "C"
{
#include "tcutil.h"

#include "myconf.h"
}
#include "tchdb.h"
#include <zlib.h>

enum cache_type
{
CACHE_NOTING, //禁止
CACHE_FIFO//先进先出
};

#define MAX_RECNUM 20000
#define CRY_KEYLEN 3
#define MIN_FLEN 1*1024*1024
#define DEF_DELRATE 0.15
#define DCRY_STEP 64
typedef struct _store_opt
{
    unsigned long _max_len;
    bool _isZip;
} store_opt;

/*typedef struct _item_info{
    time_t  times;
    bool iszip;
    uLong   size;
} item_info;*/

class mStore {
public:
    mStore();
    /*
     * 功能：构造函数
     * 参数：name 项目名称.
     * path 存储文件路径
     * max_size 最大使用空间
     * cache_rule采用缓存策略
     * compress压缩方法：0不压缩，1 snappy
     * 返回：
     */
    mStore(const char *name, const char *path, unsigned int max_size,cache_type cache_rule, bool compress);
    ~mStore();
    /*
     * 功能：打开指定的缓存文件， 文件不存在创建新文件
     * 参数：name 项目名称.
     * path 存储文件路径
     * max_size 最大使用空间
     * cache_rule采用缓存策略
     * compress压缩方法：false不压缩，true 压缩
     * 返回：
     */
    bool open(const char *name, const char *path, unsigned long max_size,cache_type cache_rule, bool compress);
    
    /*
     * 功能：插入一条数据
     * 参数：key 关键词
     keylen 关键词长度
     value 保存内容
     valen 内容长度
     iCmp 是否压缩 0：不压缩 1：压缩 -1：使用默认值
     isoverwr 记录存在是否覆盖
     * 返回：是否成功
     */
    bool insert(void* key, int keylen, void* value, int valen, bool isoverwr = true, int iCmp = -1);

    /*
     * 功能：删除一条数据
     * 参数：key 关键词
     keylen 关键词长度
     * 返回：是否成功
     */
    bool remove(void* key, int keylen);
    /*
     * 功能：查询一条数据
     * 参数：key 关键词
     keylen 关键词长度
     value 保存内容
     * 返回：返回结果长度 小于0为失败
     */
    int get(void* key, int keylen, void **ppVal);
    /*
     * 功能：关闭应用
     * 参数：
     * 返回： 是否成功
     */
    bool close();
    /*
     * 功能：摧毁该缓存
     * 参数：
     * 返回： 是否成功
     */
    bool destroy();
/*
     * 功能：返回当前记录个数
     * 参数：
     * 返回： 记录个数
     */
    uint64_t getNum(){
        return _tc_hdb? tchdbrnum(_tc_hdb) : 0;
    };
    /*
     * 功能：返回当前文件大小
     * 参数：
     * 返回： 文件大小
     */
    uint64_t getFsiz(){
        return _tc_hdb? tchdbfsiz(_tc_hdb) : 0;
    };
    /*
     * 功能：返回当前应用名称
     * 参数：
     * 返回： 名称
     */
    const char * getName(){
        return (const char*)_db_name;
    };
    /*
     * 功能：更改文件空间大小上限
     * 参数：文件最大值
     * 返回： 是否成功。 如果cache_none方式打开并上限小于当前文件大小将失败，其它会调整当前文件
     */
    bool reMaxSize(unsigned long maxsize);
    /*
     * 功能：获取第一个key值
     * 参数：
     * 返回： 第一个key值的字符串
     */
    char * getFirstKey(){
        
        if ((_tc_hdb) && (tchdbiterinit(_tc_hdb))) {
            return tchdbiternext2(_tc_hdb);
        }
        return NULL;
    }
    /*
     * 功能：获取下一个key值
     * 参数：
     * 返回：下一个key值的字符串
     */
    char * getNextKey(){
        if (_tc_hdb) {
            return tchdbiternext2(_tc_hdb);
        }
        return NULL;
    }
    
    /*
     * 功能：清空所有记录
     * 参数：
     * 返回： 是否成功。
     */
    bool clearAll(){
        if (_tc_hdb) {
            return tchdbvanish(_tc_hdb);
        }
        return false;
    }
    
private:

    int _tc_rnum;   //设置缓存记录的最大数
    int _tc_bnum;   //bucket的数量
    int _tc_apow;   // size of record alignment by power
    int _tc_fpow;   //maximum number of elements of the free block pool by power
    bool _tc_mt;    //设置文件锁
    int _tc_opts;   //压缩格式l (64位bucket数组，database容量可以超过2G)、d (Deflate压缩)、b(BZIP2压缩)、t(TCBS压缩) as  异步
    int _tc_rcnum;  //设置缓存记录的最大数
    int _tc_xmsiz;  //置额外内存映射容量
    int _tc_dfunit; //specifie the unit step number
    int _tc_omode;  //
    bool _tc_as;    //异步
    bool _tc_rnd;   //
    cache_type _cache_rule;

    TCHDB *_tc_hdb;     //tcdb
    char* _path;    //文件存储路径
    char* _db_name; //db名称
    store_opt _opt; //存储db的配置信息
    
//    char* _log_path;    //log文件路径   yf
//    FILE* _FLOG;        //log文件句柄  yf

    bool init(const char* name, const char* path);
    /*存储库的配置信息
     */
    bool saveOpt(store_opt* opt);
    /*获取库的配置信息
     */
    store_opt* getOpt();
    void* SerializeValue(void* src, int srclen, int *relen,void* key,  bool isCmp);
    void* DeSerializeValue(void* src, int srclen, void* key, int *relen);
    int removeByFIFO(int rnum);
    bool optimize();
    
    /*
     * 功能：插入一条数据,存在将无效
     * 参数：key 关键词
     keylen 关键词长度
     value 保存内容
     valen 内容长度
     iscmp 是否加密
     * 返回：是否成功
     */
    bool insertNotRep(void* key, int keylen, void* value, int valen,bool iscmp);
    void * encrypt(void *str, int str_len, char *key); //加密
    void * decrypt(void *str, int str_len, char *key); //解密
    

    
};
#endif
