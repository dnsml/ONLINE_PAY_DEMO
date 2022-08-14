package com.github.wxpayv3.service;

import com.github.wxpayv3.entity.OrderInfo;
import com.github.wxpayv3.entity.RefundInfo;
import com.github.wxpayv3.enums.OrderStatus;

import java.util.List;

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


    /**
     * 保存二维码
     * @param orderNo 订单编号
     * @param codeUrl  二维码地址
     */
     void saveCodeUrl(String orderNo,String codeUrl);


    /**
     * 获取订单列表
     * @return
     */
    List<OrderInfo> orderList();

    void updateOrderStatus(String outTradeNo, OrderStatus success);

    OrderInfo checkOrderStatus(String outTradeNo);
    OrderInfo OrderStatus(String outTradeNo);


    List<OrderInfo> queryOvertimesOrders(int minutes);


}
