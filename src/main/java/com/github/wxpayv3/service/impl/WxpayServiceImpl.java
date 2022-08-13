package com.github.wxpayv3.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.github.wxpayv3.config.WxPayConfig;
import com.github.wxpayv3.entity.OrderInfo;
import com.github.wxpayv3.enums.OrderStatus;
import com.github.wxpayv3.enums.wxpay.WxApiType;
import com.github.wxpayv3.enums.wxpay.WxNotifyType;
import com.github.wxpayv3.service.OrderInfoService;
import com.github.wxpayv3.service.WxPayService;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;


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
        map.put("out_trade_no",orderNo);

        String s = JSONObject.toJSONString(map);

        HttpPost httpPost = new HttpPost(url);
        StringEntity stringEntity = new StringEntity(s, "utf-8");
        stringEntity.setContentType(ContentType.APPLICATION_JSON.toString());

        httpPost.setEntity(stringEntity);


        CloseableHttpClient wxKeyClient = wxPayConfig.getWxKeyClient();
        wxKeyClient.execute(httpPost);

        orderInfoService.updateOrderStatus(orderNo, OrderStatus.CANCEL);
    }

    @Override
    public String queryOrder(String orderNo) throws IOException {
        String url=String.format(WxApiType.ORDER_QUERY_BY_NO.getType(),orderNo);
        url=wxPayConfig.getDomain().concat(url).concat("?mchid=").concat(wxPayConfig.getMchId());

        log.debug("url:{}",url);
        HttpGet httpGet = new HttpGet(url);

        httpGet.setHeader("Accept","application/json");
        CloseableHttpClient wxKeyClient = wxPayConfig.getWxKeyClient();
        CloseableHttpResponse response = wxKeyClient.execute(httpGet);

        try {
            String res = EntityUtils.toString(response.getEntity());
            int statusCode = response.getStatusLine().getStatusCode();

            if (statusCode==200){
                log.info("处理成功，返回结果200+",res);
            }else if (statusCode==204){
                log.info("处理成功，返回结果204+",res);
            }else {
                log.error("查询失败",statusCode);
                throw new RuntimeException("查询失败");

            }

            return res;
        } finally {
            response.close();
        }




    }

}
