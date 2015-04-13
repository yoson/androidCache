//
//  TBStorage.m
//  MStore
//
//  Created by xuancong on 14-12-17.
//  Copyright (c) 2014年 TaoBao. All rights reserved.
//
#include "mStore.h"
#import "TBStorage.h"


@interface TBStorage ()
{
    mStore	*_store;
}

@end


@implementation TBStorage

+ (id)storageWithName:(const char *)name path:(const char *)path size:(unsigned int)maxSize rule:(int)rule compress:(bool)compress
{
    return [[TBStorage alloc] initWithName:name path:path size:maxSize rule:rule compress:compress];
}

- (id)initWithName:(const char *)name path:(const char *)path size:(unsigned int)maxSize rule:(int)rule compress:(bool)compress
{
    self = [super init];
    if (nil != self) {
        _store = new mStore(name, path, maxSize, (cache_type)rule, compress);
    }
    
    return self;
}

- (void)dealloc
{
    if (NULL != _store) {
        _store->close();
        delete _store;
    }
}

- (bool)reMaxSize:(unsigned long)maxSize
{
    if (NULL != _store) {
        return _store->reMaxSize(maxSize);
    }
    return false;
}

- (bool)insertWithKey:(void *)key keylen:(int)keylen value:(void *)value valen:(int)valen
{
    if (NULL != _store) {
        return _store->insert(key, keylen, value, valen);
    }
    return false;
}

- (bool)removeWithKey:(void *)key keylen:(int)keylen
{
    if (NULL != _store) {
        return _store->remove(key, keylen);
    }
    return false;
}

- (void)destroy
{
    if (NULL != _store) {
        _store->destroy();
    }
}

- (int)selectWithKey:(void *)key keylen:(int)keylen val:(void **)val
{
    if (NULL != _store) {
        return _store->get(key, keylen, val);
    }
    return 0;
}

- (NSMutableArray *)getAllKey
{
    NSMutableArray *result = [NSMutableArray array];
    if (const char *key = _store->getFirstKey()) {
        do {
            [result addObject:[NSString stringWithUTF8String:key]];
            key = _store->getNextKey();
        } while (NULL != key);
    }
    return result;
}

@end
