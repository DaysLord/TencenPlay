package com.tencentplay.demo.player.playerview;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import com.tencent.rtmp.TXLog;
import com.tencentplay.demo.R;

/**
 * Created by liyuejiao on 2018/7/3.
 * <p>
 * 超级播放器全屏控制界面
 */
public class TCVodControllerLarge extends TCVodControllerBase
        implements View.OnClickListener, SeekBar.OnSeekBarChangeListener, TCVodMoreView.Callback, TCVodQualityView.Callback {
    private static final String TAG = "TCVodControllerLarge";
    private RelativeLayout mLayoutTop;
    private LinearLayout mLayoutBottom;
    private Context mContext;
    private ImageView mIvBack;
    private ImageView mIvPause;
    private TextView mTvCurrent;
    private TextView mTvDuration;
    private SeekBar mSeekBarProgress;
    private TextView mTvQuality;
    private TextView mTvTitle;
    private ImageView mIvDanmuku;
    private ImageView mIvSnapshot;
    private ImageView mIvLock;
    private ImageView mIvMore;
    private TCVodQualityView mVodQulityView;
    private TCVodMoreView mVodMoreView;
    private boolean mDanmukuOn;
    private boolean mFirstShow;
    private LinearLayout mLayoutReplay;

    public TCVodControllerLarge(@NonNull Context context) {
        super(context);
        initViews(context);
    }

    public TCVodControllerLarge(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        initViews(context);
    }

    public TCVodControllerLarge(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initViews(context);
    }

    @Override
    void onShow() {
        mLayoutTop.setVisibility(View.VISIBLE);
        mLayoutBottom.setVisibility(View.VISIBLE);
    }

    @Override
    void onHide() {
        mLayoutTop.setVisibility(View.GONE);
        mLayoutBottom.setVisibility(View.GONE);
        mVodMoreView.setVisibility(View.GONE);
        mVodQulityView.setVisibility(View.GONE);
    }

    @Override
    void onTimerTicker() {
        float curTime = mVodController.getCurrentPlaybackTime();
        float durTime = mVodController.getDuration();

        if (durTime > 0 && curTime <= durTime) {
            float percentage = curTime / durTime;
            updateVideoProgress(percentage);
        }
    }

    private void initViews(Context context) {
        mContext = context;
        mLayoutInflater.inflate(R.layout.vod_controller_large, this);

        mLayoutTop = (RelativeLayout) findViewById(R.id.layout_top);
        mLayoutBottom = (LinearLayout) findViewById(R.id.layout_bottom);
        mLayoutReplay = (LinearLayout) findViewById(R.id.layout_replay);
        mIvBack = (ImageView) findViewById(R.id.iv_back);
        mIvLock = (ImageView) findViewById(R.id.iv_lock);
        mTvTitle = (TextView) findViewById(R.id.tv_title);
        mIvPause = (ImageView) findViewById(R.id.iv_pause);
        mIvDanmuku = (ImageView) findViewById(R.id.iv_danmuku);
        mIvMore = (ImageView) findViewById(R.id.iv_more);
        mIvSnapshot = (ImageView) findViewById(R.id.iv_snapshot);
        mTvCurrent = (TextView) findViewById(R.id.tv_current);
        mTvDuration = (TextView) findViewById(R.id.tv_duration);
        mSeekBarProgress = (SeekBar) findViewById(R.id.seekbar_progress);
        mSeekBarProgress.setProgress(0);
        mSeekBarProgress.setMax(100);
        mTvQuality = (TextView) findViewById(R.id.tv_quality);

        mVodQulityView = (TCVodQualityView) findViewById(R.id.vodQualityView);
        mVodQulityView.setCallback(this);
        mVodMoreView = (TCVodMoreView) findViewById(R.id.vodMoreView);
        mVodMoreView.setCallback(this);

        mLayoutReplay.setOnClickListener(this);
        mIvLock.setOnClickListener(this);
        mIvBack.setOnClickListener(this);
        mIvPause.setOnClickListener(this);
        mIvDanmuku.setOnClickListener(this);
        mIvSnapshot.setOnClickListener(this);
        mIvMore.setOnClickListener(this);
        mTvQuality.setOnClickListener(this);
        mSeekBarProgress.setOnSeekBarChangeListener(this);

        if (mDefaultVideoQuality != null) {
            mTvQuality.setText(mDefaultVideoQuality.title);
        }
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.iv_back:       // 返回
            case R.id.tv_title:
                mVodController.onBackPress(SuperPlayerConst.PLAYMODE_FULLSCREEN);
                break;
            case R.id.iv_pause:      // 暂停/播放
                changePlayState();
                break;
            case R.id.iv_danmuku:    // 弹幕
                toggleDanmu();
                break;
            case R.id.iv_snapshot:   // 截屏
                mVodController.onSnapshot();
                break;
            case R.id.iv_more:       // 显示更多
                showMoreView();
                break;
            case R.id.tv_quality:    // 清晰度选择
                showQualityView();
                break;
            case R.id.iv_lock:
                changeLockState();
                break;
            case R.id.layout_replay:
                replay();
                break;
        }
    }

    private void replay() {
        updateReplay(false);
        mVodController.onReplay();
    }

    private void changeLockState() {
        mLockScreen = !mLockScreen;
        if (mLockScreen) {
            mIvLock.setImageResource(R.mipmap.ic_player_lock);
            hide();
        } else {
            mIvLock.setImageResource(R.mipmap.ic_player_unlock);
        }
    }

    /**
     * 打开/关闭 弹幕
     */
    private void toggleDanmu() {
        mDanmukuOn = !mDanmukuOn;
        if (mDanmukuOn) {
            mIvDanmuku.setImageResource(R.mipmap.ic_danmuku_on);
        } else {
            mIvDanmuku.setImageResource(R.mipmap.ic_danmuku_off);
        }
        mVodController.onDanmuku(mDanmukuOn);
    }

    /**
     * 切换播放状态
     */
    private void changePlayState() {
        // 播放中
        if (mVodController.isPlaying()) {
            mVodController.pause();
            show();
        }
        // 未播放
        else if (!mVodController.isPlaying()) {
            updateReplay(false);
            mVodController.resume();
            show();
        }
    }

    private void showMoreView() {
        mVodMoreView.setVisibility(View.VISIBLE);
    }

    /**
     * 显示多分辨率UI
     */
    private void showQualityView() {
        if (mVideoQualityList == null || mVideoQualityList.size() == 0) {
            TXLog.i(TAG, "showQualityView mVideoQualityList null");
            return;
        }
        // 设置默认显示分辨率文字
        mVodQulityView.setVisibility(View.VISIBLE);
        if (!mFirstShow) {
            mVodQulityView.setDefaultSelectedQuality(mVideoQualityList.size() - 1);
            mFirstShow = true;
        }
        mVodQulityView.setVideoQualityList(mVideoQualityList);
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int i, boolean b) {

    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        // 拖动seekbar结束时,获取seekbar当前进度,进行seek操作,最后更新seekbar进度
        int curProgress = seekBar.getProgress();
        int maxProgress = seekBar.getMax();

        if (curProgress >= 0 && curProgress < maxProgress) {
            // 关闭重播按钮
            updateReplay(false);
            float percentage = ((float) curProgress) / maxProgress;
            int position = (int) (mVodController.getDuration() * percentage);
            mVodController.seekTo(position);
            mVodController.resume();
        }
    }

    public void updateVideoQulity(TCVideoQulity videoQulity) {
        mDefaultVideoQuality = videoQulity;
        if (mTvQuality != null) {
            mTvQuality.setText(videoQulity.title);
        }
    }

    /**
     * 更新播放进度
     *
     * @param percentage
     */
    public void updateVideoProgress(float percentage) {
        if (percentage >= 0 && percentage <= 1) {
            int progress = Math.round(percentage * mSeekBarProgress.getMax());
            mSeekBarProgress.setProgress(progress);

            int curTime = (int) mVodController.getCurrentPlaybackTime();
            int durTime = (int) mVodController.getDuration();
            if (durTime >= 0 && curTime <= durTime) {
                mTvCurrent.setText(TCUtils.formattedTime(curTime));
                mTvDuration.setText(TCUtils.formattedTime(durTime));
            }
        }
    }

    /**
     * 更新播放UI
     *
     * @param isStart
     */
    public void updatePlayState(boolean isStart) {
        // 播放中
        if (isStart) {
            mIvPause.setImageResource(R.mipmap.ic_vod_pause_normal);
        }
        // 未播放
        else {
            mIvPause.setImageResource(R.mipmap.ic_vod_play_normal);
        }
    }

    public void updateTitle(String title) {
        if (!TextUtils.isEmpty(title)) {
            mTvTitle.setText(title);
        }
    }

    public void updateReplay(boolean replay) {
        if (replay) {
            mLayoutReplay.setVisibility(View.VISIBLE);
        } else {
            mLayoutReplay.setVisibility(View.GONE);
        }
    }

    @Override
    public void onQualitySelect(TCVideoQulity quality) {
        mVodController.onQualitySelect(quality);
    }

    @Override
    public void onVolumeChange(int volume) {

    }

    @Override
    public void onLightChange(int light) {

    }

    @Override
    public void onSpeedChange(float speedLevel) {
        mVodController.onSpeedChange(speedLevel);
    }

    @Override
    public void onMirrorChange(boolean isMirror) {
        mVodController.onMirrorChange(isMirror);
    }

}
