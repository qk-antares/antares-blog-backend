package com.antares.search.datasource;

import com.antares.search.model.enums.SearchTypeEnum;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.HashMap;
import java.util.Map;

@Component
public class DataSourceRegistry {
    @Resource
    private CnBlogDataSource cnBlogDataSource;
    @Resource
    private CsdnDataSource csdnDataSource;
    @Resource
    private UserDataSource userDataSource;
    @Resource
    private ArticleDataSource articleDataSource;

    private Map<String, DataSource> typeDataSourceMap;

    @PostConstruct
    public void doInit() {
        typeDataSourceMap = new HashMap() {{
            put(SearchTypeEnum.BLOG.getValue(), articleDataSource);
            put(SearchTypeEnum.USER.getValue(), userDataSource);
            put(SearchTypeEnum.CNBLOG.getValue(), cnBlogDataSource);
            put(SearchTypeEnum.CSDN.getValue(), csdnDataSource);
        }};
    }

    public DataSource getDataSourceByType(String type) {
        if (typeDataSourceMap == null) {
            return null;
        }
        return typeDataSourceMap.get(type);
    }
}
