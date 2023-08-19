package org.example.antares.search.model.vo;

import lombok.Data;

import java.util.List;

@Data
public class CsdnBlogVo {
    private String title;
    private String articleUrl;
    private String summary;
    private String viewCount;
    private String likeCount;
    private String commentCount;
    private String author;
    private String authorUrl;
    private String createdTime;
    private List<String> picList;
}
