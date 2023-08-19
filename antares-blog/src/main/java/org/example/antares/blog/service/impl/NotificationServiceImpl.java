package org.example.antares.blog.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.extern.slf4j.Slf4j;
import org.example.antares.blog.feign.UserFeignService;
import org.example.antares.blog.mapper.ArticleCommentMapper;
import org.example.antares.blog.mapper.ArticleLikeMapper;
import org.example.antares.blog.mapper.ArticleMapper;
import org.example.antares.blog.model.dto.notification.NotificationQueryRequest;
import org.example.antares.blog.model.entity.Article;
import org.example.antares.blog.model.entity.ArticleComment;
import org.example.antares.blog.model.entity.ArticleLike;
import org.example.antares.blog.model.vo.notification.CommentNotificationVo;
import org.example.antares.blog.model.vo.notification.LikeNotificationVo;
import org.example.antares.blog.model.vo.notification.NotificationCountVo;
import org.example.antares.blog.service.NotificationService;
import org.example.antares.blog.utils.RedisUtils;
import org.example.antares.common.constant.RedisConstants;
import org.example.antares.common.model.dto.UsernameAndAvtarDto;
import org.example.antares.common.model.vo.UserInfoVo;
import org.example.antares.common.utils.BeanCopyUtils;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.stream.Collectors;

/**
 * @author Antares
 * @description 针对表【notification】的数据库操作Service实现
 * @createDate 2023-05-18 16:36:19
 */
@Service
@Slf4j
public class NotificationServiceImpl implements NotificationService {
    @Resource
    private ArticleMapper articleMapper;
    @Resource
    private ArticleCommentMapper articleCommentMapper;
    @Resource
    private ArticleLikeMapper articleLikeMapper;
    @Resource
    private UserFeignService userFeignService;
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Resource
    private RedisUtils redisUtils;
    @Resource
    private ThreadPoolExecutor threadPoolExecutor;

    @Override
    public NotificationCountVo count(HttpServletRequest request) {
        UserInfoVo currentUser = redisUtils.getCurrentUserWithValidation(request);
        String cacheKeyPrefix = RedisConstants.NOTIFICATION_PREFIX + currentUser.getUid();
        List<String> keys = Arrays.asList(
                cacheKeyPrefix + RedisConstants.LIKE_NOTIFICATION_SUFFIX,
                cacheKeyPrefix + RedisConstants.COMMENT_NOTIFICATION_SUFFIX,
                cacheKeyPrefix + RedisConstants.MSG_NOTIFICATION_SUFFIX,
                cacheKeyPrefix + RedisConstants.NOTICE_NOTIFICATION_SUFFIX
        );

        List<Object> results = stringRedisTemplate.executePipelined((RedisCallback<List<Object>>) connection -> {
            for (String key : keys) {
                connection.get(key.getBytes());
            }
            return null;
        });

        Integer likeCount = results.get(0) == null ? 0 : Integer.parseInt(results.get(0).toString());
        Integer commentCount = results.get(1) == null ? 0 : Integer.parseInt(results.get(1).toString());
        Integer msgCount = results.get(2) == null ? 0 : Integer.parseInt(results.get(2).toString());
        Integer noticeCount = results.get(3) == null ? 0 : Integer.parseInt(results.get(3).toString());

        return new NotificationCountVo(likeCount, commentCount, msgCount, noticeCount);
    }

    @Override
    public Page<LikeNotificationVo> listLikeNotificationByPage(NotificationQueryRequest notificationQueryRequest, HttpServletRequest request) {
        UserInfoVo currentUser = redisUtils.getCurrentUserWithValidation(request);
        int pageNum = notificationQueryRequest.getPageNum();
        int pageSize = notificationQueryRequest.getPageSize();

        //1. 首先查询该用户的所有文章的id
        List<Long> articleIds = articleMapper.selectList(new LambdaQueryWrapper<Article>().select(Article::getId)
                .eq(Article::getCreatedBy, currentUser.getUid())).stream()
                .map(Article::getId).collect(Collectors.toList());

        Page<LikeNotificationVo> page = new Page<>(pageNum, pageSize);
        if(articleIds.isEmpty()){
            page.setRecords(new ArrayList<>());
            return page;
        }
        //2. 分页查询这些文章的点赞记录（以createTime排序）
        Page<ArticleLike> articleLikePage = articleLikeMapper.selectPage(
                new Page<>(pageNum, pageSize),
                new QueryWrapper<ArticleLike>()
                        .ne("uid", currentUser.getUid())
                        .in("article_id", articleIds)
                        .orderBy(true, false, "create_time"));
        if(articleLikePage.getRecords().isEmpty()){
            page.setRecords(new ArrayList<>());
            return page;
        }

        //3. 转换为vos
        //3.1 查询点赞用户的信息
        CompletableFuture<Map<Long, UsernameAndAvtarDto>> userMapFuture = CompletableFuture.supplyAsync(() -> {
            Set<Long> uids = articleLikePage.getRecords().stream().map(ArticleLike::getUid).collect(Collectors.toSet());
            return userFeignService.getUsernameAndAvatarByUids(uids).stream()
                    .collect(Collectors.toMap(UsernameAndAvtarDto::getUid, dto -> dto));
        }, threadPoolExecutor);

        //3.2 查询被点赞的文章信息
        CompletableFuture<Map<Long, Article>> articleMapFuture = CompletableFuture.supplyAsync(() -> {
            Set<Long> queryArticleIds = articleLikePage.getRecords().stream().map(ArticleLike::getArticleId).collect(Collectors.toSet());
            return articleMapper.selectList(new LambdaQueryWrapper<Article>()
                            .select(Article::getId, Article::getTitle, Article::getSummary)
                            .in(Article::getId, queryArticleIds)).stream()
                    .collect(Collectors.toMap(Article::getId, article -> article));
        }, threadPoolExecutor);

        //3.3 信息拼装
        CompletableFuture<List<LikeNotificationVo>> vosFuture = userMapFuture.thenCombine(articleMapFuture, (userMap, articleMap) -> {
            List<LikeNotificationVo> vos = articleLikePage.getRecords().stream().map(articleLike -> {
                LikeNotificationVo vo = BeanCopyUtils.copyBean(articleLike, LikeNotificationVo.class);
                UsernameAndAvtarDto dto = userMap.get(articleLike.getUid());
                vo.setFromUid(dto.getUid());
                vo.setAvatar(dto.getAvatar());
                vo.setFromUsername(dto.getUsername());
                Article article = articleMap.get(articleLike.getArticleId());
                vo.setTitle(article.getTitle());
                vo.setSummary(article.getSummary());
                return vo;
            }).collect(Collectors.toList());
            return vos;
        });

        vosFuture.thenAccept(page::setRecords).join();
        return page;
    }

    @Override
    public Page<CommentNotificationVo> listCommentNotificationByPage(NotificationQueryRequest notificationQueryRequest, HttpServletRequest request) {
        UserInfoVo currentUser = redisUtils.getCurrentUserWithValidation(request);
        int pageNum = notificationQueryRequest.getPageNum();
        int pageSize = notificationQueryRequest.getPageSize();

        Page<CommentNotificationVo> page = new Page<>(pageNum, pageSize);

        //1. 分页查询用户的被评论记录（以createTime排序）
        Page<ArticleComment> articleCommentPage = articleCommentMapper.selectPage(
                new Page<>(pageNum, pageSize),
                new QueryWrapper<ArticleComment>()
                        .select("id", "to_comment_id", "from_uid", "content", "article_id", "create_time")
                        .ne("from_uid", currentUser.getUid())
                        .eq("to_uid", currentUser.getUid())
                        .orderBy(true, false, "create_time"));

        if(articleCommentPage.getRecords().isEmpty()){
            page.setRecords(new ArrayList<>());
            return page;
        }

        //2. 转换为vos
        //2.1 查询原始评论信息
        CompletableFuture<Map<Long, ArticleComment>> commentMapFuture = CompletableFuture.supplyAsync(() -> {
            Set<Long> commentIds = articleCommentPage.getRecords().stream().map(ArticleComment::getToCommentId).collect(Collectors.toSet());
            return articleCommentMapper.selectList(new LambdaQueryWrapper<ArticleComment>()
                            .select(ArticleComment::getId, ArticleComment::getContent)
                            .in(ArticleComment::getId, commentIds)).stream()
                    .collect(Collectors.toMap(ArticleComment::getId, articleComment -> articleComment));
        }, threadPoolExecutor);

        //2.2 查询用户信息
        CompletableFuture<Map<Long, UsernameAndAvtarDto>> userMapFuture = CompletableFuture.supplyAsync(() -> {
            Set<Long> uids = articleCommentPage.getRecords().stream().map(ArticleComment::getFromUid).collect(Collectors.toSet());
            return userFeignService.getUsernameAndAvatarByUids(uids).stream()
                    .collect(Collectors.toMap(UsernameAndAvtarDto::getUid, dto -> dto));
        }, threadPoolExecutor);

        //2.3 查询文章信息
        CompletableFuture<Map<Long, Article>> articleMapFuture = CompletableFuture.supplyAsync(() -> {
            List<Long> articleIds = articleCommentPage.getRecords().stream().map(ArticleComment::getArticleId).collect(Collectors.toList());
            return articleMapper.selectList(new LambdaQueryWrapper<Article>()
                            .select(Article::getId, Article::getTitle, Article::getSummary)
                            .in(Article::getId, articleIds)).stream()
                    .collect(Collectors.toMap(Article::getId, article -> article));
        }, threadPoolExecutor);

        //3. 信息拼装
        CompletableFuture<List<CommentNotificationVo>> vosFuture = CompletableFuture.allOf(commentMapFuture, userMapFuture, articleMapFuture)
            .thenApply((Void) -> {
                Map<Long, ArticleComment> commentMap = commentMapFuture.join();
                Map<Long, UsernameAndAvtarDto> userMap = userMapFuture.join();
                Map<Long, Article> articleMap = articleMapFuture.join();

                List<CommentNotificationVo> vos = articleCommentPage.getRecords().stream().map(articleComment -> {
                    CommentNotificationVo vo = BeanCopyUtils.copyBean(articleComment, CommentNotificationVo.class);
                    if(articleComment.getToCommentId() != null){
                        vo.setFromContent(commentMap.get(articleComment.getToCommentId()).getContent());
                    }
                    UsernameAndAvtarDto dto = userMap.get(articleComment.getFromUid());
                    vo.setFromUid(dto.getUid());
                    vo.setAvatar(dto.getAvatar());
                    vo.setFromUsername(dto.getUsername());
                    Article article = articleMap.get(articleComment.getArticleId());
                    vo.setTitle(article.getTitle());
                    vo.setSummary(article.getSummary());
                    return vo;
                }).collect(Collectors.toList());

                return vos;
            });
        vosFuture.thenAccept(page::setRecords).join();
        return page;
    }

    @Override
    public void clearNotification(String type, HttpServletRequest request) {
        UserInfoVo currentUser = redisUtils.getCurrentUserWithValidation(request);
        String cacheKeyPrefix = RedisConstants.NOTIFICATION_PREFIX + currentUser.getUid();
        if(type.equals("all")){
            String[] keys = {cacheKeyPrefix + RedisConstants.LIKE_NOTIFICATION_SUFFIX,
                    cacheKeyPrefix + RedisConstants.COMMENT_NOTIFICATION_SUFFIX};
            stringRedisTemplate.delete(Arrays.asList(keys));
        }
        switch (type) {
            case "like": stringRedisTemplate.delete(cacheKeyPrefix + RedisConstants.LIKE_NOTIFICATION_SUFFIX);break;
            case "comment": stringRedisTemplate.delete(cacheKeyPrefix + RedisConstants.COMMENT_NOTIFICATION_SUFFIX);break;
            case "chat":
                stringRedisTemplate.delete(cacheKeyPrefix + RedisConstants.MSG_NOTIFICATION_SUFFIX);
                userFeignService.clearConversationUnread(currentUser.getUid());
                break;
            case "notice": stringRedisTemplate.delete(cacheKeyPrefix + RedisConstants.NOTICE_NOTIFICATION_SUFFIX);break;
        }
    }
}