package com.github.wxpayv3.controller;

import com.github.wxpayv3.entity.OrderInfo;
import com.github.wxpayv3.enums.OrderStatus;
import com.github.wxpayv3.service.OrderInfoService;
import com.github.wxpayv3.vo.R;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.List;

/**
 * @Author wang
 * @Date 2022/8/2 19:49
 * @Describe
 */
@Api(tags = "订单管理")
@RestController
@RequestMapping("/api/order-info")
public class OrderInfoController {

    @Resource
    private OrderInfoService orderInfoService;

    @GetMapping("/list")
    @ApiOperation("获取订单列表")
    public R orderList(){
        List<OrderInfo> orderInfoList=orderInfoService.orderList();

        return R.ok(orderInfoList);
    }
    @ApiOperation("获取订单支付状态")
    @GetMapping("/query-order-status/{orderNo}")
    public R queryOrderStatus(@PathVariable String orderNo){
        OrderInfo orderInfo = orderInfoService.OrderStatus(orderNo);

        if (orderInfo==null){
            throw new RuntimeException("未查询到订单");
        }
        if (StringUtils.isEmpty(orderInfo.getOrderStatus())){
            throw new RuntimeException("未查询到订单状态");
        }

        if (OrderStatus.SUCCESS.getType().equals(orderInfo.getOrderStatus())){
            return R.ok(0,orderInfo);
        }
       return R.ok(orderInfo);
    }
}
