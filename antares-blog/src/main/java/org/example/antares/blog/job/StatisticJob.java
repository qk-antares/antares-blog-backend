package org.example.antares.blog.job;

import lombok.extern.slf4j.Slf4j;
import org.example.antares.blog.model.entity.Article;
import org.example.antares.blog.service.ArticleService;
import org.example.antares.common.constant.RedisConstants;
import org.example.antares.common.utils.ObjectMapperUtils;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @author Antares
 */
@Component
@Slf4j
public class StatisticJob {
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Resource
    private ArticleService articleService;
    @Resource
    private RedissonClient redissonClient;
    @Resource
    private ThreadPoolExecutor threadPoolExecutor;

    /**
     * 每10分钟执行一次。将浏览量同步到mysql。定时任务使用分布式锁防止多次执行
     */
    @Scheduled(cron = "0 0/10 * * * ? ")
    public void updateCountsJob(){
        RLock lock = redissonClient.getLock(RedisConstants.ASYNC_COUNT_LOCK);
        try {
            if (lock.tryLock(0, 30, TimeUnit.SECONDS)) {
                String viewPattern = "article:id:*:view";
                Set<String> viewKeys = stringRedisTemplate.keys(viewPattern);
                ArrayList<Article> updates = new ArrayList<>();
                if (viewKeys != null && !viewKeys.isEmpty()) {
                    for (String key : viewKeys) {
                        Long id = Long.valueOf(key.split(":")[2]);
                        Integer viewCount = Integer.valueOf(stringRedisTemplate.opsForValue().get(key));
                        Article tmp = new Article();
                        tmp.setId(id);
                        tmp.setViewCount(viewCount.longValue());
                        updates.add(tmp);
                    }
                    articleService.updateBatchById(updates);
                }
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

    /**
     * 更新文章的score和hot值，每小时执行1次
     */
    @Scheduled(cron = "0 0 0/1 * * ? ")
    public void updateScoreAndHotJob(){
        RLock lock = redissonClient.getLock(RedisConstants.ASYNC_SCORE_LOCK);
        try {
            if (lock.tryLock(0, 30, TimeUnit.SECONDS)) {
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
                List<Article> hots = articleService.lambdaQuery().orderBy(true, false, Article::getHot).last("limit 8").list();
                stringRedisTemplate.opsForValue().set(RedisConstants.HOT_ARTICLES, ObjectMapperUtils.writeValueAsString(hots));
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
}