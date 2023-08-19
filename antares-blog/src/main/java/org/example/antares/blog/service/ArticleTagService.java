package org.example.antares.blog.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.example.antares.blog.model.dto.tag.ArticleTagAddRequest;
import org.example.antares.blog.model.entity.ArticleTag;
import org.example.antares.common.model.response.R;

import javax.servlet.http.HttpServletRequest;

/**
* @author Antares
* @description 针对表【article_tag(文章标签表)】的数据库操作Service
* @createDate 2023-03-24 20:40:13
*/
public interface ArticleTagService extends IService<ArticleTag> {
    R getAllTags();
    R addATag(ArticleTagAddRequest articleTagAddRequest, HttpServletRequest request);
}
