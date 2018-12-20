package com.ruisasi.core;

import android.util.Log;

import java.io.FileDescriptor;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketImpl;
import java.net.SocketTimeoutException;

public class SocketSend {
       private static  Socket socket;
        private static SocketSend socketSend = new SocketSend();

    public static Socket getSocket() {
        return socket;
    }





}
