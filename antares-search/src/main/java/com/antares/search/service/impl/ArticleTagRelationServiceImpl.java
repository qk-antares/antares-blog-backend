package com.antares.search.service.impl;

import com.antares.search.mapper.ArticleTagRelationMapper;
import com.antares.search.model.entity.ArticleTagRelation;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.antares.search.service.ArticleTagRelationService;
import org.springframework.stereotype.Service;

/**
* @author Antares
* @description 针对表【article_tag_relation(文章和文章标签关联表)】的数据库操作Service实现
* @createDate 2023-05-21 10:46:46
*/
@Service
public class ArticleTagRelationServiceImpl extends ServiceImpl<ArticleTagRelationMapper, ArticleTagRelation>
    implements ArticleTagRelationService{

}




