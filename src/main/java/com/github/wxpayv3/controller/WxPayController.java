package com.github.wxpayv3.controller;

import com.alibaba.fastjson.JSONObject;
import com.github.wxpayv3.config.WxPayConfig;
import com.github.wxpayv3.entity.OrderInfo;
import com.github.wxpayv3.enums.OrderStatus;

import com.github.wxpayv3.service.OrderInfoService;
import com.github.wxpayv3.service.PaymentInfoService;
import com.github.wxpayv3.service.WxPayService;
import com.github.wxpayv3.util.HttpUtils;
import com.github.wxpayv3.vo.R;
import com.wechat.pay.contrib.apache.httpclient.auth.Verifier;
import com.wechat.pay.contrib.apache.httpclient.cert.CertificatesManager;
import com.wechat.pay.contrib.apache.httpclient.constant.WechatPayHttpHeaders;
import com.wechat.pay.contrib.apache.httpclient.notification.Notification;
import com.wechat.pay.contrib.apache.httpclient.notification.NotificationHandler;
import com.wechat.pay.contrib.apache.httpclient.notification.NotificationRequest;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;

import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

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
    @Resource
    private WxPayConfig wxPayConfig;
    @Resource
    private OrderInfoService orderInfoService;
    @Resource
    private PaymentInfoService paymentInfoService;

    private final ReentrantLock lock=new ReentrantLock();


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

    /**
     * 接收微信通知
     */
   @PostMapping("/native/notify")
    public String nativeNotify(HttpServletRequest request, HttpServletResponse response){

       try {
           String data = HttpUtils.readData(request);

           CertificatesManager certificatesManager = CertificatesManager.getInstance();
           Verifier verifier = certificatesManager.getVerifier(wxPayConfig.getMchId());

           String nonce = request.getHeader(WechatPayHttpHeaders.WECHAT_PAY_NONCE);
           String signature = request.getHeader(WechatPayHttpHeaders.WECHAT_PAY_SIGNATURE);
           String timestamp = request.getHeader(WechatPayHttpHeaders.WECHAT_PAY_TIMESTAMP);
           String serial = request.getHeader(WechatPayHttpHeaders.WECHAT_PAY_SERIAL);



//           构建request 传输必要参数
           NotificationRequest  builder = new NotificationRequest.Builder()
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


    /**
     * 取消订单
     * @param orderNo 订单号
     * @return
     * @throws IOException
     */
   @PostMapping("/cancel/{orderNo}")
   public R  cancelOrder(@PathVariable String orderNo) throws IOException {
       wxPayService.cancelOrder(orderNo);
       String message="订单已取消";
       return R.ok(message);
   }


    /**
     * 查单接口
     */
    @ApiOperation("查询订单")
    @GetMapping("/query-order-status/{orderNo}")
    public R queryOrder(@PathVariable String orderNo) throws IOException {

        String str = wxPayService.queryOrder(orderNo);
        return R.ok(str);
    }

}
