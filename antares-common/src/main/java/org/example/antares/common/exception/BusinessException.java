package org.example.antares.common.exception;


import org.example.antares.common.model.enums.AppHttpCodeEnum;

/**
 * @author Antares
 */
public class BusinessException extends RuntimeException{
    private int code;

    private String msg;

    public int getCode() {
        return code;
    }

    public String getMsg() {
        return msg;
    }

    public BusinessException(AppHttpCodeEnum httpCodeEnum) {
        super(httpCodeEnum.getMsg());
        this.code = httpCodeEnum.getCode();
        this.msg = httpCodeEnum.getMsg();
    }

    public BusinessException(AppHttpCodeEnum httpCodeEnum, String msg) {
        super(httpCodeEnum.getMsg());
        this.code = httpCodeEnum.getCode();
        this.msg = httpCodeEnum.getMsg() + "：" + msg;
    }
}