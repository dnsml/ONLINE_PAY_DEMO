package com.github.wxpayv3.service;

import com.baomidou.mybatisplus.extension.service.IService;

import java.io.IOException;
import java.util.Map;

/**
 * @Author wang
 * @Date 2022/8/1 21:56
 * @Describe
 */
public interface WxPayService  {
    Map nativePay(Long productId) throws IOException;
}
