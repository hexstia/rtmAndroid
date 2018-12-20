package com.ruisasi.core;


import android.system.Os;
import android.util.Log;

import com.genymobile.scrcpy.EventController;
import com.genymobile.scrcpy.IO;
import com.wangheart.rtmpfile.rtmp.RtmpHandle;
import com.wangheart.rtmpfile.rtmp.model.ApcMsg;

import java.io.FileDescriptor;
import java.io.IOException;
import android.os.Handler;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;


public class SocketRecvThread extends Thread{
    private FileDescriptor fd;
    private Socket socket;
    public boolean flag = true;
    private VideoStreamSend vss;

    private Handler handler;
    byte bs[] = new byte[16];
    public SocketRecvThread(FileDescriptor fd, Socket socket, VideoStreamSend vss,  Handler handler){
    this.fd = fd;
    this.socket = socket;
    this.vss = vss;

    this.handler = handler;
    }

    @Override
    public void run() {
        super.run();
        while(flag){
                if(socket.isConnected()){

                    try{
                      //  Log.i(StartActivity.TAG,"coming SocketRecvThread run()");

//                        int len = Os.read(fd,bs,0,bs.length);
                        InputStream is =  socket.getInputStream();
                       int len =  is.read(bs,0,bs.length);

                        if(len ==0||len ==-1){
                            Log.i(StartActivity.TAG,"lenth :"+len);
                            MainService.Sleep(1000);
                            continue;
                        }
                        Log.i(StartActivity.TAG,"读取内容的长度"+len);
                    }catch (Exception e){
                        Log.i(StartActivity.TAG,"READ 函数不行");
                        flag = false;
            }
                    ApcMsg apc = new ApcMsg(new ApcMsg.MsgHead(bs));

                    if(apc.getCmdId() == 0x06){//打开屏幕控制

                        vss = new VideoStreamSend(socket);
                        vss.start();//屏幕与键盘


                        //长度合成的头
                        byte[] head = CommandParser.SetcombinLen(bs, 1);
                        try{
                            Log.i(StartActivity.TAG,"打开流");
                            //写入头
                            IO.writeFully(fd,head,0,head.length);
                            //生成身体
                            byte[] body = CommandParser.Videopacket(true);

                            IO.writeFully(fd,body,0,body.length);

                        }catch (IOException e){

                        }

                    }
                    if(apc.getCmdId() == 0x07){//关闭屏幕控制
                        //长度合成的头
                        byte[] head = CommandParser.SetcombinLen(bs, 1);
                        if(vss!=null) {
                            VideoStreamSend.stopStream = false;
//                                RtmpHandle.getInstance().close();
                            if(vss.isAlive()){
                                vss.stop();
                                RtmpHandle.getInstance().close();
                            }

                            vss = null;
                        }

                        try{
                            Log.i(StartActivity.TAG,"关闭流");
                            //写入头
                            IO.writeFully(fd,head,0,head.length);
                            //生成身体
                            byte[] body = CommandParser.Videopacket(true);

                            IO.writeFully(fd,body,0,body.length);

                        }catch (IOException e){
                        }
                    }
//==================================================================================================
                    if(apc.getCmdId() == 0x08){//键盘事件
                        //唤醒
                        synchronized (EventController.flag) {
                            try {

                                EventController.flag.notifyAll();
                                MainService.sleepBitcon = true;//不唤醒
                                EventController.flag = false;//唤醒
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }

//                        int len = apc.getTotalLen();
//                        Log.i("StartActivity","tTotal"+len);
//                        len = len-16;
//                        byte[] content = new byte[len];
//                        try {
//                            Os.read(fd,content,0,len);
//                        } catch (ErrnoException e) {
//                            e.printStackTrace();
//                        } catch (InterruptedIOException e) {
//                            e.printStackTrace();
//                        }
//                        for(int i = 0;i<content.length;i++){
//                            Log.i(StartActivity.TAG,"数据值"+ i +": "+content[i]);
//                        }

                        //长度合成的头
                        byte[] head = CommandParser.SetcombinLen(bs, 1);
                        try{
                            Log.i(StartActivity.TAG,"键盘鼠标事件");
                            //写入头
                            IO.writeFully(fd,head,0,head.length);
                            //生成身体
                            byte[] body = CommandParser.Videopacket(true);

                            IO.writeFully(fd,body,0,body.length);

                        }catch (IOException e){
                        }

                    }

            }else{
                    Log.i(StartActivity.TAG,"SocketRecvThread  Line 111 链接失败");
                }
        }
    }
}
