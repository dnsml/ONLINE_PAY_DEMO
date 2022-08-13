package com.github.wxpayv3.task;

import com.alibaba.fastjson.JSONObject;
import com.github.wxpayv3.entity.OrderInfo;
import com.github.wxpayv3.enums.OrderStatus;
import com.github.wxpayv3.enums.wxpay.WxTradeState;
import com.github.wxpayv3.service.OrderInfoService;
import com.github.wxpayv3.service.PaymentInfoService;
import com.github.wxpayv3.service.WxPayService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @Author wang
 * @Date 2022/8/13 16:16
 * @Describe
 */

@Slf4j
@Component
public class WxPayTask {

    @Resource
    private OrderInfoService orderInfoService;

    @Resource
    private WxPayService wxPayService;

    @Resource
    private PaymentInfoService paymentInfoService;


    private final ReentrantLock lock=new ReentrantLock();




    /**
    * 秒 分 时 日 月 周
    * *： 每秒都执行
    * 1-3: 从第一秒开始执行 到第三秒结束
    * 0/3: 从第0秒开始，每三秒执行1一次
    * 1，2，3 在指定的第1，2，3秒执行
    * ?: 不指定
    * 日和周不能同时指定 如一个已经指定,另外一个用?
    * */
//    @Scheduled(cron = "* * * * * ?")
    public void task1(){
        log.info("task1执行");
    }

    @Scheduled(cron = "0/30 * * * * ?" )
    public void queryOvertimesOrders() throws IOException {


        if (lock.tryLock()){
            //        查询创建超过五分种并未支付的订单
            List<OrderInfo> orderInfoList= orderInfoService.queryOvertimesOrders(5);

            log.warn("查询超时订单");
            log.warn("超时订单:{}",orderInfoList);
            try {
                for (OrderInfo orderInfo : orderInfoList) {

                    String order = wxPayService.queryOrder(orderInfo.getOrderNo());
                    Map map = JSONObject.parseObject(order, Map.class);

    //              获取商户订单号
                    String outTradeNo = map.get("out_trade_no").toString();
    //            获取该订单的支付状态
                    String tradeState = map.get("trade_state").toString();

    //            如果是已经支付成功 就更新状态
                    if (WxTradeState.SUCCESS.getType().equals(tradeState)){
                        log.info("更新订单状态");
                        //          更新订单状态
                        orderInfoService.updateOrderStatus(outTradeNo,OrderStatus.SUCCESS);
                        log.info("日志记录");
                        paymentInfoService.creatPaymentInfo(map);
                    }

                    if (WxTradeState.NOTPAY.getType().equals(tradeState)){
                        wxPayService.cancelOrder(outTradeNo);
                        orderInfoService.updateOrderStatus(outTradeNo,OrderStatus.CLOSED);
                        paymentInfoService.creatPaymentInfo(map);
                    }

                }
            } finally {
                    lock.unlock();
            }

        }


    }
}
