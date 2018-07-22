package com.tencentplay.demo.player.playerview;

import android.app.Activity;
import android.app.AppOpsManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.tencent.liteav.basic.log.TXCLog;
import com.tencent.rtmp.ITXVodPlayListener;
import com.tencent.rtmp.TXBitrateItem;
import com.tencent.rtmp.TXLiveConstants;
import com.tencent.rtmp.TXLivePlayer;
import com.tencent.rtmp.TXVodPlayConfig;
import com.tencent.rtmp.TXVodPlayer;
import com.tencent.rtmp.ui.TXCloudVideoView;
import com.tencentplay.demo.R;
import com.tencentplay.demo.player.SuperPlayerModel;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

/**
 * Created by liyuejiao on 2018/7/3.
 */

public class SuperPlayerView extends RelativeLayout implements ITXVodPlayListener {
    private static final String TAG = "SuperVodPlayerView";
    private Context mContext;

    private int mPlayMode = SuperPlayerConst.PLAYMODE_WINDOW;
    private boolean mLockScreen = false;

    // UI
    private ViewGroup mRootView;
    private TXCloudVideoView mTXCloudVideoView;
    private TCVodControllerLarge mVodControllerLarge;
    private TCVodControllerSmall mVodControllerSmall;
    private TCVodControllerFloat mVodControllerFloat;

    private TCDanmuView mDanmuView;
    private ViewGroup.LayoutParams mLayoutParamWindowMode;
    private ViewGroup.LayoutParams mLayoutParamFullScreenMode;
    private LayoutParams mVodControllerSmallParams;
    private LayoutParams mVodControllerLargeParams;
    // 播放器
    private TXVodPlayer mVodPlayer;
    private TXVodPlayConfig mVodPlayConfig;
    private PlayerViewCallback mPlayerViewCallback;
    private String mCurrentVideoURL;
    private int mCurrentPlayState = SuperPlayerConst.PLAYSTATE_PLAY;
    private boolean mDefaultSet;

    public SuperPlayerView(Context context) {
        super(context);
        initView(context);
        initVodPlayer(context);
    }

    public SuperPlayerView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initView(context);
        initVodPlayer(context);
    }

    public SuperPlayerView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initView(context);
        initVodPlayer(context);
    }

    private void initView(Context context) {
        mContext = context;
        mRootView = (ViewGroup) LayoutInflater.from(context).inflate(R.layout.super_vod_player_view, null);
        mTXCloudVideoView = (TXCloudVideoView) mRootView.findViewById(R.id.cloud_video_view);
        mVodControllerLarge = (TCVodControllerLarge) mRootView.findViewById(R.id.controller_large);
        mVodControllerSmall = (TCVodControllerSmall) mRootView.findViewById(R.id.controller_small);
        mVodControllerFloat = (TCVodControllerFloat) mRootView.findViewById(R.id.controller_float);
        mDanmuView = (TCDanmuView) mRootView.findViewById(R.id.danmaku_view);

        mVodControllerSmallParams = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
        mVodControllerLargeParams = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);

        mVodControllerLarge.setVodController(mVodController);
        mVodControllerSmall.setVodController(mVodController);
        mVodControllerFloat.setVodController(mVodController);

        removeAllViews();
        mRootView.removeView(mDanmuView);
        mRootView.removeView(mTXCloudVideoView);
        mRootView.removeView(mVodControllerSmall);
        mRootView.removeView(mVodControllerLarge);
        mRootView.removeView(mVodControllerFloat);

        addView(mTXCloudVideoView);
        if (mPlayMode == SuperPlayerConst.PLAYMODE_FULLSCREEN) {
            addView(mVodControllerLarge);
            mVodControllerLarge.hide();
        } else if (mPlayMode == SuperPlayerConst.PLAYMODE_WINDOW) {
            addView(mVodControllerSmall);
            mVodControllerSmall.hide();
        }
        addView(mDanmuView);

        post(new Runnable() {
            @Override
            public void run() {
                if (mPlayMode == SuperPlayerConst.PLAYMODE_WINDOW) {
                    mLayoutParamWindowMode = getLayoutParams();
                }
                try {
                    // 依据上层Parent的LayoutParam类型来实例化一个新的fullscreen模式下的LayoutParam
                    Class parentLayoutParamClazz = getLayoutParams().getClass();
                    Constructor constructor = parentLayoutParamClazz.getDeclaredConstructor(int.class, int.class);
                    mLayoutParamFullScreenMode = (android.view.ViewGroup.LayoutParams) constructor.newInstance(android.view.ViewGroup.LayoutParams.MATCH_PARENT, android.view.ViewGroup.LayoutParams.MATCH_PARENT);
                } catch (Exception e) {
                    e.printStackTrace();
                }

            }
        });

    }

    /**
     * 初始化点播播放器
     *
     * @param context
     */
    private void initVodPlayer(Context context) {
        mVodPlayer = new TXVodPlayer(context);

        SuperPlayerGlobalConfig config = SuperPlayerGlobalConfig.getInstance();

        mVodPlayConfig = new TXVodPlayConfig();
        mVodPlayConfig.setCacheFolderPath(Environment.getExternalStorageDirectory().getPath() + "/txcache");
        mVodPlayConfig.setMaxCacheItems(config.maxCacheItem);

        mVodPlayer.setConfig(mVodPlayConfig);
        mVodPlayer.setRenderMode(config.renderMode);
        mVodPlayer.setPlayerView(mTXCloudVideoView);
        mVodPlayer.setVodListener(this);
        mVodPlayer.enableHardwareDecode(config.enableHWAcceleration);
    }

    public void playWithSuperPlayerMode(SuperPlayerModel superPlayerModel) {
        if (!TextUtils.isEmpty(superPlayerModel.videoURL)) {
            playWithURL(superPlayerModel);
        } else {
            playWithFileId(superPlayerModel);
        }

        mVodControllerSmall.updateReplay(false);
        mVodControllerLarge.updateReplay(false);
    }

    private void playWithURL(SuperPlayerModel superPlayerModel) {
        if (mVodPlayer != null) {
            mDefaultSet = false;
            mCurrentVideoURL = superPlayerModel.videoURL;
            mVodPlayer.stopPlay(true);
            mVodPlayer.setAutoPlay(true);
            mVodPlayer.startPlay(superPlayerModel.videoURL);
        }
    }

    private void playWithFileId(final SuperPlayerModel superPlayerModel) {
        SuperVodInfoLoader loader = new SuperVodInfoLoader();
        loader.setOnVodInfoLoadListener(new SuperVodInfoLoader.OnVodInfoLoadListener() {
            @Override
            public void onSuccess(TXPlayInfoResponse response) {
                mVodControllerSmall.updateTitle(superPlayerModel.title);
                mVodControllerLarge.updateTitle(superPlayerModel.title);

                SuperPlayerModel playerModel = new SuperPlayerModel();
                TXPlayInfoStream masterPlayList = response.getMasterPlayList();
                if (masterPlayList != null) { //有masterPlaylist
                    String videoURL = masterPlayList.getUrl();
                    playerModel.videoURL = videoURL;
                    playWithURL(playerModel);
                    return;
                }

                HashMap<String, TXPlayInfoStream> transcodeList = response.getTranscodePlayList();
                if (transcodeList != null && transcodeList.size() != 0) { //没有transcodePlaylist
                    String defaultClassification = response.getDefaultVideoClassification();
                    TXPlayInfoStream stream = transcodeList.get(defaultClassification);
                    String videoURL = stream.getUrl();
                    playerModel.videoURL = videoURL;
                    playWithURL(playerModel);

                    TCVideoQulity defaultVideoQulity = SuperPlayerUtil.convertToVideoQuality(stream);
                    mVodControllerLarge.updateVideoQulity(defaultVideoQulity);

                    ArrayList<TCVideoQulity> videoQulities = SuperPlayerUtil.convertToVideoQualityList(transcodeList);
                    mVodControllerLarge.setVideoQualityList(videoQulities);
                    return;
                }
                TXPlayInfoStream sourceStream = response.getSource();
                if (sourceStream != null) {
                    String videoURL = sourceStream.getUrl();
                    playerModel.videoURL = videoURL;
                    playWithURL(playerModel);
                    String defaultClassification = response.getDefaultVideoClassification();

                    if (defaultClassification != null) {
                        TCVideoQulity defaultVideoQulity = SuperPlayerUtil.convertToVideoQuality(sourceStream, defaultClassification);
                        mVodControllerLarge.updateVideoQulity(defaultVideoQulity);

                        ArrayList<TCVideoQulity> videoQulities = new ArrayList<>();
                        videoQulities.add(defaultVideoQulity);
                        mVodControllerLarge.setVideoQualityList(videoQulities);
                    }
                }
            }

            @Override
            public void onFail(int errCode) {

            }
        });
        loader.getVodByFileId(superPlayerModel);
    }

    public void onResume() {
        if (mDanmuView != null && mDanmuView.isPrepared() && mDanmuView.isPaused()) {
            mDanmuView.resume();
        }
        if (mVodPlayer != null) {
            mVodPlayer.resume();
        }
    }

    public void onPause() {
        if (mDanmuView != null && mDanmuView.isPrepared()) {
            mDanmuView.pause();
        }
        if (mVodPlayer != null) {
            mVodPlayer.pause();
        }
    }

    public void onDestroy() {
        if (mDanmuView != null) {
            mDanmuView.release();
            mDanmuView = null;
        }
        if (mVodPlayer != null) {
            mVodPlayer.stopPlay(false);
        }
    }

    /**
     * 设置超级播放器的回掉
     *
     * @param callback
     */
    public void setPlayerViewCallback(PlayerViewCallback callback) {
        mPlayerViewCallback = callback;
    }

    private WindowManager mWindowManager;
    private WindowManager.LayoutParams mWindowParams;
    /**
     * 播放器控制
     */
    private TCVodControllerBase.VodController mVodController = new TCVodControllerBase.VodController() {
        @Override
        public void onRequestPlayMode(int requestPlayMode) {
            if (mPlayMode == requestPlayMode)
                return;

            if (mLockScreen) //锁屏
                return;

            //请求全屏模式
            if (requestPlayMode == SuperPlayerConst.PLAYMODE_FULLSCREEN) {
                TXCLog.i(TAG, "requestPlayMode FullScreen");

                if (mLayoutParamFullScreenMode == null)
                    return;
                removeView(mVodControllerSmall);
                addView(mVodControllerLarge, mVodControllerLargeParams);
                setLayoutParams(mLayoutParamFullScreenMode);
                rotateScreenOrientation(SuperPlayerConst.ORIENTATION_LANDSCAPE);
                mVodControllerLarge.hide();

                if (mPlayerViewCallback != null) {
                    mPlayerViewCallback.hideViews();
                }
            }
            // 请求窗口模式
            else if (requestPlayMode == SuperPlayerConst.PLAYMODE_WINDOW) {
                TXCLog.i(TAG, "requestPlayMode Window");

                // 当前是悬浮窗
                if (mPlayMode == SuperPlayerConst.PLAYMODE_FLOAT) {
                    Intent intent = new Intent();
                    intent.setAction("cn.kuwo.player.action.SHORTCUT");
                    mContext.startActivity(intent);

                    mVodPlayer.pause();
                    if (mLayoutParamWindowMode == null)
                        return;
                    mWindowManager.removeView(mVodControllerFloat);

                    mVodPlayer.setPlayerView(mTXCloudVideoView);
                    mVodPlayer.resume();
                }
                // 当前是全屏模式
                else if (mPlayMode == SuperPlayerConst.PLAYMODE_FULLSCREEN) {
                    if (mLayoutParamWindowMode == null)
                        return;
                    removeView(mVodControllerLarge);
                    addView(mVodControllerSmall, mVodControllerSmallParams);
                    setLayoutParams(mLayoutParamWindowMode);
                    rotateScreenOrientation(SuperPlayerConst.ORIENTATION_PORTRAIT);
                    mVodControllerSmall.hide();

                    if (mPlayerViewCallback != null) {
                        mPlayerViewCallback.showViews();
                    }
                }
            }
            //请求悬浮窗模式
            else if (requestPlayMode == SuperPlayerConst.PLAYMODE_FLOAT) {
                TXCLog.i(TAG, "requestPlayMode Float :" + Build.MANUFACTURER);

                SuperPlayerGlobalConfig prefs = SuperPlayerGlobalConfig.getInstance();
                if (!prefs.enableFloatWindow) {
                    return;
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) { // 6.0动态申请悬浮窗权限
                    if (!Settings.canDrawOverlays(mContext)) {
                        Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION);
                        mContext.startActivity(intent);
                        return;
                    }
                } else {
                    if (!checkOp(mContext, OP_SYSTEM_ALERT_WINDOW)) {
                        Toast.makeText(mContext, "进入设置页面失败,请手动开启悬浮窗权限", Toast.LENGTH_SHORT).show();
                        return;
                    }
                }

                mVodPlayer.pause();

                mWindowManager = (WindowManager) mContext.getApplicationContext().getSystemService(Context.WINDOW_SERVICE);
                mWindowParams = new WindowManager.LayoutParams();
                mWindowParams.type = WindowManager.LayoutParams.TYPE_SYSTEM_ALERT;
                mWindowParams.flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                        | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
                mWindowParams.format = PixelFormat.TRANSLUCENT;
                mWindowParams.gravity = Gravity.LEFT | Gravity.TOP;

                SuperPlayerGlobalConfig.TXRect rect = prefs.floatViewRect;
                mWindowParams.x = rect.x;
                mWindowParams.y = rect.y;
                mWindowParams.width = rect.width;
                mWindowParams.height = rect.height;

                mWindowManager.addView(mVodControllerFloat, mWindowParams);

                TXCloudVideoView videoView = mVodControllerFloat.getFloatVideoView();
                if (videoView != null) {
                    mVodPlayer.setPlayerView(videoView);
                    mVodPlayer.resume();
                }
            }
            mPlayMode = requestPlayMode;
        }

        @Override
        public void onBackPress(int playMode) {
            // 当前是全屏模式，返回切换成窗口模式
            if (playMode == SuperPlayerConst.PLAYMODE_FULLSCREEN) {
                onRequestPlayMode(SuperPlayerConst.PLAYMODE_WINDOW);
            }
            // 当前是窗口模式，返回退出播放器
            else if (playMode == SuperPlayerConst.PLAYMODE_WINDOW) {
                if (mPlayerViewCallback != null) {
                    mPlayerViewCallback.onQuit(SuperPlayerConst.PLAYMODE_WINDOW);
                }
                onRequestPlayMode(SuperPlayerConst.PLAYMODE_FLOAT);
            }
            // 当前是悬浮窗，退出
            else if (playMode == SuperPlayerConst.PLAYMODE_FLOAT) {
                mWindowManager.removeView(mVodControllerFloat);
                if (mPlayerViewCallback != null) {
                    mPlayerViewCallback.onQuit(SuperPlayerConst.PLAYMODE_FLOAT);
                }
            }
        }

        @Override
        public void onControllerShow(int playMode) {

        }

        @Override
        public void onControllerHide(int playMode) {

        }

        @Override
        public void onRequestLockMode(boolean lockMode) {

        }

        @Override
        public void resume() {
            if (mVodPlayer != null) {
                mVodPlayer.resume();
            }
            mCurrentPlayState = SuperPlayerConst.PLAYSTATE_PLAY;
            mVodControllerSmall.updatePlayState(true);
            mVodControllerLarge.updatePlayState(true);
        }

        @Override
        public void pause() {
            if (mVodPlayer != null) {
                mVodPlayer.pause();
            }
            mCurrentPlayState = SuperPlayerConst.PLAYSTATE_PAUSE;
            mVodControllerSmall.updatePlayState(false);
            mVodControllerLarge.updatePlayState(false);
        }

        @Override
        public float getDuration() {
            return mVodPlayer.getDuration();
        }

        @Override
        public float getCurrentPlaybackTime() {
            return mVodPlayer.getCurrentPlaybackTime();
        }

        @Override
        public void seekTo(int position) {
            if (mVodPlayer != null) {
                mVodPlayer.seek(position);
            }
        }

        @Override
        public boolean isPlaying() {
            return mVodPlayer.isPlaying();
        }

        @Override
        public void onDanmuku(boolean on) {
            if (mDanmuView != null) {
                mDanmuView.toggle(on);
            }
        }

        @Override
        public void onSnapshot() {
            if (mVodPlayer != null) {
                mVodPlayer.snapshot(new TXLivePlayer.ITXSnapshotListener() {
                    @Override
                    public void onSnapshot(Bitmap bmp) {
                        showSnapshotWindow(bmp);
                    }
                });
            }
        }

        @Override
        public void onQualitySelect(TCVideoQulity quality) {
            mVodControllerLarge.updateVideoQulity(quality);
            if (mVodPlayer != null) {
                TXCLog.i(TAG, "setBitrateIndex quality.index:" + quality.index);
                mVodPlayer.setBitrateIndex(quality.index);
            }
        }

        @Override
        public void onSpeedChange(float speedLevel) {
            if (mVodPlayer != null) {
                mVodPlayer.setRate(speedLevel);
            }
        }

        @Override
        public void onMirrorChange(boolean isMirror) {
            if (mVodPlayer != null) {
                mVodPlayer.setMirror(isMirror);
            }
        }

        @Override
        public void onFloatUpdate(int x, int y) {
            mWindowParams.x = x;
            mWindowParams.y = y;
            mWindowManager.updateViewLayout(mVodControllerFloat, mWindowParams);
        }

        @Override
        public void onReplay() {
            if (!TextUtils.isEmpty(mCurrentVideoURL)) {
                SuperPlayerModel superPlayerModel = new SuperPlayerModel();
                superPlayerModel.videoURL = mCurrentVideoURL;
                playWithSuperPlayerMode(superPlayerModel);
            }
        }
    };

    /**
     * 显示截图窗口
     *
     * @param bmp
     */
    private void showSnapshotWindow(Bitmap bmp) {
        PopupWindow popupWindow = new PopupWindow(mContext);
        popupWindow.setWidth(ViewGroup.LayoutParams.WRAP_CONTENT);
        popupWindow.setHeight(ViewGroup.LayoutParams.WRAP_CONTENT);
        View view = LayoutInflater.from(mContext).inflate(R.layout.layout_new_vod_snap, null);
        ImageView imageView = (ImageView) view.findViewById(R.id.iv_snap);
        imageView.setImageBitmap(bmp);
        popupWindow.setContentView(view);
        popupWindow.setOutsideTouchable(true);
        popupWindow.setFocusable(true);
        popupWindow.showAtLocation(mRootView, Gravity.TOP, 1800, 300);
    }

    /**
     * 旋转屏幕方向
     *
     * @param orientation
     */
    private void rotateScreenOrientation(int orientation) {
        switch (orientation) {
            case SuperPlayerConst.ORIENTATION_LANDSCAPE:
                ((Activity) mContext).setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
                break;
            case SuperPlayerConst.ORIENTATION_PORTRAIT:
                ((Activity) mContext).setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
                break;
        }
    }

    /**
     * 点播播放器回调
     *
     * @param player
     * @param event  事件id.id类型请参考 {@linkplain com.tencent.rtmp.TXLiveConstants#PLAY_EVT_CONNECT_SUCC 播放事件列表}.
     * @param param
     */
    @Override
    public void onPlayEvent(TXVodPlayer player, int event, Bundle param) {
        String playEventLog = "TXVodPlayer onPlayEvent event: " + event + ", " + param.getString(TXLiveConstants.EVT_DESCRIPTION);
        TXCLog.d(TAG, playEventLog);

        if (event == TXLiveConstants.PLAY_EVT_VOD_PLAY_PREPARED) { //视频播放开始
            mVodControllerSmall.updatePlayState(true);
            mVodControllerLarge.updatePlayState(true);

            mVodControllerSmall.updateReplay(false);
            mVodControllerLarge.updateReplay(false);

            ArrayList<TXBitrateItem> bitrateItems = mVodPlayer.getSupportedBitrates();

            if (bitrateItems == null || bitrateItems.size() == 0)
                return;
            Collections.sort(bitrateItems); //masterPlaylist多清晰度，按照码率排序，从低到高

            ArrayList<TCVideoQulity> videoQulities = new ArrayList<>();
            int size = bitrateItems.size();
            for (int i = 0; i < size; i++) {
                TXBitrateItem bitrateItem = bitrateItems.get(i);
                TCVideoQulity quality = SuperPlayerUtil.convertToVideoQuality(bitrateItem, i);
                videoQulities.add(quality);
            }

            if (!mDefaultSet) {
                TXBitrateItem defaultitem = bitrateItems.get(bitrateItems.size() - 1);
                mVodPlayer.setBitrateIndex(defaultitem.index); //默认播放码率最高的
                // 180x320 流畅, 360x640 标清, 720x1280 高清
                TXBitrateItem bitrateItem = bitrateItems.get(bitrateItems.size() - 1);
                TCVideoQulity defaultVideoQuality = SuperPlayerUtil.convertToVideoQuality(bitrateItem, bitrateItems.size() - 1);
                mVodControllerLarge.updateVideoQulity(defaultVideoQuality);
                mDefaultSet = true;
            }
            mVodControllerLarge.setVideoQualityList(videoQulities);
        } else if (event == TXLiveConstants.PLAY_EVT_PLAY_END) {
            mVodControllerSmall.updatePlayState(false);
            mVodControllerLarge.updatePlayState(false);

            mVodControllerSmall.updateReplay(true);
            mVodControllerLarge.updateReplay(true);
        }
        if (event < 0) {
            mVodPlayer.stopPlay(true);
            mVodControllerSmall.updatePlayState(false);
            mVodControllerLarge.updatePlayState(false);
            Toast.makeText(mContext, param.getString(TXLiveConstants.EVT_DESCRIPTION), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onNetStatus(TXVodPlayer player, Bundle status) {

    }

    public void requestPlayMode(int playMode) {
        if (playMode == SuperPlayerConst.PLAYMODE_WINDOW) {
            if (mVodController != null) {
                mVodController.onRequestPlayMode(SuperPlayerConst.PLAYMODE_WINDOW);
            }
        } else if (playMode == SuperPlayerConst.PLAYMODE_FLOAT) {
            if (mPlayerViewCallback != null) {
                mPlayerViewCallback.onQuit(SuperPlayerConst.PLAYMODE_WINDOW);
            }
            if (mVodController != null) {
                mVodController.onRequestPlayMode(SuperPlayerConst.PLAYMODE_FLOAT);
            }
        }
    }

    private final int OP_SYSTEM_ALERT_WINDOW = 24;

    /**
     * API <18，默认有悬浮窗权限，不需要处理。无法接收无法接收触摸和按键事件，不需要权限和无法接受触摸事件的源码分析
     * API >= 19 ，可以接收触摸和按键事件
     * API >=23，需要在manifest中申请权限，并在每次需要用到权限的时候检查是否已有该权限，因为用户随时可以取消掉。
     * API >25，TYPE_TOAST 已经被谷歌制裁了，会出现自动消失的情况
     */
    private boolean checkOp(Context context, int op) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
            AppOpsManager manager = (AppOpsManager) context.getSystemService(Context.APP_OPS_SERVICE);
            try {
                Method method = AppOpsManager.class.getDeclaredMethod("checkOp", int.class, int.class, String.class);
                return AppOpsManager.MODE_ALLOWED == (int) method.invoke(manager, op, Binder.getCallingUid(), context.getPackageName());
            } catch (Exception e) {
                Log.e(TAG, Log.getStackTraceString(e));
            }
        }
        return true;
    }

    public int getPlayMode() {
        return mPlayMode;
    }

    public int getPlayState() {
        return mCurrentPlayState;
    }

    /**
     * SuperVodPlayerActivity的回调接口
     */
    public interface PlayerViewCallback {

        void hideViews();

        void showViews();

        void onQuit(int playMode);
    }
}
