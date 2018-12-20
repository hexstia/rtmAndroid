package com.genymobile.scrcpy;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;

import com.wangheart.rtmpfile.R;
import com.wangheart.rtmpfile.ffmpeg.FFmpegHandle;
import com.wangheart.rtmpfile.rtmp.RtmpHandle;

public class ProjectActivtiy extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        FFmpegHandle.init(this);
        RtmpHandle.getInstance().connect("rtmp://39.106.105.131:1935/live/10000");
        


    }
}
