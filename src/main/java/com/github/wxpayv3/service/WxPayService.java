package com.github.wxpayv3.service;



import java.io.IOException;
import java.util.Map;

/**
 * @Author wang
 * @Date 2022/8/1 21:56
 * @Describe
 */
public interface WxPayService  {
    Map nativePay(Long productId) throws IOException;

    void cancelOrder(String orderNo) throws IOException;

    String queryOrder(String orderNo) throws IOException;
}
