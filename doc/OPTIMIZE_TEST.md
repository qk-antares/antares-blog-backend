# 优化和测试

## Lua脚本

使用Lua脚本将查询缓存的逻辑前置到nginx（OpenResty），如果缓存命中则由nginx进行响应，缓存未命中才查询Tomcat，可以缓解Tomcat的压力，提升QPS

![image-20230917141209765](https://qk-antares.github.io/img/blog/optimize_test/89dac515b5c979cb8b1ab0f72ee9732038279841.png)

### 测试条件

![image-20230917154529256](https://qk-antares.github.io/img/blog/optimize_test/65a9588063ea103b4ee31a80a8a4348338279841.png)

### 优化前

在Tomcat中查询缓存（缓存命中的情况）并返回

```Java
/**
 * 这是一个测试接口，用来对比lua脚本和直接请求后端的性能差异
 * @param id
 * @param request
 * @return
 */
@Override
public ArticleVo getArticleById(Long id, HttpServletRequest request) {
    UserInfoVo currentUser = redisUtils.getCurrentUser(request);

    String articleCacheKey = "article:id:" + id + ":cover";
    String articleJson = stringRedisTemplate.opsForValue().get(articleCacheKey);
    ArticleVo articleVo = JSONUtil.toBean(articleJson, ArticleVo.class);

    //设置点赞、收藏、评论这些异步缓存写入数据 以及 是否点赞、收藏这些个性化信息
    String viewCacheKey = "article:id:" + id + ":view";
    String likeCacheKey = "article:id:" + id + ":like";
    String starCacheKey = "article:id:" + id + ":star";
    String commentCacheKey = "article:id:" + id + ":comment";

    //浏览数+1
    Long viewCount = stringRedisTemplate.opsForValue().increment(viewCacheKey);
    articleVo.setViewCount(viewCount);
    //获取like信息
    Set<String> likes = stringRedisTemplate.opsForSet().members(likeCacheKey);
    if(likes != null){
        articleVo.setLikeCount((long) likes.size());
        articleVo.setIsLiked(currentUser != null && likes.contains(currentUser.getUid().toString()));
    } else {
        articleVo.setLikeCount(0L);
        articleVo.setIsLiked(false);
    }
    //获取star信息
    Set<String> stars = stringRedisTemplate.opsForSet().members(starCacheKey);
    if(stars != null){
        articleVo.setStarCount((long) stars.size());
        articleVo.setIsStared(currentUser != null && stars.contains(currentUser.getUid().toString()));
    } else {
        articleVo.setStarCount(0L);
        articleVo.setIsStared(false);
    }
    //获取commentCount
    String commentCountStr = stringRedisTemplate.opsForValue().get(commentCacheKey);
    if(commentCountStr != null){
        articleVo.setCommentCount(Long.valueOf(commentCountStr));
    } else {
        articleVo.setCommentCount(0L);
    }
    //获取content
    ArticleContentVo content = getArticleContentById(id);
    articleVo.setContent(content.getContent());

    return articleVo;
}
```

![image-20230917154859625](https://qk-antares.github.io/img/blog/optimize_test/da7d89ca002ab0008fd5b6826e35cf2338279841.png)

![image-20230917154912989](https://qk-antares.github.io/img/blog/optimize_test/0cb1bf8598d89ac32ed993e23842e7d538279841.png)

可以看到，在线程数100，Ramp-up时间1s，循环次数10次的情况下，**QPS为45.5**，**平均响应时间2069ms**。测试还发现，进一步提升线程数，吞吐量虽然会上升，但是会出现异常响应。

### 优化后

在OpenResty中查询缓存（缓存命中的情况）并返回

```conf
location ~ /api/ngx/blog/article/(\d+)$ {
    default_type application/json;
    content_by_lua_file lua/article.lua;
    include cors.conf;
}
```

```lua
-- /api/ngx/blog/article/{id}，用于获取文章的完整信息
-- 导入httpUtils和redisUtils
local httpUtils = require('httpUtils')
local redisUtils = require('redisUtils')
local read_http = httpUtils.read_http
local read_redis_string = redisUtils.read_redis_string
local increment_redis = redisUtils.increment_redis
local get_set_size = redisUtils.get_set_size
local is_member = redisUtils.is_member
local read_data = redisUtils.read_data

-- 导入cjson库
local cjson = require('cjson')

-- 获取路径参数
local id = ngx.var[1]
-- 获取cookie参数
local token = ngx.var.cookie_TOKEN

-- 查询article信息
local articleCacheKey = "article:id:" .. id .. ":cover"
local articlePath = "/api/blog/article/" .. id .. "/cover"
-- 查询redis
local articleCover = read_redis_string(articleCacheKey)
-- redis查询失败
if not articleCover then
    ngx.log(ngx.ERR, "redis查询失败，尝试查询http， key: ", articleCacheKey)
    -- 去查询http（假定http查询一定有结果）
    articleCover = read_http(articlePath, nil)
-- redis查询成功（还需要拼装浏览数、点赞数、收藏数、评论数、是否点赞和收藏）
else
    local viewCacheKey = "article:id:" .. id .. ":view"
    local likeCacheKey = "article:id:" .. id .. ":like"
    local starCacheKey = "article:id:" .. id .. ":star"
    local commentCacheKey = "article:id:" .. id .. ":comment"
    articleCover = cjson.decode(articleCover)
    -- 将viewCount+1并返回
    local view = increment_redis(viewCacheKey)
    articleCover.viewCount = view
    -- 获取likeCount
    local like = get_set_size(likeCacheKey)
    articleCover.likeCount = like
    -- 获取starCount
    local star = get_set_size(starCacheKey)
    articleCover.starCount = star
    -- 查询commentCount
    local comment = read_redis_string(commentCacheKey)
    articleCover.commentCount = comment
    -- 查询isLiked和isStared
    if token ~= nil then
        local currentUserCacheKey = "user:session:" .. token
        local currentUser = read_redis_string(currentUserCacheKey)
        if not currentUser then
            ngx.log(ngx.ERR, "token非法")
            articleCover.isLiked = false
            articleCover.isStared = false
        else
            currentUser = cjson.decode(currentUser)
            local uid = currentUser.uid
            local isLiked = is_member(likeCacheKey, uid)
            local isStared = is_member(starCacheKey, uid)
            articleCover.isLiked = isLiked
            articleCover.isStared = isStared
        end
    else
        ngx.log(ngx.ERR, "当前用户未登录")
        articleCover.isLiked = false
        articleCover.isStared = false
    end
end

-- 查询content信息
local articleContent = read_data("article:id:" .. id .. ":content", "/api/blog/article/" .. id .. "/content", nil)
articleCover.content = articleContent.content

local response = {
    code = 200,
    msg = '操作成功！',
    data = articleCover
}

-- 把item序列化为json 返回结果
ngx.say(cjson.encode(response))
```

![image-20230917154949491](https://qk-antares.github.io/img/blog/optimize_test/5a6c24d0812d973ba1ecee71d92cb09a38279841.png)

![image-20230917155022965](https://qk-antares.github.io/img/blog/optimize_test/a300c2e569b0949443ea2edd4062e04d38279841.png)

可以看到，在线程数100，Ramp-up时间1s，循环次数10次的情况下，**QPS为104.1**，**平均响应时间739ms**。相比于之前**QPS为45.5**，**平均响应时间2069ms**的结果，QPS提升超过1倍，平均响应时间缩短为原来的将近三分之一。

测试还发现这仍然不是使用Lua脚本的性能上限，如果将线程数提升至400，则QPS还会进一步提升。

![image-20230917155314283](https://qk-antares.github.io/img/blog/optimize_test/9e437500050e0d50b8a33bb75abce62a38279841.png)

可以看到，在线程数400，Ramp-up时间1s，循环次数10次的情况下，**QPS为241.6**，**平均响应时间1479ms**。**平均响应时间依然比原本小**，而且**QPS提升了5倍多**。

---

## CompletableFuture和线程池

### 介绍

分页获取文章是一个复杂的查询接口，它有多个步骤且涉及到嵌套查询

- 根据分页请求参数到article表中查询最原始的信息
- 对于分页中的每个article：
  - 根据id到article_tag_relation表中查询这个article涉及的标签id，得到一个tagIds，并根据该tagIds，到article_tag表中查询对应的tag信息
  - 根据created_by属性查询文章作者信息
  - 由于文章浏览量、点赞、收藏、评论数使用的是异步缓存写入，最新数据位于Redis中，因此还需要到Redis中查询这些数据，此外还要查询用户是否点赞、收藏了该文章

### 测试条件

![image-20230917154529256](https://qk-antares.github.io/img/blog/optimize_test/65a9588063ea103b4ee31a80a8a4348338279841.png)

![image-20230917165125457](https://qk-antares.github.io/img/blog/optimize_test/8b2db98b1938c87ccc8ff2111461e28e38279841.png)

### 优化前

以上的这些步骤全部在一个线程中完成，同步执行：

```Java
private List<ArticleVo> articlesToVos(List<Article> articles, Long uid){
    return articles.stream().map(article -> uid == null ?
            articleToVo(article, false) : articleToVo(article, uid, false))
            .collect(Collectors.toList());
}

private ArticleVo articleToVo(Article article, boolean incrViewCount){
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
    //查询该文章涉及的标签
    List<ArticleTag> tags = articleTagRelationService.getTagsByArticleId(vo.getId());
    vo.setTags(tags);

    //2. 设置作者
    //远程调用查询作者信息
    UsernameAndAvtarDto author = userFeignService.getUsernameAndAvatar(article.getCreatedBy());
    vo.setAuthor(author);

    String viewCacheKey = ARTICLE_VIEW_PREFIX + article.getId() + ARTICLE_VIEW_SUFFIX;
    //如果要增加浏览量（点击了详情）
    if(incrViewCount){
        vo.setViewCount(stringRedisTemplate.opsForValue().increment(viewCacheKey));
    } else {
        String viewCount = stringRedisTemplate.opsForValue().get(viewCacheKey);
        vo.setViewCount(viewCount != null ? Long.valueOf(viewCount) : 0);
    }

    return vo;
}

public ArticleVo articleToVo(Article article, Long uid, boolean incrViewCount){
    ArticleVo vo = new ArticleVo();
    //1. 查询点赞
    Integer like = articleLikeMapper.selectCount(new LambdaUpdateWrapper<ArticleLike>()
            .eq(ArticleLike::getArticleId, article.getId()).eq(ArticleLike::getUid, uid));
    if(like > 0){
        vo.setIsLiked(true);
    }

    //2. 查询收藏
    Integer star = articleStarMapper.selectCount(new LambdaUpdateWrapper<ArticleStar>()
            .eq(ArticleStar::getArticleId, article.getId()).eq(ArticleStar::getUid, uid));
    if(star > 0){
        vo.setIsStared(true);
    }

    //3. 转化成vo
    ArticleVo tmp = articleToVo(article, incrViewCount);
    BeanUtils.copyProperties(tmp, vo, "isLiked", "isStared");

    return vo;
}
```

![image-20230917165051036](https://qk-antares.github.io/img/blog/optimize_test/848e85cd755b5c4f2d7093b80358575938279841.png)

可以看到，在线程数100，Ramp-up时间1s，循环次数10次的情况下，**QPS为19**，**平均响应时间5077ms**。这主要是因为这个接口涉及的流程确实很多，而且中间还有一些远程调用（获取文章作者信息），并发量稍微高一点，响应时间就会非常长。

我没有进一步测试，因为这个响应时间已经非常不可接受了，提升线程数平均响应时间会进一步增加。

### 优化后

```Java
private List<ArticleVo> articlesToVos(List<Article> articles, Long uid){
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

    return Arrays.asList(articleVos);
}

public ArticleVo articleToVo(Article article, Long uid, boolean incrViewCount){
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

    return vo;
}

private ArticleVo articleToVo(Article article, boolean incrViewCount){
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

    return vo;
}
```

![image-20230917170017139](https://qk-antares.github.io/img/blog/optimize_test/39c1cc1b85548df8e7a9b85fe22b844f38279841.png)

可以看到，在线程数100，Ramp-up时间1s，循环次数10次的情况下，**QPS为143.4**，**平均响应时间584ms**。相比与之前**QPS为19，平均响应时间5077ms**，得到了质的提升。我进一步测试了线程数200、400的情况，QPS仍然有小幅度提升，最多到160，但是响应时间被拉长到1000+ms。

