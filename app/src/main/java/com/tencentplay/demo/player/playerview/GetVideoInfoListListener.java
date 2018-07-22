package com.tencentplay.demo.player.playerview;

import java.util.List;

/**
 * Created by vinsonswang on 2018/3/29.
 */

public interface GetVideoInfoListListener {
    void onGetVideoInfoList(List<VideoInfo> videoInfoList);

    void onFail(int errCode);
}
