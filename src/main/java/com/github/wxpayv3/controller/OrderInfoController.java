package com.github.wxpayv3.controller;

import com.github.wxpayv3.entity.OrderInfo;
import com.github.wxpayv3.service.OrderInfoService;
import com.github.wxpayv3.vo.R;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.GetMapping;
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
}
