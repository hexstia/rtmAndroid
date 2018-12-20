package com.ruisasi.core;

import android.util.Log;

import com.genymobile.scrcpy.EventController;

import java.net.Socket;

public class NotifyThread extends Thread {
;
    public boolean flag =true;
    public NotifyThread(){

    }


    @Override
    public void run() {
        super.run();
        while(flag){
            if(!MySocket.socket.isConnected()){
                Log.i("StartActivity","NotifyThread Socket 断开连接");
                synchronized (MainService.sleepBitcon) {
                    try {
                        Log.i("StartActivity"," notify");
                            MainService.sleepBitcon.notifyAll();
                            MainService.sleepBitcon = false;//唤醒
                            EventController.flag = true;//不唤醒
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
           MainService.Sleep(5000);

        }
    }
}
