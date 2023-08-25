package org.example.antares.blog.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.example.antares.blog.model.entity.ArticleLike;
import org.example.antares.common.model.response.R;

import javax.servlet.http.HttpServletRequest;

/**
* @author Antares
* @description 针对表【article_like】的数据库操作Service
* @createDate 2023-05-09 20:05:25
*/
public interface ArticleLikeService extends IService<ArticleLike> {

    void likeBlog(Long id, HttpServletRequest request);

    void likeBlog(Long uid, Long articleId, Long authorId);
}
