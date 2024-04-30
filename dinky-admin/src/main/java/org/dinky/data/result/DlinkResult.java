package org.dinky.data.result;

import cn.hutool.core.date.DateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.dinky.data.enums.CodeEnum;

import java.io.Serializable;

/**
 *适配Dinky0.7版本的返回对象
 */

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DlinkResult<T> implements Serializable {
    private T datas;
    private Integer code;
    private String msg;
    private String time;

    public static <T> DlinkResult<T> succeed(String msg) {
        return of(null, CodeEnum.SUCCESS.getCode(), msg);
    }

    public static <T> DlinkResult<T> succeed(T model, String msg) {
        return of(model, CodeEnum.SUCCESS.getCode(), msg);
    }

    public static <T> DlinkResult<T> succeed(T model) {
        return of(model, CodeEnum.SUCCESS.getCode(), "");
    }

    public static <T> DlinkResult<T> data(T model) {
        return of(model, CodeEnum.SUCCESS.getCode(), "");
    }

    public static <T> DlinkResult<T> of(T datas, Integer code, String msg) {
        return new DlinkResult<>(datas, code, msg, new DateTime().toString());
    }

    public static <T> DlinkResult<T> failed(String msg) {
        return of(null, CodeEnum.ERROR.getCode(), msg);
    }

    public static <T> DlinkResult<T> notLogin(String msg) {
        return of(null, CodeEnum.NOTLOGIN.getCode(), msg);
    }

    public static <T> DlinkResult<T> failed(T model, String msg) {
        return of(model, CodeEnum.ERROR.getCode(), msg);
    }
}
