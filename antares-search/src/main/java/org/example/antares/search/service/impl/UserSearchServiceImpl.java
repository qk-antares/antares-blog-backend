package org.example.antares.search.service.impl;

import com.alibaba.nacos.common.utils.CollectionUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.example.antares.common.constant.SystemConstants;
import org.example.antares.common.model.vo.UserInfoVo;
import org.example.antares.search.feign.UserFeignService;
import org.example.antares.search.model.dto.article.ArticleEsDTO;
import org.example.antares.search.model.dto.user.UserEsDTO;
import org.example.antares.search.model.dto.user.UserQueryRequest;
import org.example.antares.search.model.vo.ArticleVo;
import org.example.antares.search.service.UserSearchService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.core.ElasticsearchRestTemplate;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.NativeSearchQuery;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class UserSearchServiceImpl implements UserSearchService {
    @Resource
    private UserFeignService userFeignService;
    @Resource
    private ElasticsearchRestTemplate elasticsearchRestTemplate;

    @Override
    public Page<UserInfoVo> searchFromEs(UserQueryRequest userQueryRequest) {
        String keyword = userQueryRequest.getKeyword();
        List<String> tags = userQueryRequest.getTags();
        int pageNum = userQueryRequest.getPageNum();

        BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();

        // 必须包含所有标签
        if (CollectionUtils.isNotEmpty(tags)) {
            for (String tag : tags) {
                boolQueryBuilder.filter(QueryBuilders.termQuery("tags", tag));
            }
        }

        if (StringUtils.isNotBlank(keyword)) {
            boolQueryBuilder.should(QueryBuilders.matchQuery("username", keyword));
            boolQueryBuilder.should(QueryBuilders.matchQuery("signature", keyword));
            boolQueryBuilder.minimumShouldMatch(1);
        }

        // 分页（es的起始页为0）
        PageRequest pageRequest = PageRequest.of(pageNum - 1, 10);
        // 构造查询
        NativeSearchQuery searchQuery = new NativeSearchQueryBuilder().withQuery(boolQueryBuilder)
                .withPageable(pageRequest).build();
        SearchHits<UserEsDTO> searchHits = elasticsearchRestTemplate.search(searchQuery, UserEsDTO.class);
        Page<UserInfoVo> page = new Page<>();
        page.setTotal(searchHits.getTotalHits());
        // 查出结果后，从 db 获取最新动态数据（比如点赞数）
        if (searchHits.hasSearchHits()) {
            List<SearchHit<UserEsDTO>> searchHitList = searchHits.getSearchHits();
            List<Long> uids = searchHitList.stream().map(searchHit -> searchHit.getContent().getUid())
                    .collect(Collectors.toList());
            // 从数据库中取出更完整的数据
            List<UserInfoVo> userInfoVoList = userFeignService.getUserListByUids(uids);
            if(userInfoVoList != null){
                page.setRecords(userInfoVoList);
            }
            return page;
        } else {
            page.setRecords(new ArrayList<>());
        }
        return page;
    }
}
