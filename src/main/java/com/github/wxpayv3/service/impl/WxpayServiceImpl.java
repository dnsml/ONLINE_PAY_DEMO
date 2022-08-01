package com.github.wxpayv3.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.github.wxpayv3.config.WxPayConfig;
import com.github.wxpayv3.entity.OrderInfo;
import com.github.wxpayv3.enums.OrderStatus;
import com.github.wxpayv3.enums.wxpay.WxApiType;
import com.github.wxpayv3.enums.wxpay.WxNotifyType;
import com.github.wxpayv3.mapper.WxPayMapper;
import com.github.wxpayv3.service.WxPayService;
import com.github.wxpayv3.util.OrderNoUtils;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;


/**
 * @Author wang
 * @Date 2022/8/1 21:57
 * @Describe
 */
@Service
public class WxpayServiceImpl implements WxPayService {

    @Resource
    private WxPayConfig wxPayConfig;

    @Resource
    private WxPayMapper wxPayMapper;


    /**
     *
     * @param productId
     * @return CODE_URL 和 订单号
     * @throws IOException
     */
    @Override
    public Map nativePay(Long productId) throws IOException {

//        生成订单 存入数据库
        OrderInfo orderInfo = new OrderInfo();
        orderInfo.setOrderNo(OrderNoUtils.getOrderNo());
        orderInfo.setTitle("TEST");
        orderInfo.setProductId(productId);
        orderInfo.setTotalFee(1);
        orderInfo.setOrderStatus(OrderStatus.NOTPAY.getType());

//        todo 用户id写死
        orderInfo.setUserId(123456L);





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

            HashMap<String, String> hashMap = new HashMap<>();

            hashMap.put("codeUrl",codeUrl);
            hashMap.put("orderNo",orderInfo.getOrderNo());
            orderInfo.setCodeUrl(codeUrl);
            wxPayMapper.insert(orderInfo);
            return hashMap;
        }  finally {
            response.close();
        }

    }
}
