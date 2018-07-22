package com.tencentplay.demo.player.playerview;

import android.content.Context;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.widget.RelativeLayout;

import com.tencent.liteav.basic.log.TXCLog;

import java.lang.ref.WeakReference;
import java.util.ArrayList;

/**
 * Created by liyuejiao on 2018/7/3.
 */

public abstract class TCVodControllerBase extends RelativeLayout {
    private static final String TAG = "TCVodControllerBase";
    protected LayoutInflater mLayoutInflater;
    protected VodController mVodController;
    protected GestureDetector mGestureDetector;
    private boolean isShowing;
    protected boolean mLockScreen;
    private static final double RADIUS_SLOP = Math.PI * 1 / 4;
    protected TCVideoQulity mDefaultVideoQuality;
    protected ArrayList<TCVideoQulity> mVideoQualityList;
    private TimeTickHandler mHandler;

    public TCVodControllerBase(@NonNull Context context) {
        super(context);
        init();
    }

    public TCVodControllerBase(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public TCVodControllerBase(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        mLayoutInflater = LayoutInflater.from(getContext());
        mHandler = new TimeTickHandler(this);
        mGestureDetector = new GestureDetector(getContext(), new GestureDetector.OnGestureListener() {

            @Override
            public boolean onSingleTapUp(MotionEvent e) {
//                if (mCurrentGesture == GESTURE_NONE) {
                toggle();
//                }
                return false;
            }

            @Override
            public void onShowPress(MotionEvent e) {

            }

            @Override
            public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
                if (e1 == null || e2 == null || mLockScreen) {
                    return false;
                }

                float oldX = e1.getX();
                final double distance = Math.sqrt(Math.pow(distanceX, 2) + Math.pow(distanceY, 2));
                int selfWidth = getMeasuredWidth();
                final double radius = distanceY / distance;

                // 当角度值大于设置值时,当做垂直方向处理,反之当做水平方向处理
                if (Math.abs(radius) > RADIUS_SLOP) {
                    //  右半部分处理声音的逻辑
//                    if (oldX > selfWidth / 2) {
//                        if (mCurrentGesture == GESTURE_NONE || mCurrentGesture == GESTURE_VOLUME) {
//                            mCurrentGesture = GESTURE_VOLUME;
//                            if (mWidgetLightView != null) mWidgetLightView.hide(true);
//                            if (mWidgetSeekView != null) mWidgetSeekView.hide(true);
//                            IAudioManager audioManager = AudioManagerFactory.createAudioManager(mMediaPlayerController.getMediaPlayer(), getContext());
//                            TXCLog.i(TAG, "onGestureVolumeChange");
//                            float totalVolumeDistance = getMeasuredHeight();
//                            if (totalVolumeDistance <= 0)
//                                totalVolumeDistance = KankanUIUtils.getRealDisplayHeight(mHostWindow);
//                            if (mWidgetVolumeView != null)
//                                mWidgetVolumeView.onGestureVolumeChange(distanceY, totalVolumeDistance / 4, audioManager);
//                        }
//                    }
                    //  左半部分处理亮度的逻辑
//                    else {
//                        if (mCurrentGesture == GESTURE_NONE || mCurrentGesture == GESTURE_LIGHT) {
//                            mCurrentGesture = GESTURE_LIGHT;
//                            if (mWidgetVolumeView != null) mWidgetVolumeView.hide(true);
//                            if (mWidgetSeekView != null) mWidgetSeekView.hide(true);
//                            TXCLog.d(TAG, "onGestureLightChange");
//                            float totalLightDistance = getMeasuredHeight();
//                            if (totalLightDistance <= 0)
//                                totalLightDistance = KankanUIUtils.getRealDisplayHeight(mHostWindow);
//                            if (mWidgetLightView != null)
//                                mWidgetLightView.onGestureLightChange(distanceY, totalLightDistance / 4, mHostWindow);
//                        }
//                    }
                }
                // 处理视频进度
                else {
                    show();

//                    if (mCurrentGesture == GESTURE_NONE || mCurrentGesture == GESTURE_SEEK) {
//                        mCurrentGesture = GESTURE_SEEK;
//                        show();
//                        if (mWidgetVolumeView != null) mWidgetVolumeView.hide(true);
//                        if (mWidgetLightView != null) mWidgetLightView.hide(true);
//                        TXCLog.d(TAG, "onGestureSeekChange");
//                        float totalSeekDistance = getMeasuredWidth();
//                        if (totalSeekDistance <= 0)
//                            totalSeekDistance = KankanUIUtils.getRealDisplayWidth(mHostWindow);
//                        if (mWidgetSeekView != null)
//                            mWidgetSeekView.onGestureSeekChange(-distanceX, totalSeekDistance);
//                    }
                }

                return false;
            }

            @Override
            public void onLongPress(MotionEvent e) {
            }

            @Override
            public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                return false;
            }

            @Override
            public boolean onDown(MotionEvent e) {
                return false;
            }
        });
    }

    public void setVideoQualityList(ArrayList<TCVideoQulity> videoQualityList) {
        mVideoQualityList = videoQualityList;
    }

    public void setVodController(VodController vodController) {
        mVodController = vodController;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int action = event.getAction();
        if (action == MotionEvent.ACTION_DOWN) {
            if (!mLockScreen)
                toggle();
        }
//        if (mGestureDetector != null)
//            mGestureDetector.onTouchEvent(event);
        return true;
    }

    public void toggle() {
        if (isShowing) {
            hide();
        } else {
            show();
        }
    }

    public void show() {
        isShowing = true;
        mHandler.removeMessages(MSG_HIDE);
        mHandler.sendEmptyMessage(MSG_SHOW);
    }

    public void hide() {
        isShowing = false;
        mHandler.removeMessages(MSG_SHOW);
        mHandler.sendEmptyMessage(MSG_HIDE);
    }

    protected static final int MSG_SHOW = 101;
    protected static final int MSG_HIDE = 102;
    protected static final int MSG_TIMER_TICKER = 103;
    protected static final int TIMER_TICKER_INTERVAL_DEFAULT = 1000;
    private boolean mIsTimerTickerStarted;

    protected static class TimeTickHandler extends Handler {
        private WeakReference<TCVodControllerBase> vodController;

        public TimeTickHandler(TCVodControllerBase vodControllerBase) {
            vodController = new WeakReference<TCVodControllerBase>(vodControllerBase);
        }

        public void handleMessage(android.os.Message msg) {
            TCVodControllerBase controllerBase = vodController.get();
            if (controllerBase != null) {
                switch (msg.what) {
                    case MSG_SHOW:
                        controllerBase.startTimerTicker();
                        controllerBase.onShow();
                        break;
                    case MSG_HIDE:
                        controllerBase.stopTimerTicker();
                        controllerBase.onHide();
                        break;
                    case MSG_TIMER_TICKER:
                        controllerBase.onTimerTicker();
                        sendEmptyMessageDelayed(MSG_TIMER_TICKER, TIMER_TICKER_INTERVAL_DEFAULT);
                        break;
                    default:
                        break;
                }
            }
        }
    }

    private void startTimerTicker() {
        if (mIsTimerTickerStarted)
            return;
        mIsTimerTickerStarted = true;

        TXCLog.i(TAG, "startTimerTicker");
        mHandler.removeMessages(MSG_TIMER_TICKER);
        mHandler.sendEmptyMessage(MSG_TIMER_TICKER);
    }

    private void stopTimerTicker() {
        if (!mIsTimerTickerStarted)
            return;
        mIsTimerTickerStarted = false;

        TXCLog.i(TAG, "stopTimerTicker");
        mHandler.removeMessages(MSG_TIMER_TICKER);
    }

    abstract void onShow();

    abstract void onHide();

    abstract void onTimerTicker();

    public interface VodController {

        void onRequestPlayMode(int requestPlayMode);

        void onBackPress(int playMode);

        void onControllerShow(int playMode);

        void onControllerHide(int playMode);

        void onRequestLockMode(boolean lockMode);

        void resume();

        void pause();

        float getDuration();

        float getCurrentPlaybackTime();

        void seekTo(int position);

        boolean isPlaying();

        void onDanmuku(boolean on);

        void onSnapshot();

        void onQualitySelect(TCVideoQulity quality);

        void onSpeedChange(float speedLevel);

        void onMirrorChange(boolean isMirror);

        void onFloatUpdate(int x, int y);

        void onReplay();
    }

}
