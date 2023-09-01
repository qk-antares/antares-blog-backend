package com.antares.search.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.util.Date;
import lombok.Data;

/**
 * 文章表
 * @TableName article
 */
@TableName(value ="article")
@Data
public class Article implements Serializable {
    /**
     * 
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 标题
     */
    private String title;

    /**
     * 文章摘要
     */
    private String summary;

    /**
     * 文章内容
     */
    private String content;

    /**
     * 是否精华
     */
    private Integer prime;

    /**
     * 是否置顶（0否，1是）
     */
    private Integer isTop;

    /**
     * 是否全局置顶
     */
    private Integer isGlobalTop;

    /**
     * 状态（1已发布，0草稿）
     */
    private Integer status;

    /**
     * 是否允许评论 1是，0否
     */
    private Integer closeComment;

    /**
     * 访问量
     */
    private Long viewCount;

    /**
     * 
     */
    private Long likeCount;

    /**
     * 
     */
    private Long starCount;

    /**
     * 
     */
    private Long commentCount;

    /**
     * 缩略图1
     */
    private String thumbnail1;

    /**
     * 缩略图2
     */
    private String thumbnail2;

    /**
     * 缩略图3
     */
    private String thumbnail3;

    /**
     * 
     */
    private Long createdBy;

    /**
     * 文章的总分数，是根据浏览、点赞、收藏、评论数计算得来的
     */
    private Integer score;

    /**
     * 文章的热度，有一个定时任务，每小时计算增加的score
     */
    private Integer hot;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;

    /**
     * 删除标志
     */
    private Integer isDelete;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}