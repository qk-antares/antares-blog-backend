package org.example.antares.search.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.example.antares.search.model.entity.ArticleTag;
import org.example.antares.search.service.ArticleTagService;
import org.example.antares.search.mapper.ArticleTagMapper;
import org.springframework.stereotype.Service;

/**
* @author Antares
* @description 针对表【article_tag(文章标签表)】的数据库操作Service实现
* @createDate 2023-05-21 10:46:46
*/
@Service
public class ArticleTagServiceImpl extends ServiceImpl<ArticleTagMapper, ArticleTag>
    implements ArticleTagService{

}




