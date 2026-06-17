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
    private ArticleContentVersionService articleContentVersionService;

    @Resource
    private WechatApiClient wechatApiClient;

    @Resource
    private WechatMarkdownRenderService wechatMarkdownRenderService;

    @Resource
    private WechatConfig wechatConfig;

    @Resource
    private WechatCredentialService wechatCredentialService;

    private final ConcurrentHashMap<String, Object> locks = new ConcurrentHashMap<>();

    public WechatPublishVO saveDraft(String taskId, WechatPublishRequest request, User loginUser) {
        Long requestedAccountId = request == null ? null : request.getWechatAccountId();
        return withTaskLock(lockKey(taskId, requestedAccountId), () -> {
            Article article = getPublishableArticle(taskId, loginUser);
            String contentHash = currentContentHash(article);
            WechatCredential credential = wechatCredentialService.resolve(requestedAccountId, loginUser);
            boolean force = request != null && Boolean.TRUE.equals(request.getForce());
            if (!force) {
                WechatPublishRecord existing = findLatestNonFailed(
                        taskId,
                        loginUser.getId(),
                        credential.getWechatAccountId(),
                        contentHash
                );
                if (existing != null) {
                    return WechatPublishVO.objToVo(existing);
                }
            }
            return WechatPublishVO.objToVo(createDraft(article, request, loginUser, credential));
        });
    }

    public WechatPublishVO publish(String taskId, WechatPublishRequest request, User loginUser) {
        Long requestedAccountId = request == null ? null : request.getWechatAccountId();
        return withTaskLock(lockKey(taskId, requestedAccountId), () -> {
            Article article = getPublishableArticle(taskId, loginUser);
            String contentHash = currentContentHash(article);
            WechatCredential credential = wechatCredentialService.resolve(requestedAccountId, loginUser);
            boolean force = request != null && Boolean.TRUE.equals(request.getForce());

            WechatPublishRecord record = null;
            if (!force) {
                record = findLatestNonFailed(taskId, loginUser.getId(), credential.getWechatAccountId(), contentHash);
                if (record != null && isAlreadySubmitted(record)) {
                    return WechatPublishVO.objToVo(record);
                }
            }
            if (record == null) {
                record = createDraft(article, request, loginUser, credential);
            }

            try {
                String publishId = wechatApiClient.submitPublish(credential.getAccessToken(), record.getMediaId());
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

    public WechatPublishVO getLatestStatus(String taskId, Long wechatAccountId, User loginUser) {
        Article article = articleService.getByTaskId(taskId);
        if (article != null) {
            checkPermission(article, loginUser);
        }
        if (wechatAccountId != null) {
            wechatCredentialService.resolve(wechatAccountId, loginUser);
        }
        WechatPublishRecord record = findLatest(taskId, wechatAccountId, loginUser);
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

        WechatCredential credential = wechatCredentialService.resolve(record.getWechatAccountId(), loginUser);
        JsonObject official = wechatApiClient.getPublishStatus(credential.getAccessToken(), publishId);
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

    private WechatPublishRecord createDraft(
            Article article,
            WechatPublishRequest request,
            User loginUser,
            WechatCredential credential) {
        WechatPublishRecord record = buildBaseRecord(article, loginUser, credential);
        try {
            String markdown = resolveArticleMarkdown(article);
            String contentHash = ArticleContentVersionService.hashMarkdown(markdown);
            String html = wechatMarkdownRenderService.renderForWechat(markdown, credential.getAccessToken());
            String coverUrl = resolveCoverUrl(article, request);
            String thumbMediaId = wechatApiClient.uploadCoverImage(credential.getAccessToken(), coverUrl);
            String title = StrUtil.blankToDefault(article.getMainTitle(), article.getTopic());
            String digest = buildDigest(markdown);
            String mediaId = wechatApiClient.addDraft(
                    credential.getAccessToken(),
                    title,
                    wechatConfig.getDefaultAuthor(),
                    digest,
                    html,
                    thumbMediaId
            );

            record.setArticleTitle(title);
            record.setContentHash(contentHash);
            record.setContentVersionNo(articleContentVersionService.getLatestVersionNo(article.getTaskId()));
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

    private WechatPublishRecord buildBaseRecord(
            Article article,
            User loginUser,
            WechatCredential credential) {
        return WechatPublishRecord.builder()
                .articleId(article.getId())
                .taskId(article.getTaskId())
                .userId(loginUser.getId())
                .wechatAccountId(credential.getWechatAccountId())
                .authorizerAppid(credential.getAuthorizerAppid())
                .attemptNo(nextAttemptNo(
                        article.getTaskId(),
                        loginUser.getId(),
                        credential.getWechatAccountId()
                ))
                .isDelete(0)
                .build();
    }

    private int nextAttemptNo(String taskId, Long userId, Long wechatAccountId) {
        QueryWrapper queryWrapper = QueryWrapper.create()
                .eq("taskId", taskId)
                .eq("userId", userId)
                .eq("isDelete", 0);
        addAccountFilter(queryWrapper, wechatAccountId);
        return this.list(queryWrapper).size() + 1;
    }

    private WechatPublishRecord findLatestNonFailed(
            String taskId,
            Long userId,
            Long wechatAccountId,
            String contentHash) {
        QueryWrapper queryWrapper = QueryWrapper.create()
                .eq("taskId", taskId)
                .eq("userId", userId)
                .eq("isDelete", 0)
                .ne("status", WechatPublishStatusEnum.FAILED.getValue())
                .orderBy("createTime", false);
        addAccountFilter(queryWrapper, wechatAccountId);
        if (StrUtil.isBlank(contentHash)) {
            queryWrapper.isNull("contentHash");
        } else {
            queryWrapper.eq("contentHash", contentHash);
        }
        List<WechatPublishRecord> records = this.list(queryWrapper);
        return records == null || records.isEmpty() ? null : records.get(0);
    }

    private WechatPublishRecord findLatest(String taskId, Long wechatAccountId, User loginUser) {
        QueryWrapper queryWrapper = QueryWrapper.create()
                .eq("taskId", taskId)
                .eq("isDelete", 0)
                .orderBy("createTime", false);
        addAccountFilter(queryWrapper, wechatAccountId);
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

    private String currentContentHash(Article article) {
        return ArticleContentVersionService.hashMarkdown(resolveArticleMarkdown(article));
    }

    private String resolveArticleMarkdown(Article article) {
        return StrUtil.blankToDefault(article.getFullContent(), article.getContent());
    }

    private void addAccountFilter(QueryWrapper queryWrapper, Long wechatAccountId) {
        if (wechatAccountId == null) {
            queryWrapper.isNull("wechatAccountId");
        } else {
            queryWrapper.eq("wechatAccountId", wechatAccountId);
        }
    }

    private String lockKey(String taskId, Long wechatAccountId) {
        return taskId + ":" + (wechatAccountId == null ? "platform" : wechatAccountId);
    }

    private <T> T withTaskLock(String lockKey, Supplier<T> supplier) {
        Object lock = locks.computeIfAbsent(lockKey, key -> new Object());
        synchronized (lock) {
            try {
                return supplier.get();
            } finally {
                locks.remove(lockKey, lock);
            }
        }
    }
}
