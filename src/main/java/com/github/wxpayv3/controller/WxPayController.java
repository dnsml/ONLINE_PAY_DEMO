package com.github.wxpayv3.controller;

import com.github.wxpayv3.service.WxPayService;
import com.github.wxpayv3.vo.R;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.Map;

/**
 * @Author wang
 * @Date 2022/8/1 21:55
 * @Describe
 */
@RestController
@RequestMapping("/api/wx-pay")
@Api(tags = "网站支付微信API")
@Slf4j
public class WxPayController {

    @Resource
    private WxPayService wxPayService;


    /**
     * 生成二维码
     */
    @PostMapping("/native/{productId}")
    @ApiOperation("调用统一下单接口，生成二维码")
    public R nativePay(@PathVariable("productId") Long productId){

        try {
            Map map= wxPayService.nativePay(productId);
            return R.ok(map);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
