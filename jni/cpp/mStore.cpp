//
//  mStore.mm
//  CacheTest
//
//  Created by 容隽 on 14-7-7.
//  Copyright (c) 2014年 容隽. All rights reserved.
//
#include "mStore.h"

extern void *_tc_recencode(const void *ptr, int size, int *sp, void *op);
extern void *_tc_recdecode(const void *ptr, int size, int *sp, void *op);


static int compareTime(const void*a,const void*b)
{
    return (int)(*(time_t*)a - *(time_t*)b);
}


mStore::mStore(){
    _tc_hdb = tchdbnew();
    _tc_apow = -1;
    _tc_fpow = 10;
    _tc_mt = 0;
    _tc_opts = 0;
    _tc_rcnum = -1;
    _tc_xmsiz = 700*1024;//最大内存映射700Kb
    _tc_dfunit = 0;
    _tc_omode = 0;
    _tc_as = 0;
    _tc_rnd = 0;
    _tc_rnum = MAX_RECNUM;
    _tc_bnum = _tc_rnum * 4;    //
    _tc_opts = 0;
    _db_name = NULL;
    _path = NULL;
}

    
mStore::mStore(const char *name, const char *path, unsigned int max_size,cache_type cache_rule, bool compress)
{
    _tc_hdb = tchdbnew();
    _tc_apow = -1;
    _tc_fpow = 10;
    _tc_mt = 0;
    _tc_opts = 0;
    
    _tc_rcnum = -1;
    _tc_xmsiz = max_size;

    _tc_dfunit = 0;
    _tc_omode = 0;
    _tc_as = 0;
    _tc_rnd = 0;
    _tc_rnum = MAX_RECNUM;
    _tc_bnum = _tc_rnum * 4;    //
    _tc_opts = 0;
    _db_name = NULL;
    _path = NULL;
    
    open(name, path, max_size, cache_rule, compress);
}

mStore::~mStore()
{
    if (_tc_hdb){
        close();
    }
    if (!_db_name){
        free(_db_name);
    }
    if (!_path){
        free(_path);
    }
}

bool mStore::init(const char* name, const char* path)
{
    //设置线程安全
    if ((!name) || (!path))
        return false;
        _db_name = (char*)malloc(strlen(name)+1);
    strcpy(_db_name, name);

    _path = (char*)malloc((strlen(path) + strlen(name) + 6));
    memset(_path, 0, strlen(path) + strlen(name) + 6);
    if (strncasecmp(path, "file://",7) == 0)
        snprintf(_path, (strlen(path) + strlen(name) + 6 - 6), "%s/%s.tbs\0", &path[7], name);

    else
        snprintf(_path, (strlen(path) + strlen(name) + 6), "%s/%s.tbs\0", path, name);
    
//    _log_path = (char*)malloc((strlen(path) + strlen(name) + 6));
//    if (strncasecmp(path, "file://",7) == 0)
//        sprintf(_log_path, "%s/%s.log\0", &path[7], name);
//    else
//        sprintf(_log_path, "%s/%s.log\0", path, name);
    
    
    if(_tc_mt && !tchdbsetmutex(_tc_hdb)){
        return false;
    }
    //设置codeing函数
    if(!tchdbsetcodecfunc(_tc_hdb, _tc_recencode, NULL, _tc_recdecode, NULL)){
        return false;
    }
    //set the tuning parameters of a hash database object.
    if(!tchdbtune(_tc_hdb, _tc_bnum, _tc_apow, _tc_fpow, _tc_opts)){
        return false;
    }
    //set the caching parameters of a hash database object.
    if(!tchdbsetcache(_tc_hdb, _tc_rcnum)){
        return false;
    }
    //set the size of the extra mapped memory of a hash database object.
    if(_tc_xmsiz >= 0 && !tchdbsetxmsiz(_tc_hdb, _tc_xmsiz)){
        return false;
    }
    //set the unit step number of auto defragmentation of a hash database object.
    if(_tc_dfunit >= 0 && !tchdbsetdfunit(_tc_hdb, _tc_dfunit)){
        return false;
    }
    //if(!rnd) omode |= HDBOTRUNC;
    return true;
}
    
bool mStore::open(const char *name, const char *path, unsigned long max_size,cache_type cache_rule, bool compress)
{
    if ((!name) || (!path)){
        return false;
    }

    if (!init(name, path)){
        return false;
    }
    _cache_rule = cache_rule;
    _tc_omode = HDBOWRITER;
    
    FILE *fp;
    bool isCreate = false;
    if (!(fp = fopen(_path, "r"))){
        isCreate = true;
        _tc_omode |= HDBOCREAT;
    }
    else{
        fclose(fp);
        _tc_omode &= ~HDBOCREAT;
    }
    
    
 //   _FLOG = fopen(_log_path, "w");

    
    if(!tchdbopen(_tc_hdb, _path, HDBOWRITER | HDBOREADER | _tc_omode)){
        if(!isCreate){ //无法打开文件，文件被损坏将重新创建
            destroy();
            return open(name, path, max_size, cache_rule, compress);
        }
        return false;
    }
    
    if (isCreate) {
        _opt._max_len = max_size;
        _opt._isZip = compress;
        saveOpt(&_opt); //存储配置
//        fprintf(_FLOG, "create new\n");
//        fflush(_FLOG);
    }
    else{
        void* dbOpt = getOpt();
        if (!dbOpt) {
            destroy();
            return open(name, path, max_size, cache_rule, compress);
        }
        memcpy(&_opt, dbOpt, sizeof(store_opt));    //获取配置
        if (_opt._max_len != max_size){
            if (!reMaxSize(max_size)) {
                //close();
//                //fprintf(_FLOG, "open faile\n");
//                //fflush(_FLOG);
                return false;
            }
            _opt._max_len = max_size;
        }
        _opt._isZip = compress;
        saveOpt(&_opt); //存储配置

//        //fprintf(_FLOG, "open succ\n");
//        //fflush(_FLOG);
        int fsiz = 0;
        struct stat buf;
        if (stat(_path, &buf) == 0){
            fsiz = buf.st_size;
        }
        
        int tbsfsiz = getFsiz();
        if (fsiz != tbsfsiz){        //未正常关闭，可能导致异常。重新恢复文件
            if (!optimize()) {        //重新清理文件
                destroy();
                return open(name, path, max_size, cache_rule, compress);
            }
        }
        
    }

    return true;
}

void* mStore::SerializeValue(void* src, int srclen, int *relen,void* key, bool isCmp)
{
    if ((!src) || (srclen<=0) || (!relen)){
        if(relen)
            *relen = 0;
        return NULL;
    }
    char *re_buf;
    
    unsigned long realLength=0; //64位下转换

    int cur = (int)time(NULL);
    char *cmp_buf;
    int headsiz = sizeof(int) + sizeof(isCmp) + sizeof(int);
    
    if (isCmp){
        unsigned long maxlen = compressBound(srclen+1);
        re_buf = ( char *) malloc (maxlen+headsiz);
        realLength=maxlen;
        memcpy(re_buf, &cur, sizeof(int));
        re_buf[sizeof(int)] = isCmp ;
        memcpy(&re_buf[sizeof(int) + sizeof(isCmp)], &srclen, sizeof(int));
        cmp_buf = re_buf + headsiz;
        if(compress((unsigned char*)cmp_buf, (uLong*)&realLength, (const unsigned char*)src, srclen) != Z_OK){
            free(re_buf);
            //fprintf(_FLOG, "commpress faile\n");
            //fflush(_FLOG);
            *relen = 0;
            return NULL;
        }
        
        realLength += headsiz;
    }
    else{
        realLength = srclen+headsiz;
        re_buf = (char*)malloc(realLength);
        memcpy(re_buf, &cur, sizeof(int));
        re_buf[sizeof(int)] = isCmp ;
        memcpy(&re_buf[sizeof(int) + sizeof(isCmp)], &srclen, sizeof(int));
        cmp_buf = re_buf + headsiz;
        memcpy(cmp_buf, src, srclen);
    }
    
    *relen=(int)realLength;
    
    
    char *pcrybuf = (char*)encrypt(cmp_buf, *relen - headsiz, (char*)key);
    if (pcrybuf) {
        memcpy(cmp_buf, pcrybuf, *relen - headsiz);
        free(pcrybuf);
    }
    
    //fprintf(_FLOG, "ser succ len %d => %d\n", srclen, *relen);
    //fflush(_FLOG);

    return re_buf;
}

void* mStore::DeSerializeValue(void* src, int srclen, void* key, int *relen)
{
    if ((!src) || (srclen<=0) || (!relen)){
        if (relen)
            *relen = 0;
        return NULL;
    }

    int iTime = *(int*)src;
    bool isCmp = ((bool*)src)[sizeof(iTime)];
    int headsiz = sizeof(int) + sizeof(isCmp);
    int dest_len;
    memcpy(&dest_len, &((char*)src)[headsiz], sizeof(int));
    headsiz  += sizeof(int);
    
    
    void * dest = malloc(dest_len);
    char *pSrc = &((char*)src)[headsiz];
    int valen = srclen-headsiz;
    char* decpydst = (char*)decrypt(pSrc, valen, (char*)key);
    
    unsigned long realLength=0; //64位转换指针

    if (isCmp) {
        realLength=dest_len;
        int unflag = uncompress((unsigned char*)dest, (uLong*)&realLength,(unsigned char*)decpydst, valen);

        if (unflag != Z_OK){
            *relen = 0;
            free(decpydst);
            free(dest);
//            fprintf(_FLOG, "%s\n",key);
//            fprintf(_FLOG, "%s\n",src);
//            fflush(_FLOG);
            return NULL;
        }
    }
    else{
        realLength = valen;
        memcpy(dest, decpydst, valen);
    }
    
    *relen=(int)realLength;
    
    free(decpydst);
    
    //fprintf(_FLOG, "des succ %d =》 %d\n", srclen, *relen);
    //fflush(_FLOG);
    
    return dest;
}


bool mStore::insert(void* key, int keylen, void* value, int valen, bool isreplace, int iCmp)
{
    if ((!key) || (keylen<=0)){
        
        //fprintf(_FLOG, "ins key null \n");
        //fflush(_FLOG);
        return false;
    }
    
    //fprintf(_FLOG, "inser keylen %d valen = %d replace=%d cmpr=%d \n", keylen,valen, isreplace, iCmp);
    //fflush(_FLOG);
    
    if (getFsiz() > (_opt._max_len-keylen-valen)) {
        if (_cache_rule == CACHE_NOTING) {
            //fprintf(_FLOG, "fsize > max and cache_noting\n");
            //fflush(_FLOG);
            return false;
        }
        else{
            removeByFIFO(getNum()*DEF_DELRATE);
            if (!optimize()){
                //fprintf(_FLOG, "optimeze fail\n");
                //fflush(_FLOG);
                return false;
            }
            if (getFsiz() > (_opt._max_len-keylen-valen)){
                //fprintf(_FLOG, "optimeze fail 2\n");
                //fflush(_FLOG);
                return false;
            }
        }
    }
    bool isCmp;
    if (iCmp == 0)
        isCmp = false;
    else if(iCmp == 1)
        isCmp = true;
    else
        isCmp = _opt._isZip;
    
    
    if (isreplace)
    {
        int servalen;
        void* serval = SerializeValue(value, valen, &servalen, key, isCmp);
        if (!serval){
            //fprintf(_FLOG, "ser faile\n");
            //fflush(_FLOG);
            return false;
        }
        if(_tc_as){
            if(!tchdbputasync(_tc_hdb, key, keylen, serval, servalen)){
                free(serval);
                //fprintf(_FLOG, "ins asc faile\n");
                //fflush(_FLOG);
                return false;
            }
        } 
        else {
            if(!tchdbput(_tc_hdb, key, keylen, serval, servalen)){
                free(serval);
                //fprintf(_FLOG, "ins faile\n");
                //fflush(_FLOG);
                return false;
            }
        }
        free(serval);
    }
    else{
        return insertNotRep(key, keylen, value, valen, isCmp);
    }
    
    
    //fprintf(_FLOG, "ins succ\n");
    //fflush(_FLOG);

    return true;
}

bool mStore::insertNotRep(void* key, int keylen, void* value, int valen,bool iscmp)
{
    if ((!key) || (keylen<=0)){
        return false;
    }
    int servalen;
    void* serval = SerializeValue(value, valen, &servalen, key, iscmp);
    if (!serval){
        
        //fprintf(_FLOG, "ser faile\n");
        //fflush(_FLOG);
        return false;
    }
    if(!tchdbputkeep(_tc_hdb, key, keylen, serval, servalen)){
        free(serval);
        
        //fprintf(_FLOG, "ins norep faile\n");
        //fflush(_FLOG);
        return false;
    }
    
    
    //fprintf(_FLOG, "ins norep succ\n");
    //fflush(_FLOG);
    free(serval);
    return true;
}

    
bool mStore::remove(void* key, int keylen)
{
    if ((!key) || (keylen<=0)){
        return false;
    }
    if (!tchdbout(_tc_hdb, key, keylen)){
        return false;
    }
    
    
    //fprintf(_FLOG, "del succ\n");
    //fflush(_FLOG);
    return true;
}

int mStore::get(void* key, int keylen, void **ppVal)
{
    int valen = -1;
    if ((!key) || (keylen<=0) || (!ppVal)){
        return -2;
    }
    
    
//    fprintf(_FLOG, "get keylen %s\n", keylen);
//    fflush(_FLOG);
    
    void* pValbuf = NULL;
    int valbuflen;
    pValbuf = tchdbget(_tc_hdb, key, keylen, &valbuflen);
    if(!(pValbuf)) {
        
        //fprintf(_FLOG, "get faile\n");
        //fflush(_FLOG);
        return -1;
    }
    
    *ppVal = DeSerializeValue(pValbuf, valbuflen, key, &valen);
    if (ppVal){
        free(pValbuf);
    }
    
    
    //fprintf(_FLOG, "get succ\n");
    //fflush(_FLOG);
    return valen;
}

bool mStore::close()
{
    if(!tchdbclose(_tc_hdb)){
        
        //fprintf(_FLOG, "close falie\n");
        //fflush(_FLOG);
        return false;
    }
    tchdbdel(_tc_hdb);
    _tc_hdb = NULL;
    
    //fprintf(_FLOG, "close succ\n");
    //fflush(_FLOG);
    return true;
}
    
bool mStore::destroy()
{
    if (_path){
        unlink(_path);
    }
    return true;
}


int mStore::removeByFIFO(int rnum)
{
    uint64_t dbnum = getNum();
    
    //fprintf(_FLOG, "fifo begin\n");
    //fflush(_FLOG);
    if ((rnum <= 0) || (dbnum==0)){
        return 0;
    }
    if (rnum >= dbnum){
        tchdbvanish(_tc_hdb);
        return (int)dbnum;
    }

    int num = 0;
    tchdbiterinit(_tc_hdb);
    
    time_t* pTimes = (time_t*)malloc(sizeof(time_t)*dbnum);
    char *pKey;
    time_t timeflg;
    
    while(tchdbiternext4(_tc_hdb, &pKey, &timeflg)){
        pTimes[num++] = timeflg;
        TCFREE(pKey);
    }
    
    qsort(pTimes, num, sizeof(time_t), compareTime);
    time_t mintime = pTimes[rnum];
    
    int crivnum = 0;
    for (int i=0; i<rnum; i++) {
        if (pTimes[i] == mintime) {
            crivnum ++;
        }
    }
    
    free(pTimes);
    num = 0;
    tchdbiterinit(_tc_hdb);
    while(tchdbiternext4(_tc_hdb, &pKey, &timeflg)){
        if (timeflg <= mintime){
            if (timeflg == mintime){
                if (crivnum>0)
                    crivnum--;
                else{
                    TCFREE(pKey);
                    continue;
                }
            
            }
            
            if (tchdbout2(_tc_hdb, pKey)){
                num++;
            }
        }
        TCFREE(pKey);
    }
    
    
    //fprintf(_FLOG, "fifo ok del %d\n", num);
    //fflush(_FLOG);
    
    return num;
}

bool mStore::optimize()
{
    tchdboptimize(_tc_hdb, _tc_bnum, _tc_apow, _tc_fpow, _tc_opts);
    
    //fprintf(_FLOG, "optimize\n");
    //fflush(_FLOG);
    return true;
}

bool mStore::saveOpt(store_opt* opt)
{
    
    //fprintf(_FLOG, "save opt\n");
    //fflush(_FLOG);
    return tchdbwrite(_tc_hdb, 128, (void*)opt, sizeof(store_opt));
}

store_opt* mStore::getOpt()
{
    
    //fprintf(_FLOG, "load opt\n");
    //fflush(_FLOG);
    return (store_opt*)tchdbopaque( _tc_hdb );
}

bool mStore::reMaxSize(unsigned long maxsize)
{
    
    //fprintf(_FLOG, "remax \n");
    //fflush(_FLOG);
    if (maxsize < MIN_FLEN){
        return false;
    }
    uint64_t curfsize = getFsiz();
    unsigned long  curmaxsize = _opt._max_len;
    if ((maxsize >= curmaxsize) || (maxsize >= curfsize)) {
        _opt._max_len = maxsize;
        //fprintf(_FLOG, "remax succ new max > cur\n");
        //fflush(_FLOG);
        return saveOpt(&_opt);
    }
    else{
        if (_cache_rule == CACHE_NOTING) {
            return false;
        }
        else {
            int deltime = 0;
            while (getFsiz() > maxsize) {
                float delrate = 1.0 - ((double)maxsize/(double)curfsize);
                int delnum = getNum() * delrate ;
                if (delnum > 0) {
                    removeByFIFO(delnum);
                    optimize();
                }
                
                if ((++deltime > 3) || (getNum() == 0)) {
                    //fprintf(_FLOG, "remax fifo more than 3 times \n");
                    //fflush(_FLOG);
                    return false;
                }
            }
            _opt._max_len = maxsize;
            //fprintf(_FLOG, "remax succ\n");
            //fflush(_FLOG);
            return saveOpt(&_opt);
        }
    }
    
    
    //fprintf(_FLOG, "remax faile\n");
    //fflush(_FLOG);
    return false;
}

void * mStore::encrypt(void *str, int str_len, char *key)
{
    
    //fprintf(_FLOG, "encrypt %d\n", str_len);
    //fflush(_FLOG);
	if (!str || !key)
    {
        return NULL;
    }
    
    long i = 0, j = 0, k, key_len = strlen(key);
    if (key_len > CRY_KEYLEN)
    {
        key_len = CRY_KEYLEN;
    }
    
    
    unsigned char *out_str = (unsigned char *) malloc (str_len);
    if (!out_str)
    {
        return NULL;
    }
    memcpy(out_str, str, str_len);
    
    int isetp = str_len > DCRY_STEP ? str_len/DCRY_STEP : 1;
    for (i = 0; i < str_len; i+=isetp)
    {
        k = ((unsigned char *)str)[i];

        for (j = 0; j < key_len; j++)
        {
            k = k + (j + 1) * key[j];
            k = k % 256;
        }
        ((unsigned char *)out_str)[i] = k;
    }
    
    
    //fprintf(_FLOG, "encrypt succ\n");
    //fflush(_FLOG);
    return out_str;
}

void * mStore::decrypt(void *str, int str_len, char *key)
{
    
    //fprintf(_FLOG, "decrypt len %d\n", str_len);
    //fflush(_FLOG);
	if (!str || !key)
    {
        return NULL;
    }
    
    long i = 0, j = 0, k, key_len = strlen(key);

    if (key_len > CRY_KEYLEN)
    {
        key_len = CRY_KEYLEN;
    }
    unsigned char *out_str = (unsigned char *) malloc (str_len);
    if (!out_str)
    {
        return NULL;
    }
    memcpy(out_str, str, str_len);
    
    int isetp = str_len > DCRY_STEP ? str_len/DCRY_STEP : 1;
    for (i = 0; i < str_len; i+=isetp)
    {
        k = ((unsigned char *)str)[i];

        for (j = key_len - 1; j >= 0; j--)
        {
            k = k - (j + 1) * key[j];
            
            k = k % 256;
        }
        if (k < 0)
        {
            k = k + ((long)(-k/256) + 1) * 256;
        }
        ((unsigned char *)out_str)[i] = k;
    }
    
    
    //fprintf(_FLOG, "decrypt succ\n");
    //fflush(_FLOG);
    
    return out_str;
}

