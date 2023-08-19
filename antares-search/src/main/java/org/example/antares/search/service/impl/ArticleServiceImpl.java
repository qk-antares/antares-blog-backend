package org.example.antares.search.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.example.antares.search.mapper.ArticleMapper;
import org.example.antares.search.model.entity.Article;
import org.example.antares.search.service.ArticleService;
import org.springframework.stereotype.Service;

/**
* @author Antares
* @description 针对表【article(文章表)】的数据库操作Service实现
* @createDate 2023-05-21 10:42:23
*/
@Service
public class ArticleServiceImpl extends ServiceImpl<ArticleMapper, Article>
    implements ArticleService{
}




