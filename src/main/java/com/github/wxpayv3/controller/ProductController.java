package com.github.wxpayv3.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.github.wxpayv3.entity.Product;
import com.github.wxpayv3.service.ProductService;
import com.github.wxpayv3.vo.R;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.Date;
import java.util.List;

/**
 * @Author wang
 * @Date 2022/8/1 15:30
 * @Describe
 */

@Api(tags = "商品管理")
@RestController
@RequestMapping("/api/product")
public class ProductController {


    @Resource
    private ProductService productService;

    @ApiOperation("测试接口")
    @GetMapping("/test")
    public R sayStr(){

       String str="这是个测试";
        Date date = new Date();
        return R.ok(date);
    }
//    @RequestParam(required = false,defaultValue = "1")Integer page,
//    @RequestParam(required = false,defaultValue = "3")Integer pageSize
//                    )
    @GetMapping("/list")
    @ApiOperation("获取全部商品")
    public R getAll(){

        Page<Product> objectPage = new Page<>();
        Page<Product> productPage = productService.page(objectPage);
        return R.ok(productPage);

    }
}
