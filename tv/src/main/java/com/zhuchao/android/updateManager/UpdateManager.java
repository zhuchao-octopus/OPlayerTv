package com.zhuchao.android.updateManager;

import android.app.Activity;
import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Environment;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;


public class UpdateManager {
    private final String TAG = "UpdateManager-->";
    private static UpdateManager manager = null;
    private int serverVersion = 0;
    private String serverVersionName = null;
    private String serverVersionUrl = "";
    private String serverVersionAppUrl = "";
    private String localFilePathName = "";
    private Activity activity;
    private String serverAppName = "";
    private static String authoritiesAppID = null;

    private UpdateManager() {
        //this.activity = activity;
    }

    public  String getAuthoritiesAppID() {
        return authoritiesAppID;
    }

    public void setAuthoritiesAppID(String authoritiesAppID) {
        UpdateManager.authoritiesAppID = authoritiesAppID;
    }

    public static UpdateManager getInstance() {
        manager = new UpdateManager();
        return manager;
    }

    public String getSerVersionUrl() {
        return serverVersionUrl;
    }

    public String getSerVersionAppUrl() {
        return serverVersionAppUrl;
    }

    public String getLocalFilePathName() {
        return localFilePathName;
    }

    public void setServerVersionUrl(String serverVersionUrl) {
        this.serverVersionUrl = serverVersionUrl;
    }

    public void setServerVersionAppUrl(String serverVersionAppUrl) {
        this.serverVersionAppUrl = serverVersionAppUrl;
    }

    public void setLocalFilePathName(String localFilePathName) {
        this.localFilePathName = localFilePathName;
    }

    public int getServerVersion() {
        return serverVersion;
    }

    public int getVersion(Context context, String packageName) {
        int version = 0;
        try {
            version = context.getPackageManager().getPackageInfo(packageName, 0).versionCode;
        } catch (Exception e) {
            Log.d(TAG, packageName + " version =" + version);
        }
        return version;
    }

    public String getVersionName(Context context, String packageName) {
        String versionName = null;
        try {
            versionName = context.getPackageManager().getPackageInfo(packageName, 0).versionName;
        } catch (Exception e) {
            Log.d(TAG, packageName + " versionName =" + versionName);
        }
        return versionName;
    }

    public String getVersionFromServer(String url) {
        String serverJson = null;
        byte[] buffer = new byte[300];

        try {
            URL serverURL = new URL(url);
            HttpURLConnection connect = (HttpURLConnection) serverURL.openConnection();
            BufferedInputStream bis = new BufferedInputStream(connect.getInputStream());
            int n = 0;
            while ((n = bis.read(buffer)) != -1) {
                serverJson = new String(buffer);
            }
        } catch (Exception e) {
            Log.d(TAG, serverJson + " serverJson,  url=" + url);
        }

        return serverJson;
    }

    public void isNeedToUpgradeFromServer(Context context, final int cVersion, final String cVersionName) {
        final Context contextTemp = context;
        //final boolean[] b = {false};
        new Thread() {
            public void run() {
                Looper.prepare();
                String serverJson = manager.getVersionFromServer(serverVersionUrl);
                if(serverJson == null)  return;
                Log.d(TAG,  "升级信息=" + serverJson);
                try {
                    JSONObject object = new JSONObject(serverJson);
                    String sVersion = object.getString("versionCode");
                    String sVersionName = object.getString("versionName");

                    serverVersion = Integer.parseInt(sVersion);
                    serverVersionName = sVersionName;

                    if (cVersion < serverVersion) {
                        Intent intent = new Intent();
                        intent.setClass(contextTemp, UpdateActivity.class);
                        contextTemp.startActivity(intent);
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                    System.out.println("" + e);
                }

                Looper.loop();
            }

        }.start();
    }

    public Map<Integer, String> JsonToMap(String Jsonstr) {
        Gson gson = new GsonBuilder().serializeNulls().disableHtmlEscaping().create();
        try {
            return gson.fromJson(Jsonstr, new TypeToken<Map<Integer, String>>() {
            }.getType());
        } catch (JsonSyntaxException e) {
            e.printStackTrace();
        }
        return null;
    }

    public boolean checkVersionFromServer(Context context, final int cVersion, final String cVersionName) {

        final Context contextTemp = context;

        new Thread() {
            public void run() {
                Looper.prepare();
                String serverJson = manager.getVersionFromServer(serverVersionUrl);
                try {
                    //JSONArray array = new JSONArray(serverJson);
                    JSONObject object = new JSONObject(serverJson);

                    String sVersion = object.getString("versionCode");
                    String sVersionName = object.getString("versionName");
                    String sDescription = object.getString("description");
                    serverAppName = object.getString("AppName");
                    serverVersionAppUrl = serverVersionAppUrl + serverAppName;
                    serverVersion = Integer.parseInt(sVersion);
                    serverVersionName = sVersionName;
                    localFilePathName = localFilePathName + serverAppName;
                    if (cVersion < serverVersion) {
                        Builder builder = new Builder(contextTemp);
                        builder.setTitle("Oplayer 播放器版本升级");
                        builder.setMessage("当前版本：" + cVersionName + "\n" + "最新版本：" + serverVersionName+"\n"+sDescription);

                        builder.setNegativeButton("下次再说", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                Message msg = new Message();
                                msg.arg1 = 300;
                                UpdateActivity.handler.sendMessage(msg);
                            }
                        });
                        builder.setPositiveButton("立即升级", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int arg1) {
                                new Thread() {
                                    public void run() {
                                        Looper.prepare();
                                        downloadApkFile(contextTemp);
                                        Looper.loop();
                                    }
                                }.start();
                            }
                        });

                        builder.show();

                    } else {
                        Builder builder = new Builder(contextTemp);
                        builder.setTitle("版本信息");
                        builder.setMessage("当前已经是最新版本");
                        builder.setPositiveButton("确定", null);
                        builder.show();
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                    //System.out.println("" + e);
                }

                Looper.loop();
            }

        }.start();
        Message msg = new Message();
        msg.arg1 = 200;
        UpdateActivity.handler.sendMessage(msg);
        return false;
    }

    public void downloadApkFile(Context context) {
        String savePath = localFilePathName;//Environment.getExternalStorageDirectory() + "/OKan-Oplayer.apk";
        String serverFilePath = serverVersionAppUrl;
        int progress = 0;
        try {
            if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
                URL serverURL = new URL(serverFilePath);
                HttpURLConnection connect = (HttpURLConnection) serverURL.openConnection();
                //获取到文件的大小
                int fileLength = connect.getContentLength();
                InputStream is = connect.getInputStream();
                File file = new File(savePath);
                FileOutputStream fos = new FileOutputStream(file);
                BufferedInputStream bis = new BufferedInputStream(is);
                byte[] buffer = new byte[1024];
                int len;
                int total = 0;
                while ((len = bis.read(buffer)) != -1) {
                    fos.write(buffer, 0, len);
                    total += len;
                    //获取当前下载量
                    progress = (int) (((float) total / fileLength) * 100);
                    Message msg = new Message();
                    msg.arg1 = progress;
                    UpdateActivity.handler.sendMessage(msg);
                }
                fos.close();
                bis.close();
                is.close();
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
