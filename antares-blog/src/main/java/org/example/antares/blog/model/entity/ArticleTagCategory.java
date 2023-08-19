package org.example.antares.blog.model.entity;

import com.baomidou.mybatisplus.annotation.*;

import java.io.Serializable;
import java.util.Date;
import lombok.Data;

/**
 * 
 * @TableName article_tag_category
 */
@TableName(value ="article_tag_category")
@Data
public class ArticleTagCategory implements Serializable {
    /**
     * 主键自增
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 类别名
     */
    private String name;

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
    @TableLogic
    @TableField(select = false)
    private Integer isDelete;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}