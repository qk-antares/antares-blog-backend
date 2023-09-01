package com.antares.blog.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.antares.blog.mapper.ArticleLikeMapper;
import com.antares.blog.mapper.ArticleMapper;
import com.antares.blog.model.entity.Article;
import com.antares.blog.model.entity.ArticleLike;
import com.antares.blog.service.ArticleLikeService;
import com.antares.blog.utils.RedisUtils;
import com.antares.common.exception.BusinessException;
import com.antares.common.model.enums.AppHttpCodeEnum;
import com.antares.common.model.vo.UserInfoVo;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;

import static com.antares.common.constant.RedisConstants.*;

/**
* @author Antares
* @description 针对表【article_like】的数据库操作Service实现
* @createDate 2023-05-09 20:05:25
*/
@Service
public class ArticleLikeServiceImpl extends ServiceImpl<ArticleLikeMapper, ArticleLike>
    implements ArticleLikeService{
    @Resource
    private ArticleMapper articleMapper;
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Resource
    private RedisUtils redisUtils;
    @Resource
    private RabbitTemplate rabbitTemplate;
    @Resource(name = "threadPoolExecutor")
    private ThreadPoolExecutor threadPoolExecutor;

    @Override
    public void likeBlog(Long id, HttpServletRequest request) {
        UserInfoVo currentUser = redisUtils.getCurrentUserWithValidation(request);
        //查询文章是否存在，只要文章存在就返回成功
        Article article = articleMapper.selectById(id);
        if(article != null){
            // 发送点赞消息到队列，最后一个参数是消息的唯一ID
            rabbitTemplate.convertAndSend("exchange.direct", "like",
                    new Long[]{currentUser.getUid(), id, article.getCreatedBy()},
                    new CorrelationData(UUID.randomUUID().toString()));
        } else {
            throw new BusinessException(AppHttpCodeEnum.NOT_EXIST);
        }
    }

    /**
     * uid点赞authorId的articleId
     * @param uid
     * @param articleId
     * @param authorId
     */
    @Transactional
    @Override
    public void likeBlog(Long uid, Long articleId, Long authorId){
        String likeCacheKey = ARTICLE_LIKE_PREFIX + articleId + ARTICLE_LIKE_SUFFIX;
        String likeMsgCacheKey = NOTIFICATION_PREFIX + authorId + LIKE_NOTIFICATION_SUFFIX;

        SetOperations<String, String> setOperations = stringRedisTemplate.opsForSet();
        ValueOperations<String, String> valueOperations = stringRedisTemplate.opsForValue();

        Boolean isLiked = setOperations.isMember(likeCacheKey, uid.toString());
        //1.已经点赞了，取消
        if(isLiked){
            CompletableFuture.runAsync(() -> {
                //1.1 article表的like_count-1
                articleMapper.update(null, new LambdaUpdateWrapper<Article>()
                        .setSql("like_count = like_count - 1").eq(Article::getId, articleId));
            }, threadPoolExecutor);

            //todo: pipeline优化
            CompletableFuture.runAsync(() -> {
                //1.2 redis的set中删除用户
                setOperations.remove(likeCacheKey, uid.toString());
            }, threadPoolExecutor);

            CompletableFuture.runAsync(() -> {
                //1.3 article_like表删除记录
                remove(new LambdaQueryWrapper<ArticleLike>()
                        .eq(ArticleLike::getUid, uid)
                        .eq(ArticleLike::getArticleId, articleId));
            }, threadPoolExecutor);

            CompletableFuture.runAsync(() -> {
                //1.4 redis中点赞消息数-1
                if(!uid.equals(authorId)){
                    valueOperations.decrement(likeMsgCacheKey);
                }
            }, threadPoolExecutor);
        } else {
            //todo: 异步编排优化
            //2. 没点赞，添加
            CompletableFuture.runAsync(() -> {
                //2.1 article表的like_count+1
                articleMapper.update(null, new LambdaUpdateWrapper<Article>()
                        .setSql("like_count = like_count + 1").eq(Article::getId, articleId));
            }, threadPoolExecutor);

            CompletableFuture.runAsync(() -> {
                //2.2 redis的set中添加用户
                setOperations.add(likeCacheKey, uid.toString());
            }, threadPoolExecutor);

            CompletableFuture.runAsync(() -> {
                //2.3 增加记录
                ArticleLike articleLike = new ArticleLike();
                articleLike.setArticleId(articleId);
                articleLike.setUid(uid);
                save(articleLike);
            }, threadPoolExecutor);

            CompletableFuture.runAsync(() -> {
                //2.4 redis中点赞消息+1
                if(!uid.equals(authorId)){
                    valueOperations.increment(likeMsgCacheKey);
                }
            }, threadPoolExecutor);
        }
    }
}




