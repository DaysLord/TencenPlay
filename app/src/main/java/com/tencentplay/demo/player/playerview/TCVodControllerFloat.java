package com.tencentplay.demo.player.playerview;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;

import com.tencent.rtmp.ui.TXCloudVideoView;
import com.tencentplay.demo.R;

/**
 * Created by liyuejiao on 2018/7/3.
 * <p>
 * 超级播放器悬浮窗控制界面
 */
public class TCVodControllerFloat extends TCVodControllerBase implements View.OnClickListener {
    private ImageView mIvClose;
    private ImageView mIvExpand;
    private float lastX;
    private float lastY;
    private float mTouchStartX;
    private float mTouchStartY;
    private SuperPlayerGlobalConfig.TXRect mRect;
    private int startPositionX;
    private int startPositionY;
    private TXCloudVideoView mFloatVideoView;

    public TCVodControllerFloat(@NonNull Context context) {
        super(context);
        initViews();
    }

    public TCVodControllerFloat(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        initViews();
    }

    public TCVodControllerFloat(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initViews();
    }

    private void initViews() {
        mLayoutInflater.inflate(R.layout.vod_controller_float, this);
        mFloatVideoView = (TXCloudVideoView) findViewById(R.id.float_cloud_video_view);
        mIvClose = (ImageView) findViewById(R.id.iv_close);
        mIvExpand = (ImageView) findViewById(R.id.iv_expand);

        mIvClose.setOnClickListener(this);
        mIvExpand.setOnClickListener(this);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        lastX = event.getRawX();
        lastY = event.getRawY();

        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                mTouchStartX = event.getX();
                mTouchStartY = event.getY();
                //记录悬浮窗原始位置
                SuperPlayerGlobalConfig prefs = SuperPlayerGlobalConfig.getInstance();
                mRect = prefs.floatViewRect;
                startPositionX = mRect.x;
                startPositionY = mRect.y;
                break;
            case MotionEvent.ACTION_MOVE:
                //计算新的位置
                int x = (int) (lastX - mTouchStartX);
                int y = (int) (lastY - mTouchStartY);

                mVodController.onFloatUpdate(x, y);
                break;
            case MotionEvent.ACTION_UP:

                break;
        }

        return super.onTouchEvent(event);
    }

    @Override
    void onShow() {

    }

    @Override
    void onHide() {

    }

    @Override
    void onTimerTicker() {

    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.iv_close:
                if (mVodController != null) {
                    mVodController.onBackPress(SuperPlayerConst.PLAYMODE_FLOAT);
                }
                break;
            case R.id.iv_expand:
                if (mVodController != null) {
                    mVodController.onRequestPlayMode(SuperPlayerConst.PLAYMODE_WINDOW);
                }
                break;
        }
    }

    public TXCloudVideoView getFloatVideoView() {
        return mFloatVideoView;
    }
}
