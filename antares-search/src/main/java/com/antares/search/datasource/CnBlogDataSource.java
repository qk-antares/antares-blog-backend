package com.antares.search.datasource;

import com.antares.search.model.vo.CnBlogVo;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.commons.lang3.StringUtils;
import com.antares.common.exception.BusinessException;
import com.antares.common.model.enums.AppHttpCodeEnum;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * 图片服务实现类
 */
@Service
public class CnBlogDataSource implements DataSource<CnBlogVo> {

    /**
     * 根据关键词搜索博客园文章
     * @param searchText 关键词
     * @param pageNum 页码
     * @param pageSize 页大小，这里固定为10，暂不支持其他pageSize
     * @return
     */
    @Override
    public Page<CnBlogVo> doSearch(String searchText, int pageNum, int pageSize, List<String> tags) {
        if(pageSize != 10){
            throw new BusinessException(AppHttpCodeEnum.PARAMS_ERROR, "请求分页大小不合法");
        }

        String url = "https://zzk.cnblogs.com/s/blogpost?Keywords=" + searchText + "&pageindex=" + pageNum;
        Document doc = null;
        try {
            doc = Jsoup.connect(url).cookie("NotRobot", "CfDJ8M-opqJn5c1MsCC_BxLIULmQfRnFy3fYZr1nlEmklKD2-I1A_kKEZAuy_51d5KqXY4RMYlVnFJvPjhFcO2yTzZQLame1DpMuJ7I5gEGjUV3vT0XSiuAg6gIDPzmJGlstzA").get();
        } catch (IOException e) {
            throw new BusinessException(AppHttpCodeEnum.INTERNAL_SERVER_ERROR, "爬取信息失败！");
        }

        //获取总页数
        String totalPageStr = doc.select(".last").text();
        if(StringUtils.isEmpty(totalPageStr)){
            totalPageStr = doc.select(".current").text();
        }
        long totalPage = StringUtils.isNotEmpty(totalPageStr) ? Long.valueOf(totalPageStr) : 1L;

        //页数超出限制
        if(pageNum > totalPage){
            throw new BusinessException(AppHttpCodeEnum.PARAMS_ERROR, "请求页码超出限制！");
        }

        Page<CnBlogVo> cnBlogPage = new Page<>(pageNum, pageSize);
        cnBlogPage.setTotal(totalPage * pageSize);

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

        cnBlogPage.setRecords(cnBlogVos);
        return cnBlogPage;
    }
}
