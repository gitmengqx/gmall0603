package com.atguigu.gmall0603.service;

import com.atguigu.gmall0603.bean.OrderInfo;
import com.atguigu.gmall0603.bean.enums.ProcessStatus;

import java.util.List;
import java.util.Map;

public interface OrderService {

    /**
     * 保存订单
     * @param orderInfo
     * @return
     */
    String saveOrderInfo(OrderInfo orderInfo);

    /**
     * 生产流水号
     * @param userId
     * @return
     */
    String getTradeNo(String userId);

    /**
     * 比较流水号
     * @param userId 获取缓存中的流水号
     * @param tradeCodeNo   页面传递过来的流水号
     * @return
     */
    boolean checkTradeCode(String userId,String tradeCodeNo);


    /**
     * 删除流水号
     * @param userId
     */
    void deleteTradeNo(String userId);

    /**
     * 验证库存
     * @param skuId
     * @param skuNum
     * @return
     */
    boolean checkStock(String skuId, Integer skuNum);

    /**
     * 根据订单Id 查询订单信息
     * @param orderId
     * @return
     */
    OrderInfo getOrderInfo(String orderId);

    /**
     * 根据订单Id 修改订单的状态
     * @param orderId
     * @param processStatus
     */
    void updateOrderStatus(String orderId, ProcessStatus processStatus);

    /**
     * 发送消息给库存！
     * @param orderId
     */
    void sendOrderStatus(String orderId);

    /**
     * 查询过期订单
     * @return
     */
    List<OrderInfo> getExpiredOrderList();

    /**
     * 处理过期订单
     * @param orderInfo
     */
    void execExpiredOrder(OrderInfo orderInfo);

    /**
     * 将OrderInfo 对象中的某些字段转换为Map
     * @param orderInfo
     * @return
     */
    Map initWareOrder(OrderInfo orderInfo);

    /**
     *
     * @param orderId
     * @param wareSkuMap
     * @return
     */
    List<OrderInfo> orderSplit(String orderId, String wareSkuMap);

    /**
     * 关闭过期订单
     * @param outTradeNo
     * @param delaySec
     */
    void closeOrderInfo(String outTradeNo,int delaySec);


    /**
     * 关闭过期订单
     * @param orderInfo
     */
    OrderInfo getOrderInfo(OrderInfo orderInfo);

    /**
     * 检查是否支付成功！
     * @param orderInfo
     * @return
     */
    boolean checkPayment(OrderInfo orderInfo);
}
