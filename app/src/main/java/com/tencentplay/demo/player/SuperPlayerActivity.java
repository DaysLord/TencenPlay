package com.tencentplay.demo.player;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.tencent.liteav.basic.log.TXCLog;
import com.tencent.rtmp.TXLiveConstants;
import com.tencentplay.demo.R;
import com.tencentplay.demo.player.playerview.GetVideoInfoListListener;
import com.tencentplay.demo.player.playerview.SuperPlayerConst;
import com.tencentplay.demo.player.playerview.SuperPlayerGlobalConfig;
import com.tencentplay.demo.player.playerview.SuperPlayerView;
import com.tencentplay.demo.player.playerview.SuperVodListLoader;
import com.tencentplay.demo.player.playerview.TCVodPlayerListAdapter;
import com.tencentplay.demo.player.playerview.VideoInfo;
import com.tencentplay.demo.player.server.VideoDataMgr;
import com.tencentplay.demo.player.utils.TCConstants;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by liyuejiao on 2018/7/3.
 * 超级播放器主Activity
 */

public class SuperPlayerActivity extends Activity implements View.OnClickListener,
        SuperVodListLoader.OnVodInfoLoadListener, SuperPlayerView.PlayerViewCallback,
        TCVodPlayerListAdapter.OnItemClickLitener, SwipeRefreshLayout.OnRefreshListener {

    private static final String TAG = "SuperPlayerActivity";
    private Context mContext;
    //标题
    private LinearLayout mLayoutTitle;
    private ImageView mIvBack;
    //扫码
//    private Button mBtnScan;
    private SwipeRefreshLayout mSwipeRefreshLayout;
    //超级播放器View
    private SuperPlayerView mSuperPlayerView;
    //播放列表
    private RecyclerView mVodPlayerListView;
    private TCVodPlayerListAdapter mVodPlayerListAdapter;

    //添加视频
//    private ImageView mIvAdd;
    //进入默认播放的视频
    private int DEFAULT_APPID = 1252463788;
    private String DEFAULT_FILEID = "4564972819220421305";
    //获取点播信息接口
    private SuperVodListLoader mSuperVodListLoader;

    //上传文件列表
    private boolean mDefaultVideo;
    private String mVideoId;
    private GetVideoInfoListListener mGetVideoInfoListListener;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_supervod_player);
        mContext = this;
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        checkPermission();
        initView();
        initData();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
    }

    private void checkPermission() {
        if (Build.VERSION.SDK_INT >= 23) {
            List<String> permissions = new ArrayList<>();
            if (PackageManager.PERMISSION_GRANTED != ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA)) {
                permissions.add(Manifest.permission.CAMERA);
            }
            if (PackageManager.PERMISSION_GRANTED != ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
            }
            if (permissions.size() != 0) {
                ActivityCompat.requestPermissions(this, permissions.toArray(new String[0]), 100);
            }
        }
    }

    private void initView() {
        mLayoutTitle = (LinearLayout) findViewById(R.id.layout_title);
        mIvBack = (ImageView) findViewById(R.id.iv_back);
        mIvBack.setOnClickListener(this);
//        mBtnScan = (Button) findViewById(R.id.btnScan);
//        mBtnScan.setOnClickListener(this);

        mSuperPlayerView = (SuperPlayerView) findViewById(R.id.superVodPlayerView);
        mSuperPlayerView.setPlayerViewCallback(this);

        mVodPlayerListView = (RecyclerView) findViewById(R.id.recycler_view);
        mVodPlayerListView.setLayoutManager(new LinearLayoutManager(this));
        mVodPlayerListAdapter = new TCVodPlayerListAdapter(this);
        mVodPlayerListAdapter.setOnItemClickLitener(this);
        mVodPlayerListView.setAdapter(mVodPlayerListAdapter);


        mSwipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.swipe_refresh_layout_list);
        mSwipeRefreshLayout.setOnRefreshListener(this);
//        mIvAdd = (ImageView) findViewById(R.id.iv_add);
//        mIvAdd.setOnClickListener(this);
    }

    private void initData() {
        mDefaultVideo = getIntent().getBooleanExtra(TCConstants.PLAYER_DEFAULT_VIDEO, true);
        mSuperVodListLoader = new SuperVodListLoader();
        mSuperVodListLoader.setOnVodInfoLoadListener(this);

        initSuperVodGlobalSetting();

        if (mDefaultVideo) {
            ArrayList<SuperPlayerModel> superPlayerModels = mSuperVodListLoader.loadDefaultVodList();
            mSuperVodListLoader.getVodInfoOneByOne(superPlayerModels);

            playDefaultVideo(DEFAULT_APPID, DEFAULT_FILEID);
        } else {
            mVideoId = getIntent().getStringExtra(TCConstants.PLAYER_VIDEO_ID);
            if (!TextUtils.isEmpty(mVideoId)) {
                playDefaultVideo(TCConstants.VOD_APPID, mVideoId);
            }

            mGetVideoInfoListListener = new GetVideoInfoListListener() {
                @Override
                public void onGetVideoInfoList(List<VideoInfo> videoInfoList) {
                    ArrayList<SuperPlayerModel> superPlayerModels = VideoDataMgr.getInstance().loadVideoInfoList(videoInfoList);
                    if (superPlayerModels != null && superPlayerModels.size() != 0) {
                        mSuperVodListLoader.getVodInfoOneByOne(superPlayerModels);
                    }
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mSwipeRefreshLayout.setRefreshing(false);
                        }
                    });
                }

                @Override
                public void onFail(int errCode) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(mContext, "获取已上传的视频列表失败", Toast.LENGTH_SHORT).show();
                            mSwipeRefreshLayout.setRefreshing(false);
                        }
                    });
                }
            };
            if (!mDefaultVideo) {
//                mBtnScan.setVisibility(View.GONE);
//                mIvAdd.setVisibility(View.GONE);
            }

            VideoDataMgr.getInstance().setGetVideoInfoListListener(mGetVideoInfoListListener);
            VideoDataMgr.getInstance().getVideoList();
        }
    }

    private void playDefaultVideo(int appid, String fileid) {
        SuperPlayerModel superPlayerModel = new SuperPlayerModel();
        superPlayerModel.appid = appid;
        superPlayerModel.fileid = fileid;
        mSuperPlayerView.playWithSuperPlayerMode(superPlayerModel);
    }

    /**
     * 初始化超级播放器全局配置
     */
    private void initSuperVodGlobalSetting() {
        SuperPlayerGlobalConfig prefs = SuperPlayerGlobalConfig.getInstance();
        // 开启悬浮窗播放
        prefs.enableFloatWindow = true;
        // 设置悬浮窗的初始位置和宽高
        SuperPlayerGlobalConfig.TXRect rect = new SuperPlayerGlobalConfig.TXRect();
        rect.x = 0;
        rect.y = 0;
        rect.width = 810;
        rect.height = 540;
        prefs.floatViewRect = rect;
        // 播放器默认缓存个数
        prefs.maxCacheItem = 5;
        // 设置播放器渲染模式
        prefs.enableHWAcceleration = true;
        prefs.renderMode = TXLiveConstants.RENDER_MODE_ADJUST_RESOLUTION;
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mSuperPlayerView.getPlayState() == SuperPlayerConst.PLAYSTATE_PLAY) {
            if (mSuperPlayerView != null) {
                mSuperPlayerView.onResume();
            }
        }
        if (mSuperPlayerView.getPlayMode() == SuperPlayerConst.PLAYMODE_FLOAT) {
            mSuperPlayerView.requestPlayMode(SuperPlayerConst.PLAYMODE_WINDOW);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mSuperPlayerView.getPlayMode() != SuperPlayerConst.PLAYMODE_FLOAT) {
            if (mSuperPlayerView != null) {
                mSuperPlayerView.onPause();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mSuperPlayerView.getPlayMode() != SuperPlayerConst.PLAYMODE_FLOAT) {
            mSuperPlayerView.onDestroy();
        }
        VideoDataMgr.getInstance().setGetVideoInfoListListener(null);
    }

    /**
     * 获取点播信息成功
     */
    @Override
    public void onSuccess(final SuperPlayerModel superPlayerModel) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mVodPlayerListAdapter.addSuperPlayerModel(superPlayerModel);
            }
        });
    }

    /**
     * 获取点播信息失败
     *
     * @param errCode
     */
    @Override
    public void onFail(int errCode) {
        TXCLog.i(TAG, "onFail errCode:" + errCode);
    }

    @Override
    public void onItemClick(int position, SuperPlayerModel superPlayerModel) {
        mSuperPlayerView.playWithSuperPlayerMode(superPlayerModel);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
//            case R.id.iv_add:   //[点击+添加一个点播列表项]
//                showAddVodDialog();
//                break;
//            case R.id.btnScan:  //[扫描二维码播放一个视频]
//                scanQRCode();
//                break;
            case R.id.iv_back:  //悬浮窗播放
                showFloatWindow();
                break;
        }
    }

    /**
     * 悬浮窗播放
     */
    private void showFloatWindow() {
        mSuperPlayerView.requestPlayMode(SuperPlayerConst.PLAYMODE_FLOAT);
    }

    /**
     * 扫描二维码
     */
    private void scanQRCode() {
//        Intent intent = new Intent(this, QRCodeScanActivity.class);
//        startActivityForResult(intent, 100);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (data == null || data.getExtras() == null || TextUtils.isEmpty(data.getExtras().getString("result"))) {
            return;
        }
        String result = data.getExtras().getString("result");
        if (requestCode == 200) {
//            EditText editText = (EditText) findViewById(R.id.editText);
//            editText.setText(result);
        } else if (requestCode == 100) {
            // 二维码播放视频
            playNewVideo(result);
        }
    }

    private void playNewVideo(String result) {
        SuperPlayerModel superPlayerModel = new SuperPlayerModel();
        superPlayerModel.videoURL = result;
        superPlayerModel.appid = DEFAULT_APPID;
        mSuperPlayerView.playWithSuperPlayerMode(superPlayerModel);
    }

    /**
     * 点击+添加一个点播列表项
     */
    private void showAddVodDialog() {
        AlertDialog.Builder dialog = new AlertDialog.Builder(this);
        final View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_new_vod_player_fileid, null);
        dialog.setTitle("请设置AppID和FileID");
        dialog.setView(dialogView);

        final EditText etAppId = (EditText) dialogView.findViewById(R.id.et_appid);
        final EditText etFileId = (EditText) dialogView.findViewById(R.id.et_fileid);

        dialog.setNegativeButton(R.string.btn_cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        dialog.setPositiveButton(R.string.btn_ok,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String appId = etAppId.getText().toString();
                        String fileId = etFileId.getText().toString();

                        if (TextUtils.isEmpty(appId)) {
                            Toast.makeText(mContext, "请输入正确的AppId", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        if (TextUtils.isEmpty(fileId)) {
                            Toast.makeText(mContext, "请输入正确的FileId", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        int appid;
                        try {
                            appid = Integer.parseInt(appId);
                        } catch (NumberFormatException e) {
                            Toast.makeText(mContext, "请输入正确的AppId", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        SuperPlayerModel superPlayerModel = new SuperPlayerModel();
                        superPlayerModel.appid = appid;
                        superPlayerModel.fileid = fileId;

                        // 尝试请求fileid信息
                        SuperVodListLoader loader = new SuperVodListLoader();
                        loader.setOnVodInfoLoadListener(new SuperVodListLoader.OnVodInfoLoadListener() {
                            @Override
                            public void onSuccess(final SuperPlayerModel superPlayerModel) {
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        mVodPlayerListAdapter.addSuperPlayerModel(superPlayerModel);
                                    }
                                });
                            }

                            @Override
                            public void onFail(int errCode) {
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        Toast.makeText(mContext, "fileid请求失败", Toast.LENGTH_SHORT).show();
                                    }
                                });
                            }
                        });
                        loader.getVodByFileId(superPlayerModel);
                    }
                });
        dialog.show();
    }

    @Override
    public void hideViews() {
        mLayoutTitle.setVisibility(View.GONE);
    }

    @Override
    public void showViews() {
        mLayoutTitle.setVisibility(View.VISIBLE);
    }

    @Override
    public void onQuit(int playMode) {
        if (playMode == SuperPlayerConst.PLAYMODE_FLOAT) {
            if (mSuperPlayerView != null) {
                mSuperPlayerView.onDestroy();
            }
            finish();
        } else if (playMode == SuperPlayerConst.PLAYMODE_WINDOW) {
            // 返回桌面
            Intent intent = new Intent(Intent.ACTION_MAIN);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.addCategory(Intent.CATEGORY_HOME);
            startActivity(intent);
        }
    }

    @Override
    public void onRefresh() {
        if (mDefaultVideo) {
            mSwipeRefreshLayout.setRefreshing(false);
            return;
        }
        VideoDataMgr.getInstance().getVideoList();
    }

}
