package com.github.wxpayv3.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.github.wxpayv3.config.WxPayConfig;
import com.github.wxpayv3.entity.OrderInfo;
import com.github.wxpayv3.enums.OrderStatus;
import com.github.wxpayv3.enums.wxpay.WxApiType;
import com.github.wxpayv3.enums.wxpay.WxNotifyType;
import com.github.wxpayv3.mapper.OrderInfoMapper;
import com.github.wxpayv3.service.OrderInfoService;
import com.github.wxpayv3.service.PaymentInfoService;
import com.github.wxpayv3.service.WxPayService;
import com.github.wxpayv3.util.HttpUtils;
import com.wechat.pay.contrib.apache.httpclient.auth.Verifier;
import com.wechat.pay.contrib.apache.httpclient.cert.CertificatesManager;
import com.wechat.pay.contrib.apache.httpclient.constant.WechatPayHttpHeaders;
import com.wechat.pay.contrib.apache.httpclient.notification.Notification;
import com.wechat.pay.contrib.apache.httpclient.notification.NotificationHandler;
import com.wechat.pay.contrib.apache.httpclient.notification.NotificationRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.springframework.http.HttpEntity;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;


/**
 * @Author wang
 * @Date 2022/8/1 21:57
 * @Describe
 */
@Service
@Slf4j
public class WxpayServiceImpl implements WxPayService {

    @Resource
    private WxPayConfig wxPayConfig;
    @Resource
    private OrderInfoService orderInfoService;
    @Resource
    private PaymentInfoService paymentInfoService;

    @Resource
    private OrderInfoMapper orderInfoMapper;

    private final ReentrantLock lock=new ReentrantLock();


    /**
     *
     * @param productId
     * @return 返回CODE_URL 和 订单号
     * @throws IOException
     */
    @Override
    public Map nativePay(Long productId) throws IOException {

//        创建订单
        OrderInfo orderInfo = orderInfoService.creatOrderInfo(productId);


//        拼接请求地址
        HttpPost httpPost = new HttpPost(wxPayConfig.getDomain().concat(WxApiType.NATIVE_PAY.getType()));
        HashMap<String, Object> paramMap = new HashMap<>();
        paramMap.put("appid",wxPayConfig.getAppid());
        paramMap.put("mchid",wxPayConfig.getMchId());
        paramMap.put("description",orderInfo.getTitle());
        paramMap.put("out_trade_no",orderInfo.getOrderNo());
        paramMap.put("notify_url",wxPayConfig.getNotifyDomain().concat(WxNotifyType.NATIVE_NOTIFY.getType()));
        Map map = new HashMap<>();
        map.put("total",orderInfo.getTotalFee());
        map.put("currency","CNY");
        paramMap.put("amount",map);
        String reqdata = JSONObject.toJSONString(paramMap);

        // 请求body参数
        StringEntity entity = new StringEntity(reqdata,"utf-8");
        entity.setContentType("application/json");
        httpPost.setEntity(entity);
        httpPost.setHeader("Accept", "application/json");

        //完成签名并执行请求
        CloseableHttpClient httpClient = wxPayConfig.getWxKeyClient();
        CloseableHttpResponse response = httpClient.execute(httpPost);

        try {
            int statusCode = response.getStatusLine().getStatusCode();
            String resEntity = EntityUtils.toString(response.getEntity());
            if (statusCode == 200) { //处理成功
                System.out.println("成功，返回结果 = " + resEntity);
            } else if (statusCode == 204) { //处理成功，无返回Body
                System.out.println("成功");
            } else {
                System.out.println("下单失败 = " + statusCode+ ",返回结果 = " + resEntity);
                throw new IOException("request failed");
            }

            Map resultMap = JSONObject.parseObject(resEntity, Map.class);
            String codeUrl = (String) resultMap.get("code_url");

//            保存二维码
            orderInfoService.saveCodeUrl(orderInfo.getOrderNo(),codeUrl);

            OrderInfo info = new OrderInfo();
            info.setUpdateTime(new Date());
            LambdaQueryWrapper<OrderInfo> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(OrderInfo::getUpdateTime,info.getUpdateTime());

            orderInfoMapper.update(info,wrapper);

            HashMap<String, String> hashMap = new HashMap<>();
            hashMap.put("codeUrl",codeUrl);
            hashMap.put("orderNo",orderInfo.getOrderNo());
            return hashMap;
        }  finally {
            response.close();
        }

    }

    @Override
    public void cancelOrder(String orderNo) throws IOException {
        String url =  wxPayConfig.getDomain()
                .concat(String.format(WxApiType.CLOSE_ORDER_BY_NO.getType(),orderNo));

        HashMap<String, Object> map = new HashMap<>();
        String mchId = wxPayConfig.getMchId();
        map.put("mchid",mchId);
//        map.put("out_trade_no",orderNo);

        String s = JSONObject.toJSONString(map);

        HttpPost httpPost = new HttpPost(url);
        httpPost.setHeader("Accept","application/json");
        StringEntity stringEntity = new StringEntity(s, "utf-8");
        stringEntity.setContentType(ContentType.APPLICATION_JSON.toString());
        httpPost.setEntity(stringEntity);

        OrderInfo orderInfo = orderInfoService.OrderStatus(orderNo);

        CloseableHttpClient wxKeyClient=null;
        try {
            if (orderInfo.getOrderStatus().equals(OrderStatus.NOTPAY.getType())){
                wxKeyClient = wxPayConfig.getWxKeyClient();
                CloseableHttpResponse response = wxKeyClient.execute(httpPost);
                int statusCode = response.getStatusLine().getStatusCode();

                log.warn("状态码:{}",statusCode);



                orderInfoService.updateOrderStatus(orderNo, OrderStatus.CANCEL);

            }
        } finally {
            if (wxKeyClient!=null){
                wxKeyClient.close();
            }
        }


    }

    @Override
    public String queryOrder(String orderNo) throws IOException {
        String url = String.format(WxApiType.ORDER_QUERY_BY_NO.getType(), orderNo);
        url = wxPayConfig.getDomain().concat(url).concat("?mchid=").concat(wxPayConfig.getMchId());

        log.debug("url:{}", url);
        HttpGet httpGet = new HttpGet(url);
        httpGet.setHeader("Accept", "application/json");
        CloseableHttpResponse response = null;
        try {
            CloseableHttpClient wxKeyClient = wxPayConfig.getWxKeyClient();
            response = wxKeyClient.execute(httpGet);


            String res = EntityUtils.toString(response.getEntity());
            int statusCode = response.getStatusLine().getStatusCode();

            if (statusCode == 200) {
                log.info("处理成功，返回结果200+", res);
            } else if (statusCode == 204) {
                log.info("处理成功，返回结果204+", res);
            } else {
                log.error("查询失败", statusCode);
                throw new RuntimeException("查询失败");

            }

            return res;
        } finally {
            if (response!=null){
                response.close();
            }

        }


    }

    @Override
    public String readRequest(HttpServletRequest request) {
        try {
            String data = HttpUtils.readData(request);

            CertificatesManager certificatesManager = CertificatesManager.getInstance();
            Verifier verifier = certificatesManager.getVerifier(wxPayConfig.getMchId());

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

//           获取商品订单id
            String outTradeNo = decryptDataMap.get("out_trade_no").toString();

            /*尝试获取锁，成功ture 失败立即返回false*/
            if (lock.tryLock()) {

                try {
                    //           校验订单支付状态，接口幂等性
                    //           处理重复通知
                    OrderInfo orderInfo = orderInfoService.checkOrderStatus(outTradeNo);
                    if (orderInfo != null) {
                        //           更新订单支付状态
                        orderInfoService.updateOrderStatus(outTradeNo, OrderStatus.SUCCESS);
                        //           日志记录
                        paymentInfoService.creatPaymentInfo(decryptDataMap);

                    }
                } finally {
//                   主动释放锁
                    lock.unlock();

                }
            }
            Map body = JSONObject.parseObject(data, Map.class);

            log.info("id:{}", body.get("id"));
            log.info("body:{}", body);
            HashMap<String, Object> map = new HashMap<>();
            map.put("code", 200);
            map.put("message", "成功");
            return JSONObject.toJSONString(map);
        } catch (Exception e) {
            e.printStackTrace();
            HashMap<String, Object> map = new HashMap<>();
            map.put("code", 500);
            map.put("message", "失败");
            return JSONObject.toJSONString(map);

        }
    }

}
