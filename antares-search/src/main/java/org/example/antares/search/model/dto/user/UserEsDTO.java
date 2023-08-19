package org.example.antares.search.model.dto.user;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;

import java.io.Serializable;
import java.util.List;

@Document(indexName = "user")
@Data
public class UserEsDTO implements Serializable {
    private static final long serialVersionUID = -7554011624975708878L;
    @Id
    private Long uid;
    private String username;
    private String signature;
    private List<String> tags;
}
