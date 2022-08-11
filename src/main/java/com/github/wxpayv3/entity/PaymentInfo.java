package com.github.wxpayv3.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;

@Data
@TableName("t_payment_info")
public class PaymentInfo extends BaseEntity{

    private String orderNo;//商品订单编号

    private String transactionId;//支付系统交易编号

    private String paymentType;//支付类型

    private String tradeType;//交易类型

    private String tradeState;//交易状态

    private BigDecimal payerTotal;//支付金额(分)

    private String content;//通知参数
}
