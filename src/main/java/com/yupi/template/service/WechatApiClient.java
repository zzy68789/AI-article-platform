package com.yupi.template.service;

import cn.hutool.core.util.StrUtil;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.yupi.template.config.WechatConfig;
import com.yupi.template.exception.BusinessException;
import com.yupi.template.exception.ErrorCode;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class WechatApiClient {

    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    private static final MediaType OCTET_STREAM = MediaType.parse("application/octet-stream");

    @Resource
    private WechatConfig wechatConfig;

    private OkHttpClient httpClient;

    private final Gson gson = new Gson();

    private String cachedAccessToken;

    private long tokenExpireAtMillis;

    @PostConstruct
    public void init() {
        httpClient = new OkHttpClient.Builder()
                .connectTimeout(wechatConfig.getConnectTimeoutMs(), TimeUnit.MILLISECONDS)
                .readTimeout(wechatConfig.getReadTimeoutMs(), TimeUnit.MILLISECONDS)
                .writeTimeout(wechatConfig.getReadTimeoutMs(), TimeUnit.MILLISECONDS)
                .build();
    }

    public synchronized String getAccessToken() {
        validateConfig();
        long now = System.currentTimeMillis();
        if (StrUtil.isNotBlank(cachedAccessToken) && now < tokenExpireAtMillis) {
            return cachedAccessToken;
        }

        String url = wechatConfig.getBaseUrl()
                + "/token?grant_type=client_credential&appid="
                + encode(wechatConfig.getAppId())
                + "&secret="
                + encode(wechatConfig.getAppSecret());
        JsonObject json = getJson(url);
        ensureWechatSuccess(json, "获取 access_token 失败");
        if (!json.has("access_token")) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "获取 access_token 失败：未返回 access_token");
        }

        cachedAccessToken = json.get("access_token").getAsString();
        int expiresIn = json.has("expires_in") ? json.get("expires_in").getAsInt() : 7200;
        tokenExpireAtMillis = now + Math.max(60, expiresIn - 300) * 1000L;
        return cachedAccessToken;
    }

    public String uploadCoverImage(String imageUrl) {
        ImageBytes imageBytes = downloadImage(imageUrl);
        String url = wechatConfig.getBaseUrl() + "/material/add_material?access_token="
                + encode(getAccessToken()) + "&type=image";
        JsonObject json = uploadImageMultipart(url, imageBytes);
        ensureWechatSuccess(json, "上传微信封面图失败");
        if (!json.has("media_id")) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "上传微信封面图失败：未返回 media_id");
        }
        return json.get("media_id").getAsString();
    }

    public String uploadContentImage(String imageUrl) {
        ImageBytes imageBytes = downloadImage(imageUrl);
        String url = wechatConfig.getBaseUrl() + "/media/uploadimg?access_token=" + encode(getAccessToken());
        JsonObject json = uploadImageMultipart(url, imageBytes);
        ensureWechatSuccess(json, "上传微信正文图片失败");
        if (!json.has("url")) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "上传微信正文图片失败：未返回 url");
        }
        return json.get("url").getAsString();
    }

    public String addDraft(String title, String author, String digest, String contentHtml, String thumbMediaId) {
        JsonObject article = new JsonObject();
        article.addProperty("title", title);
        if (StrUtil.isNotBlank(author)) {
            article.addProperty("author", author);
        }
        article.addProperty("digest", digest);
        article.addProperty("content", contentHtml);
        article.addProperty("thumb_media_id", thumbMediaId);
        article.addProperty("need_open_comment", 0);
        article.addProperty("only_fans_can_comment", 0);
        article.addProperty("show_cover_pic", 1);

        JsonArray articles = new JsonArray();
        articles.add(article);

        JsonObject body = new JsonObject();
        body.add("articles", articles);

        String url = wechatConfig.getBaseUrl() + "/draft/add?access_token=" + encode(getAccessToken());
        JsonObject json = postJson(url, body);
        ensureWechatSuccess(json, "创建微信公众号草稿失败");
        if (!json.has("media_id")) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "创建微信公众号草稿失败：未返回 media_id");
        }
        return json.get("media_id").getAsString();
    }

    public String submitPublish(String mediaId) {
        JsonObject body = new JsonObject();
        body.addProperty("media_id", mediaId);

        String url = wechatConfig.getBaseUrl() + "/freepublish/submit?access_token=" + encode(getAccessToken());
        JsonObject json = postJson(url, body);
        ensureWechatSuccess(json, "提交微信公众号发布失败");
        if (!json.has("publish_id")) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "提交微信公众号发布失败：未返回 publish_id");
        }
        return json.get("publish_id").getAsString();
    }

    public JsonObject getPublishStatus(String publishId) {
        JsonObject body = new JsonObject();
        body.addProperty("publish_id", publishId);

        String url = wechatConfig.getBaseUrl() + "/freepublish/get?access_token=" + encode(getAccessToken());
        JsonObject json = postJson(url, body);
        ensureWechatSuccess(json, "查询微信公众号发布状态失败");
        return json;
    }

    private JsonObject uploadImageMultipart(String url, ImageBytes imageBytes) {
        String fileName = "wechat-image" + extensionFromContentType(imageBytes.getContentType());
        RequestBody fileBody = RequestBody.create(imageBytes.getBytes(), OCTET_STREAM);
        RequestBody body = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("media", fileName, fileBody)
                .build();
        Request request = new Request.Builder().url(url).post(body).build();
        return executeJson(request);
    }

    private ImageBytes downloadImage(String imageUrl) {
        if (StrUtil.isBlank(imageUrl)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "图片 URL 不能为空");
        }
        if (!imageUrl.startsWith("http://") && !imageUrl.startsWith("https://")) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "仅支持 http/https 图片：" + imageUrl);
        }

        Request request = new Request.Builder().url(imageUrl).get().build();
        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful() || response.body() == null) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "下载图片失败：" + imageUrl);
            }
            String contentType = response.header("Content-Type", "image/jpeg");
            if (!isSupportedWechatImage(contentType, imageUrl)) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "微信公众号不支持该图片类型：" + imageUrl);
            }
            return new ImageBytes(response.body().bytes(), contentType);
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "下载图片异常：" + imageUrl);
        }
    }

    private JsonObject getJson(String url) {
        return executeJson(new Request.Builder().url(url).get().build());
    }

    private JsonObject postJson(String url, JsonObject body) {
        Request request = new Request.Builder()
                .url(url)
                .post(RequestBody.create(gson.toJson(body), JSON))
                .build();
        return executeJson(request);
    }

    private JsonObject executeJson(Request request) {
        try (Response response = httpClient.newCall(request).execute()) {
            String responseBody = response.body() == null ? "" : response.body().string();
            if (!response.isSuccessful()) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "微信接口 HTTP 调用失败：" + response.code());
            }
            return JsonParser.parseString(responseBody).getAsJsonObject();
        } catch (IOException e) {
            log.error("微信接口调用异常", e);
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "微信接口调用异常");
        }
    }

    private void ensureWechatSuccess(JsonObject json, String message) {
        if (json == null) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, message);
        }
        if (json.has("errcode") && json.get("errcode").getAsInt() != 0) {
            String errMsg = json.has("errmsg") ? json.get("errmsg").getAsString() : json.toString();
            throw new BusinessException(ErrorCode.OPERATION_ERROR, message + "：" + errMsg);
        }
    }

    private void validateConfig() {
        if (StrUtil.isBlank(wechatConfig.getAppId()) || StrUtil.isBlank(wechatConfig.getAppSecret())) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "微信公众号 AppID/AppSecret 未配置");
        }
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private boolean isSupportedWechatImage(String contentType, String imageUrl) {
        String lowerType = contentType == null ? "" : contentType.toLowerCase();
        String lowerUrl = imageUrl.toLowerCase();
        return lowerType.contains("image/jpeg")
                || lowerType.contains("image/jpg")
                || lowerType.contains("image/png")
                || lowerType.contains("image/gif")
                || lowerUrl.endsWith(".jpg")
                || lowerUrl.endsWith(".jpeg")
                || lowerUrl.endsWith(".png")
                || lowerUrl.endsWith(".gif");
    }

    private String extensionFromContentType(String contentType) {
        String lower = contentType == null ? "" : contentType.toLowerCase();
        if (lower.contains("png")) {
            return ".png";
        }
        if (lower.contains("gif")) {
            return ".gif";
        }
        return ".jpg";
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    private static class ImageBytes {
        private byte[] bytes;
        private String contentType;
    }
}
