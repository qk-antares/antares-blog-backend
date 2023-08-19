package org.example.antares.common.model.response;

import org.example.antares.common.model.enums.AppHttpCodeEnum;

import java.io.Serializable;
import java.util.HashMap;

public class R extends HashMap<String, Object> implements Serializable {
    private static final long serialVersionUID = 6682215287252208284L;

    public R() {
        put("code", AppHttpCodeEnum.SUCCESS.getCode());
        put("msg", AppHttpCodeEnum.SUCCESS.getMsg());
    }

    public static R ok() {
        return new R();
    }

    public static R ok(Object data){
        R r = ok();
        r.put("data", data);
        return r;
    }

    public R put(String key, Object value) {
        super.put(key, value);
        return this;
    }

    public static R error() {
        return error(AppHttpCodeEnum.INTERNAL_SERVER_ERROR.getCode(), AppHttpCodeEnum.INTERNAL_SERVER_ERROR.getMsg());
    }

    public static R error(AppHttpCodeEnum appHttpCodeEnum){
        return error(appHttpCodeEnum.code, appHttpCodeEnum.msg);
    }

    public static R error(int code, String msg) {
        R r = new R();
        r.put("code", code);
        r.put("msg", msg);
        return r;
    }

    public Integer getCode(){
        return (Integer) this.get("code");
    }

    public Object getData(){
        return this.get("data");
    }
}