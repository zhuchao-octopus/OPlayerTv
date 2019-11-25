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
import android.support.v17.leanback.widget.ImageCardView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.ActivityOptionsCompat;
import android.util.Log;
import android.view.KeyEvent;
import android.widget.Toast;

import com.google.gson.Gson;
import com.zhuchao.android.callbackevent.NormalRequestCallback;
import com.zhuchao.android.libfilemanager.AppsChangedCallback;
import com.zhuchao.android.libfilemanager.MyAppsManager;
import com.zhuchao.android.libfilemanager.bean.AppInfor;
import com.zhuchao.android.netutil.NetUtils;
import com.zhuchao.android.netutil.OkHttpUtils;
import com.zhuchao.android.oplayertv.data.json.regoem.RecommendversionBean;
import com.zhuchao.android.video.OMedia;

import java.util.ArrayList;
import java.util.List;


/*
 * MainActivity class that loads {@link MainFragment}.
 */

public class MainActivity extends Activity implements ServiceConnection, AppsChangedCallback, NetUtils.NetChangedCallBack {
    private final String TAG = "MainActivity";
    private static final int REQUEST_PERMISSION = 0;
    //private int SourceID;
    //private int SourceType;
    //private String SourceURL;
    //private UpdateManager manager = UpdateManager.getInstance();
    //private StorageManager mStorageManager;
    //private Handler handler = new Handler();
    //private int mBackCount = 0;
    //private MainFragment mBrowseFragment;
    private MyAppsManager myAppsManager;
    private NetUtils netUtils;
    //private boolean isPlaying =false;


    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        if (getIntent() != null) {
            String musicPath = getIntent().getData().getPath(); //得到文件的路径，
            //int songIndex = musicPath.lastIndexOf("/");
            //int songNameIndex = musicPath.lastIndexOf(".");
            //String songName = musicPath.substring(songIndex + 1, songNameIndex);
            //得到歌曲的名字，当然还可以获取歌曲的大小、艺术家
            //String kkkString = musicPath.substring(lll+2, songNameIndex);
            //Log.i("MusicPlayActivity", "" + musicPath + "," + musicPath);
            OMedia mvideo = new OMedia(musicPath);
            Intent ii = new Intent(MainActivity.this, FullscreenPlayBackActivity.class);
            ii.putExtra("Video", mvideo);
           startActivity(ii);
        }
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //mBrowseFragment = (MainFragment) getFragmentManager().findFragmentById(R.id.main_browse_fragment);
        requestPermition();

        //MediaLibrary.getSessionManager(MainActivity.this).setUserSessionCallback(null);
        //myAppsManager = new MyAppsManager(MainActivity.this, this);
        //netUtils = new NetUtils(MainActivity.this, this);
        //handler.postDelayed(runnable, 10000);
    }

    @Override
    protected void onStart() {

        super.onStart();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        MediaLibrary.free();

    }

    @Override
    public void onBackPressed() {
        //super.onBackPressed();
       AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("温馨提示：");
        builder.setMessage("您真的要退出 All Player ？");

        builder.setNegativeButton("不，容我再想想", null);
        builder.setPositiveButton("退出", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                MediaLibrary.free();
                finish();
            }
        });

        builder.show();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            Log.i(TAG, "MainActivity KeyEvent.KEYCODE_BACK");
            //mBackCount++;
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
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
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
            //if (CheckNet.isInternetOk())
            //{
            //int version = manager.getVersion(MainActivity.this, MainActivity.this.getPackageName());
            //String versionName = manager.getVersionName(MainActivity.this, MainActivity.this.getPackageName());
            //manager.setServerVersionUrl("http://www.1234998.cn/downloads/Okan-OPlayer.json");
            //manager.setServerVersionAppUrl("http://www.1234998.cn/downloads/");
            //manager.setLocalFilePathName(Environment.getExternalStorageDirectory() + "/");
            //manager.setAuthoritiesAppID("com.zhuchao.android.oplayertv.provider");
            //manager.isNeedToUpgradeFromServer(MainActivity.this, version, versionName);

            //setAutoTimeZone(0);
            //}

            checkSoftwareVersion();
        }
    };

    private void checkSoftwareVersion() {
        String url = getMyUrl("jhzBox/box/appOnlineVersion.do?versionNum=" + BuildConfig.VERSION_NAME + "&cy_versions_name=" + "AllPlayer" + "&", null, netUtils.getDeviceID().toUpperCase(), 0, netUtils.getIP0(), netUtils.getChineseRegion(netUtils.getLocation()), "AllPlayer");
        Log.d(TAG, "checkSoftwareVersion=" + url);

        OkHttpUtils.request(url, new NormalRequestCallback() {
            @Override
            public void onRequestComplete(String s, int i) {
                if (i < 0) return;
                final RecommendversionBean versionBean = new Gson().fromJson(s, RecommendversionBean.class);
                if (versionBean.getStatus() == 0) {
                    final RecommendversionBean.DataBean data = versionBean.getData();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            new AlertDialog.Builder(MainActivity.this)
                                    .setTitle("All player is going to upgrade")
                                    .setMessage(data.getCy_versions_info())
                                    .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialogInterface, int i) {
                                            dialogInterface.dismiss();
                                        }
                                    })
                                    .setPositiveButton("Confirm", new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialogInterface, int i) {
                                            Toast.makeText(MainActivity.this, "AllPlayer is going to download a newer version...", Toast.LENGTH_LONG).show();
                                            downloadApk(data.getCy_versions_path());
                                        }
                                    }).show();
                        }
                    });
                }
            }
        });
    }

    private void downloadApk(final String url) {
        final String toFilePath = myAppsManager.getDownloadDir() + url.substring(url.lastIndexOf("/") + 1);
        OkHttpUtils.Download(url, toFilePath, this.getLocalClassName(), new NormalRequestCallback() {
            @Override
            public void onRequestComplete(String s, final int i) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (i >= 0) {
                            myAppsManager.install(toFilePath);
                        } else {
                            //Toast.makeText(MainActivity.this, R.string.download_failed, Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }
        });
    }

    private String getMyUrl(String api, String DMN, String DID, int CID, String IP, String RID, String LuncherName) {
        String url = "http://www.gztpapp.cn:8976/" + api +
                "cy_brand_id= null" +
                "&mac=" + netUtils.getDeviceID().toUpperCase() +
                "&netCardMac=" + netUtils.getMAC().toUpperCase() +
                "&CustomId=" + CID +
                "&codeIp=" + IP +
                "&region=" + RID +
                "&CustomId=0" +
                "&lunchname=" + LuncherName;
        return url;
    }


    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        Log.d(TAG, "-------->" + "onServiceConnected");
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        Log.d(TAG, "-------->" + "onServiceDisconnected");
    }

    @Override
    public void OnAppsChanged(String s, AppInfor appInfor) {

    }

    @Override
    public void onNetStateChanged(boolean b, int i, String s, String s1, String s2, String s3, String s4) {

    }

    @Override
    public void onWifiLevelChanged(int i) {

    }
}
