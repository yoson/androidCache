//
//  TBStorage.h
//  MStore
//
//  Created by 石勇慧 on 14-7-31.
//  Copyright (c) 2014年 TaoBao. All rights reserved.
//

#import <Foundation/Foundation.h>


@interface TBDataStorage : NSObject

+ (id)storageWithName:(NSString *)name path:(NSString *)path size:(NSNumber *)maxSize rule:(NSNumber *)rule compress:(NSNumber *)compress;

- (NSNumber *)reMaxSize:(NSNumber *)maxSize;

- (NSNumber *)insertWithKey:(NSString *)key value:(NSData *)value;
- (NSNumber *)removeWithKey:(NSString *)key;
- (NSNumber *)destroy;
- (NSNumber *)selectWithKey:(NSString *)key val:(NSValue *)val;

- (NSMutableArray *)getAllKey;

@end
