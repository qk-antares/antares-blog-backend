package com.antares.blog.service.impl;

import cn.hutool.core.collection.CollectionUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.antares.blog.mapper.ArticleMapper;
import com.antares.blog.mapper.ArticleStarMapper;
import com.antares.blog.mapper.StarBookMapper;
import com.antares.blog.model.entity.Article;
import com.antares.blog.model.entity.ArticleStar;
import com.antares.blog.model.entity.StarBook;
import com.antares.blog.service.ArticleStarService;
import com.antares.blog.utils.RedisUtils;
import com.antares.common.exception.BusinessException;
import com.antares.common.model.enums.AppHttpCodeEnum;
import com.antares.common.model.vo.UserInfoVo;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.stream.Collectors;

import static com.antares.common.constant.RedisConstants.ARTICLE_STAR_PREFIX;
import static com.antares.common.constant.RedisConstants.ARTICLE_STAR_SUFFIX;

/**
* @author Antares
* @description 针对表【article_star】的数据库操作Service实现
* @createDate 2023-04-20 21:31:59
*/
@Service
public class ArticleStarServiceImpl extends ServiceImpl<ArticleStarMapper, ArticleStar>
    implements ArticleStarService {
    @Resource
    private StarBookMapper starBookMapper;
    @Resource
    private ArticleMapper articleMapper;
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Resource
    private RedisUtils redisUtils;
    @Resource
    private ThreadPoolExecutor threadPoolExecutor;

    @Override
    @Transactional
    public Integer starBlog(Long id, List<Long> bookIds, HttpServletRequest request) {
        UserInfoVo currentUser = redisUtils.getCurrentUserWithValidation(request);

        String cacheKey = ARTICLE_STAR_PREFIX + id + ARTICLE_STAR_SUFFIX;
        SetOperations<String, String> operations = stringRedisTemplate.opsForSet();
        Boolean isStared = operations.isMember(cacheKey, currentUser.getUid().toString());
        //1. 取消收藏
        if(isStared && CollectionUtils.isEmpty(bookIds)){
            cancelStar(id, currentUser, cacheKey, operations);
            return 0;
        } else {
            //2. 新收藏或更改收藏
            //2.1 校验收藏夹参数
            //首先将所有收藏夹查出来
            List<StarBook> starBooks = starBookMapper.selectBatchIds(bookIds);
            //2.1.1 某个收藏夹不存在
            if(starBooks.size() != bookIds.size()){
                throw new BusinessException(AppHttpCodeEnum.NOT_EXIST);
            }
            //2.1.2 接着检查这些收藏夹是否都属于这个用户
            for (StarBook starBook : starBooks) {
                //某个收藏夹不属于当前用户
                if(!starBook.getCreateBy().equals(currentUser.getUid())){
                    throw new BusinessException(AppHttpCodeEnum.NO_AUTH);
                }
            }

            // 更改收藏
            if(isStared) {
                changeStar(id, bookIds, currentUser);
                return 1;
            } else {
                // 新收藏
                newStar(id, bookIds, currentUser, cacheKey, operations);
                return 2;
            }
        }
    }

    private void newStar(Long id, List<Long> bookIds, UserInfoVo currentUser, String cacheKey, SetOperations<String, String> operations) {
        CompletableFuture<Void> future0 = CompletableFuture.runAsync(() -> {
            //2.3 article表的star_count+1
            articleMapper.update(null, new LambdaUpdateWrapper<Article>()
                    .setSql("star_count = star_count + 1").eq(Article::getId, id));
        }, threadPoolExecutor);

        CompletableFuture<Void> future1 = CompletableFuture.runAsync(() -> {
            //2.4 article_star表添加记录
            List<ArticleStar> articleStars = bookIdsToArticleStars(bookIds, id, currentUser.getUid());
            saveBatch(articleStars);
        }, threadPoolExecutor);

        CompletableFuture<Void> future2 = CompletableFuture.runAsync(() -> {
            //2.5 对应的star_book count+1
            starBookMapper.update(null, new LambdaUpdateWrapper<StarBook>()
                    .setSql("count = count + 1").in(StarBook::getId, bookIds));
        }, threadPoolExecutor);

        CompletableFuture<Void> future3 = CompletableFuture.runAsync(() -> {
            //2.6 redis的set中添加用户
            operations.add(cacheKey, currentUser.getUid().toString());
        }, threadPoolExecutor);

        CompletableFuture.allOf(future0, future1, future2, future3);
    }

    private void changeStar(Long id, List<Long> bookIds, UserInfoVo currentUser) {
        //查询当前的所有收藏了该文章的收藏夹id
        Set<Long> curBookIds = lambdaQuery().select(ArticleStar::getBookId)
                .eq(ArticleStar::getUid, currentUser.getUid())
                .eq(ArticleStar::getArticleId, id).list()
                .stream().map(ArticleStar::getBookId).collect(Collectors.toSet());
        //要修改的收藏夹id
        Set<Long> updateBookIds = new HashSet<>(bookIds);

        //将要删除的：cur中有，update中没有
        Set<Long> remove = (Set<Long>) CollectionUtil.subtract(curBookIds, updateBookIds);
        //将要添加的：update中有，cur中无
        Set<Long> add = (Set<Long>) CollectionUtil.subtract(updateBookIds, curBookIds);

        CompletableFuture<Void> future0 = CompletableFuture.runAsync(() -> {
            //对于要删除的，删除article_star表中的记录，star_book中的count-1
            if (!remove.isEmpty()) {
                remove(new LambdaQueryWrapper<ArticleStar>().eq(ArticleStar::getArticleId, id).in(ArticleStar::getBookId, remove));
                starBookMapper.update(null, new LambdaUpdateWrapper<StarBook>()
                        .setSql("count = count - 1").in(StarBook::getId, remove));
            }
        }, threadPoolExecutor);

        CompletableFuture<Void> future1 = CompletableFuture.runAsync(() -> {
            //对于要添加的，插入articles_star记录，star_book中的count+1
            if (!add.isEmpty()) {
                List<ArticleStar> articleStars = bookIdsToArticleStars(add, id, currentUser.getUid());
                saveBatch(articleStars);
                starBookMapper.update(null, new LambdaUpdateWrapper<StarBook>()
                        .setSql("count = count + 1").in(StarBook::getId, add));
            }
        }, threadPoolExecutor);

        CompletableFuture.allOf(future0, future1);
    }

    private void cancelStar(Long id, UserInfoVo currentUser, String cacheKey, SetOperations<String, String> operations) {
        CompletableFuture<Void> future0 = CompletableFuture.runAsync(() -> {
            //1.1 article表的star_count-1
            articleMapper.update(null, new LambdaUpdateWrapper<Article>()
                    .setSql("star_count = star_count - 1").eq(Article::getId, id));
        }, threadPoolExecutor);

        CompletableFuture<Void> future1 = CompletableFuture.runAsync(() -> {
            //1.4 redis的set中删除用户
            operations.remove(cacheKey, currentUser.getUid().toString());
        }, threadPoolExecutor);

        List<ArticleStar> articleStars = lambdaQuery().select(ArticleStar::getId, ArticleStar::getBookId)
                .eq(ArticleStar::getUid, currentUser.getUid()).eq(ArticleStar::getArticleId, id).list();
        CompletableFuture<Void> future2 = CompletableFuture.runAsync(() -> {
            //1.2 article_star删除记录
            List<Long> ids = articleStars.stream().map(ArticleStar::getId).collect(Collectors.toList());
            removeByIds(ids);
        }, threadPoolExecutor);

        CompletableFuture<Void> future3 = CompletableFuture.runAsync(() -> {
            //1.3 star_book更新count-1
            List<Long> starBookIds = articleStars.stream().map(ArticleStar::getBookId).collect(Collectors.toList());
            starBookMapper.update(null, new LambdaUpdateWrapper<StarBook>()
                    .setSql("count = count - 1").in(StarBook::getId, starBookIds));
        }, threadPoolExecutor);

        CompletableFuture.allOf(future0, future1, future2, future3).join();
    }

    private List<ArticleStar> bookIdsToArticleStars(Collection<Long> bookIds, Long articleId, Long uid){
        List<ArticleStar> articleStars = bookIds.stream().map(bookId -> {
            ArticleStar articleStar = new ArticleStar();
            articleStar.setArticleId(articleId);
            articleStar.setBookId(bookId);
            articleStar.setUid(uid);
            return articleStar;
        }).collect(Collectors.toList());
        return articleStars;
    }
}