package com.tencentplay.demo.player;

/**
 * Created by yuejiaoli on 2018/7/4.
 */

public class SuperPlayerModel {
    /**
     * 视频标题
     */
    public String title;
    /**
     * 视频URL
     */
    public String videoURL;
    /**
     * 视频封面本地图片
     */
    public String placeholderImage;
    public int duration;

    /**
     * 播放器Model可选填上面地址或下面appid+fileid
     */
    public int appid;
    public String fileid;

}
