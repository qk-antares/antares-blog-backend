package org.example.antares.blog.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.example.antares.blog.model.entity.ArticleTag;
import org.example.antares.blog.model.entity.ArticleTagRelation;

import java.util.List;

/**
* @author Antares
* @description 针对表【article_tag_relation(文章和文章标签关联表)】的数据库操作Service
* @createDate 2023-03-24 20:40:14
*/
public interface ArticleTagRelationService extends IService<ArticleTagRelation> {
    List<ArticleTag> getTagsByArticleId(Long articleId);
}
