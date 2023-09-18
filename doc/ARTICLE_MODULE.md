## 2. 博客模块

是整个系统的核心模块，包括博客的发布、收藏、编辑、评论、点赞、推送等功能都在这个模块里。

### 2.1 表设计

#### 2.1.1 article_tag

```sql
create table if not exists antares_blog.article_tag
(
    id          bigint auto_increment comment '主键自增'
        primary key,
    parent_id   bigint                               not null comment '属于哪个category',
    created_by  bigint                               not null comment '被哪个用户创建',
    name        varchar(64)                          null comment '标签名',
    color       varchar(8)                           null comment '标签颜色',
    create_time datetime   default CURRENT_TIMESTAMP null,
    update_time datetime   default CURRENT_TIMESTAMP null comment '更新时间',
    is_delete   tinyint(1) default 0                 null comment '删除标志',
    constraint name
        unique (name)
)
    comment '文章标签表';
```

---

#### 2.1.2 article_tag_category

```sql
create table if not exists antares_blog.article_tag_category
(
    id          bigint auto_increment comment '主键自增'
        primary key,
    name        varchar(64)                          null comment '类别名',
    create_time datetime   default CURRENT_TIMESTAMP null comment '创建时间',
    update_time datetime   default CURRENT_TIMESTAMP null comment '更新时间',
    is_delete   tinyint(1) default 0                 null comment '''删除标志'
);
```

---

#### 2.1.3 article_tag_relation

```sql
create table if not exists antares_blog.article_tag_relation
(
    tag_id      bigint                             not null comment '主键',
    article_id  bigint                             not null comment '主键',
    create_time datetime default CURRENT_TIMESTAMP null comment '创建时间'
)
    comment '文章和文章标签关联表';
```

文章和标签的关联关系表

---

#### 2.1.4 article

```sql
create table if not exists antares_blog.article
(
    id            bigint auto_increment
        primary key,
    title         varchar(256)                         not null comment '标题',
    summary       varchar(1024)                        not null comment '文章摘要',
    content       longtext                             null comment '文章内容',
    prime         tinyint(1) default 0                 null comment '是否精华',
    is_top        tinyint(1) default 0                 null comment '是否置顶（0否，1是）',
    is_global_top tinyint(1) default 0                 null comment '是否全局置顶',
    status        tinyint(1) default 0                 null comment '状态（1已发布，0草稿）',
    close_comment tinyint(1) default 0                 null comment '是否允许评论 1是，0否',
    view_count    bigint     default 0                 null comment '访问量',
    like_count    bigint     default 0                 null,
    star_count    bigint     default 0                 null,
    comment_count bigint     default 0                 null,
    thumbnail1    varchar(256)                         null comment '缩略图1',
    thumbnail2    varchar(256)                         null comment '缩略图2',
    thumbnail3    varchar(256)                         null comment '缩略图3',
    created_by    bigint                               not null,
    score         int        default 0                 null comment '文章的总分数，是根据浏览、点赞、收藏、评论数计算得来的',
    hot           int        default 0                 null comment '文章的热度，有一个定时任务，每小时计算增加的score',
    create_time   datetime   default CURRENT_TIMESTAMP not null comment '创建时间',
    update_time   datetime   default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    is_delete     tinyint(1) default 0                 not null comment '删除标志'
)
    comment '文章表';
```

---

#### 2.1.5 article_comment

```sql
create table if not exists antares_blog.article_comment
(
    id            bigint auto_increment
        primary key,
    article_id    bigint                                                                                                not null comment '博客id',
    root_id       bigint       default -1                                                                               not null comment '根评论id，-1代表是根评论',
    content       varchar(4096)                                                                                         null comment '评论内容',
    from_uid      bigint                                                                                                not null comment '评论者uid',
    avatar        varchar(256) default 'https://gw.alipayobjects.com/zos/antfincdn/XAosXuNZyF/BiazfanxmamNRoxxVxka.png' null comment '评论者avatar',
    from_username varchar(64)                                                                                           not null comment '评论者用户名',
    to_uid        bigint                                                                                                null,
    to_username   varchar(64)                                                                                           null,
    to_comment_id bigint                                                                                                null comment '这条评论是回复哪条评论的，只有子评论才有（子评论的子评论，树形）',
    like_count    int          default 0                                                                                null comment '评论点赞数',
    create_time   datetime     default CURRENT_TIMESTAMP                                                                null,
    is_delete     tinyint(1)   default 0                                                                                null
);
```

需要注意的是，每进行1次评论，文章的commentCount+1，删除评论相反，commentCount的数据在redis里也有

---

#### 2.1.6 article_like

```
create table if not exists antares_blog.article_like
(
    id          bigint auto_increment comment '主键'
        primary key,
    article_id  bigint                             not null comment '文章id',
    uid         bigint                             not null,
    create_time datetime default CURRENT_TIMESTAMP not null
)
    charset = utf8mb3;
```

---

#### 2.1.7 star_book

```
create table if not exists antares_blog.star_book
(
    id          bigint auto_increment
        primary key,
    name        varchar(128)                         not null,
    create_by   bigint                               not null,
    count       int        default 0                 null,
    create_time datetime   default CURRENT_TIMESTAMP null,
    update_time datetime   default CURRENT_TIMESTAMP null,
    is_delete   tinyint(1) default 0                 null
)
    charset = utf8mb3;


```

---

#### 2.1.8 article_star

```
create table if not exists antares_blog.article_star
(
    id          bigint auto_increment
        primary key,
    book_id     bigint                             not null,
    uid         bigint                             not null,
    article_id  bigint                             null,
    create_time datetime default CURRENT_TIMESTAMP null
)
    charset = utf8mb3;
```

--------

### 2.2 功能

#### 2.2.1  缓存

笔记的缓存是整个项目的核心。涉及笔记缓存的时候要考虑以下问题：

- 由于笔记的content占用了大多的内容，而绝大多数时候我们可能只需要封面信息，有时候我们可能又只需要content信息（例如编辑），因此有必要将cover和content分开缓存
- 笔记的点赞、收藏、浏览量这些信息频繁修改，而且我们要做当前用户是否点赞、是否收藏的判断，所以要单独用别的数据结构存储，例如set

具体来说我的设计如下：

- 对于文章detail信息（也就是文章详情页），使用lua脚本来获取信息
- 对于首页的分页获取文章cover信息（以及用户页面获取某个用户的博客），我采用走后端查询mysql

articleVo有很多不同部分的数据，且分布在不同的数据库表或redis中。例如：

- article的author信息，在mysql数据库
- article的isLike、isStar，这个每个用户的返回结果都不同，查msyql太慢了（也不是不可以），所以我直接持久缓存在了redis中（set数据结构）
- article的likeCount、starCount、commentCount，这三个数据在数据库是最新的，但是我又不能保证每次用户点赞、收藏或评论就刷新缓存（这样效率太低了），所以我通过lua脚本查询set的大小来获取
- article的viewCount，这个数据变化及其频繁，频繁改mysql库也不好，因此我将其持久缓存在redis中（redis中是最新的），每隔20分钟同步到mysql（异步缓存写入）

---

#### 2.2.2 热榜功能

文章有score和hot两个字段，用来区分**热门**和**热榜**。

- score根据文章的浏览量、点赞量、收藏量、评论量计算得来
- hot根据前一个阶段的hot值和阶段之间score的增量来判断

具体来说，也写一个定时任务，执行的频率1小时。每一小时重新计算所有文章的score，得到scoreNew，则：

```
hot = hot*0.9+scoreNew-scoreOld

score=浏览量+点赞量*8+收藏量*16+评论量*8
```

然后将score和hot依次更新。**需要将hot前10的直接缓存进redis**（完整信息）

redis的缓存只应付首页右侧的热榜card，如果用户按照点击热门，则是去数据库按照score进行查询。

##### 2.2.2.1 生成虚拟数据

预计将插入1800篇文章（30\*3\*20），我需要20个关键词，每个文章0-6个标签，有0.02的概率为精华

首先需要明确文章的必要属性

```
title
summary
content
以上爬取csdn得到

create_by：随机生成11~1011
prime：随机生成，为1的概率是0.02
thumbnail1：爬虫得到

标签：0~8个
```

不要忘了文章标签是存储在一个关联表中的

```Java
@Test
void insertMockData() {
    long start = System.currentTimeMillis();

    String[] keywords = {"Java", "多线程", "Vue", "React", "TypeScript",
            "机器学习", "深度学习", "Python", "Flask", "MySQL",
    "Elastic Search", "RabbitMQ", "Netty", "CSP", "leetcode", "面经",
            "Gateway", "计算机网络", "操作系统", "数据结构", "Java容器", "Spring Boot", "Redis"};

    ArrayList<CompletableFuture<Void>> futures = new ArrayList<>();

    //获取文章标签总数
    List<ArticleTag> articleTags = articleTagMapper.selectList(null);
    int len = articleTags.size();
    for (int pageNum = 3; pageNum >=1; pageNum--) {
        final int p = pageNum;
        for (String keyword : keywords) {
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                ArrayList<Article> articles = new ArrayList<>(30);
                ArrayList<ArticleTagRelation> relations = new ArrayList<>(30*8);
                List<String> pictures = null;

                //爬取30篇文章
                String url = "https://so.csdn.net/api/v3/search?q=" + keyword + "&t=blog&p=" + p;
                String result = HttpRequest.get(url).execute().body();
                Map<String, Object> map = JSONUtil.toBean(result, Map.class);
                JSONArray records = (JSONArray) map.get("result_vos");
                //爬取30张图片
                try {
                    pictures = CrawlerUtils.fetchPicturesByKeyword(keyword, p);
                } catch (IOException e) {
                    log.error("抓取图片失败");
                }

                int size = records.size();
                int picSize = pictures.size();
                for (int i = 0; i < size; i++) {
                    JSONObject tempRecord = (JSONObject) records.get(i);
                    Article article = new Article();
                    article.setTitle(tempRecord.getStr("title").replace("<em>", "").replace("</em>",""));
                    article.setSummary(tempRecord.getStr("description").replace("<em>", "").replace("</em>",""));
                    article.setContent("# " + article.getTitle());
                    article.setCreatedBy(Long.valueOf(RandomUtils.nextInt(11, 1012)));
                    article.setPrime(RandomUtils.nextInt(0, 50) == 0 ? 1 : 0);
                    if(i<picSize){
                        article.setThumbnail1(pictures.get(i));
                    }
                    article.setViewCount(Long.valueOf(RandomUtils.nextInt(0, 2000)));

                    articles.add(article);
                }

                articleService.saveBatch(articles);

                for (Article article : articles) {
                    //代表该文章的标签数
                    int n = RandomUtils.nextInt(0, 9);
                    if(n > 0) {
                        //打乱articleTags
                        for (int k = 0; k < len; k++) {
                            int index = RandomUtils.nextInt(0, len);
                            ArticleTag tmp = articleTags.get(index);
                            articleTags.set(index, articleTags.get(k));
                            articleTags.set(k, tmp);
                        }

                        //取出前n个
                        for (int j = 0; j < n; j++) {
                            ArticleTagRelation relation = new ArticleTagRelation();
                            relation.setArticleId(article.getId());
                            relation.setTagId(articleTags.get(j).getId());
                            relations.add(relation);
                        }
                    }
                }
                articleTagRelationService.saveBatch(relations);
            }, threadPoolExecutor);
            futures.add(future);
        }
    }

    CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
    long end = System.currentTimeMillis();
    System.out.println("耗时" + (end-start));
}
```

##### 2.2.2.2 定时任务

需要启动定时任务将最新的分数刷新到MySQL数据库，同时取出分数最高的文章直接缓存到Redis中。

除了刷新文章分数这一个定时任务，系统中还有很多其他的定时任务例如同步文章的浏览量，预热推荐用户等。

所有定时任务的执行都涉及到分布式锁的问题，因为项目是分布式项目，将来一个微服务可能会部署多份，多个微服务同时执行定时任务可能会出现数据一致性和计算资源浪费的问题，我们期望的结果是只有一个微服务执行刷新分数的定时任务就好了，因此可以使用Redis作为分布式锁来解决这一问题。

```Java
/**
 * 更新文章的score和hot值，每4小时执行1次
 */
@Scheduled(cron = "0 0 0/4 * * ? ")
public void updateScoreAndHotJob(){
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
            List<Article> hots = articleService.lambdaQuery().orderBy(true, false, Article::getHot).last("limit 8").list();
            stringRedisTemplate.opsForValue().set(RedisConstants.HOT_ARTICLES, ObjectMapperUtils.writeValueAsString(hots));

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
```

----

#### 2.2.3 动态功能

这个问题本质是Feed流架构设计

##### 2.2.3.1 Feed流的3种推送模式

###### 推模式

当一个用户发送动态（一篇博客）之后，主动将这个动态推送给关注着。

推模式下，我们需要将这个动态插入到每位粉丝对应的feed表中，存储成本较高，尤其是对于粉丝量多的大V来说，每发一条动态，就需要插入粉丝量条数据，因此推模式不适合关注着粉丝多的场景

###### 拉模式

拉模式是用户主动去拉取动态，然后将这些动态按照某些相关指标（时间、热度）进行实时聚合

拉模式的存储成本低，但是查询和聚合操作的成本高，如果用户A关注了多个其他用户，那么拉取A的动态将是一个耗时的操作，实时性要比推模式差。

###### 推拉结合

简单的来说，就是大V和不活跃用户做特殊处理。

大V发送动态时，只将其推给关注他的活跃用户，而不活跃用户在获取动态信息时通过拉模式

本项目采用的是拉模式，因为考虑到平台的用户量较少，具体代码略

---

#### 2.2.4 评论功能

评论功能的一个难点是应对子评论和子子...评论，如何合理地设计数据库表进行存储以及如何在前端进行展示是一个要解决的问题。

##### 2.2.4.1 设计

我在设计评论功能时主要参考了bilibili的设计：

![image-20230710085851908](http://image.antares.cool/PicGo/image-20230710085851908.png)

可以看到这种评论有几个特点：

- 子评论并未全部展示出来，当点击查看后才分页请求子评论
- 子子评论不再单独开出一个折叠结构，而是和子评论公用一个折叠结构，用“回复@用户名”加以区分，这样做的好处是当子子...评论层级较深时不会被折叠得特别小，影响展示

##### 2.2.4.2 业务逻辑

当用户发表一个评论时：

- 查询文章是否存在
- 如果文章存在，异步执行以下任务：
    1. 保存评论到comment表
    2. article表的comment_count+1
    3. 增加redis中comment_count的计数
    4. 增加redis中被评论用户的消息通知数
       被评论用户存在不同的情况：如果评论是一个根评论，那么显然这条评论是发给文章作者的；如果评论是一个子评论，那么是发给评论者的；此外还要判断评论的targetUid是不是自己，如果是自己的话不用增加消息通知数

#### 2.2.5 消息通知功能

##### 2.2.5.1 如何设计一个站内消息系统

这部分参考了guide的《后端面试高频系统设计&场景题》

对于一个消息系统，消息的类型主要分为两种：系统通知（System Notice）和事件提醒（EventRemind）

###### 系统通知

系统通知由后台管理员发出，然后指定某一类（全体，个人等）用户接收。基于此设想，系统通知功能大致分为两张表：

1. t_manager_system_notice(管理员系统通知表)︰记录管理员发出的通知
2. t_user_system_notice(用户系统通知表)︰存储用户接受的通知

t_manager_system_notice：

```
id
titile
content
type	指定发给哪些用户，例如single、all、vip等
state	系统消息是否已经被拉去过
recipient_id	当type为single时指定接收消息的用户，其他情况下为0
manager_id	发布系统消息的管理员id
publish_time	创建时间
```

t_user_system_notice：

```
id
state	是否已读
system_notice_id
recipient_id	接收系统消息的id
pull_time	拉取时间
```

当管理员发布一条系统通知，将通知插入t_manager_system_notice表中，然后系统定时从t_manager_system_notice中拉取通知，根据type属性将通知插入t_user_system_notice表中，所以整体这里的实现就是一个定时任务

###### 事件提醒

此类消息是有用户的行为产生的，例如：

- xxx回复了你...
- xxx点赞了你的评论
- xxx收藏了你的文章

注入此类事件，我们除了需要了解事件发生之外，还需要知道事件发生的“场景”，即事件的类型（是回复、点赞还是收藏），涉及的uid、article_id是什么？

基于以上需求，可以设计t_event_remind表，其结构如下：

```
event_remind_id
action
source_id	事件源id，例如文章id、评论id，方便用户收到通知后立即定位到事件现场
source_type	事件源类型，如comment、article
source_content	事件源内容，例如评论的内容
url	事件源地址
state	是否已读
sender_id	触发事件的用户id
recipient_id	接收消息通知的用户id
remind_time	提醒的时间
```

###### 消息聚合

也就是用户查询自己的消息通知时如何处理，某一类的聚合消息之间是按照source type和source id来分组的，因此我们可以通过下面的伪sql来查询不同类别（点赞、评论、收藏）的不同事件源的消息通知：

```sql
SELECT * FROM t_event_remind wHERE recipient_id = 用户ID
AND action = 点赞 AND state = FALSE GROUP BY source_id, source_type;
```

##### 2.2.5.2 我的设计

没有增加新的表，因为通知的来源无非点赞、评论、收藏和聊天这些行为，而他们都有对应的表。解决的思路是用户执行点赞、评论、发消息行为的时候，redis中的计数+1，用户登录点击移除消息就清0。

不过上面的设计有一个BUG，考虑点赞这个行为：

- 用户A先点赞，此时redis中B的点赞消息+1；
- 用户B登录并把点赞消息清0；
- 用户A取消点赞，此时redis中B的点赞消息减为-1。

上面这种反复点赞又取消点赞，以及回复又删除回复的问题我目前还没有解决。

结合redis，redis中可以仅存储条数，这个条数暂时设置成永久存储（后续优化，设置一个月的有效期，到期删除，用户获取自己的消息数通过lua脚本）

---

#### 2.2.6 点赞

简单设计，直接将点赞用户以set存进redis，uid作为value

所有的点赞记录要在mysql也存一份，这样redis中缓存的点赞记录不用设为永久有效，当缓存没有命中可以去mysql查

---

#### 2.2.7 收藏功能

收藏涉及到收藏夹，用户可以选择将同一个博客放到不同的收藏夹中，也可以进行修改（仿B站的效果），只有在所有收藏夹中都移除了，才算取消了收藏。

和点赞功能的设计思路类似

---

### 2.3 OpenResty

#### 2.3.1 跨域配置

```
add_header 'Access-Control-Allow-Origin' $http_origin;
add_header 'Access-Control-Allow-Credentials' 'true';
add_header Access-Control-Allow-Methods 'GET, POST, OPTIONS';
add_header Access-Control-Allow-Headers '*';
if ($request_method = 'OPTIONS') {
    add_header 'Access-Control-Max-Age' 1728000;
    add_header 'Content-Type' 'text/plain; charset=utf-8';
    add_header 'Content-Length' 0;
    return 204;
}
```

#### 2.3.2 其他配置

```
server {
    listen 80;
    server_name antares.cool, *.antares.cool;

    # 指定前端项目所在的位置
    location /static/ {
        root html;
        index index.html index.htm;
    }

    location ~ /api/ngx/blog/article/(\d+)$ {
        default_type application/json;
        content_by_lua_file lua/article.lua;
        include cors.conf;
    }

    location ~ /api/ngx/blog/article/(\d+)/basic {
        default_type application/json;
        content_by_lua_file lua/article_basic.lua;
        include cors.conf;
    }

    location ~ /api/ngx/blog/article/(\d+)/content {
        default_type application/json;
        content_by_lua_file lua/article_content.lua;
        include cors.conf;
    }

    location /api {
        proxy_set_header Host $host; 
        proxy_pass http://antares_gateway;
    }

    location /{
        proxy_set_header Host localhost; 
        proxy_pass http://antares;
    }

    error_page   500 502 503 504  /50x.html;
    location = /50x.html {
        root html;
    }
}
```

#### 2.3.3 article.lua

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

该lua脚本实现了首先去redis查询文章详细信息，查询不到再去请求tomcat这一功能，值得注意的有这些点：

- 文章的详细信息分散在redis不同的key中，需要做信息的拼接
- 文章的isLiked和isStared属性是个性化的，不同的用户返回不同，需要根据请求的uid查询，而uid又是根据cookie中的token查询
- 将查询redis的逻辑完全前置到了nginx，显著降低了tomcat的压力，能够提高系统的并发能力。如果请求达到后端，证明redis中不存在数据，只需要查询数据库放进redis即可，后端不需要再查询redis

#### 2.3.4 redisUtils.lua

注意到上面的article.lua多处使用到了redisUtil.lua中的工具函数，这些函数主要负责连接redis然后查询redis中不同数据结构的数据，一个使用连接池的高效的redisUtil.lua也能显著提升效率

```lua
local httpUtils = require('httpUtils')
local read_http = httpUtils.read_http

-- 导入cjson库
local cjson = require('cjson')
-- 导入redis
local redis = require('resty.redis')

cjson.encode_empty_table_as_object(false)

-- 定义ip，port，password
local ip = "A.B.C.D"
local port = 6379
local password = "123456"

-- 关闭redis连接的工具方法，其实是放入连接池
local function close_redis(red)
    local pool_max_idle_time = 60000 -- 连接的空闲时间，单位是毫秒
    local pool_size = 100 --连接池大小
    local ok, err = red:set_keepalive(pool_max_idle_time, pool_size)
    if not ok then
        ngx.log(ngx.ERR, "放入redis连接池失败: ", err)
    end
end

-- redis连接和认证函数
local function redis_init()
    local red = redis:new()
    red:set_timeouts(10000)
    -- 获取一个连接
    local ok, err = red:connect(ip, port)
    if not ok then
        ngx.log(ngx.ERR, "连接redis失败 : ", err)
        return nil
    end

    -- 获取连接重用次数
    local times, err = red:get_reused_times()
    if times == nil or times == 0 then
        -- 认证 Redis
        if password then
            local res, err = red:auth(password)
            if not res then
                ngx.log(ngx.ERR, "Redis 认证失败: ", err)
                return nil
            end
        end
    end
    return red
end

-- 查询redis的方法（string数据结构）
local function read_redis_string(key)
    local red = redis_init()
    -- 获取连接失败
    if red == nil then
        return nil
    end
    -- 查询redis
    local resp, err = red:get(key)
    close_redis(red)
    -- 查询失败处理
    if not resp then
        ngx.log(ngx.ERR, "查询Redis失败: ", err, ", key = " , key)
        return nil
    end
    --得到的数据为空处理
    if resp == ngx.null then
        resp = nil
        ngx.log(ngx.ERR, "查询Redis数据为空, key = ", key)
    end
    return resp
end

-- 查询redis的方法（hash数据结构）
local function read_redis_hash(key)
    local red = redis_init()
    -- 获取连接失败
    if red == nil then
        return nil
    end
    -- 查询redis
    local resp, err = red:hgetall(key)
    close_redis(red)
    -- 查询失败处理
    if not resp then
        ngx.log(ngx.ERR, "查询Redis失败: ", err, ", key = " , key)
        return nil
    end
    --得到的数据为空处理
    if next(resp) == nil then
        ngx.log(ngx.ERR, "查询Redis数据为空, key = ", key)
        return nil
    end
    -- 将field和value组装成lua对象返回
    local data = {}
    for i = 1, #resp, 2 do
        local field = resp[i]
        local value = resp[i + 1]
        data[field] = value
    end
    return data
end

-- 增加count
local function increment_redis(key)
    local red = redis_init()
    -- 获取连接失败
    if red == nil then
        return nil
    end
    -- 执行浏览数+1
    local resp, err = red:incr(key)
    close_redis(red)
    -- 检查Redis命令执行结果
    if not resp then
        ngx.log(ngx.ERR, "failed to execute Redis command: ", err)
        return
    end
    return resp
end

-- 获取set大小（点赞、收藏数）
local function get_set_size(key)
    local red = redis_init()
    -- 获取连接失败
    if red == nil then
        return nil
    end
    local size = red:eval([[
        local size = redis.call('scard', KEYS[1])
        return size
    ]], 1, key)
    close_redis(red)
    return size
end

-- 判断set中是否有成员（是否点赞收藏）
local function is_member(key, member)
    local red = redis_init()
    -- 获取连接失败
    if red == nil then
        return nil
    end
    local res, err = red:sismember(key, member)
    if not res then
        ngx.log(ngx.ERR, "failed to execute SISMEMBER: ", err)
        return false
    end
    close_redis(red)
    return res == 1
end

-- 封装查询函数（适用于string数据结构，查不到了就去请求http）
function read_data(key, path, params)
    -- 查询redis
    local val = read_redis_string(key)
    -- redis查询失败
    if not val then
        ngx.log(ngx.ERR, "redis查询失败，尝试查询http， key: ", key)
        -- redis查询失败，去查询http（假定http查询一定有结果）
        val = read_http(path, params)
    -- redis查询成功
    else
        val = cjson.decode(val);
    end
    -- 返回数据
    return val
end

-- 将方法导出
local _M = {  
    read_redis_string = read_redis_string,
    read_redis_hash = read_redis_hash,
    increment_redis = increment_redis,
    get_set_size = get_set_size,
    is_member = is_member,
    read_data = read_data
}  
return _M
```

核心其实是其中获取连接的函数，注意获取连接重用次数后，判断times == nil or times == 0，如果false证明这是一个重用的连接，直接返回（网上好多代码是错误的，实际上还是要每次建立新的连接，经打日志测试这个写法是正确的）

```
-- redis连接和认证函数
local function redis_init()
    local red = redis:new()
    red:set_timeouts(10000)
    -- 获取一个连接
    local ok, err = red:connect(ip, port)
    if not ok then
        ngx.log(ngx.ERR, "连接redis失败 : ", err)
        return nil
    end

    -- 获取连接重用次数
    local times, err = red:get_reused_times()
    if times == nil or times == 0 then
        -- 认证 Redis
        if password then
            local res, err = red:auth(password)
            if not res then
                ngx.log(ngx.ERR, "Redis 认证失败: ", err)
                return nil
            end
        end
    end
    return red
end
```