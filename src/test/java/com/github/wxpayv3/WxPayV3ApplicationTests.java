package com.github.wxpayv3;

import com.github.wxpayv3.config.WxPayConfig;
import org.apache.http.impl.client.CloseableHttpClient;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.security.PrivateKey;

@SpringBootTest
class WxPayV3ApplicationTests {


    @Autowired
    WxPayConfig wxPayConfig;
    @Test
    void contextLoads() throws JSONException {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("key","哈哈哈");

        String s = jsonObject.toString();
        System.out.println(s);
    }

    @Test
    void TestRead(){
        String apiV3Key = wxPayConfig.getApiV3Key();
        System.out.println(apiV3Key);

    }


//    @Test
////    获取商户私钥
//    void getPrivateKey(){
//        PrivateKey privateKey = wxPayConfig.getPrivateKey();
//        System.out.println(privateKey);
//    }



    @Test
    void getClient(){
        CloseableHttpClient wxKeyClient = wxPayConfig.getWxKeyClient();
        System.out.println(wxKeyClient);
    }

}
