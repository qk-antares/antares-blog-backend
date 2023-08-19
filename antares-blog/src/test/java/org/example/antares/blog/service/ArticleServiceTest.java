package org.example.antares.blog.service;

import org.example.antares.blog.model.entity.Article;
import org.example.antares.blog.model.vo.article.ArticleVo;
import org.example.antares.blog.service.impl.ArticleServiceImpl;
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
