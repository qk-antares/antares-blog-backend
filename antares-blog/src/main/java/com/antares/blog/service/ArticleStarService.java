package com.antares.blog.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.antares.blog.model.entity.ArticleStar;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
* @author Antares
* @description 针对表【star】的数据库操作Service
* @createDate 2023-04-20 21:31:59
*/
public interface ArticleStarService extends IService<ArticleStar> {
    Integer starBlog(Long id, List<Long> bookIds, HttpServletRequest request);
}
