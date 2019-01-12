package com.linglong.videocode;

import android.media.MediaCodec;
import android.media.MediaFormat;
import android.util.Log;
import android.view.Surface;

import java.nio.ByteBuffer;

import static java.lang.Thread.sleep;

public class VideoDecoder2{
/*
    private static final String TAG = "VideoDecode";
    private static final boolean DEBUG_VIDEO = false;

    public VideoDecoder(String videoFilePath, Surface surface, UpstreamCallback callback) {
        super(videoFilePath, surface, callback);
    }

    @Override
    public void run() {
        // TODO Auto-generated method stub
        super.run();
        VideoDecodePrepare();
    }

    public void VideoDecodePrepare() {
        try {
            for (int i = 0; i < mExtractor.getTrackCount(); i++) {
                MediaFormat format = mExtractor.getTrackFormat(i);
                String mime = format.getString(MediaFormat.KEY_MIME);
                if (mime.startsWith("video/")) {
                    mExtractor.selectTrack(i);
                    mDecoder = MediaCodec.createDecoderByType(mime);
                    if (mCallback != null) {
                        mDecoder.configure(format, null, null, 0);  //decode flag no output for surface
                    } else {
                        mDecoder.configure(format, mSurface, null, 0);  //decode flag output to surface
                    }
                    break;
                }
            }

            if (mDecoder == null) {
                Log.e(TAG, "Can't find video info!");
                return;
            }
            mDecoder.start();
            ByteBuffer[] inputBuffers = mDecoder.getInputBuffers();
            ByteBuffer[] outputBuffers = mDecoder.getOutputBuffers();
            boolean isEOS = false;
            long startMs = System.currentTimeMillis();
            while (!Thread.interrupted()) {
                if (!isEOS) {
                    int inIndex = mDecoder.dequeueInputBuffer(TIME_US);
                    if (inIndex >= 0) {
                        ByteBuffer buffer = inputBuffers[inIndex];
                        int sampleSize = mExtractor.readSampleData(buffer, 0);
                        if (sampleSize < 0) {
                            // We shouldn't stop the playback at this point, just pass the EOS
                            // flag to mDecoder, we will get it again from the
                            // dequeueOutputBuffer
                            Log.d(TAG, "InputBuffer BUFFER_FLAG_END_OF_STREAM");
                            mDecoder.queueInputBuffer(inIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                            isEOS = true;
                        } else {
                            mDecoder.queueInputBuffer(inIndex, 0, sampleSize, mExtractor.getSampleTime(), 0);
                            mExtractor.advance();
                        }
                    }
                }
                MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
                int outIndex = mDecoder.dequeueOutputBuffer(info, TIME_US);
                switch (outIndex) {
                    case MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED:
                        Log.d(TAG, "INFO_OUTPUT_BUFFERS_CHANGED");
                        outputBuffers = mDecoder.getOutputBuffers();
                        break;
                    case MediaCodec.INFO_OUTPUT_FORMAT_CHANGED:
                        Log.d(TAG, "New format " + mDecoder.getOutputFormat());
                        break;
                    case MediaCodec.INFO_TRY_AGAIN_LATER:
                        Log.d(TAG, "dequeueOutputBuffer timed out!");
                        break;
                    default:
                        //here erro?
                        Log.d(TAG, "outIndex:" + outIndex);
                        ByteBuffer buffer = outputBuffers[outIndex];
                        Log.d(TAG, "ByteBuffer limit:" + buffer.limit() + " info size:" + info.size);
                        final byte[] chunk = new byte[info.size];
                        buffer.get(chunk);
                        if (mCallback != null) {
                            //mCallback.UpstreamCallback(chunk,info.size);
                        }
                        //clear buffer,otherwise get the same buffer which is the last buffer
                        buffer.clear();
                        if (DEBUG_VIDEO)
                            Log.v(TAG, "We can't use this buffer but render it due to the API limit, " + buffer);
                        // We use a very simple clock to keep the video FPS, or the video
                        // playback will be too fast
                        while (info.presentationTimeUs / 1000 > System.currentTimeMillis() - startMs) {
                            try {
                                sleep(10);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                                break;
                            }
                        }
                        mDecoder.releaseOutputBuffer(outIndex, true);
                        break;
                }
                // All decoded frames have been rendered, we can stop playing now
                if ((info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                    Log.d(TAG, "OutputBuffer BUFFER_FLAG_END_OF_STREAM");
                    break;
                }
            }
            mDecoder.stop();
            mDecoder.release();
            mExtractor.release();
        } catch (Exception ioe) {
            Log.d(TAG, "failed init decoder", ioe);
        }
    }
*/

}
