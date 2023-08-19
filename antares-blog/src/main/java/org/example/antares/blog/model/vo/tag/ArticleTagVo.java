package org.example.antares.blog.model.vo.tag;

import lombok.Data;

@Data
public class ArticleTagVo {
    private Long id;
    private Long parentId;
    private String name;
    private String color;
}
