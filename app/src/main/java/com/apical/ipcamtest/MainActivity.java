package com.apical.ipcamtest;

import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

public class MainActivity extends AppCompatActivity {
    private IpcamDetector     mIpcamDetector;
    private ListView          mIPCamListView;
    private ArrayAdapter      mListAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mIpcamDetector = new IpcamDetector("255.255.255.255", mHandler);
        mIPCamListView = (ListView)findViewById(R.id.lv_ipcams);
        mListAdapter   = new ArrayAdapter(this, R.layout.item, mIpcamDetector.getIpcamList());
        mIPCamListView.setAdapter(mListAdapter);
        mIPCamListView.setOnItemClickListener(mOnItemClickListener);
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
            final String[] opertions = new String[] { getString(R.string.admin_device), getString(R.string.watch_video) };
            final String strItem = mIPCamListView.getItemAtPosition(position).toString();
            AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
            builder.setItems(opertions, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int which) {
                    switch (which) {
                    case 0:
                        try {
                            String[] ss = strItem.split(" ");
                            Uri uri = Uri.parse(String.format("http://%s", ss[1]));
                            Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                            startActivity(intent);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        break;
                    case 1:
                        try {
                            String[] ss = strItem.split(" ");
                            Uri uri = Uri.parse(String.format("rtsp://%s/video0", ss[1]));
                            Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                            intent.setComponent(new ComponentName("com.apical.ipcamtest", "com.apical.fanplayer.PlayerActivity"));
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
}
