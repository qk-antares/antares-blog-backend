## 3. 搜索模块

### 3.1 数据的全量同步

首先将文章与搜索相关的数据保存至es

删除索引（如果存在的话）

```
DELETE /article
DELETE /user
```

es建表如下：

```json
PUT article_v1
{
  "aliases": {
    "article": {}
  },
  "mappings": {
    "properties": {
      "title": {
        "type": "text",
        "analyzer": "ik_max_word",
        "search_analyzer": "ik_smart",
        "fields": {
          "keyword": {
            "type": "keyword",
            "ignore_above": 256
          }
        }
      },
      "summary": {
        "type": "text",
        "analyzer": "ik_max_word",
        "search_analyzer": "ik_smart",
        "fields": {
          "keyword": {
            "type": "keyword",
            "ignore_above": 256
          }
        }
      },
      "content": {
        "type": "text",
        "analyzer": "ik_max_word",
        "search_analyzer": "ik_smart",
        "fields": {
          "keyword": {
            "type": "keyword",
            "ignore_above": 256
          }
        }
      },
        "status": {
          	"type": "integer"  
        },
      "tags": {
        "type": "keyword"
      },
      "username": {
        "type": "keyword"
      },
        "score": {
			"type": "integer"
        },
      "createTime": {
        "type": "date"
      },
      "updateTime": {
        "type": "date"
      }
    }
  }
}
```

```
PUT user_v1
{
  "aliases": {
    "user": {}
  },
  "mappings": {
    "properties": {
      "username": {
        "type": "text",
        "analyzer": "ik_max_word",
        "search_analyzer": "ik_smart",
        "fields": {
          "keyword": {
            "type": "keyword",
            "ignore_above": 256
          }
        }
      },
      "signature": {
        "type": "text",
        "analyzer": "ik_max_word",
        "search_analyzer": "ik_smart",
        "fields": {
          "keyword": {
            "type": "keyword",
            "ignore_above": 256
          }
        }
      },
      "tags": {
        "type": "keyword"
      }
    }
  }
}
```

进行全量同步

```Java
@Test
public void fullSyncArticle() {
    Map<Long, ArticleTag> tagsMap = articleTagService.list().stream()
            .collect(Collectors.toMap(ArticleTag::getId, articleTag -> articleTag));
    List<ArticleEsDTO> dtos = articleService.list().stream().map(article -> {
        ArticleEsDTO articleEsDTO = BeanCopyUtils.copyBean(article, ArticleEsDTO.class);
        //查询该文章涉及的标签
        List<Long> tagIds = articleTagRelationService.lambdaQuery().select(ArticleTagRelation::getTagId)
                .eq(ArticleTagRelation::getArticleId, article.getId()).list()
                .stream().map(ArticleTagRelation::getTagId).collect(Collectors.toList());
        ArrayList<String> tags = new ArrayList<>();
        for (Long tagId : tagIds) {
            tags.add(tagsMap.get(tagId).getName());
        }
        articleEsDTO.setTags(tags);

        //查询文章的作者信息
        User user = userService.lambdaQuery().select(User::getUsername).eq(User::getUid, article.getCreatedBy()).one();
        articleEsDTO.setUsername(user.getUsername());

        return articleEsDTO;
    }).collect(Collectors.toList());

    final int pageSize = 500;
    int total = dtos.size();
    log.info("FullSyncPostToEs start, total {}", total);
    for (int i = 0; i < total; i += pageSize) {
        int end = Math.min(i + pageSize, total);
        log.info("sync from {} to {}", i, end);
        articleEsDao.saveAll(dtos.subList(i, end));
    }
    log.info("FullSyncPostToEs end, total {}", total);
}

@Test
public void fullSyncUser() {
    Map<Long, UserTag> tagsMap = userTagService.list().stream()
            .collect(Collectors.toMap(UserTag::getId, userTag -> userTag));
    List<UserEsDTO> dtos = userService.list().stream().map(user -> {
        UserEsDTO userEsDTO = BeanCopyUtils.copyBean(user, UserEsDTO.class);
        List<Long> tagIds = JSON.parseObject(user.getTags(), new TypeReference<List<Long>>(){});
        ArrayList<String> tags = new ArrayList<>();
        for (Long tagId : tagIds) {
            tags.add(tagsMap.get(tagId).getName());
        }
        userEsDTO.setTags(tags);

        return userEsDTO;
    }).collect(Collectors.toList());

    final int pageSize = 500;
    int total = dtos.size();
    log.info("FullSyncPostToEs start, total {}", total);
    for (int i = 0; i < total; i += pageSize) {
        int end = Math.min(i + pageSize, total);
        log.info("sync from {} to {}", i, end);
        userEsDao.saveAll(dtos.subList(i, end));
    }
    log.info("FullSyncPostToEs end, total {}", total);
}
```

### 3.2 门面模式

#### 3.2.1 概念

```
为子系统中的一组接口提供一个统一的入口。外观模式定义了一个高层接口，这个接口使得这一子系统更加容易使用。
```

#### 3.2.2 怎么使用的

对于搜素这个业务，提供了一个高层的接口DataSource，不管是搜博客、用户还是站外资源，实现这个接口中的doSearch方法就好了，屏蔽了不同搜索方式的实现，用户调用更加方便。

### 3.3 适配器模式

#### 3.3.1 概念

```
将一个类的接口转换成客户端希望的另一个接口，适配器模式让那些接口不兼容的类可以一起工作。
```

#### 3.3.2 怎么使用的

定制统一的数据源接入规范（标准）：

什么数据源允许接入？

你的数据源接入时要满足什么要求？

需要接入方注意什么事情？

本系统要求：任何接入我们系统的数据，它必须要能够根据关键词搜索、并且支持分页搜索。



通过声明接口的方式来定义规范。

2）假如说我们的数据源已经支持了搜索，但是原有的方法参数和我们的规范不一致（例如多了标签参数），怎么办？

使用适配器模式：通过转换，让两个系统能够完成对接。

---

### 3.4 数据同步

使用canal，canal的原理：

```
当我们使用Canal实现MySQL和Elastic Search数据同步时，Canal会监控MySQL的binlog，并将binlog解析为MySQL的增、删、改操作，然后将这些数据变更事件发送到Elasticsearch中。

具体来说，Canal提供了一个基于数据订阅/发布的模型，它通过解析MySQL的binlog日志文件来捕获数据库的变化，然后将这些变化转化为格式化的JSON数据，这些JSON数据包含了对应的操作类型（增、删、改）、表名、行数据等。

然后，Canal把这些JSON数据发送给Elasticsearch，由Elasticsearch同步到相应的索引中。 Cana支持将数据变更事件发送到消息队列中，方便其他系统进行二次消费。

通过这种方式，我们可以实现MySQL和Elasticsearch中的数据同步，让两个系统之间实现异构数据源的通讯和同步。
```