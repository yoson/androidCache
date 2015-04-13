//
//  TBStorage.m
//  MStore
//
//  Created by 石勇慧 on 14-7-31.
//  Copyright (c) 2014年 TaoBao. All rights reserved.
//

#include "mStore.h"
#import "TBDataStorage.h"


@interface TBDataStorage ()
{
    mStore	*_store;
}

@end


@implementation TBDataStorage

+ (id)storageWithName:(NSString *)name path:(NSString *)path size:(NSNumber *)maxSize rule:(NSNumber *)rule compress:(NSNumber *)compress
{
	return [[TBDataStorage alloc] initWithName:[name UTF8String] path:[path UTF8String] size:[maxSize unsignedIntValue] rule:[rule intValue] compress:[compress boolValue]];
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

- (NSNumber *)reMaxSize:(NSNumber *)maxSize
{
	bool result = false;
	if (NULL != _store) {
		result = _store->reMaxSize([maxSize unsignedLongValue]);
	}
	return [NSNumber numberWithBool:result];
}

- (NSNumber *)insertWithKey:(NSString *)key value:(NSData *)value
{
	bool result = false;
	if (NULL != _store) {
		result = _store->insert((void *)[key UTF8String], (int)[key length], (void *)[value bytes], (int)[value length]);
	}
	return [NSNumber numberWithBool:result];
}

- (NSNumber *)removeWithKey:(NSString *)key
{
	bool result = false;
	if (NULL != _store) {
		result = _store->remove((void *)[key UTF8String], (int)[key length]);
	}
	return [NSNumber numberWithBool:result];
}

- (NSNumber *)destroy
{
	bool result = false;
	if (NULL != _store) {
		result = _store->destroy();
	}
	return [NSNumber numberWithBool:result];
}

- (NSNumber *)selectWithKey:(NSString *)key val:(NSValue *)val
{
	int result = 0;
	if (NULL != _store) {
		result = _store->get((void *)[key UTF8String], (int)[key length], (void **)[val pointerValue]);
	}
	return [NSNumber numberWithInt:result];
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
