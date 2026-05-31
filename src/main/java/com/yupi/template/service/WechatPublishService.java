package com.yupi.template.service;

import cn.hutool.core.util.StrUtil;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.yupi.template.config.WechatConfig;
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

    @Resource
    private WechatConfig wechatConfig;

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
            String mediaId = wechatApiClient.addDraft(
                    title,
                    wechatConfig.getDefaultAuthor(),
                    digest,
                    html,
                    thumbMediaId
            );

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
                    new TypeToken<List<ArticleState.ImageResult>>() {
                    }
            );
            if (images != null) {
                for (ArticleState.ImageResult image : images) {
                    if (image.getPosition() != null
                            && image.getPosition() == 1
                            && StrUtil.isNotBlank(image.getUrl())) {
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
