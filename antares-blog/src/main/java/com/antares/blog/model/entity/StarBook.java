package com.antares.blog.model.entity;

import com.baomidou.mybatisplus.annotation.*;

import java.io.Serializable;
import java.util.Date;
import lombok.Data;

/**
 * 
 * @TableName star_book
 */
@TableName(value ="star_book")
@Data
public class StarBook implements Serializable {
    @TableId
    private Long id;
    private String name;
    private Long createBy;
    private Integer count;
    private Date createTime;

    /**
     * 
     */
    private Date updateTime;

    /**
     * 
     */
    @TableLogic
    private Integer isDelete;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}