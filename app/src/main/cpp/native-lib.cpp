#include <jni.h>
#include <string>
#include <android/log.h>
#include "com_linglong_videocode_DataTransfer.h"
extern "C"{
    #include "tools.h"
}


#define  LOG_TAG    "KuiTag"
#define  LOGD(...)  __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)

extern "C"{
    void nv21ToYUN420(unsigned char* src,unsigned char* dst,int width, int height){
        int size = width*height;
        memcpy(dst,src,size);
        int le = strlen(reinterpret_cast<const char *>(src));
        LOGD("le:%d",le);
        for (int i = size;i<le;i+=2){
            dst[i] = src[i+1];
            dst[i+1] = src[i];
        }
    }
}

JNIEXPORT void JNICALL Java_com_linglong_videocode_DataTransfer_nv21ToYuv420
        (JNIEnv * env, jclass mClass, jbyteArray  arraySrc, jbyteArray arrayDst, jint width, jint height){
    jbyte *srcByte = env->GetByteArrayElements(arraySrc, JNI_FALSE);
    jbyte *dstByte = env->GetByteArrayElements(arrayDst,JNI_FALSE);
    unsigned char * src =(unsigned char *)(srcByte);
    unsigned char * dst =(unsigned char *)(dstByte);
    nv21ToYUN420(src,dst,width,height);
    env->ReleaseByteArrayElements(arraySrc,srcByte,JNI_FALSE);
    env->ReleaseByteArrayElements(arrayDst,dstByte,JNI_FALSE);
}