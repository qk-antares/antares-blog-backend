package org.example.antares.blog.service.impl;

import cn.hutool.core.collection.CollectionUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.example.antares.blog.feign.UserFeignService;
import org.example.antares.blog.mapper.ArticleLikeMapper;
import org.example.antares.blog.mapper.ArticleMapper;
import org.example.antares.blog.mapper.ArticleStarMapper;
import org.example.antares.blog.model.dto.article.ArticleCreateRequest;
import org.example.antares.blog.model.dto.article.ArticleQueryRequest;
import org.example.antares.blog.model.entity.*;
import org.example.antares.blog.model.vo.article.ArticleContentVo;
import org.example.antares.blog.model.vo.article.ArticleVo;
import org.example.antares.blog.service.ArticleService;
import org.example.antares.blog.service.ArticleTagRelationService;
import org.example.antares.blog.utils.RedisUtils;
import org.example.antares.common.constant.CommonConstant;
import org.example.antares.common.constant.RedisConstants;
import org.example.antares.common.constant.SystemConstants;
import org.example.antares.common.exception.BusinessException;
import org.example.antares.common.model.dto.UsernameAndAvtarDto;
import org.example.antares.common.model.enums.AppHttpCodeEnum;
import org.example.antares.common.model.response.R;
import org.example.antares.common.model.vo.UserInfoVo;
import org.example.antares.common.utils.BeanCopyUtils;
import org.example.antares.common.utils.ObjectMapperUtils;
import org.example.antares.common.utils.SqlUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.example.antares.common.constant.RedisConstants.*;
import static org.example.antares.common.utils.ObjectMapperUtils.MAPPER;

/**
* @author Antares
* @description 针对表【article(文章表)】的数据库操作Service实现
* @createDate 2023-03-24 20:40:13
 * 文章的每次更新操作要进行updateTime的更新，这一点可以利用AOP来实现
*/
@Service
@Slf4j
public class ArticleServiceImpl extends ServiceImpl<ArticleMapper, Article>
    implements ArticleService {
    @Resource
    private UserFeignService userFeignService;
    @Resource
    private ArticleLikeMapper articleLikeMapper;
    @Resource
    private ArticleTagRelationService articleTagRelationService;
    @Resource
    private ArticleStarMapper articleStarMapper;
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Resource
    private RedisUtils redisUtils;
    @Resource(name = "threadPoolExecutor")
    private ThreadPoolExecutor threadPoolExecutor;
    @Resource(name = "childThreadPoolExecutor")
    private ThreadPoolExecutor childThreadPoolExecutor;

    @Override
    public R createDraft(ArticleCreateRequest articleCreateRequest, HttpServletRequest request) {
        //todo: 消息队列优化（能否待考究）
        UserInfoVo currentUser = redisUtils.getCurrentUserWithValidation(request);

        Article article = BeanCopyUtils.copyBean(articleCreateRequest, Article.class);
        article.setCreatedBy(currentUser.getUid());
        //设置文章内容，这里默认生成了标题
        article.setContent("# " + article.getTitle() + "\n");
        try{
            article.setThumbnail1(articleCreateRequest.getThumbnails()[0]);
            article.setThumbnail2(articleCreateRequest.getThumbnails()[1]);
            article.setThumbnail3(articleCreateRequest.getThumbnails()[2]);
        } catch (IndexOutOfBoundsException e){}
        //先把文章除标签之外的信息存进数据库
        save(article);
        //存储该文章涉及的标签
        List<ArticleTagRelation> relations = articleCreateRequest.getTags().stream().map(articleTagId -> {
            ArticleTagRelation articleTagRelation = new ArticleTagRelation();
            articleTagRelation.setArticleId(article.getId());
            articleTagRelation.setTagId(articleTagId);
            return articleTagRelation;
        }).collect(Collectors.toList());
        articleTagRelationService.saveBatch(relations);
        return R.ok(article.getId());
    }

    @Override
    public R getArticleCoverById(Long id, HttpServletRequest request) {
        //查询除了内容之外的信息
        Article article = lambdaQuery()
                .select(Article.class, item -> !item.getColumn().equals("content"))
                .eq(Article::getId, id).one();
        //文章不存在
        if(article == null){
            return R.error(AppHttpCodeEnum.NOT_EXIST);
        }

        UserInfoVo currentUser = redisUtils.getCurrentUser(request);

        //获取vo，同时还要增加浏览量
        ArticleVo vo = articleToVo(article, true);
        //将basic存进redis的同时，将浏览量、点赞数据、收藏量、评论量这些存进redis，由于要对这些属性进行单独的+1操作，所以采用hash结构
        try {
            String coverCacheKey = ARTICLE_COVER_PREFIX + id + ARTICLE_COVER_SUFFIX;
            String likeCacheKey = ARTICLE_LIKE_PREFIX + id + ARTICLE_LIKE_SUFFIX;
            String starCacheKey = ARTICLE_STAR_PREFIX + id + ARTICLE_STAR_SUFFIX;

            //Todo: （优先级极高）后期异步编排优化
            //1.将basic信息存进redis
            stringRedisTemplate.opsForValue().set(coverCacheKey,
                    MAPPER.writeValueAsString(vo), ARTICLE_TTL, TimeUnit.HOURS);
            if(currentUser != null){
                //2.查询点赞
                vo.setIsLiked(stringRedisTemplate.opsForSet()
                        .isMember(likeCacheKey, currentUser.getUid().toString()));
                //Set<String> uids = articleLikeMapper.selectList(new LambdaQueryWrapper<ArticleLike>()
                //                .select(ArticleLike::getUid).eq(ArticleLike::getArticleId, id))
                //        .stream().map(articleLike -> articleLike.getUid().toString()).collect(Collectors.toSet());
                //if(!uids.isEmpty()){
                //    stringRedisTemplate.opsForSet().add(likeCacheKey, uids.toArray(new String[0]));
                //}
                //3.查询收藏
                vo.setIsStared(stringRedisTemplate.opsForSet()
                        .isMember(starCacheKey, currentUser.getUid().toString()));
                /*
                Set<String> ownerIds = articleStarMapper.selectList(new QueryWrapper<ArticleStar>()
                                .select("DISTINCT uid").lambda().eq(ArticleStar::getArticleId, id))
                        .stream().map(articleStar -> articleStar.getUid().toString()).collect(Collectors.toSet());
                if(!CollectionUtils.isEmpty(ownerIds)){
                    stringRedisTemplate.opsForSet().add(starCacheKey, ownerIds.toArray(new String[0]));
                }
                6.设置是否点赞和收藏
                if(currentUser != null){
                    String uid = currentUser.getUid().toString();
                    vo.setIsLiked(uids.contains(uid));
                    vo.setIsStared(ownerIds.contains(uid));
                }
                */
            }
            return R.ok(vo);
        } catch (JsonProcessingException e) {
            throw new BusinessException(AppHttpCodeEnum.INTERNAL_SERVER_ERROR, "JSON转换异常");
        }
    }

    @Override
    public ArticleVo getArticleBasicById(Long id) {
        //不查询内容
        Article article = lambdaQuery()
                .select(Article.class, item -> !item.getColumn().equals("content"))
                .eq(Article::getId, id).one();

        if(article != null) {
            ArticleVo vo = articleToVo(article, false);
            //将basic信息存进redis
            String coverCacheKey = ARTICLE_COVER_PREFIX + id + ARTICLE_COVER_SUFFIX;
            try {
                stringRedisTemplate.opsForValue().set(coverCacheKey,
                        MAPPER.writeValueAsString(vo), ARTICLE_TTL, TimeUnit.HOURS);
            } catch (JsonProcessingException e) {
                throw new BusinessException(AppHttpCodeEnum.INTERNAL_SERVER_ERROR, "JSON转换异常");
            }
            return vo;
        }
        throw new BusinessException(AppHttpCodeEnum.NOT_EXIST);
    }

    @Override
    @Transactional
    public R updateBasicById(Long id, ArticleCreateRequest articleCreateRequest,
                             HttpServletRequest request) {
        //查询当前用户是否是作者
        UserInfoVo currentUser = redisUtils.getCurrentUserWithValidation(request);
        Long createdBy = lambdaQuery().select(Article::getCreatedBy).eq(Article::getId, id).one().getCreatedBy();
        if(!currentUser.getUid().equals(createdBy)){
            return R.error(AppHttpCodeEnum.NO_AUTH);
        }

        //todo: （优先级极高）异步编排优化
        //1.更新文章
        Article article = new Article();
        article.setId(id);
        BeanUtils.copyProperties(articleCreateRequest, article);

        String[] thumbnails = new String[3];
        int i = 0;
        for(;i < articleCreateRequest.getThumbnails().length;i++){
            thumbnails[i] = articleCreateRequest.getThumbnails()[i];
        }
        while (i < 3){
            thumbnails[i++] = "";
        }
        article.setThumbnail1(thumbnails[0]);
        article.setThumbnail2(thumbnails[1]);
        article.setThumbnail3(thumbnails[2]);

        article.setUpdateTime(new Date());

        updateById(article);

        //2.更新对应的标签
        //查询当前文章的标签
        Set<Long> curTagIds = articleTagRelationService.lambdaQuery().select(ArticleTagRelation::getTagId)
                .eq(ArticleTagRelation::getArticleId, id).list()
                .stream().map(articleTagRelation -> articleTagRelation.getTagId()).collect(Collectors.toSet());
        //要更新的文章标签
        HashSet<Long> updateTagIds = new HashSet<>(articleCreateRequest.getTags());

        //将要删除的标签：cur中有，update中没有
        Set<Long> remove = (Set<Long>) CollectionUtil.subtract(curTagIds, updateTagIds);
        //将要添加的标签：cur中无，update中有
        Set<Long> add = (Set<Long>) CollectionUtil.subtract(updateTagIds, curTagIds);

        if(!remove.isEmpty()){
            articleTagRelationService.remove(new LambdaQueryWrapper<ArticleTagRelation>()
                    .eq(ArticleTagRelation::getArticleId, id).in(ArticleTagRelation::getTagId, remove));
        }
        if(!add.isEmpty()){
            articleTagRelationService.saveBatch(add.stream().map(tagId -> {
                ArticleTagRelation articleTagRelation = new ArticleTagRelation();
                articleTagRelation.setArticleId(id);
                articleTagRelation.setTagId(tagId);
                return articleTagRelation;
            }).collect(Collectors.toList()));
        }

        //3. 将其从redis缓存中删除
        deleteCoverCache(id);

        return R.ok();
    }

    @Override
    public R getArticleContentById(Long id) {
        Article article = lambdaQuery().select(Article::getContent).eq(Article::getId, id).one();
        if(article != null){
            ArticleContentVo vo = new ArticleContentVo(article.getContent());
            try {
                String contentCacheKey = ARTICLE_CONTENT_PREFIX + id + ARTICLE_CONTENT_SUFFIX;
                stringRedisTemplate.opsForValue().set(contentCacheKey,
                        MAPPER.writeValueAsString(vo), ARTICLE_TTL, TimeUnit.HOURS);
                return R.ok(vo);
            } catch (JsonProcessingException e) {
                throw new BusinessException(AppHttpCodeEnum.INTERNAL_SERVER_ERROR);
            }
        }
        return R.error(AppHttpCodeEnum.NOT_EXIST);
    }

    @Override
    public R updateContentById(Long id, String content, HttpServletRequest request) {
        //todo: 消息队列优化
        //1.首先判断当前用户有没有权限
        Article article = getById(id);
        UserInfoVo userInfoVo = redisUtils.getCurrentUserWithValidation(request);
        if(!article.getCreatedBy().equals(userInfoVo.getUid())){
            return R.error(AppHttpCodeEnum.NO_AUTH);
        }
        //Todo: 异步编排优化
        //2.更新文章内容
        Article updateArticle = new Article();
        updateArticle.setId(id);
        updateArticle.setContent(content);
        updateArticle.setUpdateTime(new Date());

        updateById(updateArticle);
        //3.文章内容更新，要将其从redis中删除
        deleteContentCache(id);

        return R.ok();
    }

    @Override
    public void publishArticle(Long id, ArticleContentVo articleContentVo, HttpServletRequest request) {
        if(articleContentVo == null){
            Article article = new Article();
            article.setId(id);
            article.setStatus(SystemConstants.ARTICLE_STATUS_PUBLISHED);
            updateById(article);
        } else {
            //todo: 消息队列优化
            //1.首先要判断用户是否有权限
            UserInfoVo currentUser = redisUtils.getCurrentUserWithValidation(request);
            Article article = lambdaQuery().select(Article::getCreatedBy).eq(Article::getId, id).one();
            if(article == null){
                throw new BusinessException(AppHttpCodeEnum.NOT_EXIST);
            }
            if(!article.getCreatedBy().equals(currentUser.getUid())){
                throw new BusinessException(AppHttpCodeEnum.NO_AUTH);
            }
            //2.对文章进行更新
            Article updateArticle = new Article();
            updateArticle.setId(id);
            updateArticle.setContent(articleContentVo.getContent());
            updateArticle.setStatus(SystemConstants.ARTICLE_STATUS_PUBLISHED);
            updateById(updateArticle);
            //3.文章一旦发布，也要删除(content)缓存
            deleteContentCache(id);
        }
    }

    @Override
    public R getArticlesByUid(ArticleQueryRequest articleQueryRequest, HttpServletRequest request) {
        UserInfoVo currentUser = redisUtils.getCurrentUser(request);
        Long uid = currentUser == null ? null : currentUser.getUid();

        LambdaQueryWrapper<Article> wrapper = new LambdaQueryWrapper<Article>()
                .select(Article.class, item -> !item.getColumn().equals("content"))//不查询内容
                .eq(Article::getCreatedBy, articleQueryRequest.getUid());

        //如果不是当前用户，只查询已发布的
        if(currentUser == null || !currentUser.getUid().equals(articleQueryRequest.getUid())){
            wrapper = wrapper.eq(Article::getStatus, "1");
        }
        //是当前用户，根据传来的type属性决定查询哪些（1查询所有，2查询已发布，3查询草稿）
        else {
            switch (articleQueryRequest.getSelectType()){
                case 2 : wrapper = wrapper.eq(Article::getStatus, "1");break;
                case 3 : wrapper = wrapper.eq(Article::getStatus, "0");break;
            }
        }

        //首先按照isTop排序，再按照updateTime排序
        wrapper = wrapper.orderByDesc(Article::getIsTop, Article::getUpdateTime);
        Page<Article> page = page(new Page<>(articleQueryRequest.getPageNum(), articleQueryRequest.getPageSize()), wrapper);

        List<ArticleVo> articleVos = articlesToVos(page.getRecords(), uid);
        Page<ArticleVo> vosPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        vosPage.setRecords(articleVos);
        return R.ok(vosPage);
    }

    @Override
    public List<ArticleVo> getArticlesByIds(List<Long> articleIds, HttpServletRequest request) {
        if(articleIds.isEmpty()){
            return new ArrayList<>();
        }
        UserInfoVo currentUser = request == null ? null : redisUtils.getCurrentUser(request);
        List<Article> articles = listByIds(articleIds);
        return articlesToVos(articles, currentUser == null ? null : currentUser.getUid());
    }

    @Override
    public R listArticleVoByPage(ArticleQueryRequest articleQueryRequest, HttpServletRequest request) {
        UserInfoVo currentUser = redisUtils.getCurrentUser(request);

        int pageNum = articleQueryRequest.getPageNum();
        int pageSize = articleQueryRequest.getPageSize();
        String sortField = articleQueryRequest.getSortField();
        String sortOrder = articleQueryRequest.getSortOrder();

        //数据库中的数据除了访问量其他数据都可以确保是最新的
        //1. 构造查询条件
        QueryWrapper<Article> queryWrapper = new QueryWrapper<Article>()
                .select(Article.class, item -> !item.getColumn().equals("content"))
                .eq("status", SystemConstants.ARTICLE_STATUS_PUBLISHED)
                .eq(articleQueryRequest.getPrime(), "prime", 1)
                .orderBy(SqlUtils.validSortField(sortField), sortOrder.equals(CommonConstant.SORT_ORDER_ASC), sortField);

        //2. 查询数据库中的信息
        long start = System.currentTimeMillis();
        Page<Article> articlePage = page(new Page<>(pageNum, pageSize), queryWrapper);
        long end = System.currentTimeMillis();

        //3. 转换为vos
        List<ArticleVo> articleVos = articlesToVos(articlePage.getRecords(), currentUser == null ? null : currentUser.getUid());
        Page<ArticleVo> articleVoPage = new Page<>(articleQueryRequest.getPageNum(), articleQueryRequest.getPageSize(), articlePage.getTotal());
        articleVoPage.setRecords(articleVos);
        return R.ok(articleVoPage);
    }

    @Override
    public List<Article> getHots() {
        String hotsJSON = stringRedisTemplate.opsForValue().get(HOT_ARTICLES);
        List<Article> hots;
        if(StringUtils.isEmpty(hotsJSON)){
            hots = lambdaQuery().orderBy(true, false, Article::getHot).last("limit 8").list();
            stringRedisTemplate.opsForValue().set(RedisConstants.HOT_ARTICLES, ObjectMapperUtils.writeValueAsString(hots));
        } else {
            try {
                hots = MAPPER.readValue(hotsJSON, new TypeReference<List<Article>>() {});
            } catch (JsonProcessingException e) {
                throw new BusinessException(AppHttpCodeEnum.INTERNAL_SERVER_ERROR, "JSON转换异常");
            }
        }
        return hots;
    }

    @Override
    public List<Article> getGlobalTop() {
        String globalTopJSON = stringRedisTemplate.opsForValue().get(GLOBAL_TOP);
        List<Article> tops;
        if(StringUtils.isEmpty(globalTopJSON)){
            tops = lambdaQuery()
                    .select(Article.class, item -> !item.getColumn().equals("content"))
                    .eq(Article::getIsGlobalTop, SystemConstants.ARTICLE_GLOBAL_TOP)
                    .orderBy(true, false, Article::getUpdateTime).list();
            stringRedisTemplate.opsForValue().set(RedisConstants.GLOBAL_TOP, ObjectMapperUtils.writeValueAsString(tops));
        } else {
            try {
                tops = MAPPER.readValue(globalTopJSON, new TypeReference<List<Article>>() {});
            } catch (JsonProcessingException e) {
                throw new BusinessException(AppHttpCodeEnum.INTERNAL_SERVER_ERROR, "JSON转换异常");
            }
        }
        return tops;
    }

    @Override
    public Page<ArticleVo> getUpdates(ArticleQueryRequest articleQueryRequest, HttpServletRequest request) {
        UserInfoVo currentUser = redisUtils.getCurrentUserWithValidation(request);
        LambdaQueryWrapper<Article> wrapper = new LambdaQueryWrapper<>();

        if(articleQueryRequest.getUid().equals(-1L)){
            //远程调用获取当前用户的所有关注者
            List<Long> followIds = userFeignService.getFollowIdsOfCurrent();
            if(followIds.isEmpty()){
                return null;
            }
            wrapper.in(Article::getCreatedBy, followIds);
        } else {
            wrapper.eq(Article::getCreatedBy, articleQueryRequest.getUid());
        }
        wrapper.orderBy(true, false, Article::getCreateTime);
        //2. 首先获取所有Article对象
        Page<Article> page = page(new Page<>(articleQueryRequest.getPageNum(), articleQueryRequest.getPageSize()), wrapper);
        //3. 转换为vos
        List<ArticleVo> articleVos = articlesToVos(page.getRecords(), currentUser.getUid());
        Page<ArticleVo> vosPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        vosPage.setRecords(articleVos);
        return vosPage;
    }

    @Override
    public void deleteArticle(Long id, HttpServletRequest request) {
        //1.首先要判断用户是否有权限
        UserInfoVo currentUser = redisUtils.getCurrentUserWithValidation(request);
        Article article = lambdaQuery().select(Article::getCreatedBy).eq(Article::getId, id).one();
        if(article == null){
            throw new BusinessException(AppHttpCodeEnum.NOT_EXIST);
        }
        if(!article.getCreatedBy().equals(currentUser.getUid())){
            throw new BusinessException(AppHttpCodeEnum.NO_AUTH);
        }
        removeById(id);
    }

    /**
     * 将一个Article转成前端需要的ArticleVo
     * @param article
     * @return
     */
    private ArticleVo articleToVo(Article article, boolean incrViewCount){
        long start = System.currentTimeMillis();
        //复制基本的属性
        ArticleVo vo = BeanCopyUtils.copyBean(article, ArticleVo.class);
        //设置缩略图
        ArrayList<String> thumbnails = new ArrayList<>(3);
        if(StringUtils.isNotBlank(article.getThumbnail1())){
            thumbnails.add(article.getThumbnail1());
        }
        if(StringUtils.isNotBlank(article.getThumbnail2())){
            thumbnails.add(article.getThumbnail2());
        }
        if(StringUtils.isNotBlank(article.getThumbnail3())){
            thumbnails.add(article.getThumbnail3());
        }
        vo.setThumbnails(thumbnails);
        //1. 设置标签
        CompletableFuture<Void> tagsFuture = CompletableFuture.runAsync(() -> {
            //查询该文章涉及的标签
            List<ArticleTag> tags = articleTagRelationService.getTagsByArticleId(vo.getId());
            vo.setTags(tags);
        }, childThreadPoolExecutor);

        //2. 设置作者
        CompletableFuture<Void> authorFuture = CompletableFuture.runAsync(() -> {
            //远程调用查询作者信息
            UsernameAndAvtarDto author = userFeignService.getUsernameAndAvatar(article.getCreatedBy());
            vo.setAuthor(author);
        }, childThreadPoolExecutor);

        //3. 设置浏览量
        CompletableFuture<Void> viewCountFuture = CompletableFuture.runAsync(() -> {
            String viewCacheKey = ARTICLE_VIEW_PREFIX + article.getId() + ARTICLE_VIEW_SUFFIX;
            //如果要增加浏览量（点击了详情）
            if(incrViewCount){
                vo.setViewCount(stringRedisTemplate.opsForValue().increment(viewCacheKey));
            } else {
                String viewCount = stringRedisTemplate.opsForValue().get(viewCacheKey);
                vo.setViewCount(viewCount != null ? Long.valueOf(viewCount) : 0);
            }
        }, childThreadPoolExecutor);

        try {
            CompletableFuture.allOf(tagsFuture,authorFuture,viewCountFuture).join();
        } catch (Exception e) {
            throw new BusinessException(AppHttpCodeEnum.INTERNAL_SERVER_ERROR);
        }

        long end = System.currentTimeMillis();
        return vo;
    }

    /**
     * 将一个Article转成前端需要的ArticleVo，包括当前用户是否点赞和收藏的信息
     * @param article
     * @param uid
     * @return
     */
    public ArticleVo articleToVo(Article article, Long uid, boolean incrViewCount){
        long start = System.currentTimeMillis();

        ArticleVo vo = new ArticleVo();
        //1. 查询点赞
        CompletableFuture<Void> likeFuture = CompletableFuture.runAsync(() -> {
            Integer count = articleLikeMapper.selectCount(new LambdaUpdateWrapper<ArticleLike>()
                    .eq(ArticleLike::getArticleId, article.getId()).eq(ArticleLike::getUid, uid));
            if(count > 0){
                vo.setIsLiked(true);
            }
        }, childThreadPoolExecutor);

        //2. 查询收藏
        CompletableFuture<Void> starFuture = CompletableFuture.runAsync(() -> {
            Integer count = articleStarMapper.selectCount(new LambdaUpdateWrapper<ArticleStar>()
                    .eq(ArticleStar::getArticleId, article.getId()).eq(ArticleStar::getUid, uid));
            if(count > 0){
                vo.setIsStared(true);
            }
        }, childThreadPoolExecutor);

        //3. 转化成vo
        ArticleVo tmp = articleToVo(article, incrViewCount);
        BeanUtils.copyProperties(tmp, vo, "isLiked", "isStared");

        try {
            CompletableFuture.allOf(likeFuture, starFuture).join();
        } catch (Exception e) {
            throw new BusinessException(AppHttpCodeEnum.INTERNAL_SERVER_ERROR);
        }

        long end = System.currentTimeMillis();
        return vo;
    }

    /**
     * 将Article列表转换成前端需要的ArticleVo列表，包含用户是否点赞的信息
     * @param articles
     * @param uid 传null代表用户没有登录
     * @return
     */
    private List<ArticleVo> articlesToVos(List<Article> articles, Long uid){
        long start = System.currentTimeMillis();
        int size = articles.size();
        ArticleVo[] articleVos = new ArticleVo[size];
        ArrayList<CompletableFuture<Void>> futures = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            final int index = i;
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                //uid为null代表没有登录，不查询点赞信息
                ArticleVo vo = uid == null ?
                        articleToVo(articles.get(index), false) :
                        articleToVo(articles.get(index), uid, false);
                articleVos[index] = vo;
            }, threadPoolExecutor);
            futures.add(future);
        }
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        long end = System.currentTimeMillis();

        return Arrays.asList(articleVos);
    }

    /**
     * 删除某文章Cover缓存
     * @param articleId
     */
    private void deleteCoverCache(Long articleId){
        String coverCacheKey = ARTICLE_COVER_PREFIX + articleId + ARTICLE_COVER_SUFFIX;
        stringRedisTemplate.delete(coverCacheKey);
    }

    /**
     * 删除某文章Content缓存
     * @param articleId
     */
    private void deleteContentCache(Long articleId){
        String contentCacheKey = ARTICLE_CONTENT_PREFIX + articleId + ARTICLE_CONTENT_SUFFIX;
        stringRedisTemplate.delete(contentCacheKey);
    }
}