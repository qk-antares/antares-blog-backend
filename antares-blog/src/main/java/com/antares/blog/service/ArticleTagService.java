package com.antares.blog.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.antares.blog.model.dto.tag.ArticleTagAddRequest;
import com.antares.blog.model.entity.ArticleTag;
import com.antares.blog.model.vo.tag.ArticleTagCategoryVo;
import com.antares.blog.model.vo.tag.ArticleTagVo;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
* @author Antares
* @description 针对表【article_tag(文章标签表)】的数据库操作Service
* @createDate 2023-03-24 20:40:13
*/
public interface ArticleTagService extends IService<ArticleTag> {
    List<ArticleTagCategoryVo> getAllTags();
    ArticleTagVo addATag(ArticleTagAddRequest articleTagAddRequest, HttpServletRequest request);
}
