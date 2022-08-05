package com.github.wxpayv3.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.github.wxpayv3.entity.OrderInfo;
import com.github.wxpayv3.entity.Product;
import com.github.wxpayv3.enums.OrderStatus;
import com.github.wxpayv3.mapper.OrderInfoMapper;
import com.github.wxpayv3.mapper.ProductMapper;
import com.github.wxpayv3.service.OrderInfoService;
import com.github.wxpayv3.util.OrderNoUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

/**
 * @Author wang
 * @Date 2022/8/2 18:02
 * @Describe
 */
@Service
@Slf4j
public class OrderInfoServiceImpl implements OrderInfoService {

    @Resource
    private ProductMapper productMapper;
    @Resource
    private OrderInfoMapper orderInfoMapper;

    @Override
    public OrderInfo creatOrderInfo(Long productId) {

//        查询当前用户未支付订单
        OrderInfo unpaidOrder= this.getUnpaidOrder(productId);
        if (unpaidOrder!=null){
            return unpaidOrder;
        }


        log.info("生成订单");
        //        查询商品信息
        Product product = productMapper.selectById(productId);

        //        生成订单
        OrderInfo orderInfo = new OrderInfo();
        orderInfo.setOrderNo(OrderNoUtils.getOrderNo());
        orderInfo.setTitle(product.getTitle());
        orderInfo.setProductId(productId);
        orderInfo.setTotalFee(product.getPrice());
        orderInfo.setOrderStatus(OrderStatus.NOTPAY.getType());

        //        todo 用户id写死
        orderInfo.setUserId(123456L);

        orderInfoMapper.insert(orderInfo);
        return orderInfo;
    }

    @Override
    public void saveCodeUrl(String orderNo,String codeUrl) {

        OrderInfo orderInfo = new OrderInfo();
        orderInfo.setCodeUrl(codeUrl);

        LambdaQueryWrapper<OrderInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OrderInfo::getOrderNo,orderNo);
        orderInfoMapper.update(orderInfo,wrapper);
    }

    @Override
    public List<OrderInfo> orderList() {

        QueryWrapper<OrderInfo> wrapper = new QueryWrapper<>();
        wrapper.orderByDesc("create_time");
        return orderInfoMapper.selectList(wrapper);

    }

    /**
     * 获取未支付订单
     * @param productId
     * @return
     */
    private OrderInfo getUnpaidOrder(Long productId) {

        LambdaQueryWrapper<OrderInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OrderInfo::getUserId,123456L)
                .eq(OrderInfo::getProductId,productId)
                .eq(OrderInfo::getOrderStatus,OrderStatus.NOTPAY.getType());
        return orderInfoMapper.selectOne(wrapper);

    }
}
