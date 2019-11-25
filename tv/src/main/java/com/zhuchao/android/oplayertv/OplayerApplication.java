package com.zhuchao.android.oplayertv;

import android.app.Application;
import android.content.Context;

public class OplayerApplication extends Application {

    //private static OPlayerSessionManager mOPlayerSessionManager = null;

    private static Context appContext;//需要使用的上下文对象

    public OplayerApplication() {
        super();

    }

    public static Context getAppContext() {
        return appContext;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        appContext = this.getApplicationContext();
        //getOpsM();

    }

    @Override
    public void onTerminate() {
        super.onTerminate();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
    }

    @Override
    public void onTrimMemory(int level) {
        super.onTrimMemory(level);
    }

    @Override
    public void registerActivityLifecycleCallbacks(ActivityLifecycleCallbacks callback) {
        super.registerActivityLifecycleCallbacks(callback);
    }



}
