package com.zhuchao.android.oplayertv;


import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Canvas;
import android.graphics.Color;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.KeyEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import com.zhuchao.android.callbackevent.PlayerCallback;
import com.zhuchao.android.netutil.TimeDateUtils;
import com.zhuchao.android.video.OMedia;


/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
public class PlayBackManagerActivity extends Activity implements PlayerCallback {
    private static final String TAG = "PlayBackManagerActivity-->";
    private static SurfaceView mSurfaceView;
    private static OMedia mvideo = null;
    //private static CountDownTimer mCountDownTimerChannelControl;
    private static CountDownTimer mCountDownTimer1;
    private static int Counter = 0;
    private static HomeWatcherReceiver mHomeKeyReceiver = null;
    Handler mMyHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {

        }
    };
    private TextView tvFileName;
    private ProgressBar mProgressBar = null;
    private MyReceiver mMyReceiver;
    //private byte[] temp = {0, 0, 0, 0};
    private SeekBar seekBar;
    private ImageView ivplay;
    private RelativeLayout topbar;
    private LinearLayout bottombar;
    private TextView tvprogress;

    //MyPlayer OPlayer = null;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON, WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_fullscreen);
        tvFileName = findViewById(R.id.textView);

        mSurfaceView = findViewById(R.id.surfaceView);
        //mSurfaceView.setVisibility(View.INVISIBLE);

        mProgressBar = findViewById(R.id.progressBar);
        mProgressBar.setVisibility(View.VISIBLE);

        seekBar = findViewById(R.id.seekBar);
        seekBar.setVisibility(View.VISIBLE);

        ivplay = findViewById(R.id.ivallplay);
        ivplay.setVisibility(View.INVISIBLE);

        topbar = findViewById(R.id.topbar);
        bottombar = findViewById(R.id.bottombar);
        //bottombar.getBackground().setAlpha(100);
        tvprogress = findViewById(R.id.tvprogress);

        topbar.bringToFront();
        //mSurfaceView.setZOrderOnTop(true);
        //mSurfaceView.getHolder().setFormat(PixelFormat.TRANSLUCENT);
        mSurfaceView.getHolder().addCallback((new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                Canvas canvas = mSurfaceView.getHolder().lockCanvas();
                canvas.drawColor(Color.BLACK);  //随便设置背景颜色
                mSurfaceView.getHolder().unlockCanvasAndPost(canvas);
            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {

            }
        }));


        try {
            mvideo = (OMedia) getIntent().getSerializableExtra("Video");
            if (mvideo != null) {
                tvFileName.setText(mvideo.getMovie().getSourceUrl());
                mvideo.with(this).callback(this);
                mvideo.setNormalRage();
            } else {
                stopPlay();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }


        mCountDownTimer1 = new CountDownTimer(1000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
            }

            @Override
            public void onFinish() {
                if (mvideo != null)
                    mvideo = playVideo(mvideo);
            }
        };


        mMyReceiver = new MyReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction("com.zhuchao.android.oplayertv");
        filter.addAction("com.zhuchao.android.oplayertv.PAUSE");
        filter.addAction("com.zhuchao.android.oplayertv.PLAY");
        filter.addAction("com.zhuchao.android.oplayertv.NEXT");
        filter.addAction("com.zhuchao.android.oplayertv.PREV");
        registerReceiver(mMyReceiver, filter);
        registerHomeKeyReceiver(this);
    }

    @Override
    protected void onStart() {
        super.onStart();
        mvideo = playVideo(mvideo);
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopPlay();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        Log.i(TAG, "onBackPressed");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mCountDownTimer1.cancel();
        stopPlay();

        if (mMyReceiver != null) {
            unregisterReceiver(mMyReceiver);
            mMyReceiver = null;
        }

        if (null != mHomeKeyReceiver)
            unregisterHomeKeyReceiver(this);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {

        if (keyCode == KeyEvent.KEYCODE_BACK) {
            Log.i(TAG, "onKeyDown KeyEvent.KEYCODE_BACK");

            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("退出提示：");
            builder.setMessage("您真的要退出吗？");

            builder.setNegativeButton("我要继续观看", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                }
            });

            builder.setPositiveButton("退出", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    stopPlay();
                }
            });

            if (bottombar.getVisibility() == View.VISIBLE) {
                bottombar.setVisibility(View.INVISIBLE);
                topbar.setVisibility(View.INVISIBLE);
                return true;
            } else {
                builder.show();
            }

        }

        switch (keyCode) {
            case KeyEvent.KEYCODE_MEDIA_PREVIOUS:
            case KeyEvent.KEYCODE_DPAD_UP:
                if (mvideo != null) {
                    if (mvideo.getPreOMedia() != null) {
                        mCountDownTimer1.cancel();
                        mvideo = mvideo.getPreOMedia();
                        tvFileName.setText(mvideo.getMovie().getMovieName());
                        mCountDownTimer1.start();
                    }
                }
                break;
            case KeyEvent.KEYCODE_MEDIA_NEXT:
            case KeyEvent.KEYCODE_DPAD_DOWN:
                if (mvideo != null) {
                    if (mvideo.getNextOMedia() != null) {
                        mCountDownTimer1.cancel();
                        mvideo = mvideo.getNextOMedia();
                        tvFileName.setText(mvideo.getMovie().getMovieName());
                        mCountDownTimer1.start();
                    }
                }
                break;
            case KeyEvent.KEYCODE_DPAD_CENTER:
            case KeyEvent.KEYCODE_ENTER:
            case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
                if (mvideo != null) {
                    mvideo.setNormalRage();
                    mvideo.playPause();
                }
                break;
            case KeyEvent.KEYCODE_MEDIA_STOP:
                stopPlay();
                break;

            case KeyEvent.KEYCODE_DPAD_RIGHT:
            case KeyEvent.KEYCODE_MEDIA_FAST_FORWARD:
                if (mvideo != null) mvideo.fastForward(10000l);
                break;

            case KeyEvent.KEYCODE_DPAD_LEFT:
            case KeyEvent.KEYCODE_MEDIA_REWIND:
                if (mvideo != null) mvideo.fastBack(10000l);
                break;
            default:
                break;
        }

        return super.onKeyDown(keyCode, event);
    }


    @SuppressLint("LongLogTag")
    private void registerHomeKeyReceiver(Context context) {
        Log.i(TAG, "registerHomeKeyReceiver");
        mHomeKeyReceiver = new HomeWatcherReceiver();
        final IntentFilter homeFilter = new IntentFilter(Intent.ACTION_CLOSE_SYSTEM_DIALOGS);

        context.registerReceiver(mHomeKeyReceiver, homeFilter);
    }

    @SuppressLint("LongLogTag")
    private void unregisterHomeKeyReceiver(Context context) {
        Log.i(TAG, "unregisterHomeKeyReceiver");
        try {
            if (null != mHomeKeyReceiver) {
                context.unregisterReceiver(mHomeKeyReceiver);
                mHomeKeyReceiver = null;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    class HomeWatcherReceiver extends BroadcastReceiver {
        private static final String LOG_TAG = "HomeReceiver";
        private static final String SYSTEM_DIALOG_REASON_KEY = "reason";
        private static final String SYSTEM_DIALOG_REASON_RECENT_APPS = "recentapps";
        private static final String SYSTEM_DIALOG_REASON_HOME_KEY = "homekey";
        private static final String SYSTEM_DIALOG_REASON_LOCK = "lock";
        private static final String SYSTEM_DIALOG_REASON_ASSIST = "assist";

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.i(LOG_TAG, "onReceive: action: " + action);
            if (action.equals(Intent.ACTION_CLOSE_SYSTEM_DIALOGS)) {
                String reason = intent.getStringExtra(SYSTEM_DIALOG_REASON_KEY);
                if (SYSTEM_DIALOG_REASON_HOME_KEY.equals(reason)) {
                    // 短按Home键
                    stopPlay();
                    Log.i(LOG_TAG, "homekey");

                } else if (SYSTEM_DIALOG_REASON_RECENT_APPS.equals(reason)) {
                    // 长按Home键 或者 activity切换键
                    stopPlay();
                    Log.i(LOG_TAG, "long press home key or activity switch");
                } else if (SYSTEM_DIALOG_REASON_LOCK.equals(reason)) {
                    // 锁屏
                    stopPlay();
                    Log.i(LOG_TAG, "lock");
                } else if (SYSTEM_DIALOG_REASON_ASSIST.equals(reason)) {
                    stopPlay();
                    Log.i(LOG_TAG, "assist");
                }
            }
        }
    }

    public class MyReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
        }
    }

    private synchronized void stopPlay() {
        new Thread() {
            public void run() {
                if (mvideo != null)
                    mvideo.stop();

            }
        }.start();

        PlayBackManagerActivity.this.finish();
    }

    private synchronized OMedia playVideo(final OMedia video) {
        if (video != null) {
            mSurfaceView.setVisibility(View.VISIBLE);
            mProgressBar.setVisibility(View.VISIBLE);
            ivplay.setVisibility(View.INVISIBLE);
            topbar.setVisibility(View.VISIBLE);
            bottombar.setVisibility(View.VISIBLE);
            tvFileName.setText(video.getMovie().getSourceUrl());
            try {
                video.with(this).playOn(mSurfaceView);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return video;
    }

    @Override
    public void OnEventCallBack(int i, final long l, long l1, float v, int i1, int i2, int i3, float v1, long l2) {
        //Log.d(TAG, "PlayState=" + mvideo.getPlayState() + ",event.type=" + i + ",TimeChanged=" + l + ", LengthChanged=" + l1 + ", PositionChanged=" + v + ", VoutCount=" + i1 + ", i2=" + i2 + ", i3=" + i3 + ", v1=" + v1 + ",Length=" + l2 + ",getPosition()=" + mvideo.getPosition());

        if (i <= 259)//缓冲
        {
            bottombar.setVisibility(View.VISIBLE);
            topbar.setVisibility(View.VISIBLE);
            mProgressBar.setVisibility(View.VISIBLE);
            ivplay.setVisibility(View.INVISIBLE);
            //tvprogress.setText(l1 + " / " + l2);
        } else if (i == 260)//开始播放
        {
            mProgressBar.setVisibility(View.INVISIBLE);
            ivplay.setVisibility(View.INVISIBLE);
            //tvprogress.setText(l1 + " / " + l2);
        } else if (i == 261)//暂停
        {
            bottombar.setVisibility(View.VISIBLE);
            topbar.setVisibility(View.VISIBLE);
            ivplay.setVisibility(View.VISIBLE);
            ivplay.bringToFront();
            bottombar.bringToFront();
            topbar.bringToFront();
        } else if (i == 267)//正在播放
        {
            if (l2 > 0) {
                try {
                    tvprogress.setText(TimeDateUtils.getTimeByMillisecond(l1) + " / " + TimeDateUtils.getTimeByMillisecond(l2));
                } catch (Exception e) {
                    //e.printStackTrace();
                    tvprogress.setText(l1 + " / " + l2);
                }
                seekBar.setVisibility(View.VISIBLE);
                seekBar.setMax((int) l2);
                seekBar.setProgress((int) l1, true);
                mProgressBar.setVisibility(View.INVISIBLE);
            } else {
                bottombar.setVisibility(View.INVISIBLE);
                mProgressBar.setVisibility(View.INVISIBLE);
            }
        } else if (i >= 270)//准备
        {
            mProgressBar.setVisibility(View.VISIBLE);
        }

        int ii= mvideo.getPlayState();
        switch (ii) {
            case 0:
            case 1:
            case 2:
            case 3:
            case 4:
                break;
            case 5:
            case 6:
            case 7:

                if (mvideo.getPlayOrder() == 0) //列表循环
                {
                    if (mvideo.getNextOMedia() != null)
                        mvideo = playVideo(mvideo.getNextOMedia());
                    else
                        stopPlay();
                }
                else if (mvideo.getPlayOrder() == 1)//单次播放，只播放一次
                {
                    stopPlay();
                }
                else //if (mvideo.getPlayOrder() == 2) //单曲循环
                {
                    //mvideo.setPlayOrder(mvideo.getPlayOrder()-1);
                    playVideo(mvideo);
                }

                //Log.d(TAG, "PlayState=" + mvideo.getPlayState() + ",event.type=" + i + ",TimeChanged=" + l + ", LengthChanged=" + l1 + ", PositionChanged=" + v + ", VoutCount=" + i1 + ", i2=" + i2 + ", i3=" + i3 + ", v1=" + v1 + ",Length=" + l2 + ",getPosition()=" + mvideo.getPosition());

                break;

        }

    }


}
