package org.example.antares.blog.model.dto.star;

import lombok.Data;
import org.example.antares.common.utils.PageRequest;

import java.io.Serializable;

@Data
public class StarBookQueryRequest extends PageRequest implements Serializable {
    private static final long serialVersionUID = 2222903543143705835L;

    private Long bookId;
}
