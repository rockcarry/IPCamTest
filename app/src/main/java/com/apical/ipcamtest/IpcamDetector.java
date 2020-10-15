package com.apical.ipcamtest;

import android.os.Handler;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.ArrayList;

class IpcamDev {
    public long   tick;
    public String ip;
    public String uid;
}

public class IpcamDetector {
    public static final int MSG_LIST_CHANGED = 1;

    private final static boolean DEBUG          = false;
    private final static int     PORT           = 8313;
    private final static int     DEVLOST_TIMEOUT= 5000;
    private DatagramSocket       mSocket        = null;
    private Thread               mTxThread      = null;
    private Thread               mRxThread      = null;
    private boolean              mStopFlag      = false;
    private DatagramPacket       mTxPacket      = null;
    private DatagramPacket       mRxPacket      = null;
    private byte[]               mRxData        = new byte[256];
    private IpcamDev[]           mDevList       = new IpcamDev[256];
    private ArrayList<String>    mIpcamList     = new ArrayList<String>();
    private Handler              mHandler       = null;
    private String               mIPAddess      = "255.255.255.255";

    public IpcamDetector(String ip, Handler handler) {
        for (int i=0; i<mDevList.length; i++) mDevList[i] = new IpcamDev();
        byte[] senddata = "uid?".getBytes();
        try {
            mSocket = new DatagramSocket();
            mSocket.setSoTimeout(1000);
            mTxPacket = new DatagramPacket(senddata, senddata.length, InetAddress.getByName(ip), PORT);
            mRxPacket = new DatagramPacket(mRxData, mRxData.length);
        } catch (Exception e) { e.printStackTrace(); }
        mHandler = handler;
        mIPAddess= ip;
    }

    public void start() {
        mStopFlag = false;
        if (mTxThread == null) {
            mTxThread = new Thread() {
                @Override
                public void run() {
                    while (!mStopFlag) {
//                      System.out.println("tx thread running...");
                        try {
                            mSocket.send(mTxPacket);
                        } catch (Exception e1) {
                            e1.printStackTrace();
                            try {
                                byte[] senddata = "uid?".getBytes();
                                mTxPacket = new DatagramPacket(senddata, senddata.length, InetAddress.getByName(mIPAddess), PORT);
                            } catch (Exception e2) {}
                        }
                        try {
                            Thread.sleep(1000);
                        } catch (Exception e) { e.printStackTrace(); }
                    }
                    mTxThread = null;
                }
            };
            mTxThread.start();
        }
        if (mRxThread == null) {
            mRxThread = new Thread() {
                @Override
                public void run() {
                    while (!mStopFlag) {
//                      System.out.println("rx thread running...");
                        boolean recvok = true;
                        long    curtick= 0;
                        boolean changed= false;

                        try { mSocket.receive(mRxPacket); } catch (Exception e) { recvok = false; }
                        curtick = System.currentTimeMillis();

                        if (recvok) {
                            String data = new String(mRxPacket.getData()).trim();
                            String ip   = new String(mRxPacket.getAddress().getHostAddress());
                            int    n    = Integer.parseInt(ip.substring(ip.lastIndexOf(".") + 1));
//                          System.out.println("received data = " + data);

                            mDevList[n].ip  = ip;
                            mDevList[n].uid = data.startsWith("uid:") ? data.substring(4) : "";
                            if (mDevList[n].uid.equals("")) {
                                mDevList[n].uid = "unknown";
                            }
                            if (mDevList[n].tick == 0) {
//                              System.out.println("device found: uid = " + mDevList[n].uid + ", ip = " + mDevList[n].ip);
                                changed = true;
                            }
                            mDevList[n].tick= curtick;
                        }

                        for (int i=0; i<mDevList.length; i++) {
                            if (mDevList[i].tick != 0 && curtick - mDevList[i].tick > DEVLOST_TIMEOUT) {
//                              System.out.println("device lost: uid = " + mDevList[i].uid + ", ip = " + mDevList[i].ip);
                                changed = true;
                                mDevList[i].tick = 0;
                                mDevList[i].ip   = "";
                                mDevList[i].uid  = "";
                            }
                        }

                        if (changed) {
                            if (DEBUG) {
                                boolean nodev = true;
                                System.out.println("\ndevice list:");
                                System.out.println("----------------------------------");
                                for (int i=0; i<mDevList.length; i++) {
                                    if (mDevList[i].tick != 0) {
                                        System.out.println(mDevList[i].uid + " " + mDevList[i].ip);
                                        nodev = false;
                                    }
                                }
                                if (nodev) System.out.println("no device.");
                                System.out.println("\n\n");
                            }

                            mIpcamList.clear();
                            for (int i=0; i<mDevList.length; i++) {
                                if (mDevList[i].tick != 0) {
                                    mIpcamList.add(String.format("%-20s %s", mDevList[i].uid, mDevList[i].ip));
                                }
                            }
                            if (mHandler != null) {
                                mHandler.sendEmptyMessage(MSG_LIST_CHANGED);
                            }
                        }
                    }
                    mRxThread = null;
                }
            };
            mRxThread.start();
        }
    }

    public void stop() {
        mStopFlag = true;
        if (mTxThread != null) {
            try { mTxThread.join(); } catch (Exception e) { e.printStackTrace(); }
        }
        if (mRxThread != null) {
            try { mRxThread.join(); } catch (Exception e) { e.printStackTrace(); }
        }
    }

    public ArrayList<String> getIpcamList() {
        return mIpcamList;
    }

    /*
    public static void main(String args[]) {
        String ip = args.length > 0 ? new String(args[0]) : "255.255.255.255";
        IpcamDetector detector = new IpcamDetector(ip);
        detector.start();
        while (true) {
            int key = 0;
            try { key =  System.in.read(); } catch (Exception e) { e.printStackTrace(); }
            if (key == 'q' || key == 'Q') break;
        }
        detector.stop();
    }*/
}

