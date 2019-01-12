//
// Created by kui on 2018/12/6.
//

#ifndef NDKTHREAD_TOOLS_H
#define NDKTHREAD_TOOLS_H

#include <jni.h>

char *jstringTostring(JNIEnv * env, jstring jstr);

char * strAdd(char * str1, char * str2);
jstring charTojstring(JNIEnv* env, const char* pat);
#endif //NDKTHREAD_TOOLS_H
