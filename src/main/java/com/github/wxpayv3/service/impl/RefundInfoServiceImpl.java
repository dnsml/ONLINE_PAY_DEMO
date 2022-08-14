package com.github.wxpayv3.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.github.wxpayv3.config.WxPayConfig;
import com.github.wxpayv3.entity.OrderInfo;
import com.github.wxpayv3.entity.RefundInfo;
import com.github.wxpayv3.enums.OrderStatus;
import com.github.wxpayv3.mapper.OrderInfoMapper;
import com.github.wxpayv3.mapper.RefundInfoMapper;
import com.github.wxpayv3.service.RefundInfoService;
import com.github.wxpayv3.util.HttpUtils;
import com.github.wxpayv3.util.OrderNoUtils;
import com.wechat.pay.contrib.apache.httpclient.auth.Verifier;
import com.wechat.pay.contrib.apache.httpclient.cert.CertificatesManager;
import com.wechat.pay.contrib.apache.httpclient.constant.WechatPayHttpHeaders;
import com.wechat.pay.contrib.apache.httpclient.exception.NotFoundException;
import com.wechat.pay.contrib.apache.httpclient.exception.ParseException;
import com.wechat.pay.contrib.apache.httpclient.exception.ValidationException;
import com.wechat.pay.contrib.apache.httpclient.notification.Notification;
import com.wechat.pay.contrib.apache.httpclient.notification.NotificationHandler;
import com.wechat.pay.contrib.apache.httpclient.notification.NotificationRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * @Author wang
 * @Date 2022/8/13 23:06
 * @Describe
 */
@Service
@Slf4j
public class RefundInfoServiceImpl implements RefundInfoService {

    @Resource
    private OrderInfoMapper orderInfoMapper;
    @Resource
    private RefundInfoMapper refundInfoMapper;
    @Resource
    private WxPayConfig wxPayConfig;

    @Override
    public RefundInfo refundOrder(String orderNo, String reason) {

        LambdaQueryWrapper<OrderInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OrderInfo::getOrderNo,orderNo);
        OrderInfo orderInfo = orderInfoMapper.selectOne(wrapper);

        RefundInfo refundInfo = new RefundInfo();
        refundInfo.setRefundNo(OrderNoUtils.getRefundNo());
        refundInfo.setOrderNo(orderInfo.getOrderNo());
        refundInfo.setTotalFee(orderInfo.getTotalFee());
        refundInfo.setRefund(orderInfo.getTotalFee());
        refundInfo.setRefundStatus(OrderStatus.REFUND_PROCESSING.getType());




        refundInfo.setReason(reason);

        refundInfoMapper.insert(refundInfo);


        return refundInfo;
    }

    @Override
    public void updateInfo(RefundInfo refundInfo) {

        LambdaQueryWrapper<RefundInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RefundInfo::getOrderNo,refundInfo.getOrderNo());

        refundInfoMapper.update(refundInfo,wrapper);


    }

    @Override
    public void readData(HttpServletRequest request) throws NotFoundException, ValidationException, ParseException {
        String data = HttpUtils.readData(request);

        String mchId = wxPayConfig.getMchId();
        CertificatesManager certificatesManager = CertificatesManager.getInstance();
        Verifier verifier = certificatesManager.getVerifier(mchId);


        String nonce = request.getHeader(WechatPayHttpHeaders.WECHAT_PAY_NONCE);
        String signature = request.getHeader(WechatPayHttpHeaders.WECHAT_PAY_SIGNATURE);
        String timestamp = request.getHeader(WechatPayHttpHeaders.WECHAT_PAY_TIMESTAMP);
        String serial = request.getHeader(WechatPayHttpHeaders.WECHAT_PAY_SERIAL);



        //           构建request 传输必要参数
        NotificationRequest builder = new NotificationRequest.Builder()
                .withTimestamp(timestamp)
                .withSerialNumber(serial)
                .withNonce(nonce)
                .withSignature(signature)
                .withBody(data)
                .build();

        NotificationHandler notificationHandler =
                new NotificationHandler(verifier,
                        wxPayConfig.getApiV3Key().getBytes(StandardCharsets.UTF_8));

//           验签解析请求体
        Notification notification = notificationHandler.parse(builder);
        log.debug("验签===：{}",notification);


//           解析密文
        String decryptData = notification.getDecryptData();
        log.debug("获取解密工具:{}",decryptData);

        Map decryptDataMap = JSONObject.parseObject(decryptData, Map.class);

        log.info("解密:{}",decryptDataMap);
    }
}
