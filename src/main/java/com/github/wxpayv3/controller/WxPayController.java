package com.github.wxpayv3.controller;


import com.alibaba.fastjson.JSONObject;
import com.github.wxpayv3.config.WxPayConfig;


import com.github.wxpayv3.entity.RefundInfo;
import com.github.wxpayv3.enums.OrderStatus;
import com.github.wxpayv3.enums.wxpay.WxApiType;
import com.github.wxpayv3.enums.wxpay.WxNotifyType;
import com.github.wxpayv3.enums.wxpay.WxRefundStatus;

import com.github.wxpayv3.service.OrderInfoService;
import com.github.wxpayv3.service.PaymentInfoService;
import com.github.wxpayv3.service.RefundInfoService;
import com.github.wxpayv3.service.WxPayService;


import com.github.wxpayv3.vo.R;


import com.wechat.pay.contrib.apache.httpclient.exception.NotFoundException;
import com.wechat.pay.contrib.apache.httpclient.exception.ParseException;
import com.wechat.pay.contrib.apache.httpclient.exception.ValidationException;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;


import java.util.HashMap;
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
    @Resource
    private WxPayConfig wxPayConfig;
    @Resource
    private OrderInfoService orderInfoService;
    @Resource
    private PaymentInfoService paymentInfoService;
    @Resource
    private RefundInfoService refundInfoService;




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
   @ApiOperation("接收微信通知")
    public String nativeNotify(HttpServletRequest request, HttpServletResponse response){
     return  wxPayService.readRequest(request);
   }


    /**
     * 取消订单
     * @param orderNo 订单号
     * @return
     * @throws IOException
     */
   @ApiOperation("取消订单")
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


    /**
     * 申请退款,生成订单
     * @return
     */
    @ApiOperation("申请退款")
    @PostMapping("/refunds/{orderNo}/{reason}")
    public R refundOrder(@PathVariable(name = "orderNo") String orderNo,@PathVariable(name = "reason") String reason) throws IOException, NotFoundException, ValidationException, ParseException {

        RefundInfo refundInfo = refundInfoService.refundOrder(orderNo, reason);

//        组装url
        String url=wxPayConfig.getDomain().concat(WxApiType.DOMESTIC_REFUNDS.getType());

        HttpPost httpPost = new HttpPost(url);
        HashMap<String, Object> map = new HashMap<>();

        map.put("out_trade_no",orderNo);
        map.put("out_refund_no",refundInfo.getRefundNo());
        map.put("reason",reason);
//        退款通知地址
        map.put("notify_url",wxPayConfig.getDomain().concat(WxNotifyType.REFUND_NOTIFY.getType()));

        HashMap<String, Object> amount = new HashMap<>();
        amount.put("refund",refundInfo.getRefund());
        amount.put("total",refundInfo.getTotalFee());
        amount.put("currency","CNY");
        map.put("amount",amount);

        String s = JSONObject.toJSONString(map);

        StringEntity stringEntity = new StringEntity(s, "utf-8");
        stringEntity.setContentType(ContentType.APPLICATION_JSON.toString());
        httpPost.setHeader("Accept","application/json");
        httpPost.setEntity(stringEntity);


        CloseableHttpClient wxKeyClient = wxPayConfig.getWxKeyClient();
        CloseableHttpResponse response = wxKeyClient.execute(httpPost);


        try {
            String res = EntityUtils.toString(response.getEntity());
            int statusCode = response.getStatusLine().getStatusCode();

            if (statusCode==500){
                log.error("500,系统超时");
            }else if (statusCode==403){
                log.error("403,退款失败");
            }

            Map resultMap = JSONObject.parseObject(res, Map.class);

            log.debug("退款结果通知，resultMap{}",resultMap);




            String outRefundNo = resultMap.get("out_refund_no").toString();
            String status = resultMap.get("status").toString();
            String refundId = resultMap.get("refund_id").toString();
//            String successTime = resultMap.get("success_time").toString();
//            String createTime = resultMap.get("create_time").toString();

//            Date successDate = new Date(successTime);
//            Date createDate = new Date(createTime);


            RefundInfo refundInfo1 = new RefundInfo();
            refundInfo1.setOrderNo(refundInfo.getOrderNo());
            refundInfo1.setRefundNo(outRefundNo);


            if (WxRefundStatus.PROCESSING.getType().equals(status)){
                refundInfo1.setRefundStatus(status);
            }

            refundInfo1.setRefundId(refundId);

//            refundInfo1.setUpdateTime(successDate);

            log.info("更新退款单:{}",refundInfo1);
            refundInfoService.updateInfo(refundInfo1);
            orderInfoService.updateOrderStatus(refundInfo.getOrderNo(), OrderStatus.REFUND_PROCESSING);
            return R.ok(refundInfo1);
        } finally {

            if (response!=null){
                response.close();
            }

        }



    }



    @ApiOperation("退款通知")
    @PostMapping("/refunds/notify")
    public R refundNotify(HttpServletRequest request,HttpServletResponse response) throws Exception {

        refundInfoService.readData(request);
        return R.ok("接收成功");

    }
}
