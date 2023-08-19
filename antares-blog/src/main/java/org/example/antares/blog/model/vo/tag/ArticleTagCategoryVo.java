package org.example.antares.blog.model.vo.tag;

import lombok.Data;
import org.example.antares.blog.model.dto.tag.ArticleTagAddRequest;

import java.util.List;

@Data
public class ArticleTagCategoryVo {
    private Long id;

    private String name;

    private List<ArticleTagVo> tags;
}
