package org.example.antares.oj.model.entity;

import com.baomidou.mybatisplus.annotation.*;

import java.io.Serializable;
import java.util.Date;
import lombok.Data;

/**
 * 帖子收藏
 * @TableName post_favour
 */
@TableName(value ="post_favour")
@Data
public class PostFavour implements Serializable {
    /**
     * id
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 帖子 id
     */
    private Long postId;

    /**
     * 创建用户 id
     */
    private Long userId;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    @TableLogic
    private Date updateTime;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}