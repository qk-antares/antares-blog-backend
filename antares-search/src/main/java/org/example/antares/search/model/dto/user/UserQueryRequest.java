package org.example.antares.search.model.dto.user;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.example.antares.common.utils.PageRequest;

import java.io.Serializable;
import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
public class UserQueryRequest extends PageRequest implements Serializable {
    private String keyword;
    private Long uid;
    private List<String> tags;

    private static final long serialVersionUID = 1L;
}
