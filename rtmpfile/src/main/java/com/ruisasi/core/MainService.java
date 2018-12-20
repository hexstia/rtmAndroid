package com.ruisasi.core;

import android.app.Service;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import com.genymobile.scrcpy.EventController;
import com.wangheart.rtmpfile.ffmpeg.FFmpegHandle;
import com.wangheart.rtmpfile.rtmp.RtmpHandle;

import java.io.FileDescriptor;
import java.net.Socket;


public class MainService extends Service {
public static Boolean sleepBitcon =true;
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
    public SystemInfo si;
    private SocketSendThread  sst;
    private SocketRecvThread srt;
    private VideoStreamSend vss;

    private Bundle b;
    private Handler handler;

    public void excute(Socket socket,FileDescriptor fd,SystemInfo si){
        sst = new SocketSendThread(fd,si);
        sst.start();//心跳程序
        srt =    new SocketRecvThread(fd, socket,vss, handler);
        srt.start(); //接受响应程序
        vss = new VideoStreamSend(socket);
        VideoStreamSend.stopStream = true;
        vss.start();//屏幕与键盘
        NotifyThread nt =new NotifyThread();
        nt.start();
    }
    private    MySocket msocket;
    private boolean mainflag = true;

    @Override
    public void onCreate() {
        super.onCreate();
        InitData();
        mainflag = true;
        MainThread mt = new MainThread();
        mt.start();
        Intent i = new Intent(this,AudioStreamService.class);
        startService(i);
    }
    public class MainThread extends Thread{
        @Override
        public void run() {
            msocket = new MySocket();

            while(mainflag){
                msocket.SocketCreat();
//                msocket.socketLink("222.222.120.169",4008);
                msocket.socketLink("192.168.255.103",4008);
                if(msocket.getSocket().isConnected()){
                    //连接成功
                    sleepBitcon = true;
                    Socket socket  =msocket.getSocket();
                    msocket.initSocketFd();//初始化fd;
                    FileDescriptor fd = msocket.getSocketFd();
                    //开启线程
                    VideoStreamSend.stopStream = true;
                    excute(socket,fd,si);
                    if(msocket.getSocket().isConnected()&&sst!=null&&sst.isAlive()&&mainflag){
                        //一直循环直到失去连接

                        synchronized (sleepBitcon) {
                            try {
                            while(sleepBitcon) {
                                sleepBitcon.wait();
                                Log.i("StartActivity","coming waiting");
                            }
                            } catch (InterruptedException e) {
                                // TODO Auto-generated catch block
                                e.printStackTrace();
                            }
                        }
                        Log.i("StartActivity","HAHAHA");
                        onStop();
                    }
                    Log.i("StartActivity","aaaaaaa");
                }else{//连接失败
                    Sleep(3000);
                    Log.i("StartActivity","连接失败，正在重连");
                }
            }
        }

    }
    public void InitData(){ //获取系统信息
        si =  SystemInfo.getInstance(this);
        Log.i(StartActivity.TAG,si.IP);
        FFmpegHandle.init(this);
    }
    public static void Sleep(long s){
        try {
            Thread.sleep(s);
        }catch (InterruptedException e){}
    }
    public void onStop(){

        if(vss!=null) {//视频流线程
            VideoStreamSend.stopStream = false;
            if(vss.isAlive()){
                RtmpHandle.getInstance().close();
            }
            synchronized (EventController.flag) {
                EventController.flag.notifyAll();
                EventController.flag = false;//唤醒
            }
        }
        //音频线程
        Intent i = new Intent(this,AudioStreamService.class);
        stopService(i);
//        msocket = null;//主线程
        if(srt !=null&&sst!=null) {
            srt.flag = false;//接受线程
            sst.flag = false;//发送线程
            srt = null;
            sst = null;
        }
    }
    @Override
        public void onDestroy() {
        super.onDestroy();
        onStop();
        mainflag = false;//主线程
    }
}
