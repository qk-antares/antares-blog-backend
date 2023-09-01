package com.antares.blog.service;

import com.antares.blog.mapper.ArticleMapper;
import com.antares.blog.model.entity.Article;
import lombok.extern.slf4j.Slf4j;
import com.antares.common.constant.RedisConstants;
import com.antares.common.utils.ObjectMapperUtils;
import org.junit.jupiter.api.Test;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;

import javax.annotation.Resource;
import java.util.List;
import java.util.concurrent.TimeUnit;

@SpringBootTest
@Slf4j
public class JobTest {
    @Resource
    private ArticleMapper articleMapper;
    @Resource
    private ArticleService articleService;
    @Resource
    private RedissonClient redissonClient;
    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Test
    void testStatistic(){
        RLock lock = redissonClient.getLock(RedisConstants.ASYNC_SCORE_LOCK);
        try {
            if (lock.tryLock(0, 30, TimeUnit.SECONDS)) {
                long start = System.currentTimeMillis();

                //首先获取所有文章(只获取id，score，hot)
                List<Article> articles = articleService.lambdaQuery()
                        .select(Article::getViewCount, Article::getLikeCount, Article::getStarCount, Article::getCommentCount,
                                Article::getId, Article::getScore, Article::getHot).list();

                for (Article article : articles) {
                    //score=浏览量+点赞量*8+收藏量*16+评论量*8
                    int newScore = (int) (article.getViewCount() + (article.getLikeCount() << 3) + (article.getStarCount() << 4) + (article.getCommentCount() << 3));
                    //hot=hot*0.9+scoreNew-scoreOld
                    int newHot = (int) (article.getHot() * 0.9 + newScore - article.getScore());
                    article.setScore(newScore);
                    article.setHot(newHot);
                }
                articleService.updateBatchById(articles);

                //将hot前10的文章缓存起来
                List<Article> articleList = articleService.lambdaQuery().orderBy(true, false, Article::getHot).last("limit 10").list();
                stringRedisTemplate.opsForValue().set(RedisConstants.HOT_ARTICLES, ObjectMapperUtils.writeValueAsString(articleList));

                long end = System.currentTimeMillis();
                log.info("更新score和hot任务总耗时：", end - start);
            }
        } catch (InterruptedException e) {
            log.error(e.getMessage());
        } finally {
            //只能释放自己的锁
            if(lock.isHeldByCurrentThread()){
                lock.unlock();
            }
        }
    }

    @Test
    void cacheTop10(){
    }

    /**
     * 更新文章的score和hot，是用stream.map还是for，stream.foreach能修改元素吗
     */
    @Test
    void testStreamAndFor(){
        //首先获取所有文章(只获取id，score，hot)
        List<Article> articles = articleService.lambdaQuery()
                .select(Article::getViewCount, Article::getLikeCount, Article::getStarCount,
                        Article::getCommentCount, Article::getId, Article::getScore, Article::getHot)
                .gt(Article::getId, 1700)
                .list();

        for (Article article : articles) {
            //score=浏览量+点赞量*8+收藏量*16+评论量*8
            int newScore = (int) (article.getViewCount() + (article.getLikeCount() << 3) + (article.getStarCount() << 4) + (article.getCommentCount() << 3));
            //hot=hot*0.9+scoreNew-scoreOld
            int newHot = (int) (article.getHot() * 0.9 + newScore - article.getScore());
            article.setScore(newScore);
            article.setHot(newHot);
        }
        articleService.updateBatchById(articles);
    }
}
