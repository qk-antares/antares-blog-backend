package org.example.antares.search.manager;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.extern.slf4j.Slf4j;
import org.example.antares.common.exception.BusinessException;
import org.example.antares.common.model.enums.AppHttpCodeEnum;
import org.example.antares.search.datasource.*;
import org.example.antares.search.model.dto.SearchRequest;
import org.example.antares.search.model.enums.SearchTypeEnum;
import org.example.antares.search.model.vo.SearchVO;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestBody;

import javax.annotation.Resource;
import java.util.List;

@Component
@Slf4j
public class SearchFacade {
    @Resource
    private DataSourceRegistry dataSourceRegistry;

    public SearchVO search(@RequestBody SearchRequest searchRequest) {
        String type = searchRequest.getType();
        SearchTypeEnum searchTypeEnum = SearchTypeEnum.getEnumByValue(type);

        String searchText = searchRequest.getKeyword();
        int pageNum = searchRequest.getPageNum();
        int pageSize = searchRequest.getPageSize();
        List<String> tags = searchRequest.getTags();
        if (searchTypeEnum == null) {
            throw new BusinessException(AppHttpCodeEnum.PARAMS_ERROR, "未指定查询类型");
        } else {
            SearchVO searchVO = new SearchVO();
            DataSource<?> dataSource = dataSourceRegistry.getDataSourceByType(type);
            Page<?> page = dataSource.doSearch(searchText, pageNum, pageSize, tags);
            searchVO.setPageData(page);
            return searchVO;
        }
    }
}
