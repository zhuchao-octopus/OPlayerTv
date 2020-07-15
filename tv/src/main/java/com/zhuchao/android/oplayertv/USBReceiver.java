package com.zhuchao.android.oplayertv;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.storage.StorageManager;
import android.text.TextUtils;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import Myutils.MediaFile;

public class USBReceiver extends BroadcastReceiver {
    private static final String TAG = USBReceiver.class.getSimpleName();
    private static final String MOUNTS_FILE = "/proc/mounts";
    private static String BlockName = null;
    private static String BlockPath = null;
    private ArrayList FileList = null;
    private StorageManager mStorageManager;


    @Override
    public void onReceive(Context context, Intent intent) {
        mStorageManager = (StorageManager) context.getSystemService(Activity.STORAGE_SERVICE);
        String action = intent.getAction();
        if (action.equals(Intent.ACTION_MEDIA_MOUNTED)) {
            String mountPath = intent.getData().getPath();
            Uri data = intent.getData();
            Log.d(TAG, "mountPath = " + mountPath);
            if (!TextUtils.isEmpty(mountPath)) {
                //读取到U盘路径再做其他业务逻辑
                //SPUtils.getInstance().put("UsbPath", mountPath);
                //boolean mounted = isMounted(mountPath);
                //Log.d(TAG, "onReceive: " + "U盘挂载" + mounted);
                //MediaLibrary.updateMobileMedia(BlockName,BlockPath);
            }
        } else if (action.equals(Intent.ACTION_MEDIA_UNMOUNTED) || action.equals(Intent.ACTION_MEDIA_EJECT)) {
            //Log.d(TAG, "onReceive: " + "U盘移除了");
            //MediaLibrary.updateMobileMedia(BlockName,BlockPath);
        } else if (action.equals("android.intent.action.BOOT_COMPLETED")) {
            //如果是开机完成，则需要调用另外的方法获取U盘的路径
        }
    }


    /**
     * 判断是否有U盘插入,当U盘开机之前插入使用该方法.
     *
     * @param path
     * @return
     */
    public boolean isMounted(String path) {
        boolean blnRet = false;
        String strLine = null;
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader(MOUNTS_FILE));
            while ((strLine = reader.readLine()) != null) {
                if (strLine.contains(path)) {
                    blnRet = true;
                    break;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                reader = null;
            }
        }
        return blnRet;
    }


    /**
     * 获取U盘的路径和名称
     */
    private void getUName() {
        Class<?> volumeInfoClazz = null;
        Method getDescriptionComparator = null;
        Method getBestVolumeDescription = null;
        Method getVolumes = null;
        Method isMountedReadable = null;
        Method getType = null;
        Method getPath = null;
        List<?> volumes = null;
        try {
            volumeInfoClazz = Class.forName("android.os.storage.VolumeInfo");
            getDescriptionComparator = volumeInfoClazz.getMethod("getDescriptionComparator");
            getBestVolumeDescription = StorageManager.class.getMethod("getBestVolumeDescription", volumeInfoClazz);
            getVolumes = StorageManager.class.getMethod("getVolumes");
            isMountedReadable = volumeInfoClazz.getMethod("isMountedReadable");
            getType = volumeInfoClazz.getMethod("getType");
            getPath = volumeInfoClazz.getMethod("getPath");
            volumes = (List<?>) getVolumes.invoke(mStorageManager);

            for (Object vol : volumes) {
                if (vol != null && (boolean) isMountedReadable.invoke(vol) && (int) getType.invoke(vol) == 0) {
                    File path2 = (File) getPath.invoke(vol);
                    String p1 = (String) getBestVolumeDescription.invoke(mStorageManager, vol);
                    String p2 = path2.getPath();
                    Log.d(TAG, "-----------path1-----------------" + p1);         //打印U盘卷标名称
                    Log.d(TAG, "-----------path2-----------------" + p2);         //打印U盘路径
                    BlockName=p1;
                    BlockPath=p2;
                    //getFiles(p2);
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void getFiles(String FilePath) {
        File path = new File(FilePath);//外置U盘路径
        File[] files = path.listFiles();// 读取
        getFileName(files);
    }

    private void getFileName(File[] files) {
        if (FileList == null)
            FileList = new ArrayList();
        if (files != null) {// 先判断目录是否为空，否则会报空指针
            String fileName = null;
            for (File file : files) {
                if (file.isDirectory()) {
                    //Log.e(TAG, "若是文件目录。继续读1" + file.getName().toString() + file.getPath().toString());
                    getFileName(file.listFiles());
                    //Log.e(TAG, "若是文件目录。继续读2" + file.getName().toString() + file.getPath().toString());
                } else {
                    fileName =file.getPath().toString();// +"  "+ file.getName() ;
                    //if (fileName.endsWith(".txt"))
                    MediaFile.MediaFileType mm = MediaFile.getFileType(fileName);
                    if (mm != null)
                    if(MediaFile.isMimeTypeMedia(mm.mimeType))
                    {
                        HashMap map = new HashMap();
                        //String s = fileName.substring(0, fileName.lastIndexOf(".")).toString();
                        Log.i(TAG, "文件名: " + fileName);
                        map.put("Name", fileName);
                        FileList.add(map);
                    }
                }
            }
        }
    }

}