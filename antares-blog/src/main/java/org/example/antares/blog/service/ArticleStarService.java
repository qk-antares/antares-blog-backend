package org.example.antares.blog.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.example.antares.blog.model.entity.ArticleStar;
import org.example.antares.common.model.response.R;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
* @author Antares
* @description 针对表【star】的数据库操作Service
* @createDate 2023-04-20 21:31:59
*/
public interface ArticleStarService extends IService<ArticleStar> {
    R starBlog(Long id, List<Long> bookIds, HttpServletRequest request);
}
