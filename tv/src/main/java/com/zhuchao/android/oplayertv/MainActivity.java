/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.zhuchao.android.oplayertv;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Instrumentation;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.storage.StorageManager;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.view.KeyEvent;

import com.zhuchao.android.netutil.CheckNet;
import com.zhuchao.android.updateManager.UpdateManager;

import java.util.ArrayList;
import java.util.List;


/*
 * MainActivity class that loads {@link MainFragment}.
 */

public class MainActivity extends Activity implements ServiceConnection {
    private final String TAG = "MainActivity";
    private static final int REQUEST_PERMISSION = 0;
    private int SourceID;
    private int SourceType;
    private String SourceURL;
    private UpdateManager manager = UpdateManager.getInstance();
    private StorageManager mStorageManager;
    private Handler handler = new Handler();
    private int mBackCount = 0;
    private MainFragment mBrowseFragment;

    //private boolean isPlaying =false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mBrowseFragment = (MainFragment) getFragmentManager().findFragmentById(R.id.main_browse_fragment);
        requestPermition();

        MediaLibrary.getSessionManager(this).setUserSessionCallback(null);

        try {
            bindService(new Intent(this, MyService.class), this, BIND_AUTO_CREATE);
        } catch (Exception e) {
            e.printStackTrace();
        }
        handler.postDelayed(runnable, 10000);
    }

    @Override
    protected void onStart() {
        super.onStart();

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        //super.onBackPressed();
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("退出提示：");
        builder.setMessage("您真的要退出吗？");

        builder.setNegativeButton("不，容我再想想", null);
        builder.setPositiveButton("确定", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                finish();
            }
        });

        mBackCount = 0;
        builder.show();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            Log.i(TAG, "MainActivity KeyEvent.KEYCODE_BACK");
            mBackCount++;
        }
        return super.onKeyDown(keyCode, event);
    }

    public void requestPermition() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

            int hasWritePermission = checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE);
            int hasReadPermission = checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE);

            List<String> permissions = new ArrayList<String>();
            if (hasWritePermission != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
            } else {
                //preferencesUtility.setString("storage", "true");
            }

            if (hasReadPermission != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE);

            } else {
                //preferencesUtility.setString("storage", "true");
            }

            if (!permissions.isEmpty()) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_PHONE_STATE},
                        REQUEST_PERMISSION);

                //sendKeyEvent(KeyEvent.KEYCODE_DPAD_RIGHT);
                //Log.d("onRequestPermissionsResult","onRequestPermissionsResult");
                //sendKeyEvent(KeyEvent.KEYCODE_DPAD_RIGHT);
                //Log.d("onRequestPermissionsResult","onRequestPermissionsResult");
                //sendKeyEvent(KeyEvent.KEYCODE_DPAD_CENTER);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults)
    {
        switch (requestCode)
        {
            case REQUEST_PERMISSION: {
                for (int i = 0; i < permissions.length; i++) {
                    if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                        System.out.println("Permissions --> " + "Permission Granted: " + permissions[i]);
                    } else if (grantResults[i] == PackageManager.PERMISSION_DENIED) {
                        System.out.println("Permissions --> " + "Permission Denied: " + permissions[i]);
                    }
                }
            }
            break;
            default: {
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
            }
        }

    }

    Runnable runnable = new Runnable() {
        @Override
        public void run() {
            // TODO Auto-generated method stub
            if (CheckNet.isInternetOk())
            {
                //int version = manager.getVersion(MainActivity.this, MainActivity.this.getPackageName());
                //String versionName = manager.getVersionName(MainActivity.this, MainActivity.this.getPackageName());

                //manager.setServerVersionUrl("http://www.1234998.cn/downloads/Okan-OPlayer.json");
                //manager.setServerVersionAppUrl("http://www.1234998.cn/downloads/");
                //manager.setLocalFilePathName(Environment.getExternalStorageDirectory() + "/");
                //manager.setAuthoritiesAppID("com.zhuchao.android.oplayertv.provider");
                //manager.isNeedToUpgradeFromServer(MainActivity.this, version, versionName);

                setAutoTimeZone(0);
            }
        }
    };

    public void setAutoTimeZone(int checked) {
        android.provider.Settings.Global.putInt(this.getContentResolver(),
                android.provider.Settings.Global.AUTO_TIME_ZONE, checked);
    }

    public void sendKeyEvent(final int KeyCode) {
        new Thread() {     //不可在主线程中调用
            public void run() {
                try {
                    Instrumentation inst = new Instrumentation();
                    inst.sendKeyDownUpSync(KeyCode);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

        }.start();
    }


    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        Log.d(TAG, "-------->" + "onServiceConnected");
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        Log.d(TAG, "-------->" + "onServiceDisconnected");
    }
}
