package com.tencentplay.demo.player.playerview;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by annidy on 2017/12/13.
 */

public class TXPlayInfoResponse {
    protected JSONObject response;

    public TXPlayInfoResponse(JSONObject response) {
        this.response = response;
    }

    /**
     * 获取服务器下发的播放地址
     *
     * @return 播放地址
     */
    public String playUrl() {
        if (getMasterPlayList() != null) {
            return getMasterPlayList().url;
        }
        if (getStreamList().size() != 0) {
            return getStreamList().get(0).url;
        }
        if (getSource() != null) {
            return getSource().url;
        }
        return null;
    }


    /**
     * 获取封面图片
     *
     * @return 图片url
     */
    public String coverUrl() {
        try {
            JSONObject coverInfo = response.getJSONObject("coverInfo");
            if (coverInfo != null) {
                return coverInfo.getString("coverUrl");
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }

    public List<TXPlayInfoStream> getStreamList() {
        ArrayList<TXPlayInfoStream> streamList = new ArrayList<>();
        try {
            JSONObject videoInfo = response.getJSONObject("videoInfo");
            if (!videoInfo.has("transcodeList"))
                return streamList;
            JSONArray transcodeList = response.getJSONObject("videoInfo").getJSONArray("transcodeList");
            if (transcodeList != null) {
                for (int i = 0; i < transcodeList.length(); i++) {
                    JSONObject transcode = transcodeList.getJSONObject(i);

                    TXPlayInfoStream stream = new TXPlayInfoStream();
                    stream.url = transcode.getString("url");
                    stream.duration = transcode.getInt("duration");
                    stream.width = transcode.getInt("width");
                    stream.height = transcode.getInt("height");
                    stream.size = transcode.getInt("size");
                    stream.bitrate = transcode.getInt("bitrate");
                    stream.definition = transcode.getInt("definition");

                    streamList.add(stream);
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return streamList;
    }

    public TXPlayInfoStream getSource() {
        try {
            JSONObject sourceVideo = response.getJSONObject("videoInfo").getJSONObject("sourceVideo");

            TXPlayInfoStream stream = new TXPlayInfoStream();
            stream.url = sourceVideo.getString("url");
            stream.duration = sourceVideo.getInt("duration");
            stream.width = sourceVideo.getInt("width");
            stream.height = sourceVideo.getInt("height");
            stream.size = sourceVideo.getInt("size");
            stream.bitrate = sourceVideo.getInt("bitrate");

            return stream;
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }

    public TXPlayInfoStream getMasterPlayList() {
        try {
            JSONObject videoInfo = response.getJSONObject("videoInfo");
            if (!videoInfo.has("masterPlayList"))
                return null;

            JSONObject masterPlayList = response.getJSONObject("videoInfo").getJSONObject("masterPlayList");

            TXPlayInfoStream stream = new TXPlayInfoStream();
            stream.url = masterPlayList.getString("url");

            return stream;
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 获取视频名称
     *
     * @return
     */
    public String name() {
        try {
            JSONObject basicInfo = response.getJSONObject("videoInfo").getJSONObject("basicInfo");
            if (basicInfo != null) {
                return basicInfo.getString("name");
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 获取视频描述
     *
     * @return
     */
    public String description() {
        try {
            JSONObject basicInfo = response.getJSONObject("videoInfo").getJSONObject("basicInfo");
            if (basicInfo != null) {
                return basicInfo.getString("description");
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 获取默认播放清晰度
     *
     * @return
     */
    public String getDefaultVideoClassification() {
        try {
            return response.getJSONObject("playerInfo").getString("defaultVideoClassification");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 获取transcode类型视频清晰度匹配列表
     *
     * @return
     */
    public List<TXVideoClassification> getVideoClassificationList() {
        ArrayList<TXVideoClassification> arrayList = new ArrayList<>();
        try {
            JSONArray videoClassificationArray = response.getJSONObject("playerInfo").getJSONArray("videoClassification");
            if (videoClassificationArray != null) {
                for (int i = 0; i < videoClassificationArray.length(); i++) {
                    JSONObject object = videoClassificationArray.getJSONObject(i);

                    TXVideoClassification classification = new TXVideoClassification();
                    classification.setId(object.getString("id"));
                    classification.setName(object.getString("name"));

                    ArrayList definitionList = new ArrayList();
                    JSONArray array = object.getJSONArray("definitionList");
                    if (array != null) {
                        for (int j = 0; j < array.length(); j++) {
                            int definiaton = array.getInt(j);
                            definitionList.add(definiaton);
                        }
                    }
                    classification.setDefinitionList(definitionList);
                    arrayList.add(classification);
                }
            }
            return arrayList;
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }

    public HashMap<String, TXPlayInfoStream> getTranscodePlayList() {
        List<TXVideoClassification> classificationList = getVideoClassificationList();
        List<TXPlayInfoStream> transcodeList = getStreamList();
        if (transcodeList != null) {
            for (int i = 0; i < transcodeList.size(); i++) {
                TXPlayInfoStream stream = transcodeList.get(i);

                // 寻找匹配的清晰度
                if (classificationList != null) {
                    for (int j = 0; j < classificationList.size(); j++) {
                        TXVideoClassification classification = classificationList.get(j);
                        ArrayList<Integer> definitionList = classification.getDefinitionList();
                        if (definitionList.contains(stream.definition)) {
                            stream.id = classification.getId();
                            stream.name = classification.getName();
                        }
                    }
                }
            }
        }
        //清晰度去重
        HashMap<String, TXPlayInfoStream> idList = new HashMap<>();
        for (int i = 0; i < transcodeList.size(); i++) {
            TXPlayInfoStream stream = transcodeList.get(i);
            if (!idList.containsKey(stream.id)) {
                idList.put(stream.id, stream);
            } else {
                TXPlayInfoStream copy = idList.get(stream.id);
                if (copy.getUrl().endsWith("mp4")) {  // 列表中url是mp4，则进行下一步
                    continue;
                }
                if (stream.getUrl().endsWith("mp4")) { // 新判断的url是mp4，则替换列表中
                    idList.remove(copy);
                    idList.put(stream.id, stream);
                }
            }
        }
        //按清晰度排序
        return idList;
    }
}
