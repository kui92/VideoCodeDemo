package com.linglong.videocode;
import java.nio.ByteBuffer;

import android.media.MediaCodec;
import android.media.MediaFormat;
import android.os.Build;
import android.view.SurfaceView;


public class H264Dec{


	private MediaCodec codec;
	public boolean bFristDec = false;
	private SurfaceView sfview;
	private int type = 2;
	String MIME_TYPE = "video/avc";
	public H264Dec(SurfaceView holderview) {
		sfview = holderview;
		type = 2;
	}
	public void createCodec() {
		if(Build.VERSION.SDK_INT < 16){
			return;
		}
		//codec = MediaCodec.createDecoderByType(MIME_TYPE);

	}
	public void tryConfig(int width,int height,byte[] sps,byte[] pps){
		if(codec == null) return;
		MediaFormat mediaFormat = MediaFormat.createVideoFormat(MIME_TYPE,
				width, height);
		if(sps != null || pps != null){
			mediaFormat.setByteBuffer("csd-0", ByteBuffer.wrap(sps));
			mediaFormat.setByteBuffer("csd-1", ByteBuffer.wrap(pps));
		}
		codec.configure(mediaFormat, sfview.getHolder().getSurface(), null, 0);
//		codec.setVideoScalingMode(/*2*/type);
		codec.start();
	};
	public void reConfig(int width,int height,byte[] sps,byte[] pps){
		try{
			releaseCodec();//���ͷŽ�����
		}catch(Exception e){}

		createCodec();//�ٳ�ʼ��������

		tryConfig(width,height,sps,pps);

	}
	public void releaseCodec() {
		if(codec!= null){
			try {
				codec.stop();
				codec.release();
			} catch (Exception e) {
				e.printStackTrace();
			}finally{
				codec = null;
			}
		}
	}

	private void decodeAndPlayBack(byte[] in, int offset, int length,int type) {
		ByteBuffer[] inputBuffers = codec.getInputBuffers();
		int inputBufferIndex = codec.dequeueInputBuffer(-1);
		if (inputBufferIndex >= 0) {
			ByteBuffer inputBuffer = inputBuffers[inputBufferIndex];
			inputBuffer.clear();
			inputBuffer.put(in, offset, length);
			codec.queueInputBuffer(inputBufferIndex, 0, length, 0, type);
		}
		MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
		int outputBufferIndex = codec.dequeueOutputBuffer(bufferInfo, 0);
		if (outputBufferIndex >= 0) {
			codec.releaseOutputBuffer(outputBufferIndex, true);
		}
	}

	public void PlayDecode(byte[] recBuffer,int type){
		if (codec != null) {
			decodeAndPlayBack(recBuffer, 0, recBuffer.length, type);
		}
	}
}
