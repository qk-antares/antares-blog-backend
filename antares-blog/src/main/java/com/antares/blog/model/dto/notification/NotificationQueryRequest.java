package com.antares.blog.model.dto.notification;

import lombok.Data;
import com.antares.common.utils.PageRequest;

import java.io.Serializable;

@Data
public class NotificationQueryRequest extends PageRequest implements Serializable {
    private static final long serialVersionUID = 1L;
}
