package com.dpwgc.fastim.util;


/**
 * 接口数据返回模板
 */
public class ResultUtil<T> {

    private Integer code;   //状态码
    private String msg;     //提示信息
    private T data;         //具体内容

    public Integer getCode() {
        return code;
    }

    public void setCode(Integer code) {
        this.code = code;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }
}
