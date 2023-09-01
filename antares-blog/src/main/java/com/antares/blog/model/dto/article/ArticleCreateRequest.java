package com.antares.blog.model.dto.article;

import lombok.Data;
import org.hibernate.validator.constraints.Length;

import javax.validation.constraints.NotBlank;
import java.io.Serializable;
import java.util.List;

@Data
public class ArticleCreateRequest implements Serializable {
    private List<Long> tags;

    /**
     * 标题
     */
    @NotBlank(message = "标题不能为空")
    private String title;
    /**
     * 文章摘要
     */
    @Length(min = 50, max = 250, message = "摘要的长度在50到250字之间")
    private String summary;

    /**
     * 缩略图
     */
    private String[] thumbnails;

    /**
     * 是否置顶（0否，1是）
     */
    private Integer isTop;

    /**
     * 是否允许评论 1是，0否
     */
    private Integer closeComment;
}
