package com.atguigu.gmall0603.item.contorller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.fastjson.JSON;
import com.atguigu.gmall0603.bean.SkuInfo;
import com.atguigu.gmall0603.bean.SkuSaleAttrValue;
import com.atguigu.gmall0603.bean.SpuSaleAttr;
import com.atguigu.gmall0603.config.LoginRequire;
import com.atguigu.gmall0603.service.ListService;
import com.atguigu.gmall0603.service.ManageService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
public class ItemController {

    @Reference
    private ManageService manageService;

    @Reference
    private ListService listService;

    @RequestMapping("{skuId}.html")
//    @LoginRequire
    public String getItem(@PathVariable String skuId, HttpServletRequest request){
        // 通过skuId 查询skuInfo
        SkuInfo skuInfo = manageService.getSkuInfo(skuId);
        // 销售属性-销售属性值回显并锁定
        List<SpuSaleAttr> spuSaleAttrList =  manageService.getSpuSaleAttrListCheckBySku(skuInfo);

        // 通过sql 语句查询数据 由查询出来的数据组成{"123|125":"37","123|126":"38"...} Json 字符串
        List<SkuSaleAttrValue> skuSaleAttrValueListBySpu = manageService.getSkuSaleAttrValueListBySpu(skuInfo.getSpuId());
        // 数据拼接生成json | map.put("123|125","37")

        Map skuValueIdsMap =  manageService.getSkuValueIdsMap(skuInfo.getSpuId());

//        String key = "";
//        HashMap<String, Object> map = new HashMap<>();
//        if (skuSaleAttrValueListBySpu!=null && skuSaleAttrValueListBySpu.size()>0){
//            // 拼接规则：第一个：skuId 相同则拼接，不相同则停止拼接，并放入map集合中！第二个：循环到集合最后则停止拼接，放入map集合中
//            // itar ,itco, iter
//            for (int i = 0; i < skuSaleAttrValueListBySpu.size(); i++) {
//                // 一个对象
//                SkuSaleAttrValue skuSaleAttrValue = skuSaleAttrValueListBySpu.get(i);
//                /*
//                拼接：第一回 key = 123
//                    ：第二回 key = 123|
//                    ：第三回 key = 123|125
//                 */
//                if (key.length()>0){
//                    key+="|";
//                }
//                key+=skuSaleAttrValue.getSaleAttrValueId(); // key = key + skuSaleAttrValue.getSaleAttrValueId();
//                // !skuSaleAttrValue.getSkuId().equals(skuSaleAttrValueListBySpu.get(i+1).getSkuId()) 第九次的时候 get(10) 数组下标越界
//                if ((i+1)==skuSaleAttrValueListBySpu.size() || !skuSaleAttrValue.getSkuId().equals(skuSaleAttrValueListBySpu.get(i+1).getSkuId())){
//                    // 放入map
//                    map.put(key,skuSaleAttrValue.getSkuId());
//                    // 并将key 清空！
//                    key="";
//                }
//            }
//        }

//        String valuesSkuJson  = JSON.toJSONString(map);

        SkuInfo skuInfoDB = manageService.getSkuInfoDB(skuId);
        String valuesSkuJson = JSON.toJSONString(skuValueIdsMap);

        // 获取价格
        request.setAttribute("price",skuInfoDB.getPrice());
        System.out.println("valuesSkuJson: "+valuesSkuJson );
        // 保存valuesSkuJson
        request.setAttribute("valuesSkuJson",valuesSkuJson);
        // 保存数据
        request.setAttribute("spuSaleAttrList",spuSaleAttrList);
        // 保存skuInfo
        request.setAttribute("skuInfo",skuInfo);

        listService.incrHotScore(skuId);
        return "item";
    }

}
