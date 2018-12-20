package com.genymobile.scrcpy;

import com.genymobile.scrcpy.wrappers.SurfaceControl;
import com.ruisasi.core.AudioStreamSend;
import com.ruisasi.core.AudioStreamService;
import com.ruisasi.core.VideoStreamSend;
import com.wangheart.rtmpfile.flv.FlvPacker;
import com.wangheart.rtmpfile.flv.Packer;
import com.wangheart.rtmpfile.rtmp.RtmpHandle;
import com.wangheart.rtmpfile.utils.LogUtils;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Rect;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;


import android.os.IBinder;

import android.util.Log;
import android.view.Surface;


import java.io.FileDescriptor;

import java.io.IOException;

import java.nio.ByteBuffer;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

public class ScreenEncoder implements Device.RotationListener {

//	private static final int DEFAULT_FRAME_RATE = 60; // fps
//	private static final int DEFAULT_I_FRAME_INTERVAL = 10; // seconds
	private static final int DEFAULT_FRAME_RATE = 30; // fps
	private static final int DEFAULT_I_FRAME_INTERVAL = 5;
	private static final int REPEAT_FRAME_DELAY = 3; // repeat after 6 frames

	private static final int MICROSECONDS_IN_ONE_SECOND = 1_000_000;

	private final AtomicBoolean rotationChanged = new AtomicBoolean();

	private int bitRate;
	private int frameRate;
	private int iFrameInterval;
	public static  FlvPacker mFlvPacker =null;

	public ScreenEncoder(int bitRate, int frameRate, int iFrameInterval) {
		Log.i("StartActivity","ScreenEncoder() constructor2");
		this.bitRate = bitRate;
		this.frameRate = frameRate;
		this.iFrameInterval = iFrameInterval;
		Log.i("StartActivity","ScreenEncoder() constructor1");
	}

	public ScreenEncoder(int bitRate) {
		this(bitRate, DEFAULT_FRAME_RATE, DEFAULT_I_FRAME_INTERVAL);

	}


	@Override
	public void onRotationChanged(int rotation) {
		rotationChanged.set(true);
	}

	public boolean consumeRotationChange() {
		return rotationChanged.getAndSet(false);
	}


	ExecutorService pushExecutor = Executors.newSingleThreadExecutor();
public void createFlvPack(int width,int height){
	mFlvPacker = new FlvPacker();
	mFlvPacker.initVideoParams(width, height, frameRate);
	mFlvPacker.initAudioParams(16000,2560,false);

//		//flv流的发送
	mFlvPacker.setPacketListener(new Packer.OnPacketListener() {
		@Override
		public void onPacket(final byte[] data, final int packetType) {
			pushExecutor.execute(new Runnable() {
				@Override
				public void run() {
					int ret = RtmpHandle.getInstance().push(data, data.length);
				//	Log.i("RTMPActivity","type：" + packetType + "  length:" + data.length + "  推流结果:" + ret);

				}
			});
		}
	});
}
	public void streamScreen(Device device, FileDescriptor fd) throws IOException {
		MediaFormat format = createFormat(bitRate, frameRate, iFrameInterval);
		device.setRotationListener(this);
		boolean alive;

		try {
			do {
				MediaCodec codec = createCodec();
				IBinder display = createDisplay();
				Rect contentRect = device.getScreenInfo().getContentRect();
				Rect videoRect = device.getScreenInfo().getVideoSize().toRect();
				createFlvPack(videoRect.width(),videoRect.height());
				Log.i("StartActivity","videoRect.width() :"+videoRect.width());
				Log.i("StartActivity","videoRect.height() :"+videoRect.height());
				setSize(format, videoRect.width(), videoRect.height());
				configure(codec, format);
				Surface surface = codec.createInputSurface();
				setDisplaySurface(display, surface, contentRect, videoRect);
				codec.start();
				mFlvPacker.start();
					try {
					alive = encode(codec, fd);
				} finally {
					Log.i("StartActivity","Error ScreenEncoder.java streamScreen()  stop");
					codec.stop();
					destroyDisplay(display);
					codec.release();
					surface.release();
					mFlvPacker.stop();
					mFlvPacker = null;
					if(VideoStreamSend.stopStream ==false){
						break;
					}
				}

			} while (alive);


		} finally {
			device.setRotationListener(null);
		}
	}


	private boolean encode(MediaCodec codec, FileDescriptor fd) throws IOException {
		boolean eof = false;
		MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
		while (!consumeRotationChange() && !eof) {
			int outputBufferId = codec.dequeueOutputBuffer(bufferInfo, -1);
			eof = (bufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0;
			if(VideoStreamSend.stopStream ==false){
				break;
			}
			try {
				if (consumeRotationChange()) {
					// must restart encoding with new size
					break;
				}
				if (outputBufferId >= 0) {
					if((bufferInfo.flags & MediaCodec.BUFFER_FLAG_KEY_FRAME) != 0){

					}
					ByteBuffer codecBuffer = codec.getOutputBuffer(outputBufferId);

					//IO.writeFully(fd, codecBuffer);//想socket接口写入数据 h264数据
					//数据封包
					mFlvPacker.onVideoData(codecBuffer, bufferInfo);


//					channel.write(codecBuffer);
				}
			} finally {
				if (outputBufferId >= 0) {
					codec.releaseOutputBuffer(outputBufferId, false);
				}
			}
		}

		return !eof;
	}

	private static MediaCodec createCodec() throws IOException {
		return MediaCodec.createEncoderByType("video/avc");
	}

	private static MediaFormat createFormat(int bitRate, int frameRate, int iFrameInterval) throws IOException {
		MediaFormat format = new MediaFormat();
		format.setString(MediaFormat.KEY_MIME, "video/avc");
		format.setInteger(MediaFormat.KEY_BIT_RATE, bitRate);

		format.setInteger(MediaFormat.KEY_FRAME_RATE, frameRate);
		format.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
		format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, iFrameInterval);
		// display the very first frame, and recover from bad quality when no new frames
		format.setLong(MediaFormat.KEY_REPEAT_PREVIOUS_FRAME_AFTER,
				MICROSECONDS_IN_ONE_SECOND * REPEAT_FRAME_DELAY / frameRate*10); // µs
		return format;
	}

	private static IBinder createDisplay() {
		return SurfaceControl.createDisplay("scrcpy", false);
	}

	private static void configure(MediaCodec codec, MediaFormat format) {
		codec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
	}

	private static void setSize(MediaFormat format, int width, int height) {
		format.setInteger(MediaFormat.KEY_WIDTH, width);

		format.setInteger(MediaFormat.KEY_HEIGHT, height);
	}

	private static void setDisplaySurface(IBinder display, Surface surface, Rect deviceRect, Rect displayRect) {
		SurfaceControl.openTransaction();
		try {


			SurfaceControl.setDisplaySurface(display, surface);
			SurfaceControl.setDisplayProjection(display, 0, deviceRect, displayRect);
			SurfaceControl.setDisplayLayerStack(display, 0);
		} finally {
			SurfaceControl.closeTransaction();
		}
	}

	private static void destroyDisplay(IBinder display) {
		SurfaceControl.destroyDisplay(display);
	}

}
