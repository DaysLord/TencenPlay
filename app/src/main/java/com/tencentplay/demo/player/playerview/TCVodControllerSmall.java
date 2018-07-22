package com.tencentplay.demo.player.playerview;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import com.tencentplay.demo.R;


/**
 * Created by liyuejiao on 2018/7/3.
 * <p>
 * 超级播放器小窗口控制界面
 */
public class TCVodControllerSmall extends TCVodControllerBase implements View.OnClickListener, SeekBar.OnSeekBarChangeListener {
    private LinearLayout mLayoutTop;
    private LinearLayout mLayoutBottom;
    private ImageView mIvPause;
    private ImageView mIvFullScreen;
    private TextView mTvCurrent;
    private TextView mTvDuration;
    private SeekBar mSeekBarProgress;
    private TextView mTvTitle;
    private LinearLayout mLayoutReplay;

    public TCVodControllerSmall(@NonNull Context context) {
        super(context);
        initViews();
    }

    public TCVodControllerSmall(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        initViews();
    }

    public TCVodControllerSmall(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initViews();
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

    private void initViews() {
        mLayoutInflater.inflate(R.layout.vod_controller_small, this);

        mLayoutTop = (LinearLayout) findViewById(R.id.layout_top);
        mLayoutBottom = (LinearLayout) findViewById(R.id.layout_bottom);
        mLayoutReplay = (LinearLayout) findViewById(R.id.layout_replay);
        mTvTitle = (TextView) findViewById(R.id.tv_title);
        mIvPause = (ImageView) findViewById(R.id.iv_pause);
        mTvCurrent = (TextView) findViewById(R.id.tv_current);
        mTvDuration = (TextView) findViewById(R.id.tv_duration);
        mSeekBarProgress = (SeekBar) findViewById(R.id.seekbar_progress);
        mSeekBarProgress.setProgress(0);
        mSeekBarProgress.setMax(100);
        mIvFullScreen = (ImageView) findViewById(R.id.iv_fullscreen);

        mIvPause.setOnClickListener(this);
        mIvFullScreen.setOnClickListener(this);
        mLayoutTop.setOnClickListener(this);
        mLayoutReplay.setOnClickListener(this);

        mSeekBarProgress.setOnSeekBarChangeListener(this);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.layout_top:
                onBack();
                break;
            case R.id.iv_pause:      // 暂停/播放
                changePlayState();
                break;
            case R.id.iv_fullscreen:
                fullScreen();
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

    private void onBack() {
        mVodController.onBackPress(SuperPlayerConst.PLAYMODE_WINDOW);
    }

    /**
     * 全屏
     */
    private void fullScreen() {
        mVodController.onRequestPlayMode(SuperPlayerConst.PLAYMODE_FULLSCREEN);
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
            if (durTime > 0 && curTime <= durTime) {
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
}
