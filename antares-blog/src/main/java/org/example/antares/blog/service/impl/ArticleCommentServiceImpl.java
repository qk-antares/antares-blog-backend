package org.example.antares.blog.service.impl;

import cn.hutool.core.collection.CollectionUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.example.antares.blog.feign.UserFeignService;
import org.example.antares.blog.mapper.ArticleCommentMapper;
import org.example.antares.blog.mapper.ArticleMapper;
import org.example.antares.blog.model.dto.comment.PostCommentRequest;
import org.example.antares.blog.model.vo.comment.ChildrenCommentVo;
import org.example.antares.blog.model.vo.comment.RootCommentVo;
import org.example.antares.blog.model.entity.Article;
import org.example.antares.blog.model.entity.ArticleComment;
import org.example.antares.blog.service.ArticleCommentService;
import org.example.antares.blog.utils.RedisUtils;
import org.example.antares.common.constant.RedisConstants;
import org.example.antares.common.exception.BusinessException;
import org.example.antares.common.model.dto.UsernameAndAvtarDto;
import org.example.antares.common.model.enums.AppHttpCodeEnum;
import org.example.antares.common.model.vo.UserInfoVo;
import org.example.antares.common.utils.BeanCopyUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.stream.Collectors;

import static org.example.antares.common.constant.RedisConstants.ARTICLE_COMMENT_PREFIX;
import static org.example.antares.common.constant.RedisConstants.ARTICLE_COMMENT_SUFFIX;

/**
* @author Antares
* @description 针对表【article_comment】的数据库操作Service实现
* @createDate 2023-04-20 21:31:59
*/
@Service
public class ArticleCommentServiceImpl extends ServiceImpl<ArticleCommentMapper, ArticleComment>
    implements ArticleCommentService{
    @Resource
    private ArticleMapper articleMapper;
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Resource
    private UserFeignService userFeignService;
    @Resource
    private RedisUtils redisUtils;
    @Resource
    private ThreadPoolExecutor threadPoolExecutor;

    @Override
    @Transactional
    public void publishComment(PostCommentRequest postCommentRequest, HttpServletRequest request) {
        UserInfoVo currentUser = redisUtils.getCurrentUserWithValidation(request);

        //1.首先要查询文章是否存在
        Article article = articleMapper.selectOne(new LambdaQueryWrapper<Article>()
                .select(Article::getCreatedBy).eq(Article::getId, postCommentRequest.getArticleId()));
        if(article == null){
            throw new BusinessException(AppHttpCodeEnum.NOT_EXIST);
        }

        ArticleComment comment = BeanCopyUtils.copyBean(postCommentRequest, ArticleComment.class);
        CompletableFuture<Void> saveFuture = CompletableFuture.runAsync(() -> {
            //2.保存评论
            if (postCommentRequest.getToUid() == null) {
                comment.setToUid(article.getCreatedBy());
            }
            comment.setFromUid(currentUser.getUid());
            save(comment);
        }, threadPoolExecutor);

        CompletableFuture<Void> countFuture = CompletableFuture.runAsync(() -> {
            //3.article表的comment_count+1
            articleMapper.update(null, new LambdaUpdateWrapper<Article>()
                    .setSql("comment_count = comment_count + 1").eq(Article::getId, postCommentRequest.getArticleId()));
        }, threadPoolExecutor);

        //todo: pipeline优化
        CompletableFuture<Void> cacheFuture = CompletableFuture.runAsync(() -> {
            //4.增加redis中的计数
            String commentCacheKey = ARTICLE_COMMENT_PREFIX + postCommentRequest.getArticleId() + ARTICLE_COMMENT_SUFFIX;
            stringRedisTemplate.opsForValue().increment(commentCacheKey);
        }, threadPoolExecutor);

        CompletableFuture<Void> notificationFuture = CompletableFuture.runAsync(() -> {
            //5.增加redis中的消息通知
            Long targetUid;
            if(postCommentRequest.getRootId().equals(-1L)){
                //评论是根评论的，查询文章作者是谁
                targetUid = articleMapper.selectOne(new LambdaQueryWrapper<Article>()
                        .select(Article::getCreatedBy).eq(Article::getId, postCommentRequest.getArticleId())).getCreatedBy();
            } else {
                //评论是回复给某个用户的
                targetUid = postCommentRequest.getToUid();
            }
            //为其添加一条评论通知
            if(!targetUid.equals(currentUser.getUid())){
                String commentMsgCacheKey = RedisConstants.NOTIFICATION_PREFIX + targetUid + RedisConstants.COMMENT_NOTIFICATION_SUFFIX;
                stringRedisTemplate.opsForValue().increment(commentMsgCacheKey);
            }
        }, threadPoolExecutor);

        CompletableFuture.allOf(saveFuture, countFuture, cacheFuture, notificationFuture).join();
    }

    @Override
    public List<RootCommentVo> getRootCommentsOfArticle(Long id) {
        List<ArticleComment> rootComments = lambdaQuery().eq(ArticleComment::getArticleId, id)
                .eq(ArticleComment::getRootId, -1).list();
        if(rootComments.isEmpty()){
            return new ArrayList<>();
        }
        //查询发表用户的信息
        Set<Long> uids = rootComments.stream().map(ArticleComment::getFromUid).collect(Collectors.toSet());
        List<UsernameAndAvtarDto> dtos = userFeignService.getUsernameAndAvatarByUids(uids);
        Map<Long, UsernameAndAvtarDto> dtoMap = dtos.stream().collect(Collectors.toMap(UsernameAndAvtarDto::getUid, dto -> dto));

        return rootComments.stream().map(rootComment -> {
            RootCommentVo vo = BeanCopyUtils.copyBean(rootComment, RootCommentVo.class);
            vo.setFromUsername(dtoMap.get(vo.getFromUid()).getUsername());
            vo.setAvatar(dtoMap.get(vo.getFromUid()).getAvatar());
            //查询子评论
            Integer count = lambdaQuery().eq(ArticleComment::getRootId, rootComment.getId()).count();
            vo.setReplyCount(count);
            return vo;
        }).collect(Collectors.toList());
    }

    @Override
    public void likeComment(Long id) {
    }

    @Override
    public List<ChildrenCommentVo> getChildrenOfRoot(Long id) {
        List<ArticleComment> childrenComments = lambdaQuery().eq(ArticleComment::getRootId, id).list();
        if(childrenComments.isEmpty()){
            return new ArrayList<>();
        }
        Set<Long> fromUids = childrenComments.stream().map(ArticleComment::getFromUid).collect(Collectors.toSet());
        Set<Long> toUids = childrenComments.stream().map(ArticleComment::getToUid).collect(Collectors.toSet());
        Set<Long> uids = (Set<Long>) CollectionUtil.addAll(fromUids, toUids);
        //远程调用获取用户信息
        List<UsernameAndAvtarDto> dtos = userFeignService.getUsernameAndAvatarByUids(uids);
        Map<Long, UsernameAndAvtarDto> dtoMap = dtos.stream().collect(Collectors.toMap(UsernameAndAvtarDto::getUid, dto -> dto));
        return childrenComments.stream().map(childrenComment -> {
            ChildrenCommentVo vo = BeanCopyUtils.copyBean(childrenComment, ChildrenCommentVo.class);
            vo.setFromUsername(dtoMap.get(vo.getFromUid()).getUsername());
            vo.setAvatar(dtoMap.get(vo.getFromUid()).getAvatar());
            if(vo.getToUid() != null){
                vo.setToUsername(dtoMap.get(vo.getToUid()).getUsername());
            }
            return vo;
        }).collect(Collectors.toList());
    }
}




