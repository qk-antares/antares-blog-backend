package org.example.antares.search.model.vo;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.Data;

import java.io.Serializable;

/**
 * 聚合搜索
 */
@Data
public class SearchVo implements Serializable {
    private Page<?> pageData;
}
