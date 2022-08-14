package com.github.wxpayv3.service;

import com.github.wxpayv3.entity.RefundInfo;
import com.wechat.pay.contrib.apache.httpclient.exception.NotFoundException;
import com.wechat.pay.contrib.apache.httpclient.exception.ParseException;
import com.wechat.pay.contrib.apache.httpclient.exception.ValidationException;

import javax.servlet.http.HttpServletRequest;

/**
 * @Author wang
 * @Date 2022/8/13 23:06
 * @Describe
 */
public interface RefundInfoService {

    RefundInfo refundOrder(String orderNo, String reason);

    void updateInfo(RefundInfo refundInfo);

    void readData(HttpServletRequest request) throws Exception;
}
