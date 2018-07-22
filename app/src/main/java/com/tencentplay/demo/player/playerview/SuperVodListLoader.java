package com.tencentplay.demo.player.playerview;

import android.os.Handler;
import android.os.HandlerThread;
import android.text.TextUtils;


import com.tencent.liteav.basic.log.TXCLog;
import com.tencentplay.demo.player.SuperPlayerModel;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Created by liyuejiao on 2018/7/3.
 * 获取点播信息
 */

public class SuperVodListLoader {

    private static final String TAG = "SuperVodListLoader";
    private static SuperVodListLoader sInstance;
    private Handler mHandler;
    private HandlerThread mHandlerThread;

    private boolean mIsHttps;
    private final String BASE_URL = "http://playvideo.qcloud.com/getplayinfo/v2";
    private final String BASE_URLS = "https://playvideo.qcloud.com/getplayinfo/v2";
    private OnVodInfoLoadListener mOnVodInfoLoadListener;
    private ArrayList<SuperPlayerModel> mDefaultList;

    public SuperVodListLoader() {
        mHandlerThread = new HandlerThread("SuperVodListLoader");
        mHandlerThread.start();
        mHandler = new Handler(mHandlerThread.getLooper());

        mDefaultList = new ArrayList();
    }

//    public static SuperVodListLoader getInstance() {
//        if (sInstance == null) {
//            sInstance = new SuperVodListLoader();
//        }
//        return sInstance;
//    }

    public void setOnVodInfoLoadListener(OnVodInfoLoadListener listener) {
        mOnVodInfoLoadListener = listener;
    }

    public ArrayList<SuperPlayerModel> loadDefaultVodList() {
        SuperPlayerModel model1 = new SuperPlayerModel();
        model1.appid = 1252463788;
        model1.fileid = "4564972819220421305";

        SuperPlayerModel model2 = new SuperPlayerModel();
        model2.appid = 1252463788;
        model2.fileid = "4564972819219071568";

        SuperPlayerModel model3 = new SuperPlayerModel();
        model3.appid = 1252463788;
        model3.fileid = "4564972819219071668";

        SuperPlayerModel model4 = new SuperPlayerModel();
        model4.appid = 1252463788;
        model4.fileid = "4564972819219071679";

        SuperPlayerModel model5 = new SuperPlayerModel();
        model5.appid = 1252463788;
        model5.fileid = "4564972819219081699";

        mDefaultList.clear();
        mDefaultList.add(model1);
        mDefaultList.add(model2);
        mDefaultList.add(model3);
        mDefaultList.add(model4);
        mDefaultList.add(model5);

        return mDefaultList;
    }

    public void getVodInfoOneByOne(ArrayList<SuperPlayerModel> superPlayerModels) {
        if (superPlayerModels == null || superPlayerModels.size() == 0)
            return;

        for (SuperPlayerModel model : superPlayerModels) {
            getVodByFileId(model);
        }
    }

    public void getVodByFileId(final SuperPlayerModel model) {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                String urlStr = makeUrlString(model.appid, model.fileid, null, null, -1, null);

                OkHttpClient okHttpClient = new OkHttpClient();
                okHttpClient.newBuilder().connectTimeout(5, TimeUnit.SECONDS);
                Request request = new Request.Builder().url(urlStr).build();
                Call call = okHttpClient.newCall(request);
                call.enqueue(new Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {
                        //获取请求信息失败
                        if (mOnVodInfoLoadListener != null) {
                            mOnVodInfoLoadListener.onFail(-1);
                        }
                    }

                    @Override
                    public void onResponse(Call call, Response response) throws IOException {
                        String content = response.body().string();
                        parseJson(model, content);
                    }
                });
            }
        });
    }

    private void parseJson(SuperPlayerModel superPlayerModel, String content) {
        if (TextUtils.isEmpty(content)) {
            TXCLog.e(TAG, "parseJson err, content is empty!");
            return;
        }
        try {
            JSONObject jsonObject = new JSONObject(content);
            int code = jsonObject.getInt("code");
            if (code != 0) {
                String message = jsonObject.getString("message");
                if (mOnVodInfoLoadListener != null) {
                    mOnVodInfoLoadListener.onFail(-1);
                }
                TXCLog.e(TAG, message);
                return;
            }
            TXPlayInfoResponse playInfoResponse = new TXPlayInfoResponse(jsonObject);
            superPlayerModel.placeholderImage = playInfoResponse.coverUrl();

            TXPlayInfoStream stream = playInfoResponse.getSource();
            if (stream != null) {
                superPlayerModel.duration = stream.getDuration();
            }
            superPlayerModel.title = playInfoResponse.description();
            if (superPlayerModel.title == null || superPlayerModel.title.length() == 0) {
                superPlayerModel.title = playInfoResponse.name();
            }
            if (mOnVodInfoLoadListener != null) {
                mOnVodInfoLoadListener.onSuccess(superPlayerModel);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private String makeUrlString(int appId, String fileId, String timeout, String us, int exper, String sign) {
        String urlStr;
        if (mIsHttps) {
            urlStr = String.format("%s/%d/%s", BASE_URL, appId, fileId);
        } else {
            urlStr = String.format("%s/%d/%s", BASE_URLS, appId, fileId);
        }
        String query = makeQueryString(timeout, us, exper, sign);
        if (query != null) {
            urlStr = urlStr + "?" + query;
        }
        return urlStr;
    }

    private String makeQueryString(String timeout, String us, int exper, String sign) {
        StringBuilder str = new StringBuilder();
        if (timeout != null) {
            str.append("t=" + timeout + "&");
        }
        if (us != null) {
            str.append("us=" + us + "&");
        }
        if (sign != null) {
            str.append("sign=" + sign + "&");
        }
        if (exper >= 0) {
            str.append("exper=" + exper + "&");
        }
        if (str.length() > 1) {
            str.deleteCharAt(str.length() - 1);
        }
        return str.toString();
    }

    public interface OnVodInfoLoadListener {
        void onSuccess(SuperPlayerModel superPlayerModel);

        void onFail(int errCode);
    }

}
