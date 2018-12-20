package com.ruisasi.core;

import android.util.Log;

import com.genymobile.scrcpy.IO;
import com.wangheart.rtmpfile.rtmp.model.ApcMsg;

import java.io.FileDescriptor;

public class SocketSendThread extends Thread {
    public boolean flag =true;
    private FileDescriptor fd;
    private  SystemInfo si;
    public SocketSendThread(FileDescriptor fd,SystemInfo si){
            this.fd = fd;
            this.si = si;
    }
    @Override
    public void run() {
        super.run();
        while(flag) {
            //生成头部
            Log.i(StartActivity.TAG,"SocketSendThread run()" );
            ApcMsg apc = new ApcMsg(ApcMsg.CMD_ID_HEAT_BEAT);
            //6 7位定义长度
            // 生成 身体 身体长度位10
            byte[] body = CommandParser.HeartPack(si.IP,"i0:08:22:98:CD:FB");

            try {
//                合成长度
                byte[] head = CommandParser.SetcombinLen(apc.getBytes(), body.length);

                IO.writeFully(fd,head,0,head.length);
                IO.writeFully(fd,body,0,body.length);
            }catch (Exception e){
                Log.i(StartActivity.TAG,"StartActivity Exception 86line" );
                flag = false;
            }

            MainService.Sleep(10000);

        }

    }
}
