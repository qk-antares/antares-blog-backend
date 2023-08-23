package org.example.antares.blog;

import cn.hutool.http.HttpRequest;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomUtils;
import org.example.antares.blog.mapper.ArticleTagMapper;
import org.example.antares.blog.model.entity.Article;
import org.example.antares.blog.model.entity.ArticleTag;
import org.example.antares.blog.model.entity.ArticleTagRelation;
import org.example.antares.blog.service.ArticleService;
import org.example.antares.blog.service.ArticleTagRelationService;
import org.example.antares.common.utils.CrawlerUtils;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;

@SpringBootTest
@Slf4j
public class InsertBlogTest {
    @Resource
    private ArticleTagMapper articleTagMapper;
    @Resource
    private ArticleTagRelationService articleTagRelationService;
    @Resource
    private ArticleService articleService;
    @Resource
    private ThreadPoolExecutor threadPoolExecutor;

    /**
     * 预计将插入1800篇文章（30*3*20）,我需要20个关键词
     */
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
                        pictures = CrawlerUtils.fetchPicturesByKeyword(keyword, p, null);
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
    }
}
