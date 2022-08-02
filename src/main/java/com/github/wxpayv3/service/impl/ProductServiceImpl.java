package com.github.wxpayv3.service.impl;


import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.wxpayv3.entity.Product;
import com.github.wxpayv3.mapper.ProductMapper;
import com.github.wxpayv3.service.ProductService;
import org.springframework.stereotype.Service;


/**
 * @Author wang
 * @Date 2022/8/1 16:47
 * @Describe
 */

@Service
public class ProductServiceImpl extends ServiceImpl<ProductMapper, Product> implements ProductService {

}
