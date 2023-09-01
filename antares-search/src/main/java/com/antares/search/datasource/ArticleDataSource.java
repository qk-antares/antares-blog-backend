package com.antares.search.datasource;

import com.antares.search.model.dto.article.ArticleQueryRequest;
import com.antares.search.model.vo.ArticleVo;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import lombok.extern.slf4j.Slf4j;
import com.antares.search.service.ArticleSearchService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

/**
 * 帖子服务实现
 */
@Service
@Slf4j
public class ArticleDataSource implements DataSource<ArticleVo> {
    @Resource
    private ArticleSearchService articleSearchService;

    @Override
    public Page<ArticleVo> doSearch(String searchText, int pageNum, int pageSize, List<String> tags) {
        ArticleQueryRequest articleQueryRequest = new ArticleQueryRequest();
        articleQueryRequest.setKeyword(searchText);
        articleQueryRequest.setPageNum(pageNum);
        articleQueryRequest.setPageSize(pageSize);
        articleQueryRequest.setTags(tags);
        //只搜索出对应的id
        return articleSearchService.searchFromEs(articleQueryRequest);
    }
}




