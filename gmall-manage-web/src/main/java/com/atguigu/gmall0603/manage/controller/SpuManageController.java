package com.atguigu.gmall0603.manage.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.atguigu.gmall0603.bean.BaseSaleAttr;
import com.atguigu.gmall0603.bean.SpuInfo;
import com.atguigu.gmall0603.service.ManageService;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@CrossOrigin
public class SpuManageController {

    // 调用服务层
    @Reference
    private ManageService manageService;

    // http://localhost:8082/spuList?catalog3Id=61 {"":""}
    // springmvc 对象传值 传递过来的参数与实体类的属性名称一致的时候，可以直接写实体类
    @RequestMapping("spuList")
    public List<SpuInfo> getSpuList(SpuInfo spuInfo){

        return manageService.getSpuInfoList(spuInfo);
    }

    @RequestMapping("baseSaleAttrList")
    public List<BaseSaleAttr> getBaseSaleAttrList(){
        return manageService.getBaseSaleAttrList();
    }

    @RequestMapping("saveSpuInfo")
    public void saveSpuInfo(@RequestBody SpuInfo spuInfo){
        // 调用服务层的保存方法
        manageService.saveSpuInfo(spuInfo);

    }
}
