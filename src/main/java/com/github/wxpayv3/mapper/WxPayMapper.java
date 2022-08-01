package com.github.wxpayv3.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.github.wxpayv3.entity.OrderInfo;
import org.apache.ibatis.annotations.Mapper;

/**
 * @Author wang
 * @Date 2022/8/1 21:57
 * @Describe
 */
@Mapper
public interface WxPayMapper extends BaseMapper<OrderInfo> {
}
