package com.ruisasi.core;

import android.util.Log;

import com.genymobile.scrcpy.ControlEvent;
import com.genymobile.scrcpy.ControlEventReader;
import com.genymobile.scrcpy.IO;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;

public class KeyMouseSend extends Thread{
    private final ControlEventReader reader = new ControlEventReader();
    private  InputStream inputStream;

    @Override
    public void run() {
        super.run();

    }

    public KeyMouseSend(Socket s){
        Log.i("StartActivity","KeyMouseSend() construct success!");

        try {
            inputStream = s.getInputStream();
        }catch (IOException e){
            Log.i("StartActivity","KeyMouseSend() get inputstream failed");
        }
    }

    public ControlEvent receiveControlEvent() throws IOException {

        ControlEvent event = reader.next();
        while (event == null) {
            reader.readFrom(inputStream);
            event = reader.next();
        }
        return event;
    }

}
