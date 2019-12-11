package com.atguigu.gmall0603.service;

import com.atguigu.gmall0603.bean.BaseAttrInfo;
import com.atguigu.gmall0603.bean.SkuLsInfo;
import com.atguigu.gmall0603.bean.SkuLsParams;
import com.atguigu.gmall0603.bean.SkuLsResult;

import java.util.List;

public interface ListService {
    // 保存商品
    void saveSkuLsInfo(SkuLsInfo skuLsInfo);

    /**
     * 根据用户输入的参数查询数据
     * @param skuLsParams
     * @return
     */
    SkuLsResult search(SkuLsParams skuLsParams);

    /**
     * 更新商品
     * @param skuId
     */
    void incrHotScore(String skuId);

}
