package com.linglong.videocode;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.util.Log;
import android.view.Surface;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;

public class MyRecord {

    private static final String FOLDER = Environment.getExternalStorageDirectory().getAbsolutePath()+ File.separator+"aaa";

    public static final String TAG = "MyRecord";
    private MediaMuxer mMuxer;  //多路复用器，用于音视频混合
    private volatile boolean mMuxerStart = false;


    public void startRecord(){
        startNanoTime = System.nanoTime();
        startVideo();
        startAudioRecord();
    }

    public void stopRecord(){
        stopAudio();
        stopVideo();
        if (mMuxer!=null){
            mMuxer.stop();
            mMuxer.release();
            mMuxer = null;
            mMuxerStart = false;
        }
        Log.i(TAG,"编码过程完成 feed:"+feedCount+"--fram:"+framCoun);
    }

    /**************************** Audio部分 **********************/
    private String audioMime = "audio/mp4a-latm";   //音频编码的Mime
    private AudioRecord mAudioRecorder;   //录音器
    private MediaCodec mAudioEnc;   //编码器，用于音频编码
    private int audioRate=128000;   //音频编码的密钥比特率
    private int sampleRate=48000;   //音频采样率
    private int channelCount=2;     //音频编码通道数
    private int channelConfig= AudioFormat.CHANNEL_IN_STEREO;   //音频录制通道,默认为立体声
    private int audioFormat=AudioFormat.ENCODING_PCM_16BIT; //音频录制格式，默认为PCM16Bit
    private int bufferSize;
    private int mAudioTrack = -1;

    private HandlerThread audioThread;
    private Handler audioHandler;
    private volatile boolean audioRecoding = false;
    private boolean initAudio(){
        //准备Audio
        MediaFormat format = MediaFormat.createAudioFormat(audioMime,sampleRate,channelCount);
        format.setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC);
        format.setInteger(MediaFormat.KEY_BIT_RATE, audioRate);
        try {
            mAudioEnc = MediaCodec.createEncoderByType(audioMime);
        } catch (IOException e) {
            e.printStackTrace();
            Log.i(TAG,"audio 初始化失败:"+e.getMessage());
            return false;
        }
        mAudioEnc.configure(format,null,null,MediaCodec.CONFIGURE_FLAG_ENCODE);
        bufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)*2;
//        buffer=new byte[bufferSize];
        mAudioRecorder = new AudioRecord(MediaRecorder.AudioSource.MIC,sampleRate,channelConfig,
                audioFormat,bufferSize);
        return true;
    }
    private void startAudioRecord(){
        if (audioHandler!=null){
            audioHandler.removeCallbacksAndMessages(null);
        }
        if (audioThread!=null&&audioThread.isAlive()){
            quiThreadHandler(audioThread);
        }
        audioRecoding = true;
        if (!initAudio()){
            return;
        }
        mAudioEnc.start();
        mAudioRecorder.startRecording();
        audioThread = new HandlerThread("audioRecord");
        audioThread.start();
        audioHandler = new Handler(audioThread.getLooper()){
            @Override
            public void handleMessage(Message msg) {
                if (msg.what == VIDEO_QUIT){
                    audioHandler.removeCallbacksAndMessages(null);
                    quiThreadHandler(audioThread);
                }
            }
        };
        audioHandler.post(new Runnable() {
            @Override
            public void run() {
                while (audioRecoding){
                    try {
                        audioRecord();
                    }catch (Exception e){
                        Log.i(TAG,"audio Exception:"+e.getMessage());
                        //break;
                    }
                }
                audioRecoding = false;
            }
        });
    }

    private void audioRecord(){
        int index = mAudioEnc.dequeueInputBuffer(-1);
        if (index>=0){
            ByteBuffer buffer = getInputBuffer(mAudioEnc,index);
            buffer.clear();
            int len = mAudioRecorder.read(buffer,bufferSize);
            Log.i(TAG,"audio 读取到音频帧:"+len);
            int flag = audioRecoding?0:MediaCodec.BUFFER_FLAG_END_OF_STREAM;
            mAudioEnc.queueInputBuffer(index,0,len,(System.nanoTime()-startNanoTime)/1000,flag);
        }
        MediaCodec.BufferInfo mInfo = new MediaCodec.BufferInfo();
        int outIndex = 0;
        while (outIndex>=0){
            outIndex = mAudioEnc.dequeueOutputBuffer(mInfo,0);
            if (mInfo.flags == MediaCodec.BUFFER_FLAG_END_OF_STREAM){
                Log.i(TAG,"audio读取到结束帧");
                audioRecoding = false;
                break;
            }
            if (outIndex>=0&&mMuxerStart){
                ByteBuffer outBuffeer = getOutputBuffer(mAudioEnc,outIndex);
                Log.i(TAG,"audio mAudioTrack:"+mAudioTrack+"--mVideoTrack:"+mVideoTrack+"--mInfo"+mInfo.size+"--mInfo.presentationTimeUs:"+mInfo.presentationTimeUs+"--flag:"+mInfo.flags);
                outBuffeer.position(mInfo.offset);
                if (mAudioTrack>=0&&mVideoTrack>=0&&mInfo.size>0&&mInfo.presentationTimeUs>0){
                    try {
                        mMuxer.writeSampleData(mAudioTrack,outBuffeer,mInfo);
                    }catch (Exception e){
                        Log.e(TAG,"audio error:size="+mInfo.size+"/offset="
                                +mInfo.offset+"/timeUs="+mInfo.presentationTimeUs+"--Exception:"+e.getCause().getMessage());
                    }
                }
                mAudioEnc.releaseOutputBuffer(mAudioTrack,false);
            }else if (outIndex==MediaCodec.INFO_OUTPUT_FORMAT_CHANGED){
                addTrack(2);
            }else if (outIndex == MediaCodec.INFO_TRY_AGAIN_LATER){
               // Log.i(TAG,"audio 读取到未知标志位:"+outIndex);
            }
        }
    }

    private void stopAudio(){
        if (audioThread==null){
            return;
        }
        Log.i(TAG,"stopAudio");
        audioRecoding = false;
        audioHandler.removeCallbacksAndMessages(null);
        audioHandler.sendEmptyMessage(VIDEO_QUIT);
        try {
            audioThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        audioThread = null;
        if (mAudioRecorder!=null){
            mAudioRecorder.stop();
            mAudioRecorder.release();
            mAudioRecorder = null;
        }
        if (mAudioEnc!=null){
            mAudioEnc.stop();
            mAudioEnc.release();
            mAudioEnc = null;
        }
        mAudioTrack = -1;
        Log.i(TAG,"audio 结束");
    }


    /**************************** Audio部分 **********************/

    /****************  video *********************/
    private int convertType;
    private MediaCodec mVideoEnc;
    private String videoMime="video/avc";   //视频编码格式
    private int videoRate=2048000;       //视频编码波特率
    private int frameRate=25;           //视频编码帧率
    private int frameInterval=1;        //视频编码关键帧，1秒一关键帧
    private int mVideoTrack=-1;
    private int width;
    private int height;
    private volatile boolean videoRecording = false;
    private HandlerThread videoHandlerThread;
    private Handler videoHandler;
    private final int VIDEO_FRAM = 1;
    private final int VIDEO_QUIT = 2;
    private byte[] yuv420;
    private long startNanoTime;
    private long feedCount = 0,framCoun = 0;
    private String filePath;
    private Surface surface;
    public boolean prepare(int width,int height,Surface surface){
        this.width=width;
        this.height=height;
        this.surface = surface;
        yuv420 = new byte[width*height*3/2];
        MediaFormat videoFormat=MediaFormat.createVideoFormat(videoMime,width,height);
        videoFormat.setInteger(MediaFormat.KEY_BIT_RATE,videoRate);
        videoFormat.setInteger(MediaFormat.KEY_FRAME_RATE,frameRate);
        videoFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL,frameInterval);
        videoFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT,checkColorFormat(videoMime));
        videoFormat.setInteger(MediaFormat.KEY_ROTATION,90);
        try {
            mVideoEnc=MediaCodec.createEncoderByType(videoMime);
        } catch (IOException e) {
            Log.e(TAG,"video 初始化失败:"+e.getMessage());
            e.printStackTrace();
            return false;
        }
        LogMediaInfo.LogMediaFormat(videoFormat);
        mVideoEnc.configure(videoFormat,surface,null,MediaCodec.CONFIGURE_FLAG_ENCODE);
        Bundle bundle=new Bundle();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            bundle.putInt(MediaCodec.PARAMETER_KEY_VIDEO_BITRATE,videoRate);
            mVideoEnc.setParameters(bundle);
        }
        try {
            filePath = getFilePath();
            mMuxer = new MediaMuxer(filePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
        } catch (IOException e) {
            Log.e(TAG,"mMuxer 初始化失败:"+e.getMessage());
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public String getFilePath2() {
        return filePath;
    }

    private void startVideo(){
        if (mVideoEnc==null){
            return;
        }
        videoRecording = true;
        mVideoEnc.start();
        if (videoHandler!=null){
            videoHandler.removeCallbacksAndMessages(null);
        }
        if (videoHandlerThread!=null&&videoHandlerThread.isAlive()){
            quiThreadHandler(videoHandlerThread);
        }
        videoHandlerThread = new HandlerThread("videoThread");
        videoHandlerThread.start();
        videoHandler = new Handler(videoHandlerThread.getLooper()){
            @Override
            public void handleMessage(Message msg) {
                switch (msg.what){
                    case VIDEO_FRAM:
                        try {
                            getFramMsg(msg);
                        }catch (Exception e){
                            Log.e(TAG,"video 编错误："+e.getMessage());
                        }
                        break;
                    case VIDEO_QUIT:
                        videoHandler.removeCallbacksAndMessages(null);
                        quiThreadHandler(videoHandlerThread);
                        break;
                }
            }
        };
    }

    private void stopVideo(){
        if (!videoRecording){
            return;
        }
        Log.i(TAG,"stopVideo");
        videoRecording = false;
        videoHandler.removeCallbacksAndMessages(null);
        videoHandler.sendEmptyMessage(VIDEO_QUIT);
        try {
            videoHandlerThread.join();
            Log.i(TAG,"videoHandlerThread end");
        } catch (InterruptedException e) {
            e.printStackTrace();
            Log.i(TAG,"videoHandlerThread end InterruptedException:"+e.getCause().getMessage());
        }
        if (mVideoEnc!=null){
            mVideoEnc.stop();
            mVideoEnc.release();
            mVideoEnc = null;
        }
        mVideoTrack = -1;
    }

    private void getFramMsg(Message message){
        FramData framData = (FramData) message.obj;
        byte[] frams = framData.getDatas();
        int index = mVideoEnc.dequeueInputBuffer(-1);
        long time = (System.nanoTime() - startNanoTime)/1000L;
        //Log.e(TAG,"读取到视频帧："+time+"******************");
        framCoun++;
        if (index>=0){
            NV21toI420SemiPlanar(frams,yuv420,width,height);
            ByteBuffer buffer = getInputBuffer(mVideoEnc,index);
            buffer.clear();
            buffer.put(yuv420);
            int flag = videoRecording?0:MediaCodec.BUFFER_FLAG_END_OF_STREAM;
            mVideoEnc.queueInputBuffer(index,0,yuv420.length,time,flag);
        }
        MediaCodec.BufferInfo mInfo=new MediaCodec.BufferInfo();
        int outIndex = 0;
       while (outIndex>=0){
           outIndex = mVideoEnc.dequeueOutputBuffer(mInfo,0);
           //Log.e(TAG,"------获取到视频帧信息 flag："+mInfo.flags+"--offset:"+mInfo.offset+"--timeUs:"+mInfo.presentationTimeUs+"--size:"+mInfo.size);
           if (mInfo.flags == MediaCodec.BUFFER_FLAG_END_OF_STREAM){
               Log.e(TAG,"读取到video结束标志:"+time+"***********"+outIndex);
               break;
           }
           if (outIndex>=0&&mMuxerStart){
               /*if (MediaCodec.BUFFER_FLAG_CODEC_CONFIG == outIndex){
                   Log.i("readPPs","-----------------config 信息==++++++++");
               }*/
                ByteBuffer outBuffer = getOutputBuffer(mVideoEnc,outIndex);
                //readPPs(outBuffer,mInfo);
                if (mVideoTrack>=0&&mAudioTrack>=0&&mInfo.size>0&&mInfo.presentationTimeUs>0){
                    try {
                        mMuxer.writeSampleData(mVideoTrack,outBuffer,mInfo);
                    }catch (Exception e){
                        Log.e(TAG,"video error:size="+mInfo.size+"/offset="
                                +mInfo.offset+"/timeUs="+mInfo.presentationTimeUs);
                        Log.e(TAG,"-->"+e.getMessage());
                    }
                }
               mVideoEnc.releaseOutputBuffer(outIndex,false);
           }else if (outIndex==MediaCodec.INFO_OUTPUT_FORMAT_CHANGED){
               addTrack(1);
               // The PPS and PPS shoud be there
               MediaFormat format = mVideoEnc.getOutputFormat();
               ByteBuffer spsb = format.getByteBuffer("csd-0");
               ByteBuffer ppsb = format.getByteBuffer("csd-1");
               byte[] data1 = new byte[spsb.array().length];
               spsb.get(data1,0,spsb.array().length);
               Log.e("readPPs","data1 INFO_OUTPUT_FORMAT_CHANGED:");
           }else {
               break;
                //Log.i(TAG,"读取到video其他状态:"+outIndex);
           }
       }
    }

    private void readPPs(ByteBuffer byteBuffer,MediaCodec.BufferInfo mInfo){
      try {
          if (byteBuffer==null||mInfo==null){
              return;
          }
          if (mInfo.size<=0){
              return;
          }
          byte[] data = new byte[mInfo.size];
          byteBuffer.get(data,0,mInfo.size);
          if (data.length>6){
              Log.i("readPPs","readPPs:"+byteToHex(data[0])+byteToHex(data[1])+byteToHex(data[2])+byteToHex(data[3])
                      +byteToHex(data[4])+byteToHex(data[5]));
          }else {
              Log.e("readPPs","readPPs:"+data.length);
          }
         // byteBuffer.reset();

          //byteBuffer.reset();
          /*ByteBuffer copyBuffer = ByteBuffer.wrap(byteBuffer.array());
          byte[] data = copyBuffer.array();*/
      }catch (Exception e){
          Log.e("readPPs","readPPs Exception:"+e.getMessage());
      }
    }

    /**
     * 字节转十六进制
     * @param b 需要进行转换的byte字节
     * @return  转换后的Hex字符串
     */
    public static String byteToHex(byte b){
        String hex = Integer.toHexString(b & 0xFF);
        if(hex.length() < 2){
            hex = "0" + hex;
        }
        return hex+",";
    }

    private ByteBuffer getInputBuffer(MediaCodec codec,int index){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            return codec.getInputBuffer(index);
        }else{
            return codec.getInputBuffers()[index];
        }
    }

    private ByteBuffer getOutputBuffer(MediaCodec codec,int index){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            return codec.getOutputBuffer(index);
        }else{
            return codec.getOutputBuffers()[index];
        }
    }

    /**
     * 写入一帧数据
     * @param data
     * @return true编码继续 false结束视频录制
     */
    public boolean feedData(byte[] data){
        if (!videoRecording){
            return false;
        }
        long time = System.nanoTime()/1000L;
        //Log.i(TAG,"喂入一帧数据："+time);
        videoHandler.sendMessage(generatFramMessage(data,time));
        feedCount++;
        return true;
    }

    private Message generatFramMessage(byte[] framData,long time){
        Message message;
        if (videoHandler!=null){
            FramData fram;
            message = Message.obtain(videoHandler);
            if (message.obj instanceof FramData){
                fram = (FramData) message.obj;
                fram.setDatas(framData);
                fram.setTime(time);
            }else {
                fram = new FramData(framData,time);
            }
            message.obj = fram;
        }else {
            message = Message.obtain();
            message.obj = new FramData(framData,time);
        }
        message.what = VIDEO_FRAM;
        return message;
    }

    /**
     * Nv21 格式转成 YUV420
     * @param nv21bytes
     * @param i420bytes
     * @param width
     * @param height
     */
    private static void NV21toI420SemiPlanar(byte[] nv21bytes, byte[] i420bytes, int width, int height) {
        DataTransfer.nv21ToYuv420(nv21bytes,i420bytes,width,height);
        /*System.arraycopy(nv21bytes, 0, i420bytes, 0, width * height);
        for (int i = width * height; i < nv21bytes.length; i += 2) {
            i420bytes[i] = nv21bytes[i + 1];
            i420bytes[i + 1] = nv21bytes[i];
        }*/
    }
    /********************** video **************/

    /**
     *
     * @param flag 1 视频  2 音频
     * @return
     */
    private void addTrack(int flag){
        if (flag == 1){
            mVideoTrack = mMuxer.addTrack(mVideoEnc.getOutputFormat());
            Log.e(TAG,"添加video轨mVideoTrack:"+mVideoTrack);
        }else if (flag == 2){
            mAudioTrack = mMuxer.addTrack(mAudioEnc.getOutputFormat());
            Log.e(TAG,"添加Audio轨mAudioTrack:"+mAudioTrack);
        }
        if (mVideoTrack>=0&&mAudioTrack>=0){
            mMuxer.start();
            mMuxerStart = true;
            Log.e(TAG," mMuxer.start 开始编码录制:");
        }
    }

    private void quiThreadHandler(HandlerThread thread){
        if (thread==null){
            return;
        }
        if ( android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2){
            thread.quitSafely();
        }else {
            thread.quit();
        }
    }

    private String getFilePath(){
        File folder = new File(FOLDER);
        if (!folder.exists()){
            folder.mkdirs();
        }
        String fileName = System.currentTimeMillis()/1000 +".mp4";
        File file = new File(FOLDER+File.separator+fileName);
        if (file.exists()){
            file.delete();
        }
        try {
            file.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return file.getPath();
    }

    private int checkColorFormat(String mime){
        if(Build.MODEL.equals("HUAWEI P6-C00")){
            convertType=DataConvert.BGRA_YUV420SP;
            return MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar;
        }
        for (int i = 0; i< MediaCodecList.getCodecCount(); i++){
            MediaCodecInfo info= MediaCodecList.getCodecInfoAt(i);
            if(info.isEncoder()){
                String[] types=info.getSupportedTypes();
                for (String type:types){
                    if(type.equals(mime)){
                        Log.e("YUV","type-->"+type);
                        MediaCodecInfo.CodecCapabilities c=info.getCapabilitiesForType(type);
                        Log.e("YUV","color-->"+ Arrays.toString(c.colorFormats));
                        for (int j=0;j<c.colorFormats.length;j++){
                            if (c.colorFormats[j]==MediaCodecInfo.CodecCapabilities
                                    .COLOR_FormatYUV420Planar){
                                convertType=DataConvert.RGBA_YUV420P;
                                return MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Planar;
                            }else if(c.colorFormats[j]==MediaCodecInfo.CodecCapabilities
                                    .COLOR_FormatYUV420SemiPlanar){
                                convertType=DataConvert.RGBA_YUV420SP;
                                return MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar;
                            }
                        }
                    }
                }
            }
        }
        convertType=DataConvert.RGBA_YUV420SP;
        return MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar;
    }

}
