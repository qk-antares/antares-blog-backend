package org.example.antares.common.utils;


import org.example.antares.common.exception.BusinessException;
import org.example.antares.common.model.enums.AppHttpCodeEnum;

/**
 * 抛异常工具类
 */
public class ThrowUtils {

    /**
     * 条件成立则抛异常
     *
     * @param condition
     * @param runtimeException
     */
    public static void throwIf(boolean condition, RuntimeException runtimeException) {
        if (condition) {
            throw runtimeException;
        }
    }

    /**
     * 条件成立则抛异常
     *
     * @param condition
     * @param errorCode
     */
    public static void throwIf(boolean condition, AppHttpCodeEnum appHttpCodeEnum) {
        throwIf(condition, new BusinessException(appHttpCodeEnum));
    }

    /**
     * 条件成立则抛异常
     *
     * @param condition
     * @param errorCode
     * @param message
     */
    public static void throwIf(boolean condition, AppHttpCodeEnum appHttpCodeEnum, String msg) {
        throwIf(condition, new BusinessException(appHttpCodeEnum, msg));
    }
}
