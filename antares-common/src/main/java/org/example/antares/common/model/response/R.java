package org.example.antares.common.model.response;

import lombok.Data;
import org.example.antares.common.model.enums.AppHttpCodeEnum;

import java.io.Serializable;

@Data
public class R<T> implements Serializable {
	private static final long serialVersionUID = 6682215287252208284L;

	private int code;
	private String msg;
	private T data;

	public R(int code, String msg){
		this.code = code;
		this.msg = msg;
	}

	public static R ok() {
		return new R(AppHttpCodeEnum.SUCCESS.getCode(), AppHttpCodeEnum.SUCCESS.getMsg());
	}

	public static <T> R<T> ok(T data){
		R<T> r = new R<>(AppHttpCodeEnum.SUCCESS.getCode(), AppHttpCodeEnum.SUCCESS.getMsg());
		r.setData(data);
		return r;
	}

	public static R error() {
		return new R(AppHttpCodeEnum.INTERNAL_SERVER_ERROR.getCode(), AppHttpCodeEnum.INTERNAL_SERVER_ERROR.getMsg());
	}

	public static R error(AppHttpCodeEnum appHttpCodeEnum){
		return new R(appHttpCodeEnum.code, appHttpCodeEnum.msg);
	}

	public static R error(int code, String msg) {
		return new R(code, msg);
	}
}