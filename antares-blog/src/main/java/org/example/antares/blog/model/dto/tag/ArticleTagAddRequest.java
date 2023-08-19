package org.example.antares.blog.model.dto.tag;

import lombok.Data;

@Data
public class ArticleTagAddRequest {
    private Long parentId;
    private String name;
}
