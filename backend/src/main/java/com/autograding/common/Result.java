package com.autograding.common;

import lombok.Data;

@Data
public class Result<T> {
    private Integer code;
    private String msg;
    private T data;

    public static <T> Result<T> success(T data) {
        Result<T> m = new Result<>();
        m.setCode(200);
        m.setMsg("成功");
        m.setData(data);
        return m;
    }

    public static <T> Result<T> error(String msg) {
        Result<T> m = new Result<>();
        m.setCode(500);
        m.setMsg(msg);
        m.setData(null);
        return m;
    }
}