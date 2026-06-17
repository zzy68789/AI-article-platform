package com.yupi.template.model.vo;

import com.yupi.template.model.entity.ArticleContentVersion;
import lombok.Data;
import org.springframework.beans.BeanUtils;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class ArticleContentVersionVO implements Serializable {

    private Long id;

    private Long articleId;

    private String taskId;

    private Long userId;

    private Integer versionNo;

    private String title;

    private String subTitle;

    private String markdown;

    private String contentHash;

    private String source;

    private Integer rollbackFromVersionNo;

    private String remark;

    private Integer wordCount;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    public static ArticleContentVersionVO objToVo(ArticleContentVersion version) {
        if (version == null) {
            return null;
        }
        ArticleContentVersionVO vo = new ArticleContentVersionVO();
        BeanUtils.copyProperties(version, vo);
        return vo;
    }

    private static final long serialVersionUID = 1L;
}
