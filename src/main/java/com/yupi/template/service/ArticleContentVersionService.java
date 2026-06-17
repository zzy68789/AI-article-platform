package com.yupi.template.service;

import cn.hutool.core.util.StrUtil;
import com.mybatisflex.core.query.QueryWrapper;
import com.yupi.template.constant.UserConstant;
import com.yupi.template.exception.BusinessException;
import com.yupi.template.exception.ErrorCode;
import com.yupi.template.mapper.ArticleContentVersionMapper;
import com.yupi.template.model.dto.article.ArticleContentRollbackRequest;
import com.yupi.template.model.dto.article.ArticleContentUpdateRequest;
import com.yupi.template.model.entity.Article;
import com.yupi.template.model.entity.ArticleContentVersion;
import com.yupi.template.model.entity.User;
import com.yupi.template.model.enums.ArticleContentVersionSourceEnum;
import com.yupi.template.model.enums.ArticleStatusEnum;
import com.yupi.template.model.vo.ArticleContentVersionVO;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;

@Service
public class ArticleContentVersionService {

    @Resource
    private ArticleContentVersionMapper versionMapper;

    @Resource
    private ArticleService articleService;

    public List<ArticleContentVersionVO> listVersions(String taskId, User loginUser) {
        Article article = getArticleAndCheckPermission(taskId, loginUser);
        return findVersions(article.getTaskId()).stream()
                .map(ArticleContentVersionVO::objToVo)
                .toList();
    }

    @Transactional(rollbackFor = Exception.class)
    public ArticleContentVersionVO updateContent(ArticleContentUpdateRequest request, User loginUser) {
        if (request == null || StrUtil.isBlank(request.getTaskId()) || StrUtil.isBlank(request.getMarkdown())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Article article = getEditableArticle(request.getTaskId(), loginUser);
        List<ArticleContentVersion> versions = ensureInitialVersion(article);
        ArticleContentVersion latest = latestVersion(versions);
        String contentHash = hashMarkdown(request.getMarkdown());
        if (latest != null && contentHash.equals(latest.getContentHash())) {
            return ArticleContentVersionVO.objToVo(latest);
        }

        ArticleContentVersion saved = buildVersion(
                article,
                nextVersionNo(versions),
                request.getMarkdown(),
                ArticleContentVersionSourceEnum.MANUAL_SAVE.getValue(),
                null,
                request.getRemark()
        );
        versionMapper.insert(saved);
        article.setFullContent(request.getMarkdown());
        articleService.updateById(article);
        return ArticleContentVersionVO.objToVo(saved);
    }

    @Transactional(rollbackFor = Exception.class)
    public ArticleContentVersionVO rollbackContent(ArticleContentRollbackRequest request, User loginUser) {
        if (request == null || StrUtil.isBlank(request.getTaskId()) || request.getVersionNo() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Article article = getEditableArticle(request.getTaskId(), loginUser);
        List<ArticleContentVersion> versions = findVersions(article.getTaskId());
        ArticleContentVersion target = versions.stream()
                .filter(version -> Objects.equals(version.getVersionNo(), request.getVersionNo()))
                .findFirst()
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND_ERROR, "版本不存在"));

        ArticleContentVersion rollback = buildVersion(
                article,
                nextVersionNo(versions),
                target.getMarkdown(),
                ArticleContentVersionSourceEnum.ROLLBACK.getValue(),
                target.getVersionNo(),
                request.getRemark()
        );
        versionMapper.insert(rollback);
        article.setFullContent(target.getMarkdown());
        articleService.updateById(article);
        return ArticleContentVersionVO.objToVo(rollback);
    }

    public Integer getLatestVersionNo(String taskId) {
        return findVersions(taskId).stream()
                .map(ArticleContentVersion::getVersionNo)
                .max(Integer::compareTo)
                .orElse(null);
    }

    public static String hashMarkdown(String markdown) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(StrUtil.nullToEmpty(markdown).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashed);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }

    private Article getEditableArticle(String taskId, User loginUser) {
        Article article = getArticleAndCheckPermission(taskId, loginUser);
        if (!ArticleStatusEnum.COMPLETED.getValue().equals(article.getStatus())) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "只有已完成文章允许编辑正文");
        }
        if (StrUtil.isBlank(article.getFullContent()) && StrUtil.isBlank(article.getContent())) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "文章正文为空");
        }
        return article;
    }

    private Article getArticleAndCheckPermission(String taskId, User loginUser) {
        if (StrUtil.isBlank(taskId)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Article article = articleService.getByTaskId(taskId);
        if (article == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "文章不存在");
        }
        if (!Objects.equals(article.getUserId(), loginUser.getId())
                && !UserConstant.ADMIN_ROLE.equals(loginUser.getUserRole())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        return article;
    }

    private List<ArticleContentVersion> ensureInitialVersion(Article article) {
        List<ArticleContentVersion> versions = findVersions(article.getTaskId());
        if (!versions.isEmpty()) {
            return versions;
        }
        String initialMarkdown = StrUtil.blankToDefault(article.getFullContent(), article.getContent());
        ArticleContentVersion initial = buildVersion(
                article,
                1,
                initialMarkdown,
                ArticleContentVersionSourceEnum.AI_GENERATED.getValue(),
                null,
                "AI generated initial version"
        );
        versionMapper.insert(initial);
        versions = findVersions(article.getTaskId());
        if (versions.isEmpty()) {
            return List.of(initial);
        }
        return versions;
    }

    private List<ArticleContentVersion> findVersions(String taskId) {
        List<ArticleContentVersion> versions = versionMapper.selectListByQuery(QueryWrapper.create()
                .eq("taskId", taskId)
                .eq("isDelete", 0)
                .orderBy("versionNo", false));
        return versions == null ? List.of() : versions;
    }

    private ArticleContentVersion latestVersion(List<ArticleContentVersion> versions) {
        return versions.stream()
                .max(Comparator.comparing(ArticleContentVersion::getVersionNo))
                .orElse(null);
    }

    private int nextVersionNo(List<ArticleContentVersion> versions) {
        return versions.stream()
                .map(ArticleContentVersion::getVersionNo)
                .filter(Objects::nonNull)
                .max(Integer::compareTo)
                .orElse(0) + 1;
    }

    private ArticleContentVersion buildVersion(
            Article article,
            Integer versionNo,
            String markdown,
            String source,
            Integer rollbackFromVersionNo,
            String remark) {
        LocalDateTime now = LocalDateTime.now();
        return ArticleContentVersion.builder()
                .articleId(article.getId())
                .taskId(article.getTaskId())
                .userId(article.getUserId())
                .versionNo(versionNo)
                .title(article.getMainTitle())
                .subTitle(article.getSubTitle())
                .markdown(markdown)
                .contentHash(hashMarkdown(markdown))
                .source(source)
                .rollbackFromVersionNo(rollbackFromVersionNo)
                .remark(remark)
                .wordCount(countWords(markdown))
                .createTime(now)
                .updateTime(now)
                .isDelete(0)
                .build();
    }

    private int countWords(String markdown) {
        String text = StrUtil.nullToEmpty(markdown).replaceAll("[\\s\\p{Punct}]+", "");
        return text.length();
    }
}
