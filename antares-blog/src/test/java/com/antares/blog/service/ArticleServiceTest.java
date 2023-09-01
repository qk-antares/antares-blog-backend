package com.antares.blog.service;

import com.antares.blog.model.entity.Article;
import com.antares.blog.model.vo.article.ArticleVo;
import com.antares.blog.service.impl.ArticleServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;

@SpringBootTest
public class ArticleServiceTest {
    @Resource
    private ArticleServiceImpl articleServiceImpl;

    @Test
    void testArticleToVo(){
        Article byId = articleServiceImpl.getById(1L);
        ArticleVo vo = articleServiceImpl.articleToVo(byId, 100L, false);
        System.out.println(vo);
    }
}
