package com.zhuchao.android.updateManager;

import java.io.File;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.zhuchao.android.oplayertv.R;

public class UpdateActivity extends Activity {
    private final String TAG = "UpdateActivity-->";
    private TextView textView;
    public static int version, serverVersion;
    public static String versionName, serverVersionName, downloadResult;
    private Button btn;
    private ProgressBar proBar;
    public static receiveVersionHandler handler;


    private UpdateManager manager = UpdateManager.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_update);

        textView = (TextView) findViewById(R.id.textview_id);
        btn = (Button) findViewById(R.id.button_id);
        proBar = (ProgressBar) findViewById(R.id.progressBar_id);
        handler = new receiveVersionHandler();
        version = manager.getVersion(this, this.getPackageName());
        versionName = manager.getVersionName(this, this.getPackageName());
        checkRequestPermission(this);
        manager.setServerVersionUrl("http://www.1234998.cn/downloads/Okan-OPlayer.json");
        manager.setServerVersionAppUrl("http://www.1234998.cn/downloads/");
        manager.setLocalFilePathName(Environment.getExternalStorageDirectory().getAbsolutePath() + "/");

        textView.setText("当前版本:" + version + "\n" + "当前版本名称:" + versionName);
        //btn.setEnabled(false);
        btn.setVisibility(View.INVISIBLE);
        manager.checkVersionFromServer(UpdateActivity.this, version, versionName);

        btn.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                manager.checkVersionFromServer(UpdateActivity.this, version, versionName);
            }
        });
    }

    public class receiveVersionHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            proBar.setProgress(msg.arg1);
            //proBar.setVisibility(R.id.button_id);
            textView.setText("下载进度：" + msg.arg1 + " %");
            proBar.setVisibility(View.VISIBLE);
            if (msg.arg1 == 100) {
                installApk(UpdateActivity.this, manager.getLocalFilePathName());
            }

            if (msg.arg1 == 200) {
                //btn.setEnabled(true);
                //btn.setVisibility(View.VISIBLE);
            }
            if (msg.arg1 == 300) {
                UpdateActivity.this.finish();
            }
            //proBar.setVisibility(R.id.button_id);
        }
    }


    private void checkRequestPermission(Context context) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            //申请WRITE_EXTERNAL_STORAGE权限
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1024);
        }
    }

    private void installApk(Context context, String filePath) {
        Log.i(TAG, "开始执行安装: " + filePath);
        File apkFile = new File(filePath);
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            Log.w(TAG, "版本大于 N ，开始使用 fileProvider 进行安装");
            intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            String authoritiesAppID = manager.getAuthoritiesAppID();
            if (authoritiesAppID == null)
                authoritiesAppID = this.getPackageName()+".provider";
            Uri contentUri = FileProvider.getUriForFile(context, authoritiesAppID, apkFile);
            intent.setDataAndType(contentUri, "application/vnd.android.package-archive");
        } else {
            Log.w(TAG, "正常进行安装");
            intent.setDataAndType(Uri.fromFile(apkFile), "application/vnd.android.package-archive");
        }
        startActivity(intent);
    }

}
