package com.apical.ipcamtest;

import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.StrictMode;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Button;
import android.widget.ListView;

import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.Inet4Address;
import java.net.NetworkInterface;
import java.util.Enumeration;

public class MainActivity extends AppCompatActivity {
    private IpcamDetector     mIpcamDetector;
    private ListView          mIPCamListView;
    private ArrayAdapter      mListAdapter;
    private Button            mBtnPairCam;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (android.os.Build.VERSION.SDK_INT > 9) {
            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
            StrictMode.setThreadPolicy(policy);
        }
        String broadcastip = "255.255.255.255";
        try {
            byte[] byte4ipaddr = getLocalInetAddress().getAddress();
            broadcastip = String.format("%d.%d.%d.255", (int)byte4ipaddr[0] & 0xff, (int)byte4ipaddr[1] & 0xff, (int)byte4ipaddr[2] & 0xff);
        } catch (Exception e) { e.printStackTrace(); }

        mIpcamDetector = new IpcamDetector(broadcastip, mHandler);
        mIPCamListView = (ListView)findViewById(R.id.lv_ipcams);
        mListAdapter   = new ArrayAdapter(this, R.layout.item, mIpcamDetector.getIpcamList());
        mBtnPairCam    = (Button)findViewById(R.id.btn_pair_camera);
        mIPCamListView.setAdapter(mListAdapter);
        mIPCamListView.setOnItemClickListener(mOnItemClickListener);
        mBtnPairCam.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                final AlertDialog dialog = builder.create();
                View dialogView = View.inflate(MainActivity.this, R.layout.dialog, null);
                dialog.setView(dialogView);
                dialog.show();
                final EditText et_name  = dialogView.findViewById(R.id.edt_wifiap_name  );
                final EditText et_pwd   = dialogView.findViewById(R.id.edt_wifiap_passwd);
                final Button btn_cancel = dialogView.findViewById(R.id.btn_cancel );
                final Button btn_confirm= dialogView.findViewById(R.id.btn_confirm);
                btn_cancel.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        dialog.dismiss();
                    }
                });
                btn_confirm.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Intent i  = new Intent(MainActivity.this, QRCodeBarActivity.class);
                        i.putExtra("wifiap_name", et_name.getText().toString());
                        i.putExtra("wifiap_pwd" , et_pwd .getText().toString());
                        startActivity(i);
                        dialog.dismiss();
                    }
                });
            }
        });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onResume() {
        super.onResume();
        mIpcamDetector.start();
    }

    @Override
    public void onPause() {
        super.onPause();
        mIpcamDetector.stop();
    }

    private AdapterView.OnItemClickListener mOnItemClickListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            final String[] opertions = new String[] { getString(R.string.play_rtsp_video), getString(R.string.play_avkcp_video), getString(R.string.play_ffrdp_video) };
            final String strItem = mIPCamListView.getItemAtPosition(position).toString();
            AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
            builder.setItems(opertions, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int which) {
                    switch (which) {
                    case 0:
                        try {
                            String[] ss = strItem.split("\\s+");
                            Uri uri = Uri.parse(String.format("rtsp://%s/livecam", ss[1]));
                            Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                            intent.setComponent(new ComponentName("com.apical.ipcamtest", "com.rockcarry.fanplayer.PlayerActivity"));
                            startActivity(intent);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        break;
                    case 1:
                        try {
                            String[] ss = strItem.split("\\s+");
                            Uri uri = Uri.parse(String.format("avkcp://%s:8000", ss[1]));
                            Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                            intent.setComponent(new ComponentName("com.apical.ipcamtest", "com.rockcarry.fanplayer.PlayerActivity"));
                            startActivity(intent);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        break;
                    case 2:
                        try {
                            String[] ss = strItem.split("\\s+");
                            Uri uri = Uri.parse(String.format("ffrdp://%s:8000", ss[1]));
                            Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                            intent.setComponent(new ComponentName("com.apical.ipcamtest", "com.rockcarry.fanplayer.PlayerActivity"));
                            startActivity(intent);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        break;
                    }
                }
            });
            builder.show();
        }
    };

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
            case IpcamDetector.MSG_LIST_CHANGED:
                mListAdapter.notifyDataSetChanged();
                break;
            }
        }
    };

    static private InetAddress getLocalInetAddress() {
        try {
            for (Enumeration<NetworkInterface> enNetI = NetworkInterface.getNetworkInterfaces(); enNetI.hasMoreElements(); ) {
                NetworkInterface netI = enNetI.nextElement();
                for (Enumeration<InetAddress> enumIpAddr = netI.getInetAddresses(); enumIpAddr.hasMoreElements(); ) {
                    InetAddress inetAddress = enumIpAddr.nextElement();
                    if (inetAddress instanceof Inet4Address && !inetAddress.isLoopbackAddress()) {
                        return inetAddress;
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
