package com.antares.blog;

import com.antares.blog.mapper.ArticleMapper;
import com.antares.blog.mapper.StarBookMapper;
import com.antares.blog.model.entity.Article;
import com.antares.blog.model.entity.StarBook;
import com.antares.blog.service.ArticleService;
import com.antares.blog.service.ArticleStarService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;
import java.util.Arrays;
import java.util.List;

@SpringBootTest
public class TestMybatis {
    @Resource
    private ArticleService articleService;
    @Resource
    private ArticleMapper articleMapper;
    @Resource
    private StarBookMapper starBookMapper;
    @Resource
    private ArticleStarService articleStarService;

    @Test
    public void test() {
        Article article = articleService.lambdaQuery().select(Article.class, item -> !item.getColumn().equals("summary")).eq(Article::getId, 2).one();
        System.out.println(article);
    }

    @Test
    public void testDelete(){
        articleMapper.deleteById(1L);
    }

    @Test
    public void testUpdate() {
        Article article = new Article();
        article.setId(1L);
        article.setTitle("hell");
        articleMapper.updateById(article);
    }

    @Test
    public void testSelectBatchList(){
        Long[] bookIds = {1L, 3L};
        List<StarBook> starBooks = starBookMapper.selectBatchIds(Arrays.asList(bookIds));
        System.out.println(starBooks);
    }

    @Test
    public void testTransactional(){
    }
}
