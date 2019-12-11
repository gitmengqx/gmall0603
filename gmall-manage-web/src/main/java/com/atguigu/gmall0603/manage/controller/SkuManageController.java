package com.atguigu.gmall0603.manage.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.atguigu.gmall0603.bean.SkuInfo;
import com.atguigu.gmall0603.bean.SkuLsInfo;
import com.atguigu.gmall0603.bean.SpuImage;
import com.atguigu.gmall0603.bean.SpuSaleAttr;
import com.atguigu.gmall0603.service.ListService;
import com.atguigu.gmall0603.service.ManageService;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@CrossOrigin
public class SkuManageController {

    @Reference
    private ManageService manageService;

    @Reference
    private ListService listService;

    // http://localhost:8082/spuImageList?spuId=60
    @RequestMapping("spuImageList")
    public List<SpuImage> getSpuImageList(SpuImage spuImage){
        return manageService.getSpuImageList(spuImage);
    }
    // http://localhost:8082/spuSaleAttrList?spuId=60
    @RequestMapping("spuSaleAttrList")
    public List<SpuSaleAttr> getSpuSaleAttrList(String spuId,SpuSaleAttr spuSaleAttr){
        // 调用服务层方法
        return manageService.getSpuSaleAttrList(spuId);
    }

    // http://localhost:8082/saveSkuInfo | @RequestBody接收前台传递的参数{json} ---> java 对象
    @RequestMapping("saveSkuInfo")
     public void saveSkuInfo(@RequestBody SkuInfo skuInfo){
        // 调用服务层
        manageService.saveSkuInfo(skuInfo);
    }

    // http://localhost:8082/onSale=skuId
    @RequestMapping("onSale")
    public void onSale(String skuId){

         // 获取skuInfo 数据
        SkuInfo skuInfo = manageService.getSkuInfo(skuId);

        SkuLsInfo skuLsInfo = new SkuLsInfo();
        // skuLsInfo 赋值 属性拷贝
        BeanUtils.copyProperties(skuInfo,skuLsInfo);

        // 调用服务层
        listService.saveSkuLsInfo(skuLsInfo);
    }
}
