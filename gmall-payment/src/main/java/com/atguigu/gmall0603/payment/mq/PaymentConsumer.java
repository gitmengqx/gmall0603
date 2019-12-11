package com.atguigu.gmall0603.payment.mq;

import com.alibaba.dubbo.config.annotation.Reference;
import com.atguigu.gmall0603.bean.OrderInfo;
import com.atguigu.gmall0603.bean.PaymentInfo;
import com.atguigu.gmall0603.service.PaymentService;
import org.springframework.expression.spel.ast.OpAnd;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

import javax.jms.JMSException;
import javax.jms.MapMessage;

@Component
public class PaymentConsumer {

    @Reference
    private PaymentService paymentService;

    // 监听消息队列
    @JmsListener(destination = "PAYMENT_RESULT_CHECK_QUEUE",containerFactory = "jmsQueueListener")
    public void consumeSkuDeduct(MapMessage mapMessage) throws JMSException {
        String outTradeNo = mapMessage.getString("outTradeNo");
        int delaySec = mapMessage.getInt("delaySec");
        int checkCount = mapMessage.getInt("checkCount");

        PaymentInfo paymentInfo = new PaymentInfo();
        paymentInfo.setOutTradeNo(outTradeNo);
        // paymentInfoQuery 有 outTradeNo , PaymentStatus
        PaymentInfo paymentInfoQuery = paymentService.getPaymentInfo(paymentInfo);
        // 检查该用户是否支付成功？
        OrderInfo orderInfo = new OrderInfo();
        orderInfo.setOutTradeNo(outTradeNo);
        boolean result = paymentService.checkPayment(orderInfo);
        System.out.println("支付结果："+result);
        if (!result && checkCount>0){
            System.out.println("检查次数："+checkCount);
            // 再次检查是否支付成功！
            paymentService.sendDelayPaymentResult(outTradeNo,delaySec,checkCount-1);
        }


    }
}
