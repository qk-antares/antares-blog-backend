package com.antares.search.manager;

import com.antares.search.datasource.DataSource;
import com.antares.search.datasource.DataSourceRegistry;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.extern.slf4j.Slf4j;
import com.antares.common.exception.BusinessException;
import com.antares.common.model.enums.AppHttpCodeEnum;
import com.antares.search.model.dto.SearchRequest;
import com.antares.search.model.enums.SearchTypeEnum;
import com.antares.search.model.vo.SearchVo;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestBody;

import javax.annotation.Resource;
import java.util.List;

@Component
@Slf4j
public class SearchFacade {
    @Resource
    private DataSourceRegistry dataSourceRegistry;

    public SearchVo search(@RequestBody SearchRequest searchRequest) {
        String type = searchRequest.getType();
        SearchTypeEnum searchTypeEnum = SearchTypeEnum.getEnumByValue(type);

        String searchText = searchRequest.getKeyword();
        int pageNum = searchRequest.getPageNum();
        int pageSize = searchRequest.getPageSize();
        List<String> tags = searchRequest.getTags();
        if (searchTypeEnum == null) {
            throw new BusinessException(AppHttpCodeEnum.PARAMS_ERROR, "未指定查询类型");
        } else {
            SearchVo searchVO = new SearchVo();
            DataSource<?> dataSource = dataSourceRegistry.getDataSourceByType(type);
            Page<?> page = dataSource.doSearch(searchText, pageNum, pageSize, tags);
            searchVO.setPageData(page);
            return searchVO;
        }
    }
}
