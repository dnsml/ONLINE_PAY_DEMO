package com.github.wxpayv3.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.github.wxpayv3.entity.PaymentInfo;
import com.github.wxpayv3.enums.PayType;
import com.github.wxpayv3.mapper.PaymentInfoMapper;
import com.github.wxpayv3.service.PaymentInfoService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.Map;

/**
 * @Author wang
 * @Date 2022/8/5 17:32
 * @Describe
 */
@Service
@Slf4j
public class PaymentInfoServiceImpl implements PaymentInfoService {

    @Resource
    private PaymentInfoMapper paymentInfoMapper;

    @Override
    public void creatPaymentInfo(Map map) {

        log.info("订单信息:{}",map);

//      订单号
        String outTradeNo = map.get("out_trade_no").toString();
//      业务编号
        String transactionId = map.get("transaction_id").toString();
//      支付类型
        String tradeType = map.get("trade_type").toString();
//      交易状态
        String tradeState = map.get("trade_state").toString();
//      用户支付金额
        Map amount = (Map) map.get("amount");
//      用户实际支付金额
        String payerTotal = amount.get("payer_total").toString();

        BigDecimal total = new BigDecimal(payerTotal);


        PaymentInfo paymentInfo = new PaymentInfo();
        paymentInfo.setOrderNo(outTradeNo);
        paymentInfo.setPaymentType(tradeType);
        paymentInfo.setTradeType(PayType.WXPAY.getType());
        paymentInfo.setTransactionId(transactionId);
        paymentInfo.setTradeState(tradeState);
        paymentInfo.setPayerTotal(total);

        paymentInfoMapper.insert(paymentInfo);

    }

    @Override
    public String queryOrder(String orderNo) {

        LambdaQueryWrapper<PaymentInfo> wrapper = new LambdaQueryWrapper<>();

        wrapper.eq(PaymentInfo::getOrderNo,orderNo);
        PaymentInfo paymentInfo = paymentInfoMapper.selectOne(wrapper);

        if (paymentInfo!=null){
            return paymentInfo.getTransactionId();
        }
        return null;
    }
}
