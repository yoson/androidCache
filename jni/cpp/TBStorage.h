//
//  TBStorage.h
//  MStore
//
//  Created by xuancong on 14-12-17.
//  Copyright (c) 2014年 TaoBao. All rights reserved.
//

#import <Foundation/Foundation.h>

@interface TBStorage : NSObject

+ (id)storageWithName:(const char *)name path:(const char *)path size:(unsigned int)maxSize rule:(int)rule compress:(bool)compress;

- (bool)reMaxSize:(unsigned long)maxSize;

- (bool)insertWithKey:(void *)key keylen:(int)keylen value:(void *)value valen:(int)valen;
- (bool)removeWithKey:(void *)key keylen:(int)keylen;
- (void)destroy;
- (int)selectWithKey:(void *)key keylen:(int)keylen val:(void **)val;

- (NSMutableArray *)getAllKey;

@end
