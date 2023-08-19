package org.example.antares.blog.model.dto.article;

import lombok.Data;
import org.example.antares.common.utils.PageRequest;

import java.io.Serializable;
import java.util.List;

@Data
public class ArticleQueryRequest extends PageRequest implements Serializable {
    private static final long serialVersionUID = -9074434720331090481L;
    private String keyword;

    /**
     * 查询类型，1代表查询所有，2代表查询已发布，3代表查询草稿
     */
    private int selectType;

    /**
     * 搜索指定用户的文章
     */
    private Long uid;

    /**
     * 搜索标签（必须全部包含）
     */
    private List<String> tags;

    /**
     * 仅搜索精华
     */
    private Boolean prime = false;
}
