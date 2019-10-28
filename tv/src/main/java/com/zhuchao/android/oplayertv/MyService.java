package com.zhuchao.android.oplayertv;

import android.app.ActivityManager;
import android.app.AlarmManager;
import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.annotation.Nullable;
import android.util.Log;

import com.zhuchao.android.playsession.OPlayerSession;
import com.zhuchao.android.playsession.SchedulePlaybackSession;
import com.zhuchao.android.playsession.SessionCompleteCallback;
import com.zhuchao.android.video.ScheduleVideo;
import com.zhuchao.android.video.Video;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import Myutils.ChangeTool;
import android_serialport_api.SerialPortDevice;

import static Myutils.ChangeTool.ByteArrToHex2;

public class MyService extends Service {

    public static SerialPortDevice serialPort3 = new SerialPortDevice("/dev/ttyS3", 9600, true);
    public static SerialPortDevice serialPort4 = new SerialPortDevice("/dev/ttyS4", 9600, false);
    //public static SerialPortDevice serialPort4 = null;
    private static byte[] data = {0x01, 0x01, 0x0b, 0x00, 0x00, 0x07, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00};
    private static byte[] dataDate = {0x01, 0x01, 0x0b, 0x00, 0x00, 0x07, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x7E};
    private byte[] SerialPortReceiveBuffer = new byte[128];
    private int SerialPortReceiveBufferIndex = 0;
    private Handler Myhandler;
    private NotificationCallback mCallback;
    private ScheduleVideo mScheduleVideo = null;

    //private List<Video> allVideos = null;
    //private int VideoIndex=0;
    private OPlayerSession LocalSession = null;
    private OPlayerSession MobileSession = null;
    private OPlayerSession MomileTFSession = null;
    //private Video video = null;
    private String mTempStr = "123";


    Handler mMyHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            try {
                ScheduleVideo scheduleVideo = (ScheduleVideo) msg.obj;

                if ((scheduleVideo != null) && scheduleVideo.isPlayScheduled()) {
                    Intent intent = new Intent(MyService.this, FullscreenPlayBackActivity.class);
                    intent.putExtra("Video", scheduleVideo);

                    if ((!scheduleVideo.isPlaying()) && (mScheduleVideo == null))
                    {
                        if (isTopActivity("com.zhuchao.android.oplayertv.FullscreenPlayBackActivity"))
                        {
                            Intent i = new Intent();
                            i.setAction("com.zhuchao.android.oplayertv.PLAY");
                            i.putExtra("Video", mScheduleVideo);
                            sendBroadcast(i);
                        }
                        else {
                            scheduleVideo.setStatus(0);
                            mScheduleVideo = scheduleVideo;
                            startActivity(intent);
                        }
                    }
                } else if ((mScheduleVideo != null) && (scheduleVideo != null) && scheduleVideo.isStopScheduled() && (mScheduleVideo.equals(scheduleVideo))) {
                    scheduleVideo.stopPlayer();
                    mScheduleVideo = null;
                    Intent intent = new Intent(MyService.this, MainActivity.class);
                    startActivity(intent);
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    };


    private static void WriteDataToDSP(final byte[] b) {
        if (serialPort3 == null) return;
        if (serialPort3.isReady())
            serialPort3.sendBuffer(b);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Myhandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
            }
        };

        try {
            if (serialPort3.openPort() == true) {
                Log.d("MyService", "onCreate：开始监听串口 3 数据！");
                OnSerialPort3Event();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            if (serialPort4 != null) {
                if (serialPort4.openPort() == true) {
                    Log.d("MyService", "onCreate：开始监听串口 4 数据！");
                    OnSerialPort4Event();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        LocalSession = MediaLibrary.getSessionManager(MyService.this).getLocalSession();
        MobileSession = MediaLibrary.getSessionManager(MyService.this).getMoBileSession();
        MomileTFSession = MediaLibrary.getSessionManager(MyService.this).getMomileTFSession();

        Log.d("MyService", "onCreate：MobileSession");
        MobileSession.printMovies();

        Log.d("MyService", "onCreate：MomileTFSession");
        MomileTFSession.printMovies();

        try
        {
            startSchedulePlaySesssion();

            writeNowDateTimeToDsp();
            if (MediaLibrary.schedulePlaybackSession != null) {
                writeScheduleVideoToDsp(MediaLibrary.schedulePlaybackSession.getVideoList());
            }

        } catch (Exception e) {
            e.printStackTrace();
        }


    }

    @Override
    public void onStart(Intent intent, int startId) {
        super.onStart(intent, startId);
        try {
            if (intent == null) return;

            byte[] dataBytes = intent.getByteArrayExtra("Data");
            if (dataBytes != null) {
                serialPort3.sendBuffer(dataBytes);
                Log.d("MyService", ChangeTool.ByteArrToHexStr(dataBytes, 0, dataBytes.length));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return new Binder();
    }

    public void setCallback(NotificationCallback callback) {
        this.mCallback = callback;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    private void OnSerialPort3Event() {
        serialPort3.setOnDataReceiveListener(new SerialPortDevice.OnDataReceiveListener() {
            Runnable runnableUpdateDateTime = new Runnable() {
                @Override
                public void run() {
                    writeNowDateTimeToDsp();
                }
            };
            Runnable runnableUpdateScheduleSession = new Runnable() {
                @Override
                public void run() {
                    if (MediaLibrary.schedulePlaybackSession != null) {
                        writeScheduleVideoToDsp(MediaLibrary.schedulePlaybackSession.getVideoList());
                    }
                }
            };

            @Override
            public void onDataReceive(byte[] buffer, int size) {
                //SerialPortReceiveBuffer = buffer;
                String str = ByteArrToHex2(buffer, size);
                Log.d("MyService", "serialPort3.OnSerialPortEvent:" + str);

                if ((buffer[2] == 0x06) && (buffer[3] == 0x00) && (buffer[5] == 0x01) && (buffer[7] == 0x0b)) {
                    //Myhandler.post(runnableUpdateDateTime);
                    writeNowDateTimeToDsp();
                } else if ((buffer[2] == 0x06) && (buffer[3] == 0x00) && (buffer[5] == 0x01) && (buffer[7] == 0x0c)) {
                    //Myhandler.post(runnableUpdateScheduleSession);
                    if (MediaLibrary.schedulePlaybackSession != null) {
                        writeScheduleVideoToDsp(MediaLibrary.schedulePlaybackSession.getVideoList());
                    }
                } else if ((buffer[2] == 0x0c) && (buffer[3] == 0x00) && (buffer[5] == 0x03)) {
                    int i = buffer[7];

                } else if (str.equals("0101010000010013177E")) //播放
                {
                    if (isTopActivity("com.zhuchao.android.oplayertv.FullscreenPlayBackActivity")) {
                        Intent i = new Intent();
                        i.setAction("com.zhuchao.android.oplayertv.PLAY");
                        sendBroadcast(i);
                    } else {
                        List<Video> allVideos = MediaLibrary.getSessionManager(MyService.this).getAllVideoList();
                        if (allVideos.size() > 0) {
                            Video video = allVideos.get(0);
                            if (video != null) {
                                Log.d("MyService", video.getmMovie().getSourceUrl().toString());
                                Intent intent = new Intent(MyService.this, FullscreenPlayBackActivity.class);
                                intent.putExtra("Video", video);
                                startActivity(intent);
                            }
                        }
                    }

                } else if (str.equals("0101010000010012167E"))//暂停
                {
                    Intent i = new Intent();
                    i.setAction("com.zhuchao.android.oplayertv.PAUSE");
                    sendBroadcast(i);
                } else if (str.equals("01010100000100181C7E"))//上一首
                {
                    Intent i = new Intent();
                    i.setAction("com.zhuchao.android.oplayertv.PREV");
                    sendBroadcast(i);

                } else if (str.equals("01010100000100191D7E"))//下一首
                {
                    Intent i = new Intent();
                    i.setAction("com.zhuchao.android.oplayertv.NEXT");
                    sendBroadcast(i);
                } else if (str.equals("010101000001000C107E"))//usb
                {
                    Video video = MobileSession.getVideoByIndex(0);
                    if(video == null)
                    {
                        Intent i = new Intent();
                        i.setAction("com.zhuchao.android.oplayertv");
                        sendBroadcast(i);
                    }
                    if (isTopActivity("com.zhuchao.android.oplayertv.FullscreenPlayBackActivity")) {
                        Intent i = new Intent();
                        i.setAction("com.zhuchao.android.oplayertv.PLAY");
                        i.putExtra("Video", video);
                        sendBroadcast(i);
                    } else {

                        if (video != null) {
                            Intent intent = new Intent(MyService.this, FullscreenPlayBackActivity.class);
                            intent.putExtra("Video", video);
                            startActivity(intent);
                        }
                    }
                } else if (str.equals("010101000001000D117E"))//tf
                {
                    Video video = MomileTFSession.getVideoByIndex(0);
                    if(video == null)
                    {
                        Intent i = new Intent();
                        i.setAction("com.zhuchao.android.oplayertv");
                        sendBroadcast(i);
                    }
                    if (isTopActivity("com.zhuchao.android.oplayertv.FullscreenPlayBackActivity")) {
                        Intent i = new Intent();
                        i.setAction("com.zhuchao.android.oplayertv.PLAY");
                        i.putExtra("Video", video);
                        sendBroadcast(i);
                    } else {

                        if (video != null) {
                            Intent intent = new Intent(MyService.this, FullscreenPlayBackActivity.class);
                            intent.putExtra("Video", video);
                            startActivity(intent);
                        }
                    }
                } else if (str.equals("010101000001000B0F7E"))//网络
                {
                    Video video = LocalSession.getVideoByIndex(0);
                    if(video == null)
                    {
                        Intent i = new Intent();
                        i.setAction("com.zhuchao.android.oplayertv");
                        sendBroadcast(i);
                    }
                    if (isTopActivity("com.zhuchao.android.oplayertv.FullscreenPlayBackActivity")) {
                        Intent i = new Intent();
                        i.setAction("com.zhuchao.android.oplayertv.PLAY");
                        i.putExtra("Video", video);
                        sendBroadcast(i);
                    } else {

                        if (video != null) {
                            Intent intent = new Intent(MyService.this, FullscreenPlayBackActivity.class);
                            intent.putExtra("Video", video);
                            startActivity(intent);
                        }
                    }

                } else if (str.equals("010101000001000A0E7E"))//tf
                {
                    Intent i = new Intent();
                    i.setAction("com.zhuchao.android.oplayertv");
                    sendBroadcast(i);
                } else if ((buffer[2] == 0x06) && (buffer[3] == 0x00) && (buffer[5] == 0x01) && (buffer[7] == 0x0d)) {

                } else if ((buffer[2] == 0x06) && (buffer[3] == 0x00) && (buffer[5] == 0x01) && (buffer[7] == 0x0e)) {
                    Intent d = new Intent();
                    d.putExtra("data", str);
                    d.setAction("com.zhuchao.android.oplayertv.MyService");
                    sendBroadcast(d);
                } else {
                    if (mCallback != null)
                        mCallback.onNotificationCallback(str);
                }
            }

        });

    }

    private boolean isTopActivity(String packageName) {
        ActivityManager activityManager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
        List<ActivityManager.RunningTaskInfo> tasksInfo = activityManager.getRunningTasks(5);
        if (tasksInfo.size() > 0) {
            //应用程序位于堆栈的顶层
            String str = tasksInfo.get(0).topActivity.getClassName();
            if (packageName.contains(str)) {
                return true;
            }
        }
        return false;
    }

    private void OnSerialPort4Event() {
        serialPort4.setOnDataReceiveListener(new SerialPortDevice.OnDataReceiveListener() {
            Runnable runnableUpdateDateTime = new Runnable() {
                @Override
                public void run() {
                    if (mTempStr.startsWith("$GBRMC") && mTempStr.contains("$GBRMC")) {
                        try {
                            String[] StringList = mTempStr.split(",");
                            int l = StringList.length;
                            if (l < 2) return;
                            String time = StringList[1];
                            if (l < 10) return;
                            String date = StringList[9];

                            String yy = "0";
                            String MM = "0";
                            String dd = "0";
                            String hh = "0";
                            String mm = "0";
                            String ss = "0";
                            if (time.length() >= 6) {
                                hh = time.substring(0, 2);
                                mm = time.substring(2, 4);
                                ss = time.substring(4, 6);
                            }
                            if (date.length() >= 6) {
                                yy = date.substring(4, 6);
                                MM = date.substring(2, 4);
                                dd = date.substring(0, 2);
                            }

                            if (serialPort4 != null)
                                updateDateTime(Integer.valueOf(yy), Integer.valueOf(MM), Integer.valueOf(dd),
                                        Integer.valueOf(hh) + 8, Integer.valueOf(mm), Integer.valueOf(ss));
                        } catch (NumberFormatException e) {
                            Log.d("runnableUpdateDateTime from bd failed,get time from internet", e.toString());
                            setAutoTimeZone(0);
                        }

                    }
                }
            };

            @Override
            public void onDataReceive(byte[] buffer, int size) {
                //SerialPortReceiveBuffer = buffer;
                for (int i = 0; i < size; i++) {
                    if (SerialPortReceiveBufferIndex >= SerialPortReceiveBuffer.length)
                        SerialPortReceiveBufferIndex = 0;

                    SerialPortReceiveBuffer[SerialPortReceiveBufferIndex] = buffer[i];
                    SerialPortReceiveBufferIndex++;
                    if (buffer[i] == 10) {
                        mTempStr = new String(SerialPortReceiveBuffer, 0, SerialPortReceiveBufferIndex);
                        SerialPortReceiveBufferIndex = 0;
                        //Log.d("MyService", "serialPort4.onDataReceive:" + mTempStr);
                        Myhandler.post(runnableUpdateDateTime);
                    }
                }
                //String str = new String(buffer,0,size);
                //Log.d("MyService", "serialPort4.onDataReceive:" + str);
            }

        });
    }

    public static synchronized void writeHHmmssToDsp(int id,int status,int b0, int b1, int b2, int b3) //当前的音乐的进度
    {
        //int i = 0;
        data[2] = 0x0e;
        data[3] = 0x00;
        data[5] = 0x06;
        data[6] = 0x00;
        data[7] = (byte) id;
        data[8] = (byte) status;
        data[9] = (byte) b0;
        data[10] = (byte) b1;
        data[11] = (byte) b2;
        data[12] = (byte) b3;
        data[13] = ChangeTool.BytesAdd(data, 13);
        data[14] = 0x7E;
        WriteDataToDSP(data);
    }

    private synchronized void writeNowDateTimeToDsp() {
        dataDate[2] = 0x0b;
        dataDate[3] = 0x00;
        dataDate[5] = 0x07;
        dataDate[6] = 0x00;
        Date date = new Date();
        //SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        SimpleDateFormat sdf = new SimpleDateFormat("yy");
        String datetime = sdf.format(date);
        dataDate[7] = (byte) Integer.parseInt(datetime);

        sdf = new SimpleDateFormat("MM");
        datetime = sdf.format(date);
        dataDate[8] = (byte) Integer.parseInt(datetime);

        sdf = new SimpleDateFormat("dd");
        datetime = sdf.format(date);
        dataDate[9] = (byte) Integer.parseInt(datetime);

        sdf = new SimpleDateFormat("HH");
        datetime = sdf.format(date);
        dataDate[11] = (byte) Integer.parseInt(datetime);

        sdf = new SimpleDateFormat("mm");
        datetime = sdf.format(date);
        dataDate[12] = (byte) Integer.parseInt(datetime);

        sdf = new SimpleDateFormat("ss");
        datetime = sdf.format(date);
        dataDate[13] = (byte) Integer.parseInt(datetime);

        dataDate[14] = ChangeTool.BytesAdd(dataDate, 14);
        dataDate[15] = 0x7E;
        WriteDataToDSP(dataDate);
    }

    private synchronized void writeScheduleVideoToDsp(List<ScheduleVideo> ScheduleVideos) {
        data[2] = 0x0c;
        data[3] = 0x00;
        data[5] = 0x03;
        data[6] = 0x00;
        int i = 0;
        byte ii = 0;

        for (ScheduleVideo scheduleVideo : ScheduleVideos) {
            String[] hhmm = scheduleVideo.getmPlayTime().split(":");
            data[7] = (byte) i;
            ii = (byte) Integer.parseInt(hhmm[0]);
            data[8] = ii;
            ii = (byte) (byte) Integer.parseInt(hhmm[1]);
            data[9] = ii;
            data[10] = ChangeTool.BytesAdd(data, 10);
            data[11] = 0x7E;
            WriteDataToDSP(data);
            i++;
        }

    }

    private void startSchedulePlaySesssion() {

        if (MediaLibrary.schedulePlaybackSession == null) {
            MediaLibrary.schedulePlaybackSession = new SchedulePlaybackSession(this, new SessionCompleteCallback() {
                @Override
                public void OnSessionComplete(int i, String s) {
                    new Thread() {
                        @Override
                        public void run() {
                            //super.run();
                            if (MediaLibrary.schedulePlaybackSession != null) {
                                writeScheduleVideoToDsp(MediaLibrary.schedulePlaybackSession.getVideoList());
                            }
                            while (true) {
                                try {
                                    sleep(5000);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                                while (MediaLibrary.schedulePlaybackSession.hasScheduleSession()) {
                                    ScheduleVideo scheduleVideo = MediaLibrary.schedulePlaybackSession.pollingScheudulePlay();
                                    if (scheduleVideo != null) {
                                        Message msg = new Message();
                                        msg.what = 100;
                                        msg.obj = scheduleVideo;
                                        mMyHandler.sendMessage(msg);
                                    }

                                    try {
                                        sleep(1000);
                                    } catch (InterruptedException e) {
                                        e.printStackTrace();
                                    }
                                }
                            }
                        }
                    }.start();
                }
            });
        }
    }

    public void setAutoTimeZone(int checked) {
        android.provider.Settings.Global.putInt(this.getContentResolver(),
                android.provider.Settings.Global.AUTO_TIME_ZONE, checked);
    }

    public void setAutoDateTime(int checked) {
        android.provider.Settings.Global.putInt(this.getContentResolver(),
                android.provider.Settings.Global.AUTO_TIME, checked);
    }

    public void updateDateTime(int yy, int MM, int dd, int hh, int mm, int ss) {
        //Log.d("MyService", "updateDateTime:" + yy + "/" + MM + "/" + dd + " " + hh + ":" + mm + ":" + ss);
        try {
            if ((dataDate[7] != yy) || (dataDate[8] != MM) || (dataDate[9] != dd)) {
                setAutoTimeZone(1);
                setTimeZone("Asia/Shanghai");
                setSysDate(yy + 2000, MM - 1, dd);
                //Log.d("MyService", "updateDateTime:" + yy + "/" + MM + "/" + dd);
            }
            if ((dataDate[11] != hh) || (dataDate[12] != mm)) {
                setAutoTimeZone(1);
                setTimeZone("Asia/Shanghai");
                setSysTime(hh, mm, ss);
                //Log.d("MyService", "updateDateTime:" + hh + ":" + mm + ":" + ss);
            }
        } catch (Exception e) {
            Log.d("MyService", "设置时区失败  " + e.toString());
        }

    }

    public void setSysDate(int year, int month, int day) {
        Calendar c = Calendar.getInstance();
        c.set(Calendar.YEAR, year);
        c.set(Calendar.MONTH, month);
        c.set(Calendar.DAY_OF_MONTH, day);

        long when = c.getTimeInMillis();

        if (when / 1000 < Integer.MAX_VALUE) {
            ((AlarmManager) this.getSystemService(this.ALARM_SERVICE)).setTime(when);
        }
    }

    public void setSysTime(int hour, int minute, int ss) {
        Calendar c = Calendar.getInstance();
        c.set(Calendar.HOUR_OF_DAY, hour);
        c.set(Calendar.MINUTE, minute);
        c.set(Calendar.SECOND, ss);
        c.set(Calendar.MILLISECOND, 0);

        long when = c.getTimeInMillis();

        if (when / 1000 < Integer.MAX_VALUE) {
            ((AlarmManager) this.getSystemService(this.ALARM_SERVICE)).setTime(when);
        }
    }

    public void setTimeZone(String timeZone) {
        final Calendar now = Calendar.getInstance();
        TimeZone tz = TimeZone.getTimeZone(timeZone);
        now.setTimeZone(tz);

        android.provider.Settings.System.putString(this.getContentResolver(),
                android.provider.Settings.System.TIME_12_24, "24");

        //TimeZone tz = TimeZone.getTimeZone("Asia/Shanghai");
        //TimeZone.setDefault(tz);
    }

    public interface NotificationCallback {
        void onNotificationCallback(String data);
    }

    public class Binder extends android.os.Binder {
        public MyService getService() {
            return MyService.this;
        }
    }


}
