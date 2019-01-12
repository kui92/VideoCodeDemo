package com.linglong.videocode;

import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.os.Build;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.util.Log;
import android.view.Surface;

import java.io.IOException;
import java.nio.ByteBuffer;

public class VideoDecoder {
    private static final String TAG = "VideoDecoder";
    private MediaCodec codec;
    private int width,height;
    private Surface surface;
    private String MIME_TYPE = "video/avc";
    private MediaExtractor videoExtractor;
    private int videoTrack = -1;
    private volatile boolean runing = false;
    private long startTime = 0;
    private Thread thread;

    private void prepare(String path,Surface surface){
        videoExtractor = new MediaExtractor();
        try {
            videoExtractor.setDataSource(path);
            //获取视频所在轨道
            videoTrack = getMediaTrackIndex(videoExtractor, "video/");
            MediaFormat mediaFormat = videoExtractor.getTrackFormat(videoTrack);
            int width = mediaFormat.getInteger(MediaFormat.KEY_WIDTH);
            int height = mediaFormat.getInteger(MediaFormat.KEY_HEIGHT);
            int rotation = mediaFormat.getInteger(MediaFormat.KEY_ROTATION);
            LogMediaInfo.LogMediaFormat(mediaFormat);
            Log.i(TAG,"rotation:"+rotation);
            //视频长度:秒
            float time = mediaFormat.getLong(MediaFormat.KEY_DURATION) / 1000000;
            videoExtractor.selectTrack(videoTrack);
            config(surface,mediaFormat);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void config(Surface surface,@NonNull MediaFormat mediaFormat){
        this.surface = surface;
        try {
            codec = MediaCodec.createDecoderByType(MIME_TYPE);
            codec.configure(mediaFormat,surface,null,0);
            codec.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void start(String path,Surface surface){
        if (thread==null){
            thread = new Thread(){
                @Override
                public void run() {
                    startTime = System.currentTimeMillis();
                    while (runing){
                        try {
                            runTask();
                        }catch (Exception e){
                            Log.e(TAG,"播放出错:"+e.getMessage());
                        }
                    }
                    Log.i(TAG,"渲染线程结束");
                }
            };
        }else if (thread.isAlive()){
            stop();
        }
        runing = true;
        prepare(path,surface);
        thread.start();
    }


    private void runTask(){
        int index = codec.dequeueInputBuffer(0);
        if (index>=0){
            ByteBuffer inputBuffer = getInputBuffer(codec,index);
            int size = videoExtractor.readSampleData(inputBuffer,0);
            long time = videoExtractor.getSampleTime();
            Log.d(TAG, "read size: " + size + "time: " + time);
            if (size>0&&time>=0){
                Log.v(TAG, "runTask555555555");
                codec.queueInputBuffer(index,0,size,time,0);
                videoExtractor.advance();//下一帧
            }else {
                Log.v(TAG, "runTask6666666");
                codec.queueInputBuffer(index, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
            }
        }


        MediaCodec.BufferInfo videoBufferInfo = new MediaCodec.BufferInfo();
        int outputBufferIndex  = codec.dequeueOutputBuffer(videoBufferInfo,0);
        switch (outputBufferIndex) {
            case MediaCodec.INFO_OUTPUT_FORMAT_CHANGED:
                Log.v(TAG, "format changed");
                break;
            case MediaCodec.INFO_TRY_AGAIN_LATER:
                Log.v(TAG, "解码当前帧超时");
                sleep(10);
                break;
            case MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED:
                //outputBuffers = videoCodec.getOutputBuffers();
                Log.v(TAG, "output buffers changed");
                break;
            default:
                //直接渲染到Surface时使用不到outputBuffer
                //ByteBuffer outputBuffer = outputBuffers[outputBufferIndex];
                //延时操作
                //如果缓冲区里的可展示时间>当前视频播放的进度，就休眠一下
                long sleepTime = videoBufferInfo.presentationTimeUs / 1000 - (System.currentTimeMillis() - startTime);
                if (sleepTime>1){
                    sleep(sleepTime);
                }
                //渲染
                codec.releaseOutputBuffer(outputBufferIndex, true);
                break;
        }

        if ((videoBufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
            Log.v(TAG, "读取到结束标志buffer stream end");
            runing = false;
        }
    }

    /**
     * 睡眠一段时间
     * @param time
     */
    private void sleep(long time){
        try {
           // Log.i(TAG,"xiu")
            Thread.sleep(time);
        }catch (Exception e){
            Log.e(TAG,"sleep Exception:"+e.getMessage());
        }
    }

    private ByteBuffer getInputBuffer(MediaCodec codec,int index){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            return codec.getInputBuffer(index);
        }else{
            return codec.getInputBuffers()[index];
        }
    }

    public void stop(){
        Log.i(TAG,"stop");
        runing = false;
        if (thread!=null&&thread.isAlive()){
            try {
                Log.i(TAG,"wait thread finish++++++++++++++++++++++");
                thread.join(2000);
                Log.i(TAG,"thread finish++++++++++++++++++++++++++");
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        if (codec!=null){
            try {
                codec.stop();
                codec.release();
                codec = null;
            }catch (Exception e){
                Log.e(TAG,"stop Exception:"+e.getCause().getMessage());
            }
        }
        if (videoExtractor!=null){
            videoExtractor.release();
            videoExtractor = null;
        }
    }

    /**
     * 获取指定类型媒体文件所在轨道
     * @param videoExtractor
     * @param MEDIA_TYPE
     * @return
     */
    private int getMediaTrackIndex(MediaExtractor videoExtractor,String MEDIA_TYPE) {
        int trackIndex = -1;
        for (int i = 0; i < videoExtractor.getTrackCount(); i++) {
            //获取视频所在轨道
            MediaFormat mediaFormat = videoExtractor.getTrackFormat(i);
            String mime = mediaFormat.getString(MediaFormat.KEY_MIME);
            if (mime.startsWith(MEDIA_TYPE)) {
                trackIndex = i;
                break;
            }
        }
        return trackIndex;
    }

}
