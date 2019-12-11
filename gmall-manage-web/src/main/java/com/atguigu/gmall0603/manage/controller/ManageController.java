package com.atguigu.gmall0603.manage.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.atguigu.gmall0603.bean.*;
import com.atguigu.gmall0603.service.ManageService;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@CrossOrigin
public class ManageController {

    // @ResponseBody jackson.jar  gson.jar
    // 100 10 1000
    @Reference
    private ManageService manageService;

    @RequestMapping("getCatalog1")
    public List<BaseCatalog1> getCatalog1(){
        return manageService.getCatalog1();
    }
    @RequestMapping("getCatalog2")
    public List<BaseCatalog2> getCatalog2(String catalog1Id){
        return manageService.getCatalog2(catalog1Id);

    }
    @RequestMapping("getCatalog3")
    public List<BaseCatalog3> getCatalog3(String catalog2Id){
        return manageService.getCatalog3(catalog2Id);

    }
    // http://localhost:8082/attrInfoList?catalog3Id=61
    @RequestMapping("attrInfoList")
    public List<BaseAttrInfo> attrInfoList(String catalog3Id){
        // 调用服务层获取数据
        return manageService.getAttrInfoList(catalog3Id);
    }
    // 接收前台传递过来的数据
    // json 字符串如何转化成java 对象！@RequestBody
    @RequestMapping("saveAttrInfo")
    public void saveAttrInfo(@RequestBody BaseAttrInfo baseAttrInfo){
        // 前台数据都被封装到该对象中baseAttrInfo
        manageService.saveAttrInfo(baseAttrInfo);
    }

    // http://localhost:8082/getAttrValueList?attrId=23
    // baseAttrInfo.id = baseAttrValue.attrId
    @RequestMapping("getAttrValueList")
    public List<BaseAttrValue> getAttrValueList(String attrId){
        // select * from baseAttrValue where attrId = ?
        // 根据平台属性Id = baseAttrValue.attrId
        // select * from baseAttrInfo where id = attrId  baseAttrInfo

        BaseAttrInfo baseAttrInfo = manageService.getAttrInfo(attrId);
        // return manageService.getAttrValueList(attrId);
        // baseAttrInfo.getAttrValueList();
        return baseAttrInfo.getAttrValueList();
    }

}
