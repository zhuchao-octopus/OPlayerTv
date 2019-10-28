package com.zhuchao.android.oplayertv;


public class ConstData {
	public static final int DB_VERSION = 1;
	//public static final String DB_DIRECTORY = CommonValues.application.getFilesDir().getPath();
	//public static final String DB_NAME = "udp_player.db";

	public static final int DEFAULT_PREVIEW_WIDTH = 800;
	public static final int DEFAULT_PREVIEW_HEIGHT = 654;

	public static final int MEDIA_TYPE_INTERNET = 0;
	public static final int MEDIA_TYPE_LOCALHOST = 1;



	public static final int MEDIA_SOURCE_ID_LOCALHOST = 1000;
    public static final int MEDIA_SOURCE_ID_OPLAYER = 1001;
	public static final int MEDIA_SOURCE_ID_BEE = 1002; //蜜蜂视频


	public static final int SESSION_TYPE_MANAGER = 0;

	public static final int SESSION_TYPE_PLAYBACK = 1; //播放

	public static final int SESSION_TYPE_GET_MOVIE_BEGIN = 11;
	public static final int SESSION_TYPE_GET_MOVIE_CATEGORY = 12;//版面分类
	public static final int SESSION_TYPE_GET_MOVIELIST_TYPE = 13;//视屏分类
	public static final int SESSION_TYPE_GET_MOVIE_INFO = 14;//视频信息
	public static final int SESSION_TYPE_GET_MOVIELIST_ALL = 15;//视频列表
	public static final int SESSION_TYPE_GET_MOVIELIST_AREA = 16;//
	public static final int SESSION_TYPE_GET_MOVIELIST_YEAR = 17;
	public static final int SESSION_TYPE_GET_MOVIELIST_ACTOR = 18;
	public static final int SESSION_TYPE_GET_MOVIELIST_VIP = 20;
	public static final int SESSION_TYPE_GET_MOVIELIST_SOURCE = 21;
	public static final int SESSION_TYPE_GET_MOVIeE_END = 29;

	public static final int SESSION_TYPE_SIGNIN = 30;
	public static final int SESSION_TYPE_SIGNOUT = 31;
	public static final int SESSION_TYPE_PAY = 32;

	//板块分类视频列表的ID
	public static final int SESSION_TYPE_ZHIBO = 2003;//30;//获取直播/电视列表
	public static final int SESSION_TYPE_DIANYIN = 2004;//SESSION_TYPE_GET_MOVIELIST_ALLTV+1;//电影
	public static final int SESSION_TYPE_DIANSHIJU = 2005;//SESSION_TYPE_GET_MOVIELIST_ALLMOVIE+1;//电视剧



	public static final String GET_MEDIA_SOURCE_PLAY_INTERFACE = "http://39.105.72.148:8001/findVideoPlayInterface?";

}
