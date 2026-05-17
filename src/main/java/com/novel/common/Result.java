package com.novel.common;

import lombok.Data;

@Data
public class Result<T> {

    private int code;
    private String message;
    private T data;

    private Result() {}

    public static <T> Result<T> success(T data) {
        Result<T> result = new Result<>();
        result.code = ResultCode.SUCCESS.getCode();
        result.message = ResultCode.SUCCESS.getMessage();
        result.data = data;
        return result;
    }

    public static <T> Result<T> success() {
        return success(null);
    }

    public static <T> Result<T> failed(String message) {
        Result<T> result = new Result<>();
        result.code = ResultCode.FAILED.getCode();
        result.message = message;
        return result;
    }

    public static <T> Result<T> failed(ResultCode resultCode) {
        Result<T> result = new Result<>();
        result.code = resultCode.getCode();
        result.message = resultCode.getMessage();
        return result;
    }

    public static <T> Result<T> failed(ResultCode resultCode, String message) {
        Result<T> result = new Result<>();
        result.code = resultCode.getCode();
        result.message = message;
        return result;
    }

    public static <T> Result<T> validateFailed(String message) {
        Result<T> result = new Result<>();
        result.code = ResultCode.VALIDATE_FAILED.getCode();
        result.message = message;
        return result;
    }

    public static <T> Result<T> unauthorized(String message) {
        Result<T> result = new Result<>();
        result.code = ResultCode.UNAUTHORIZED.getCode();
        result.message = message;
        return result;
    }
}