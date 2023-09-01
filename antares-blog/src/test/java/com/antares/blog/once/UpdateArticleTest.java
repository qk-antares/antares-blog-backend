package com.antares.blog.once;

import com.antares.blog.model.entity.Article;
import com.antares.blog.service.ArticleService;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.apache.commons.lang3.RandomUtils;
import org.example.antares.common.constant.RedisConstants;
import org.example.antares.common.utils.CrawlerUtils;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;

@SpringBootTest
public class UpdateArticleTest {
    @Resource
    private ArticleService articleService;
    @Resource
    private ThreadPoolExecutor threadPoolExecutor;
    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Test
    public void updateCreateTimeAndUpdateTime(){
        List<Article> articleList = articleService.lambdaQuery().select(Article::getId, Article::getCreateTime, Article::getUpdateTime)
                .list();
        for (Article article : articleList) {
            article.setCreateTime(new Date(article.getCreateTime().getTime() - 60000L * RandomUtils.nextInt(0, 28801)));
            article.setUpdateTime(new Date(article.getCreateTime().getTime() + 60000L * RandomUtils.nextInt(5, 1441)));
        }

        articleService.updateBatchById(articleList);
    }

    @Test
    public void updateArticleThumbnail() {
        List<Article> articleList = articleService.lambdaQuery()
                .select(Article::getId, Article::getTitle)
                .gt(Article::getId, 10)
                .list();

        ArrayList<CompletableFuture<Void>> futures = new ArrayList<>();
        for (Article article : articleList) {
            try {
                List<String> pictures = CrawlerUtils.fetchPicturesByKeyword(article.getTitle().replace("|", ""), 1, null);
                int n = Math.min(RandomUtils.nextInt(0, 4), pictures.size());
                switch (n) {
                    case 0:
                        LambdaUpdateWrapper<Article> updateWrapper = new LambdaUpdateWrapper<Article>()
                                .eq(Article::getId, article.getId())
                                .set(Article::getThumbnail1, null);
                        articleService.update(updateWrapper);
                        break;
                    case 1:
                        article.setThumbnail1(pictures.get(0));
                        articleService.updateById(article);
                        break;
                    case 2:
                        article.setThumbnail2(pictures.get(0));
                        article.setThumbnail1(pictures.get(1));
                        articleService.updateById(article);
                        break;
                    case 3:
                        article.setThumbnail2(pictures.get(0));
                        article.setThumbnail1(pictures.get(1));
                        article.setThumbnail1(pictures.get(3));
                        articleService.updateById(article);
                        break;
                }
            } catch (IOException e) {

            }
        }
    }

    @Test
    public void loadViewCountToRedis(){
        List<Article> articles = articleService.lambdaQuery().select(Article::getId, Article::getViewCount).list();
        for (Article article : articles) {
            String cacheKey = RedisConstants.ARTICLE_VIEW_PREFIX + article.getId() + RedisConstants.ARTICLE_VIEW_SUFFIX;
            stringRedisTemplate.opsForValue().set(cacheKey, article.getViewCount().toString());
        }
    }
}
