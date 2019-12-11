package com.atguigu.gmall0603.list.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.fastjson.JSON;
import com.atguigu.gmall0603.bean.*;
import com.atguigu.gmall0603.service.ListService;
import com.atguigu.gmall0603.service.ManageService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@Controller
public class ListController {

    @Reference
    private ListService listService;

    @Reference
    private ManageService manageService;

    @RequestMapping("list.html")
//    @ResponseBody
    public String search(SkuLsParams skuLsParams, HttpServletRequest request){


        // 给pageSize 设置条数
        skuLsParams.setPageSize(3);
        SkuLsResult skuLsResult = listService.search(skuLsParams);

        System.out.println(skuLsResult.getSkuLsInfoList());

        // 平台属性值Id
        List<String> attrValueIdList = skuLsResult.getAttrValueIdList();
        // 通过平台属性值Id ，查询平台属性
        List<BaseAttrInfo> baseAttrInfoList = manageService.getAttrList(attrValueIdList);
        // 如何记录用户查询的历史参数
        String urlParam=makeUrlParam(skuLsParams);

        // 声明一个面包屑的集合
        ArrayList<BaseAttrValue> baseAttrValueArrayList = new ArrayList<>();
        if (baseAttrInfoList!=null&& baseAttrInfoList.size()>0){
            // 循环
            for (Iterator<BaseAttrInfo> iterator = baseAttrInfoList.iterator(); iterator.hasNext(); ) {
                BaseAttrInfo baseAttrInfo =  iterator.next();
                // 获取平台属性值集合对象
                List<BaseAttrValue> attrValueList = baseAttrInfo.getAttrValueList();
                if (skuLsParams.getValueId()!=null && skuLsParams.getValueId().length>0){
                    for (String valueId : skuLsParams.getValueId()) {
                        for (BaseAttrValue baseAttrValue : attrValueList) {
                            // url 上的valueId 与 集合中的baseAttrValue.getId() 相同 则移除集合中的数据
                            if (valueId.equals(baseAttrValue.getId())){
                                iterator.remove();

                                // 在此声明一个平台属性值对象
                                BaseAttrValue baseAttrValueed = new BaseAttrValue();
//                                baseAttrInfo.getAttrName();// 平台属性名称
//                                baseAttrValue.getValueName(); // 平台属性值名称
                                baseAttrValueed.setValueName(baseAttrInfo.getAttrName()+":"+baseAttrValue.getValueName());

                                String newUrlParam = makeUrlParam(skuLsParams, valueId);

                                baseAttrValueed.setUrlParam(newUrlParam);

                                baseAttrValueArrayList.add(baseAttrValueed);
                            }
                        }
                    }
                }
            }
        }


        //  设置分页
        request.setAttribute("totalPages",skuLsResult.getTotalPages());
        request.setAttribute("pageNo",skuLsParams.getPageNo());
        request.setAttribute("urlParam",urlParam);
        // 保存关键字
        request.setAttribute("keyword",skuLsParams.getKeyword());

        // 保存面包屑集合
        request.setAttribute("baseAttrValueArrayList",baseAttrValueArrayList);
        // 保存到作用域
        request.setAttribute("baseAttrInfoList",baseAttrInfoList);

        // sku商品集合
        List<SkuLsInfo> skuLsInfoList = skuLsResult.getSkuLsInfoList();

        // 保存商品列表
        request.setAttribute("skuLsInfoList",skuLsInfoList);

        return "list";
    }
    // 记录用户查询的历史参数

    /**
     *
     * @param skuLsParams url 后面的参数
     * @param excludeValueIds 点击面包屑时要获取的valueId
     * @return
     */
    private String makeUrlParam(SkuLsParams skuLsParams,String ... excludeValueIds) {
        String urlParam = "";
        // 判断skuName 是否为空
        // http://list.gmall.com/list.html?keyword=小米
        if (skuLsParams.getKeyword()!=null){
            urlParam += "keyword="+skuLsParams.getKeyword();
        }
        // 判断三级分类Id
        // http://list.gmall.com/list.html?catalog3Id=61
        if (skuLsParams.getCatalog3Id()!=null){
            urlParam += "catalog3Id="+skuLsParams.getCatalog3Id();
        }
        // 判断平台属性值Id {valueId}
        if (skuLsParams.getValueId()!=null){
            for (String valueId : skuLsParams.getValueId()) {
                // 说明点击时获取到了valueId
                if (excludeValueIds!=null && excludeValueIds.length>0){
                    String excludeValueId = excludeValueIds[0];
                    if (valueId.equals(excludeValueId)){
                        // 不拼接条件
//                        return null;  break;  continue;
                        continue;
                    }
                }

                // http://list.gmall.com/list.html?catalog3Id=61&valueId=83
                if (urlParam.length()>0){
                    urlParam += "&";
                }
                urlParam += "valueId="+valueId;
            }
        }
        return urlParam;
    }
}
