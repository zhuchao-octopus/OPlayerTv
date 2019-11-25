package com.zhuchao.android.oplayertv;


import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.KeyEvent;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.zhuchao.android.callbackevent.PlayerCallback;
import com.zhuchao.android.video.OMedia;


/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
public class FullscreenPlayBackActivity extends Activity implements PlayerCallback {
    private static final String TAG = "FullscreenPlayBackActivity-->";
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
    private TextView mtextView;
    private ProgressBar mProgressBar = null;
    private MyReceiver mMyReceiver;
    //private byte[] temp = {0, 0, 0, 0};


    //MyPlayer OPlayer = null;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON, WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_fullscreen);
        mSurfaceView = findViewById(R.id.surfaceView);
        mtextView = findViewById(R.id.textView);
       // mSurfaceView.setVisibility(View.INVISIBLE);
        mProgressBar = findViewById(R.id.progressBar);
        mProgressBar.setVisibility(View.INVISIBLE);

        try {
            mvideo = (OMedia) getIntent().getSerializableExtra("Video");
            if (mvideo != null) {
                mtextView.setText(mvideo.getMovie().getMovieName());
                mvideo.with(this).callback(this);
            } else {
                stopPlay();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        /*mCountDownTimerChannelControl = new CountDownTimer(20000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                Counter++;
                if (mSurfaceView.getVisibility() != View.VISIBLE && Counter >= 2) {
                    mSurfaceView.setVisibility(View.VISIBLE);
                    Counter = 0;
                }
            }

            @Override
            public void onFinish() { //跳转到下一个
                if (mvideo != null) {
                    if (mvideo.getNextOMedia() != null) {
                        mvideo = mvideo.getNextOMedia();
                        playVideo(mvideo);
                    } else if (mvideo.getPreOMedia() != null) {
                        mvideo = mvideo.getPreOMedia();
                        playVideo(mvideo);
                    } else {
                        //FullscreenPlayBackActivity.this.finish();
                        finish();
                    }
                }
            }
        };*/

        mCountDownTimer1 = new CountDownTimer(2000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
            }

            @Override
            public void onFinish() {
                if (mvideo != null)
                    playVideo(mvideo);
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
        playVideo(mvideo);
    }

    private void playVideo(final OMedia video) {
        if (video != null) {
            //mProgressBar.setVisibility(View.VISIBLE);
            mtextView.setText(video.getMovie().getMovieName());
            try {
                video.with(this).playOn(mSurfaceView).callback(this);
                //mCountDownTimerChannelControl.start();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
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
        //mCountDownTimerChannelControl.cancel();
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
        //if(mShapeLoadingDialog !=null && mShapeLoadingDialog.isShowing()) return super.onKeyDown(keyCode, event);
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

            builder.show();
        }

        if (keyCode == KeyEvent.KEYCODE_DPAD_UP) {
            if (mvideo != null) {
                if (mvideo.getPreOMedia() != null) {
                    mvideo = mvideo.getPreOMedia();
                    mtextView.setText(mvideo.getMovie().getMovieName());
                    mCountDownTimer1.cancel();
                    mCountDownTimer1.start();
                }
            }
        }

        if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
            if (mvideo != null) {
                if (mvideo.getNextOMedia() != null) {
                    mvideo = mvideo.getNextOMedia();
                    mtextView.setText(mvideo.getMovie().getMovieName());
                    mCountDownTimer1.cancel();
                    mCountDownTimer1.start();
                }
            }
        }

        if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER) {
            if (mvideo != null)
            mvideo.playPause();
        }

        if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN_LEFT) {

        }

        if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {

        }
        if (keyCode == KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE) {
            if (mvideo != null) mvideo.playPause();
        }
        if (keyCode == KeyEvent.KEYCODE_MEDIA_STOP) {
            stopPlay();
        }
        if (keyCode == KeyEvent.KEYCODE_MEDIA_NEXT) {
            if (mvideo != null) mvideo.getNextOMedia().play();
        }
        if (keyCode == KeyEvent.KEYCODE_MEDIA_PREVIOUS) {
            if (mvideo != null) mvideo.getPreOMedia().play();
        }
        if (keyCode == KeyEvent.KEYCODE_MEDIA_FAST_FORWARD) {
            if (mvideo != null) mvideo.fastForward(500);
        }
        if (keyCode == KeyEvent.KEYCODE_MEDIA_REWIND) {
            if (mvideo != null) mvideo.fastBack(500);
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public void OnEventCallBack(int i, final long l, long l1, float v, int i1, int i2, int i3, float v1, long l2) {
        if (mvideo== null) return;

        //Log.d(TAG,"mvideo.getPlayState()="+mvideo.getPlayState());
        //Log.d(TAG,"event.type="+i+",TimeChanged="+l +", LengthChanged=" + l1+", PositionChanged="+ v +", VoutCount="+ i1  +", i2="+ i2  +", i3="+ i3  +", v1="+ v1+",Length="+l2);

        //if (mvideo.getPlayState() >= 3 && mvideo.getPlayState() <= 6 && l != 0) {
            //if (mProgressBar != null)
            //    if(mProgressBar.getVisibility() == View.VISIBLE)
            //       mProgressBar.setVisibility(View.GONE);
        //    mCountDownTimerChannelControl.cancel();
        //    return;
        //}

        if (mvideo.getPlayState() >= 6) {
            stopPlay();
            Log.d(TAG, "mvideo.getPlayState()=" + mvideo.getPlayState());
            Log.d(TAG, "event.type=" + i + ",TimeChanged=" + l + ", LengthChanged=" + l1 + ", PositionChanged=" + v + ", VoutCount=" + i1 + ", i2=" + i2 + ", i3=" + i3 + ", v1=" + v1 + ",Length=" + l2);
        }
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

    private synchronized void stopPlay() {
        new Thread() {
            public void run() {
                if (mvideo != null)
                    mvideo.stop();
                //mvideo = null;
            }
        }.start();

        FullscreenPlayBackActivity.this.finish();
    }

    public class MyReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
        }
    }

}
