package com.atguigu.gmall0603.service;

import com.atguigu.gmall0603.bean.OrderInfo;
import com.atguigu.gmall0603.bean.PaymentInfo;

import java.util.Map;

public interface PaymentService {

    /**
     * 保存交易记录
     * @param paymentInfo
     */
    void savePaymentInfo(PaymentInfo paymentInfo);

    // 根据第三方交易记录查询paymentInfo
    PaymentInfo getPaymentInfo(PaymentInfo paymentInfoQuery);
    // 根据第三方交易编号，修改支付交易记录
    void updatePaymentInfo(String out_trade_no, PaymentInfo paymentInfoUpd);

    // 退款接口
    boolean refund(String orderId);

    // 支付
    Map createNative(String orderId, String totalAmount);

    // 发送消息
    void sendPaymentResult(PaymentInfo paymentInfo,String result);

    // 发送消息
    void sendPaymentResult(String orderId ,String result);

    // 根据outTradeNo 查询是否交易成功！
    boolean checkPayment(OrderInfo orderInfo);

    /**
     *  在生成二维码的时候：每隔15秒主动向支付宝询问三次该用户是否支付成功！
     * @param outTradeNo 表示根据第三方交易编号查询是否支付成功的字段
     * @param delaySec 延迟的时间 15
     * @param checkCount 查询的次数 3
     */
    void sendDelayPaymentResult(String outTradeNo,int delaySec ,int checkCount);

    /**
     * 关闭过期交易记录
     * @param orderId
     */
    void closePayment(String orderId);
}
