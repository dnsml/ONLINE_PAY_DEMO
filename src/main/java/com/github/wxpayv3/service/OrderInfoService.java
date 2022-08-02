package com.github.wxpayv3.service;

import com.github.wxpayv3.entity.OrderInfo;

/**
 * @Author wang
 * @Date 2022/8/2 18:02
 * @Describe
 */
public interface OrderInfoService {

    /**
     * 创建订单
     * @return OrderInfo
     */
     OrderInfo creatOrderInfo(Long productId);


     void saveCodeUrl(String orderNo,String codeUrl);

}
