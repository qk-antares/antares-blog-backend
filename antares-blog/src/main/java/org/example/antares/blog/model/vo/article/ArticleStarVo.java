package org.example.antares.blog.model.vo.article;

import org.example.antares.blog.model.entity.ArticleTag;
import org.example.antares.common.model.dto.UsernameAndAvtarDto;

import java.util.Date;
import java.util.List;

public class ArticleStarVo {
    private Long id;

    private String title;
    private String summary;
    private String content;

    private Integer prime;//精华
    private Integer isTop;//个人置顶
    private Integer isGlobalTop;//全局置顶

    private Integer status;//状态

    private Integer closeComment;//关闭评论

    private Long viewCount;//浏览数
    private Long likeCount;//点赞数
    private Long starCount;//收藏数
    private Long commentCount;//评论数

    private List<String> thumbnails;//缩略图

    private Long createdBy;//作者uid

    private Date createTime;//创建时间
    private Date updateTime;//更新时间

    private UsernameAndAvtarDto author;//作者
    private List<ArticleTag> tags;//标签
}
