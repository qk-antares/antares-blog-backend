package org.example.antares.search;

import cn.hutool.http.HttpRequest;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import org.example.antares.search.model.vo.CnBlogVo;
import org.example.antares.search.model.vo.CsdnBlogVo;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;

@SpringBootTest
public class CrawlerTest {
    public static final int CSDN_PAGE_SIZE = 30;
    public static final int CN_BLOGS_PAGE_SIZE = 10;

    @Test
    void testFetchCSDN() throws IOException {
        int page = 4;
        int pageSize = 10;
        int p = (page - 1) * pageSize / CSDN_PAGE_SIZE + 1;
        String url = "https://so.csdn.net/api/v3/search?q=Java&t=blog&p=1";
        String result = HttpRequest
                .get(url)
                .execute()
                .body();

        Map<String, Object> map = JSONUtil.toBean(result, Map.class);
        JSONArray records = (JSONArray) map.get("result_vos");
        int startIndex = (page - 1) * pageSize % CSDN_PAGE_SIZE;
        ArrayList<CsdnBlogVo> csdnBlogVos = new ArrayList<>(pageSize);

        for(int i = 0;i < pageSize;i++){
            JSONObject tempRecord = (JSONObject) records.get(startIndex + i);
            CsdnBlogVo csdnBlogVo = new CsdnBlogVo();
            csdnBlogVo.setTitle(tempRecord.getStr("title"));
            csdnBlogVo.setArticleUrl(tempRecord.getStr("url"));
            csdnBlogVo.setSummary(tempRecord.getStr("description"));
            csdnBlogVo.setViewCount(tempRecord.getStr("view"));
            csdnBlogVo.setLikeCount(tempRecord.getStr("digg"));
            csdnBlogVo.setCommentCount(tempRecord.getStr("comment"));
            csdnBlogVo.setAuthor(tempRecord.getStr("nickname"));
            csdnBlogVo.setAuthorUrl(tempRecord.getStr("author_space"));
            csdnBlogVo.setCreatedTime(tempRecord.getStr("create_time_str"));
            csdnBlogVos.add(csdnBlogVo);
        }

        System.out.println(csdnBlogVos);
    }

    @Test
    void testFetchCnBlogs() throws IOException {
        int page = 4;
        int pageSize = 10;

        String url = "https://zzk.cnblogs.com/s/blogpost?Keywords=hello&pageindex=1";
        Document doc = Jsoup.connect(url).cookie("NotRobot", "CfDJ8M-opqJn5c1MsCC_BxLIULkAZlfMpNrKK6LFt-yXjZIK21kDt6yq7nv9E-So8iMjSNORiZYSo1dRl9TRVDdGF5QrSrikUwtgrCQ8jaAjcBrg-zkvnM9nkaJnfFGrG9ZQxQ").get();
        Elements elements = doc.select(".searchItem");

        ArrayList<CnBlogVo> cnBlogVos = new ArrayList<>(pageSize);
        for (Element element : elements) {
            CnBlogVo cnBlogVo = new CnBlogVo();

            Elements titleElement = element.select(".searchItemTitle");
            cnBlogVo.setTitle(titleElement.text());
            cnBlogVo.setArticleUrl(titleElement.select("a").attr("href"));

            cnBlogVo.setSummary(element.select(".searchCon").text());

            Elements searchItemInfo = element.select(".searchItemInfo");

            Elements authorElement = searchItemInfo.select(".searchItemInfo-userName");
            cnBlogVo.setAuthor(authorElement.text());
            cnBlogVo.setAuthorUrl(authorElement.select("a").attr("href"));

            cnBlogVo.setViewCount(searchItemInfo.select(".searchItemInfo-views").text());
            cnBlogVo.setLikeCount(searchItemInfo.select(".searchItemInfo-good").text());
            cnBlogVo.setCommentCount(searchItemInfo.select(".searchItemInfo-comments").text());
            cnBlogVo.setCreatedTime(searchItemInfo.select(".searchItemInfo-publishDate").text());

            cnBlogVos.add(cnBlogVo);
        }

        System.out.println(cnBlogVos);
    }
}