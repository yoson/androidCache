LOCAL_PATH := $(call my-dir)

APP_CFLAGS += -Wno-error=format-security
APP_CPPFLAGS += -Wno-error=format-security

include $(CLEAR_VARS)

LOCAL_SRC_FILES := \
		cpp/myconf.c \
		cpp/tcutil.c \
		cpp/tchdb.c \
		cpp/md5.c \
		cpp/mStore.cpp \
		com_taobao_nbcache_CacheStorage.cpp \

LOCAL_CFLAGS     := -std=c99
LOCAL_CFLAGS += -D__STDC_CONSTANT_MACROS -Wl,-Map=test.map -g


LOCAL_C_INCLUDES := $(LOCAL_PATH)/cpp

LOCAL_LDLIBS := -lz
LOCAL_LDLIBS += -L$(SYSROOT)/usr/lib -llog

LOCAL_MODULE    := CacheStorage



include $(BUILD_SHARED_LIBRARY)

