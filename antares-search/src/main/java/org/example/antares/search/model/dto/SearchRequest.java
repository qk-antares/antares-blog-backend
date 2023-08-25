package org.example.antares.search.model.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.example.antares.common.utils.PageRequest;

import java.io.Serializable;
import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
public class SearchRequest extends PageRequest implements Serializable {
    private static final long serialVersionUID = -8567834659750891431L;
    /**
     * 搜索词
     */
    private String keyword;

    /**
     * 类型
     */
    private String type;

    /**
     * 标签
     */
    private List<String> tags;
}
