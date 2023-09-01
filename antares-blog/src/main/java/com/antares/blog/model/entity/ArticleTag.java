package com.antares.blog.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 文章标签表
 * @TableName article_tag
 */
@TableName(value ="article_tag")
@Data
public class ArticleTag implements Serializable {
    /**
     * 主键自增
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long parentId;

    /**
     * 被哪个用户创建
     */
    private Long createdBy;

    /**
     * 标签名
     */
    private String name;

    /**
     * 标签颜色
     */
    private String color;

    /**
     * 
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;

    /**
     * 删除标志
     */
    @TableLogic
    @TableField(select = false)
    private Integer isDelete;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}