package com.linglong.videocode;

public class DataTransfer {

    static {
        System.loadLibrary("myvideo");
    }

    public static native void nv21ToYuv420(byte[] nv21,byte[] yuv,int width,int height);

}
