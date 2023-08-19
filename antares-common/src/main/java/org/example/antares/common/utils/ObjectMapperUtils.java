package org.example.antares.common.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.antares.common.model.enums.AppHttpCodeEnum;
import org.example.antares.common.exception.BusinessException;

public class ObjectMapperUtils {
    public static final ObjectMapper MAPPER = new ObjectMapper();

    public static String writeValueAsString(Object object) {
        try {
            return MAPPER.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            throw new BusinessException(AppHttpCodeEnum.INTERNAL_SERVER_ERROR);
        }
    }
}
