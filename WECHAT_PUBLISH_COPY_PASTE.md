# WeChat Publish Copy Paste Code

下面按文件列出需要新增或修改的代码。建议先创建分支：

```bash
git switch -c feature/wechat-publish
```

再逐个复制。当前仓库已有未提交改动，复制前先执行：

```bash
git status
```

## 1. SQL

新增文件：`sql/add_wechat_publish_record.sql`

```sql
use ai_passage_creator;

ALTER TABLE article
    MODIFY COLUMN content MEDIUMTEXT NULL COMMENT '正文（Markdown格式）',
    MODIFY COLUMN fullContent MEDIUMTEXT NULL COMMENT '完整图文（Markdown格式，含配图）';

CREATE TABLE IF NOT EXISTS wechat_publish_record
(
    id                  BIGINT AUTO_INCREMENT COMMENT 'id' PRIMARY KEY,
    articleId           BIGINT                             NOT NULL COMMENT '文章ID',
    taskId              VARCHAR(64)                        NOT NULL COMMENT '任务ID',
    userId              BIGINT                             NOT NULL COMMENT '用户ID',
    mediaId             VARCHAR(128)                       NULL COMMENT '微信草稿 media_id',
    publishId           VARCHAR(128)                       NULL COMMENT '微信发布 publish_id',
    articleIdFromWechat VARCHAR(128)                       NULL COMMENT '微信返回的 article_id',
    articleUrl          VARCHAR(1024)                      NULL COMMENT '发布后的文章 URL',
    status              VARCHAR(32)                        NOT NULL COMMENT 'DRAFT_CREATED/SUBMITTED/PUBLISHING/SUCCESS/FAILED',
    mode                VARCHAR(32)                        NOT NULL COMMENT 'DRAFT/PUBLISH',
    articleTitle        VARCHAR(255)                       NULL COMMENT '发布标题',
    officialStatusCode  VARCHAR(64)                        NULL COMMENT '微信官方状态码',
    officialResponse    MEDIUMTEXT                         NULL COMMENT '微信官方响应',
    errorMessage        TEXT                               NULL COMMENT '错误信息',
    attemptNo           INT         DEFAULT 1              NOT NULL COMMENT '发布尝试次数',
    createTime          DATETIME    DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '创建时间',
    updateTime          DATETIME    DEFAULT CURRENT_TIMESTAMP NOT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    isDelete            TINYINT     DEFAULT 0              NOT NULL COMMENT '是否删除',
    INDEX idx_taskId (taskId),
    INDEX idx_userId (userId),
    INDEX idx_publishId (publishId),
    INDEX idx_status (status),
    INDEX idx_userId_taskId (userId, taskId)
) COMMENT '微信公众号发布记录' COLLATE = utf8mb4_unicode_ci;
```

## 2. Maven 依赖

修改：`pom.xml`

在 `jsoup` 依赖附近加入：

```xml
<!-- Markdown 转 HTML，用于微信公众号发布 -->
<dependency>
    <groupId>com.vladsch.flexmark</groupId>
    <artifactId>flexmark-all</artifactId>
    <version>0.64.8</version>
</dependency>
```

## 3. 配置文件

修改：`src/main/resources/application.yml`

追加：

```yaml
wechat:
  app-id: ${WECHAT_APP_ID:}
  app-secret: ${WECHAT_APP_SECRET:}
  default-author: ${WECHAT_DEFAULT_AUTHOR:}
  base-url: ${WECHAT_BASE_URL:https://api.weixin.qq.com/cgi-bin}
  connect-timeout-ms: ${WECHAT_CONNECT_TIMEOUT_MS:10000}
  read-timeout-ms: ${WECHAT_READ_TIMEOUT_MS:30000}
```

也追加到：`src/main/resources/application-local.yml.example`

```yaml
wechat:
  app-id: ${WECHAT_APP_ID:}
  app-secret: ${WECHAT_APP_SECRET:}
  default-author: ${WECHAT_DEFAULT_AUTHOR:}
  base-url: ${WECHAT_BASE_URL:https://api.weixin.qq.com/cgi-bin}
  connect-timeout-ms: ${WECHAT_CONNECT_TIMEOUT_MS:10000}
  read-timeout-ms: ${WECHAT_READ_TIMEOUT_MS:30000}
```

## 4. 后端配置类

新增文件：`src/main/java/com/yupi/template/config/WechatConfig.java`

```java
package com.yupi.template.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "wechat")
@Data
public class WechatConfig {

    private String appId;

    private String appSecret;

    private String defaultAuthor;

    private String baseUrl = "https://api.weixin.qq.com/cgi-bin";

    private Integer connectTimeoutMs = 10000;

    private Integer readTimeoutMs = 30000;
}
```

## 5. 枚举

新增文件：`src/main/java/com/yupi/template/model/enums/WechatPublishStatusEnum.java`

```java
package com.yupi.template.model.enums;

import lombok.Getter;

@Getter
public enum WechatPublishStatusEnum {

    DRAFT_CREATED("DRAFT_CREATED", "草稿已创建"),
    SUBMITTED("SUBMITTED", "已提交发布"),
    PUBLISHING("PUBLISHING", "发布中"),
    SUCCESS("SUCCESS", "发布成功"),
    FAILED("FAILED", "发布失败");

    private final String value;

    private final String description;

    WechatPublishStatusEnum(String value, String description) {
        this.value = value;
        this.description = description;
    }
}
```

新增文件：`src/main/java/com/yupi/template/model/enums/WechatPublishModeEnum.java`

```java
package com.yupi.template.model.enums;

import lombok.Getter;

@Getter
public enum WechatPublishModeEnum {

    DRAFT("DRAFT", "保存草稿"),
    PUBLISH("PUBLISH", "提交发布");

    private final String value;

    private final String description;

    WechatPublishModeEnum(String value, String description) {
        this.value = value;
        this.description = description;
    }
}
```

## 6. 实体和 Mapper

新增文件：`src/main/java/com/yupi/template/model/entity/WechatPublishRecord.java`

```java
package com.yupi.template.model.entity;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(value = "wechat_publish_record", camelToUnderline = false)
public class WechatPublishRecord implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id(keyType = KeyType.Auto)
    private Long id;

    private Long articleId;

    private String taskId;

    private Long userId;

    private String mediaId;

    private String publishId;

    private String articleIdFromWechat;

    private String articleUrl;

    private String status;

    private String mode;

    private String articleTitle;

    private String officialStatusCode;

    private String officialResponse;

    private String errorMessage;

    private Integer attemptNo;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    @Column(isLogicDelete = true)
    private Integer isDelete;
}
```

新增文件：`src/main/java/com/yupi/template/mapper/WechatPublishRecordMapper.java`

```java
package com.yupi.template.mapper;

import com.mybatisflex.core.BaseMapper;
import com.yupi.template.model.entity.WechatPublishRecord;

public interface WechatPublishRecordMapper extends BaseMapper<WechatPublishRecord> {
}
```

## 7. DTO 和 VO

新增文件：`src/main/java/com/yupi/template/model/dto/wechat/WechatPublishRequest.java`

```java
package com.yupi.template.model.dto.wechat;

import lombok.Data;

import java.io.Serializable;

@Data
public class WechatPublishRequest implements Serializable {

    private Boolean force = false;

    private String coverImageUrl;

    private static final long serialVersionUID = 1L;
}
```

新增文件：`src/main/java/com/yupi/template/model/vo/WechatPublishVO.java`

```java
package com.yupi.template.model.vo;

import com.yupi.template.model.entity.WechatPublishRecord;
import lombok.Data;
import org.springframework.beans.BeanUtils;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class WechatPublishVO implements Serializable {

    private Long recordId;

    private String taskId;

    private String mode;

    private String status;

    private String mediaId;

    private String publishId;

    private String articleIdFromWechat;

    private String articleUrl;

    private String officialStatusCode;

    private String officialResponse;

    private String errorMessage;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    public static WechatPublishVO objToVo(WechatPublishRecord record) {
        if (record == null) {
            return null;
        }
        WechatPublishVO vo = new WechatPublishVO();
        BeanUtils.copyProperties(record, vo);
        vo.setRecordId(record.getId());
        return vo;
    }

    private static final long serialVersionUID = 1L;
}
```

## 8. 微信 API Client

新增文件：`src/main/java/com/yupi/template/service/WechatApiClient.java`

```java
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
        article.addProperty("author", author);
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
```

## 9. Markdown 渲染服务

新增文件：`src/main/java/com/yupi/template/service/WechatMarkdownRenderService.java`

```java
package com.yupi.template.service;

import cn.hutool.core.util.StrUtil;
import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.data.MutableDataSet;
import com.yupi.template.exception.BusinessException;
import com.yupi.template.exception.ErrorCode;
import jakarta.annotation.Resource;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.safety.Safelist;
import org.springframework.stereotype.Service;

@Service
public class WechatMarkdownRenderService {

    @Resource
    private WechatApiClient wechatApiClient;

    private final Parser parser;

    private final HtmlRenderer renderer;

    public WechatMarkdownRenderService() {
        MutableDataSet options = new MutableDataSet();
        this.parser = Parser.builder(options).build();
        this.renderer = HtmlRenderer.builder(options).build();
    }

    public String renderForWechat(String markdown) {
        if (StrUtil.isBlank(markdown)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "文章内容不能为空");
        }
        String rawHtml = renderer.render(parser.parse(markdown));
        Document doc = Jsoup.parseBodyFragment(rawHtml);
        replaceImages(doc);
        applyWechatStyles(doc);
        return cleanHtml(doc.body().html());
    }

    private void replaceImages(Document doc) {
        for (Element img : doc.select("img")) {
            String src = img.attr("src");
            if (StrUtil.isBlank(src)) {
                continue;
            }
            String wechatUrl = wechatApiClient.uploadContentImage(src);
            img.attr("src", wechatUrl);
        }
    }

    private void applyWechatStyles(Document doc) {
        for (Element h1 : doc.select("h1")) {
            h1.attr("style", "font-size:22px;font-weight:700;line-height:1.5;margin:24px 0 12px;color:#111;");
        }
        for (Element h2 : doc.select("h2")) {
            h2.attr("style", "font-size:20px;font-weight:700;line-height:1.5;margin:22px 0 10px;color:#111;");
        }
        for (Element h3 : doc.select("h3")) {
            h3.attr("style", "font-size:18px;font-weight:700;line-height:1.5;margin:18px 0 8px;color:#111;");
        }
        for (Element p : doc.select("p")) {
            p.attr("style", "font-size:16px;line-height:1.9;margin:0 0 14px;color:#333;");
        }
        for (Element li : doc.select("li")) {
            li.attr("style", "font-size:16px;line-height:1.8;margin:0 0 8px;color:#333;");
        }
        for (Element img : doc.select("img")) {
            img.attr("style", "max-width:100%;height:auto;display:block;margin:16px auto;");
        }
    }

    private String cleanHtml(String html) {
        Safelist safelist = Safelist.relaxed()
                .addAttributes(":all", "style")
                .addAttributes("img", "src", "alt", "title")
                .addProtocols("img", "src", "http", "https");
        Document.OutputSettings settings = new Document.OutputSettings().prettyPrint(false);
        return Jsoup.clean(html, "", safelist, settings);
    }
}
```

## 10. 发布服务

新增文件：`src/main/java/com/yupi/template/service/WechatPublishService.java`

```java
package com.yupi.template.service;

import cn.hutool.core.util.StrUtil;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.yupi.template.constant.UserConstant;
import com.yupi.template.exception.BusinessException;
import com.yupi.template.exception.ErrorCode;
import com.yupi.template.mapper.WechatPublishRecordMapper;
import com.yupi.template.model.dto.article.ArticleState;
import com.yupi.template.model.dto.wechat.WechatPublishRequest;
import com.yupi.template.model.entity.Article;
import com.yupi.template.model.entity.User;
import com.yupi.template.model.entity.WechatPublishRecord;
import com.yupi.template.model.enums.ArticleStatusEnum;
import com.yupi.template.model.enums.WechatPublishModeEnum;
import com.yupi.template.model.enums.WechatPublishStatusEnum;
import com.yupi.template.model.vo.WechatPublishVO;
import com.yupi.template.utils.GsonUtils;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

@Service
@Slf4j
public class WechatPublishService extends ServiceImpl<WechatPublishRecordMapper, WechatPublishRecord> {

    @Resource
    private ArticleService articleService;

    @Resource
    private WechatApiClient wechatApiClient;

    @Resource
    private WechatMarkdownRenderService wechatMarkdownRenderService;

    private final ConcurrentHashMap<String, Object> locks = new ConcurrentHashMap<>();

    public WechatPublishVO saveDraft(String taskId, WechatPublishRequest request, User loginUser) {
        return withTaskLock(taskId, () -> {
            Article article = getPublishableArticle(taskId, loginUser);
            boolean force = request != null && Boolean.TRUE.equals(request.getForce());
            if (!force) {
                WechatPublishRecord existing = findLatestNonFailed(taskId, loginUser.getId());
                if (existing != null) {
                    return WechatPublishVO.objToVo(existing);
                }
            }
            return WechatPublishVO.objToVo(createDraft(article, request, loginUser));
        });
    }

    public WechatPublishVO publish(String taskId, WechatPublishRequest request, User loginUser) {
        return withTaskLock(taskId, () -> {
            Article article = getPublishableArticle(taskId, loginUser);
            boolean force = request != null && Boolean.TRUE.equals(request.getForce());

            WechatPublishRecord record = null;
            if (!force) {
                record = findLatestNonFailed(taskId, loginUser.getId());
                if (record != null && isAlreadySubmitted(record)) {
                    return WechatPublishVO.objToVo(record);
                }
            }
            if (record == null) {
                record = createDraft(article, request, loginUser);
            }

            try {
                String publishId = wechatApiClient.submitPublish(record.getMediaId());
                record.setMode(WechatPublishModeEnum.PUBLISH.getValue());
                record.setStatus(WechatPublishStatusEnum.SUBMITTED.getValue());
                record.setPublishId(publishId);
                record.setErrorMessage(null);
                record.setUpdateTime(LocalDateTime.now());
                this.updateById(record);
                return WechatPublishVO.objToVo(record);
            } catch (BusinessException e) {
                record.setMode(WechatPublishModeEnum.PUBLISH.getValue());
                record.setStatus(WechatPublishStatusEnum.FAILED.getValue());
                record.setErrorMessage(e.getMessage());
                record.setUpdateTime(LocalDateTime.now());
                this.updateById(record);
                throw e;
            }
        });
    }

    public WechatPublishVO getLatestStatus(String taskId, User loginUser) {
        Article article = articleService.getByTaskId(taskId);
        if (article != null) {
            checkPermission(article, loginUser);
        }
        WechatPublishRecord record = findLatest(taskId, loginUser);
        return WechatPublishVO.objToVo(record);
    }

    public WechatPublishVO refreshOfficialStatus(String publishId, User loginUser) {
        if (StrUtil.isBlank(publishId)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "publishId 不能为空");
        }
        WechatPublishRecord record = this.getOne(QueryWrapper.create()
                .eq("publishId", publishId)
                .eq("isDelete", 0));
        if (record == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "发布记录不存在");
        }
        if (!Objects.equals(record.getUserId(), loginUser.getId())
                && !UserConstant.ADMIN_ROLE.equals(loginUser.getUserRole())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }

        JsonObject official = wechatApiClient.getPublishStatus(publishId);
        record.setOfficialResponse(official.toString());
        if (official.has("publish_status")) {
            String officialStatus = official.get("publish_status").getAsString();
            record.setOfficialStatusCode(officialStatus);
            record.setStatus(mapOfficialStatus(officialStatus));
        }
        if (official.has("article_id")) {
            record.setArticleIdFromWechat(official.get("article_id").getAsString());
        }
        if (official.has("article_url")) {
            record.setArticleUrl(official.get("article_url").getAsString());
        }
        record.setUpdateTime(LocalDateTime.now());
        this.updateById(record);
        return WechatPublishVO.objToVo(record);
    }

    private WechatPublishRecord createDraft(Article article, WechatPublishRequest request, User loginUser) {
        WechatPublishRecord record = buildBaseRecord(article, loginUser);
        try {
            String markdown = StrUtil.blankToDefault(article.getFullContent(), article.getContent());
            String html = wechatMarkdownRenderService.renderForWechat(markdown);
            String coverUrl = resolveCoverUrl(article, request);
            String thumbMediaId = wechatApiClient.uploadCoverImage(coverUrl);
            String title = StrUtil.blankToDefault(article.getMainTitle(), article.getTopic());
            String digest = buildDigest(markdown);
            String mediaId = wechatApiClient.addDraft(title, null, digest, html, thumbMediaId);

            record.setArticleTitle(title);
            record.setMediaId(mediaId);
            record.setMode(WechatPublishModeEnum.DRAFT.getValue());
            record.setStatus(WechatPublishStatusEnum.DRAFT_CREATED.getValue());
            record.setCreateTime(LocalDateTime.now());
            record.setUpdateTime(LocalDateTime.now());
            this.save(record);
            return record;
        } catch (BusinessException e) {
            record.setStatus(WechatPublishStatusEnum.FAILED.getValue());
            record.setMode(WechatPublishModeEnum.DRAFT.getValue());
            record.setErrorMessage(e.getMessage());
            record.setCreateTime(LocalDateTime.now());
            record.setUpdateTime(LocalDateTime.now());
            this.save(record);
            throw e;
        }
    }

    private Article getPublishableArticle(String taskId, User loginUser) {
        if (StrUtil.isBlank(taskId)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "taskId 不能为空");
        }
        Article article = articleService.getByTaskId(taskId);
        if (article == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "文章不存在");
        }
        checkPermission(article, loginUser);
        if (!ArticleStatusEnum.COMPLETED.getValue().equals(article.getStatus())) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "只有已完成文章允许发布");
        }
        if (StrUtil.isBlank(article.getFullContent()) && StrUtil.isBlank(article.getContent())) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "文章内容为空，无法发布");
        }
        return article;
    }

    private void checkPermission(Article article, User loginUser) {
        if (!Objects.equals(article.getUserId(), loginUser.getId())
                && !UserConstant.ADMIN_ROLE.equals(loginUser.getUserRole())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
    }

    private String resolveCoverUrl(Article article, WechatPublishRequest request) {
        if (request != null && StrUtil.isNotBlank(request.getCoverImageUrl())) {
            return request.getCoverImageUrl();
        }
        if (StrUtil.isNotBlank(article.getCoverImage())) {
            return article.getCoverImage();
        }
        if (StrUtil.isNotBlank(article.getImages())) {
            List<ArticleState.ImageResult> images = GsonUtils.fromJson(
                    article.getImages(),
                    new TypeToken<List<ArticleState.ImageResult>>() {}
            );
            if (images != null) {
                for (ArticleState.ImageResult image : images) {
                    if (image.getPosition() != null && image.getPosition() == 1 && StrUtil.isNotBlank(image.getUrl())) {
                        return image.getUrl();
                    }
                }
            }
        }
        throw new BusinessException(ErrorCode.OPERATION_ERROR, "缺少微信公众号封面图");
    }

    private WechatPublishRecord buildBaseRecord(Article article, User loginUser) {
        return WechatPublishRecord.builder()
                .articleId(article.getId())
                .taskId(article.getTaskId())
                .userId(loginUser.getId())
                .attemptNo(nextAttemptNo(article.getTaskId(), loginUser.getId()))
                .isDelete(0)
                .build();
    }

    private int nextAttemptNo(String taskId, Long userId) {
        return this.list(QueryWrapper.create()
                .eq("taskId", taskId)
                .eq("userId", userId)
                .eq("isDelete", 0)).size() + 1;
    }

    private WechatPublishRecord findLatestNonFailed(String taskId, Long userId) {
        List<WechatPublishRecord> records = this.list(QueryWrapper.create()
                .eq("taskId", taskId)
                .eq("userId", userId)
                .eq("isDelete", 0)
                .ne("status", WechatPublishStatusEnum.FAILED.getValue())
                .orderBy("createTime", false));
        return records == null || records.isEmpty() ? null : records.get(0);
    }

    private WechatPublishRecord findLatest(String taskId, User loginUser) {
        QueryWrapper queryWrapper = QueryWrapper.create()
                .eq("taskId", taskId)
                .eq("isDelete", 0)
                .orderBy("createTime", false);
        if (!UserConstant.ADMIN_ROLE.equals(loginUser.getUserRole())) {
            queryWrapper.eq("userId", loginUser.getId());
        }
        List<WechatPublishRecord> records = this.list(queryWrapper);
        return records == null || records.isEmpty() ? null : records.get(0);
    }

    private boolean isAlreadySubmitted(WechatPublishRecord record) {
        return WechatPublishStatusEnum.SUBMITTED.getValue().equals(record.getStatus())
                || WechatPublishStatusEnum.PUBLISHING.getValue().equals(record.getStatus())
                || WechatPublishStatusEnum.SUCCESS.getValue().equals(record.getStatus());
    }

    private String mapOfficialStatus(String officialStatus) {
        if ("0".equals(officialStatus)) {
            return WechatPublishStatusEnum.SUCCESS.getValue();
        }
        if ("1".equals(officialStatus)) {
            return WechatPublishStatusEnum.PUBLISHING.getValue();
        }
        return WechatPublishStatusEnum.FAILED.getValue();
    }

    private String buildDigest(String markdown) {
        String text = Jsoup.parse(markdown == null ? "" : markdown).text();
        if (text.length() <= 120) {
            return text;
        }
        return text.substring(0, 120);
    }

    private <T> T withTaskLock(String taskId, Supplier<T> supplier) {
        Object lock = locks.computeIfAbsent(taskId, key -> new Object());
        synchronized (lock) {
            try {
                return supplier.get();
            } finally {
                locks.remove(taskId, lock);
            }
        }
    }
}
```

## 11. Controller

新增文件：`src/main/java/com/yupi/template/controller/WechatPublishController.java`

```java
package com.yupi.template.controller;

import com.yupi.template.common.BaseResponse;
import com.yupi.template.common.ResultUtils;
import com.yupi.template.model.dto.wechat.WechatPublishRequest;
import com.yupi.template.model.entity.User;
import com.yupi.template.model.vo.WechatPublishVO;
import com.yupi.template.service.UserService;
import com.yupi.template.service.WechatPublishService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/article/wechat")
@Slf4j
@Tag(name = "WechatPublishController", description = "微信公众号发布接口")
public class WechatPublishController {

    @Resource
    private WechatPublishService wechatPublishService;

    @Resource
    private UserService userService;

    @PostMapping("/draft/{taskId}")
    @Operation(summary = "保存到微信公众号草稿箱")
    public BaseResponse<WechatPublishVO> saveDraft(
            @PathVariable String taskId,
            @RequestBody(required = false) WechatPublishRequest request,
            HttpServletRequest httpServletRequest) {
        User loginUser = userService.getLoginUser(httpServletRequest);
        return ResultUtils.success(wechatPublishService.saveDraft(taskId, request, loginUser));
    }

    @PostMapping("/publish/{taskId}")
    @Operation(summary = "提交发布到微信公众号")
    public BaseResponse<WechatPublishVO> publish(
            @PathVariable String taskId,
            @RequestBody(required = false) WechatPublishRequest request,
            HttpServletRequest httpServletRequest) {
        User loginUser = userService.getLoginUser(httpServletRequest);
        return ResultUtils.success(wechatPublishService.publish(taskId, request, loginUser));
    }

    @GetMapping("/status/{taskId}")
    @Operation(summary = "查询微信公众号发布记录")
    public BaseResponse<WechatPublishVO> getStatus(
            @PathVariable String taskId,
            HttpServletRequest httpServletRequest) {
        User loginUser = userService.getLoginUser(httpServletRequest);
        return ResultUtils.success(wechatPublishService.getLatestStatus(taskId, loginUser));
    }

    @GetMapping("/official-status/{publishId}")
    @Operation(summary = "查询微信官方发布状态")
    public BaseResponse<WechatPublishVO> getOfficialStatus(
            @PathVariable String publishId,
            HttpServletRequest httpServletRequest) {
        User loginUser = userService.getLoginUser(httpServletRequest);
        return ResultUtils.success(wechatPublishService.refreshOfficialStatus(publishId, loginUser));
    }
}
```

## 12. 前端 API

修改：`frontend/src/api/articleController.ts`

追加：

```ts
/** 保存到微信公众号草稿箱 POST /article/wechat/draft/${param0} */
export async function createWechatDraft(
  params: API.wechatTaskParams,
  body: API.WechatPublishRequest,
  options?: { [key: string]: any }
) {
  const { taskId: param0, ...queryParams } = params
  return request<API.BaseResponseWechatPublishVO>(`/article/wechat/draft/${param0}`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    params: { ...queryParams },
    data: body,
    ...(options || {}),
  })
}

/** 提交发布到微信公众号 POST /article/wechat/publish/${param0} */
export async function publishWechatArticle(
  params: API.wechatTaskParams,
  body: API.WechatPublishRequest,
  options?: { [key: string]: any }
) {
  const { taskId: param0, ...queryParams } = params
  return request<API.BaseResponseWechatPublishVO>(`/article/wechat/publish/${param0}`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    params: { ...queryParams },
    data: body,
    ...(options || {}),
  })
}

/** 查询微信公众号发布记录 GET /article/wechat/status/${param0} */
export async function getWechatPublishStatus(
  params: API.wechatTaskParams,
  options?: { [key: string]: any }
) {
  const { taskId: param0, ...queryParams } = params
  return request<API.BaseResponseWechatPublishVO>(`/article/wechat/status/${param0}`, {
    method: 'GET',
    params: { ...queryParams },
    ...(options || {}),
  })
}

/** 查询微信官方发布状态 GET /article/wechat/official-status/${param0} */
export async function getWechatOfficialStatus(
  params: API.wechatOfficialStatusParams,
  options?: { [key: string]: any }
) {
  const { publishId: param0, ...queryParams } = params
  return request<API.BaseResponseWechatPublishVO>(`/article/wechat/official-status/${param0}`, {
    method: 'GET',
    params: { ...queryParams },
    ...(options || {}),
  })
}
```

## 13. 前端类型

修改：`frontend/src/api/typings.d.ts`

在 `declare namespace API { ... }` 内追加：

```ts
type WechatPublishRequest = {
  force?: boolean
  coverImageUrl?: string
}

type WechatPublishVO = {
  recordId?: number
  taskId?: string
  mode?: string
  status?: string
  mediaId?: string
  publishId?: string
  articleIdFromWechat?: string
  articleUrl?: string
  officialStatusCode?: string
  officialResponse?: string
  errorMessage?: string
  createTime?: string
  updateTime?: string
}

type BaseResponseWechatPublishVO = {
  code?: number
  data?: WechatPublishVO
  message?: string
}

type wechatTaskParams = {
  taskId: string
}

type wechatOfficialStatusParams = {
  publishId: string
}
```

## 14. 前端详情页改动

修改：`frontend/src/pages/article/ArticleDetailPage.vue`

### 14.1 修改 import

把：

```ts
import { ref, onMounted, onUnmounted } from 'vue'
```

改为：

```ts
import { ref, computed, onMounted, onUnmounted } from 'vue'
```

把：

```ts
import { getArticle, getExecutionLogs } from '@/api/articleController'
```

改为：

```ts
import {
  getArticle,
  getExecutionLogs,
  createWechatDraft,
  publishWechatArticle,
  getWechatPublishStatus,
  getWechatOfficialStatus
} from '@/api/articleController'
```

在图标 import 中追加：

```ts
CloudUploadOutlined,
SendOutlined,
SyncOutlined
```

### 14.2 添加响应式状态

放在 `const executionStats = ref...` 附近：

```ts
const wechatRecord = ref<API.WechatPublishVO | null>(null)
const wechatLoading = ref(false)
const wechatStatusLoading = ref(false)

const canPublishWechat = computed(() => {
  return article.value?.status === 'COMPLETED' && !!(article.value?.fullContent || article.value?.content)
})
```

### 14.3 修改 loadArticle

在 `article.value = res.data.data || null` 后面追加：

```ts
if (article.value?.taskId) {
  await loadWechatStatus(article.value.taskId)
}
```

### 14.4 添加方法

放在 `loadExecutionLogs` 后面：

```ts
const loadWechatStatus = async (taskId: string) => {
  try {
    const res = await getWechatPublishStatus({ taskId })
    if (res.data.code === 0) {
      wechatRecord.value = res.data.data || null
    }
  } catch (error) {
    console.error('加载微信公众号发布状态失败:', error)
  }
}

const handleSaveWechatDraft = async () => {
  if (!article.value?.taskId) return
  wechatLoading.value = true
  try {
    const res = await createWechatDraft(
      { taskId: article.value.taskId },
      { force: false }
    )
    if (res.data.code === 0) {
      wechatRecord.value = res.data.data || null
      message.success('已保存到微信公众号草稿箱')
    } else {
      message.error(res.data.message || '保存草稿失败')
    }
  } catch (error) {
    message.error((error as Error).message || '保存草稿失败')
  } finally {
    wechatLoading.value = false
  }
}

const handlePublishWechat = async () => {
  if (!article.value?.taskId) return
  wechatLoading.value = true
  try {
    const res = await publishWechatArticle(
      { taskId: article.value.taskId },
      { force: false }
    )
    if (res.data.code === 0) {
      wechatRecord.value = res.data.data || null
      message.success('已提交微信公众号发布')
    } else {
      message.error(res.data.message || '提交发布失败')
    }
  } catch (error) {
    message.error((error as Error).message || '提交发布失败')
  } finally {
    wechatLoading.value = false
  }
}

const handleRefreshWechatOfficialStatus = async () => {
  if (!wechatRecord.value?.publishId) return
  wechatStatusLoading.value = true
  try {
    const res = await getWechatOfficialStatus({ publishId: wechatRecord.value.publishId })
    if (res.data.code === 0) {
      wechatRecord.value = res.data.data || null
      message.success('微信官方状态已刷新')
    } else {
      message.error(res.data.message || '查询官方状态失败')
    }
  } catch (error) {
    message.error((error as Error).message || '查询官方状态失败')
  } finally {
    wechatStatusLoading.value = false
  }
}

const getWechatStatusColor = (status?: string) => {
  const colorMap: Record<string, string> = {
    DRAFT_CREATED: 'blue',
    SUBMITTED: 'processing',
    PUBLISHING: 'processing',
    SUCCESS: 'success',
    FAILED: 'error',
  }
  return colorMap[status || ''] || 'default'
}

const getWechatStatusText = (status?: string) => {
  const textMap: Record<string, string> = {
    DRAFT_CREATED: '草稿已创建',
    SUBMITTED: '已提交发布',
    PUBLISHING: '发布中',
    SUCCESS: '发布成功',
    FAILED: '发布失败',
  }
  return textMap[status || ''] || status || '未发布'
}
```

### 14.5 Header 按钮

在 `.right-actions` 里，导出按钮前面加入：

```vue
<a-button
  v-if="canPublishWechat"
  :loading="wechatLoading"
  @click="handleSaveWechatDraft"
>
  <template #icon>
    <CloudUploadOutlined />
  </template>
  保存到公众号草稿箱
</a-button>
<a-button
  v-if="canPublishWechat"
  type="primary"
  :loading="wechatLoading"
  @click="handlePublishWechat"
>
  <template #icon>
    <SendOutlined />
  </template>
  发布到公众号
</a-button>
```

### 14.6 状态展示

放在标题区和第一个 `<a-divider />` 之间：

```vue
<div v-if="wechatRecord" class="wechat-status-section">
  <div class="wechat-status-header">
    <div>
      <span class="wechat-title">微信公众号发布</span>
      <a-tag :color="getWechatStatusColor(wechatRecord.status)">
        {{ getWechatStatusText(wechatRecord.status) }}
      </a-tag>
    </div>
    <a-button
      v-if="wechatRecord.publishId"
      size="small"
      :loading="wechatStatusLoading"
      @click="handleRefreshWechatOfficialStatus"
    >
      <template #icon>
        <SyncOutlined />
      </template>
      查询官方状态
    </a-button>
  </div>
  <div class="wechat-status-grid">
    <div v-if="wechatRecord.mediaId" class="wechat-status-item">
      <span class="label">mediaId</span>
      <span class="value">{{ wechatRecord.mediaId }}</span>
    </div>
    <div v-if="wechatRecord.publishId" class="wechat-status-item">
      <span class="label">publishId</span>
      <span class="value">{{ wechatRecord.publishId }}</span>
    </div>
    <div v-if="wechatRecord.articleUrl" class="wechat-status-item">
      <span class="label">文章链接</span>
      <a :href="wechatRecord.articleUrl" target="_blank" rel="noopener noreferrer">
        {{ wechatRecord.articleUrl }}
      </a>
    </div>
    <div v-if="wechatRecord.errorMessage" class="wechat-status-item error">
      <span class="label">失败原因</span>
      <span class="value">{{ wechatRecord.errorMessage }}</span>
    </div>
  </div>
</div>
```

### 14.7 样式

放入 `<style scoped lang="scss">` 中 `.article-detail-page { ... }` 内：

```scss
.wechat-status-section {
  margin: 20px auto 28px;
  max-width: 760px;
  padding: 16px;
  background: var(--color-background-secondary);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);

  .wechat-status-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    gap: 12px;
    margin-bottom: 12px;
  }

  .wechat-title {
    font-size: 14px;
    font-weight: 600;
    margin-right: 8px;
    color: var(--color-text);
  }

  .wechat-status-grid {
    display: grid;
    gap: 10px;
  }

  .wechat-status-item {
    display: grid;
    grid-template-columns: 90px minmax(0, 1fr);
    gap: 10px;
    font-size: 13px;

    .label {
      color: var(--color-text-muted);
    }

    .value,
    a {
      min-width: 0;
      word-break: break-all;
      color: var(--color-text);
    }

    &.error .value {
      color: var(--color-error);
    }
  }
}
```

## 15. 验证命令

```bash
mvn -q test
cd frontend
npm.cmd run build
```

## 16. 两个实现提醒

1. 如果正文图片是 SVG，当前代码会明确失败。这符合 MVP，但后续可以做 SVG 转 PNG。
2. `articleController.ts` 和 `typings.d.ts` 可能是 openapi2ts 生成文件。手动粘贴能用，但后续重新生成会覆盖，最好后端跑起来后执行 `npm run openapi2ts`。
