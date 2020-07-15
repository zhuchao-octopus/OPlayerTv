/*
 * Copyright (C) 2017 The Android Open Source Project
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

import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.support.v17.leanback.app.BackgroundManager;
import android.support.v17.leanback.app.BrowseFragment;
import android.support.v17.leanback.widget.ArrayObjectAdapter;
import android.support.v17.leanback.widget.HeaderItem;
import android.support.v17.leanback.widget.ImageCardView;
import android.support.v17.leanback.widget.ListRow;
import android.support.v17.leanback.widget.ListRowPresenter;
import android.support.v17.leanback.widget.OnItemViewClickedListener;
import android.support.v17.leanback.widget.OnItemViewSelectedListener;
import android.support.v17.leanback.widget.Presenter;
import android.support.v17.leanback.widget.Row;
import android.support.v17.leanback.widget.RowPresenter;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.content.ContextCompat;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.GlideDrawable;
import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.SimpleTarget;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import com.zhuchao.android.playsession.SessionCompleteCallback;
import com.zhuchao.android.video.Movie;
import com.zhuchao.android.video.OMedia;

import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import static com.zhuchao.android.oplayertv.MediaLibrary.MOVIE_CATEGORY;

//import com.zhuchao.android.shapeloading.ShapeLoadingDialog;


public class MainFragment extends BrowseFragment implements SessionCompleteCallback {
    private static final String TAG = "MainFragment";
    private static final int BACKGROUND_UPDATE_DELAY = 300;
    private static final int GRID_ITEM_WIDTH = 200;
    private static final int GRID_ITEM_HEIGHT = 200;
    //private static final int NUM_ROWS = 2;//6;
    //private static final int NUM_COLS = 15;

    private final Handler mHandler = new Handler();
    private Drawable mDefaultBackground;
    private DisplayMetrics mMetrics;
    private Timer mBackgroundTimer;
    private String mBackgroundUri;
    private BackgroundManager mBackgroundManager;
    //private boolean mIsInitComplete = false;

    //private ShapeLoadingDialog mShapeLoadingDialog = null;
    private ArrayObjectAdapter rowsAdapter = new ArrayObjectAdapter(new ListRowPresenter());
    private CardPresenter cardPresenter = new CardPresenter();
    private final int mDrawableID[] = {R.drawable.bg0, R.drawable.bg1, R.drawable.bg2};
    private int mDrawableIndex = 0;

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        MediaLibrary.getSessionManager(getActivity().getApplicationContext());
        prepareBackgroundManager();
        setupUIElements();


        //if (!MediaLibrary.isSessionManagerInitComplete())
        //   showLoadingDialog(getActivity(), true);

        //   Intent intent = null;
        //   intent.setPackage("com.android.time.service");
        //   try {
        //    if (checkApkExist(getActivity(), "com.android.time.service"))
        //        startActivity(intent);
        //     else
        //        Log.d(TAG,"没有找到应用"+"com.android.time.service");//Toast.makeText(getActivity(), "没有找到应用： " + map.get("package"), Toast.LENGTH_SHORT).show();
        // } catch (Exception e) {
        //            e.printStackTrace();
        //        Log.d(TAG,"没有找到应用"+"com.android.time.service");//Toast.makeText(getActivity(), "没有找到应用： " + map.get("package"), Toast.LENGTH_SHORT).show();
        //}

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (null != mBackgroundTimer) {
            Log.d(TAG, "onDestroy: " + mBackgroundTimer.toString());
            mBackgroundTimer.cancel();
        }
        MediaLibrary.ClearOPlayerSessionManager();
    }

    private void prepareBackgroundManager() {

        mBackgroundManager = BackgroundManager.getInstance(getActivity());
        mBackgroundManager.attach(getActivity().getWindow());

        //mDefaultBackground = ContextCompat.getDrawable(getContext(), R.drawable.default_background);
        mDefaultBackground = ContextCompat.getDrawable(getContext(), R.drawable.bg3);
        mMetrics = new DisplayMetrics();
        getActivity().getWindowManager().getDefaultDisplay().getMetrics(mMetrics);

        mBackgroundManager.setDrawable(ContextCompat.getDrawable(getContext(), R.drawable.bg3));
    }

    @Override
    public void onResume() {
        super.onResume();
        mBackgroundManager.setDrawable(ContextCompat.getDrawable(getContext(), R.drawable.bg3));
        try {
            loadMediaDataFromSessionManager();
            setupEventListeners();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void setupUIElements() {
        //setBadgeDrawable(getActivity().getResources().getDrawable(
        //R.drawable.videos_by_google_banner));
        setTitle(getString(R.string.app_name)); // Badge, when set, takes precedent
        // over title
        setHeadersState(HEADERS_ENABLED);
        //setHeadersTransitionOnBackEnabled(true);

        //set fastLane (or headers) background color
        setBrandColor(ContextCompat.getColor(getContext(), R.color.fastlane_background));
        //set search icon color
        //setSearchAffordanceColor(ContextCompat.getColor(getContext(), R.color.search_opaque));
    }

    private boolean loadMediaDataFromSessionManager() {

        MediaLibrary.setupCategoryList();
        rowsAdapter.clear();
        setAdapter(rowsAdapter);
        Log.d(TAG,"MediaLibrary.MOVIE_CATEGORY  size ="+ MOVIE_CATEGORY.size());

        for (int i = 0; i < MOVIE_CATEGORY.size(); i++) {   //NUM_ROWS
            List<OMedia> list = MediaLibrary.getMediaListByIndex(i);
            if (list == null) continue;
            Log.d(TAG,"MediaLibrary.getMediaListByIndex "+ i+" size ="+ list.size());
            if (list.size()<=0) continue;

            ArrayObjectAdapter listRowAdapter = new ArrayObjectAdapter(cardPresenter);
            HeaderItem header = new HeaderItem(i, MOVIE_CATEGORY.get(i).toString());


            double f = list.size() / 20;
            //int n = list.size() % 20;
            for (int j = 0; j < list.size(); j++) {//NUM_COLS

                if (f <= 1)
                {
                    if (listRowAdapter == null)
                        listRowAdapter = new ArrayObjectAdapter(cardPresenter);
                    listRowAdapter.add(list.get(j));
                }
                else
                {
                    if (listRowAdapter == null)
                        listRowAdapter = new ArrayObjectAdapter(cardPresenter);
                    listRowAdapter.add(list.get(j));

                    if (((j % 19) == 0) && (j != 0)) {
                        rowsAdapter.add(new ListRow(header, listRowAdapter));
                        //rowsAdapter.notify();
                        listRowAdapter = null;
                    }
                }
            }

            if (listRowAdapter != null) {
                rowsAdapter.add(new ListRow(header, listRowAdapter));
                //rowsAdapter.notify();
                listRowAdapter = null;
            }
        }


        //HeaderItem gridHeader = new HeaderItem(i, "关于视频");
        //GridItemPresenter mGridPresenter = new GridItemPresenter();
        //ArrayObjectAdapter gridRowAdapter = new ArrayObjectAdapter(mGridPresenter);
        //gridRowAdapter.add(getString(R.string.my_favorites));
        //gridRowAdapter.add(getResources().getString(R.string.system_settings));
        //rowsAdapter.add(new ListRow(gridHeader, gridRowAdapter));

        //rowsAdapter.notifyAll();
        return true;
    }

    private void setupEventListeners() {
        setOnSearchClickedListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ;//Toast.makeText(getActivity(), "Implement your own in-app search", Toast.LENGTH_LONG).show();
            }
        });

        setOnItemViewClickedListener(new ItemViewClickedListener());
        setOnItemViewSelectedListener(new ItemViewSelectedListener());
    }

    private final class ItemViewClickedListener implements OnItemViewClickedListener {
        @Override
        public void onItemClicked(Presenter.ViewHolder itemViewHolder, Object item,
                                  RowPresenter.ViewHolder rowViewHolder, Row row) {
            if (item instanceof OMedia) {
                OMedia video = (OMedia) item;
                Log.d(TAG, "Item: " + item.toString());
                if (video.getMovie().getSourceId() == 1004) {
                    Map<String, String> map;
                    String action = null;
                    Intent intent = null;
                    try {
                        map = JsonToMap(video.getMovie().getSourceUrl());
                        Log.d(TAG, video.getMovie().getSourceUrl());
                        action = map.get("action");
                        intent = new Intent(action);
                    } catch (Exception e) {
                        e.printStackTrace();
                        return;
                    }

                    //intent.setPackage(map.get("package"));
                    intent.putExtra("category_id", map.get("category_id"));
                    intent.putExtra("channelId", map.get("channelId"));
                    intent.putExtra("invokeFrom", map.get("invokeFrom"));
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    try {
                        if (checkApkExist(getActivity(), map.get("package")))
                            startActivity(intent);
                        else
                            Toast.makeText(getActivity(), "没有找到应用： " + map.get("package"), Toast.LENGTH_SHORT).show();
                    } catch (Exception e) {
                        e.printStackTrace();
                        Toast.makeText(getActivity(), "没有找到应用： " + map.get("package"), Toast.LENGTH_SHORT).show();
                    }

                } else if (video.getMovie().getSourceId() == 1003) {

                    Map<String, String> map;
                    String action = null;
                    Intent intent = null;
                    try {
                        map = JsonToMap(video.getMovie().getSourceUrl());
                        Log.d(TAG, video.getMovie().getSourceUrl());
                        action = map.get("action");
                        intent = new Intent(action);
                    } catch (Exception e) {
                        e.printStackTrace();
                        return;
                    }

                    intent.setPackage(map.get("package"));
                    intent.putExtra("videoId", map.get("videoId"));
                    intent.putExtra("invokeFrom", map.get("invokeFrom"));

                    try {
                        if (checkApkExist(getActivity(), map.get("package")))
                            startActivity(intent);
                        else
                            Toast.makeText(getActivity(), "没有找到应用： " + map.get("package"), Toast.LENGTH_SHORT).show();
                    } catch (Exception e) {
                        e.printStackTrace();
                        Toast.makeText(getActivity(), "没有找到应用： " + map.get("package"), Toast.LENGTH_SHORT).show();
                    }

                } else {
                    //Intent intent = new Intent(getActivity(), DetailsActivity.class);
                    Intent intent = new Intent(getActivity(), PlayBackManagerActivity.class);
                    intent.putExtra("Video", video);
                    Bundle bundle = ActivityOptionsCompat.makeSceneTransitionAnimation(
                            getActivity(),
                            ((ImageCardView) itemViewHolder.view).getMainImageView(),
                            DetailsActivity.SHARED_ELEMENT_NAME)
                            .toBundle();
                    getActivity().startActivity(intent, bundle);
                }


            } else if (item instanceof String) {
                if (((String) item).contains(getString(R.string.my_favorites))) {
                    //Intent intent = new Intent(getActivity(), BrowseErrorActivity.class);
                    //startActivity(intent);
                } else if (((String) item).contains(getString(R.string.system_settings))) {
                    Intent intent = new Intent(Settings.ACTION_SETTINGS);
                    startActivity(intent);
                } else {
                    ;//Toast.makeText(getActivity(), ((String) item), Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    private final class ItemViewSelectedListener implements OnItemViewSelectedListener {
        @Override
        public void onItemSelected(
                Presenter.ViewHolder itemViewHolder,
                Object item,
                RowPresenter.ViewHolder rowViewHolder,
                Row row) {
            if (item instanceof Movie) {
                mBackgroundUri = ((Movie) item).getBgImageUrl();
                startBackgroundTimer();
            }
        }
    }

    private void updateBackground(String uri) {
        int width = mMetrics.widthPixels;
        int height = mMetrics.heightPixels;
        Glide.with(getActivity())
                .load(uri)
                .centerCrop()
                .error(mDefaultBackground)
                .into(new SimpleTarget<GlideDrawable>(width, height) {
                    @Override
                    public void onResourceReady(GlideDrawable resource,
                                                GlideAnimation<? super GlideDrawable>
                                                        glideAnimation) {
                        mBackgroundManager.setDrawable(resource);
                    }
                });

        mBackgroundManager.setDrawable(ContextCompat.getDrawable(getContext(), mDrawableID[mDrawableIndex]));
        if (mDrawableIndex < mDrawableID.length - 1) mDrawableIndex++;
        else
            mDrawableIndex = 0;
        //mBackgroundTimer.cancel();
    }

    private void startBackgroundTimer() {
        if (null != mBackgroundTimer) {
            mBackgroundTimer.cancel();
        }
        mBackgroundTimer = new Timer();
        mBackgroundTimer.schedule(new UpdateBackgroundTask(), BACKGROUND_UPDATE_DELAY);
    }


    private class UpdateBackgroundTask extends TimerTask {

        @Override
        public void run() {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    updateBackground(mBackgroundUri);
                }
            });
        }
    }

    private class GridItemPresenter extends Presenter {
        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent) {
            TextView view = new TextView(parent.getContext());
            view.setLayoutParams(new ViewGroup.LayoutParams(GRID_ITEM_WIDTH, GRID_ITEM_HEIGHT));
            view.setFocusable(true);
            view.setFocusableInTouchMode(true);
            view.setBackgroundColor(ContextCompat.getColor(getContext(), R.color.default_background));
            view.setTextColor(Color.WHITE);
            view.setGravity(Gravity.CENTER);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(ViewHolder viewHolder, Object item) {
            ((TextView) viewHolder.view).setText((String) item);
        }

        @Override
        public void onUnbindViewHolder(ViewHolder viewHolder) {
        }

    }


    public void showLoadingDialog(Context mContext, Boolean bVisible) {
     /* if (bVisible) {
            if (mShapeLoadingDialog == null)
                mShapeLoadingDialog = (new ShapeLoadingDialog.Builder(mContext)).loadText("请稍等，正在加载媒体库...").build();
            mShapeLoadingDialog.show();
        } else if (mShapeLoadingDialog != null) {
            mShapeLoadingDialog.cancel();
            mShapeLoadingDialog.dismiss();
            mShapeLoadingDialog = null;
        }*/
    }

    public Map<String, String> JsonToMap(String Jsonstr) {
        Gson gson = new GsonBuilder().serializeNulls().disableHtmlEscaping().create();
        try {
            return gson.fromJson(Jsonstr, new TypeToken<Map<String, String>>() {
            }.getType());
        } catch (JsonSyntaxException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static boolean checkApkExist(Context context, String packageName) {
        if (TextUtils.isEmpty(packageName))
            return false;
        try {
            ApplicationInfo info = context.getPackageManager().getApplicationInfo(packageName, 0);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    @Override
    public synchronized void OnSessionComplete(int i, String s) {
        Message msg = new Message();
        msg.what = i;
        mMyHandler.sendMessage(msg);
        Log.d(TAG, "OnSessionComplete ID = " + i + "   " + s);
    }

    Handler mMyHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {

            try {
                loadMediaDataFromSessionManager();
                setupEventListeners();
                //showLoadingDialog(getActivity(), false);
            } catch (Exception e) {
                e.printStackTrace();
            }

        }
    };

}
