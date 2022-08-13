package com.github.wxpayv3.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @Author wang
 * @Date 2022/8/1 16:12
 * @Describe
 */

@Data
@NoArgsConstructor
@AllArgsConstructor
@ApiModel("统一响应实体")
public class R {

    @ApiModelProperty("状态码")
    private Integer code;
    @ApiModelProperty("描述")
    private String message;
    @ApiModelProperty("响应数据")
    private Object data;


    public static R ok(String message){
        R r = new R();
        r.setCode(200);
        r.setMessage(message);
        return r;
    }
    public static R ok(Object data){
        R r = new R();
        r.setCode(200);
        r.setMessage("成功");
        r.setData(data);
        return r;
    }

    public static R ok(Integer code,Object data){
        R r = new R();
        r.setCode(code);
        r.setMessage("成功");
        r.setData(data);
        return r;
    }


    public static R error(){
        R r = new R();
        r.setCode(500);
        r.setMessage("失败");
        return r;
    }
}
