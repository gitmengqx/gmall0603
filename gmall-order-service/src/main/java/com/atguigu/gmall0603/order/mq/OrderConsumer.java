package com.atguigu.gmall0603.order.mq;

import com.alibaba.dubbo.config.annotation.Reference;
import com.atguigu.gmall0603.bean.OrderInfo;
import com.atguigu.gmall0603.bean.PaymentInfo;
import com.atguigu.gmall0603.bean.enums.ProcessStatus;
import com.atguigu.gmall0603.service.OrderService;
import com.atguigu.gmall0603.service.PaymentService;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

import javax.jms.JMSException;
import javax.jms.MapMessage;

@Component
public class OrderConsumer {

    @Reference
    private OrderService orderService;

    @Reference
    private PaymentService paymentService;

    // 获取消息监听器工厂
    @JmsListener(destination = "PAYMENT_RESULT_QUEUE",containerFactory = "jmsQueueListener")
    public void getPaymentResultQueue(MapMessage mapMessage) throws JMSException {
        String orderId = mapMessage.getString("orderId");
        String result = mapMessage.getString("result");

        if ("success".equals(result)){
            // 支付成功！ 修改订单状态为已支付
            orderService.updateOrderStatus(orderId, ProcessStatus.PAID);
            // 发送消息：
            orderService.sendOrderStatus(orderId);
            orderService.updateOrderStatus(orderId, ProcessStatus.NOTIFIED_WARE);
        }
    }
    // 获取消息监听器工厂
    @JmsListener(destination = "SKU_DEDUCT_QUEUE",containerFactory = "jmsQueueListener")
    public void getSkuDeductQueue(MapMessage mapMessage) throws JMSException {
        String orderId = mapMessage.getString("orderId");
        String status = mapMessage.getString("status");

        if ("DEDUCTED".equals(status)){
            // 减库存成功！ 修改订单状态为已支付
            orderService.updateOrderStatus(orderId, ProcessStatus.WAITING_DELEVER);
        }else {
            /*
                减库存失败！远程调用其他仓库查看是否有库存！
                true:   orderService.sendOrderStatus(orderId); orderService.updateOrderStatus(orderId, ProcessStatus.NOTIFIED_WARE);
                false:  1.  补货  | 2.   人工客服。
             */
        }
    }

    // 监听消息队列
    @JmsListener(destination = "PAYMENT_RESULT_CHECK_QUEUE",containerFactory = "jmsQueueListener")
    public void consumeSkuDeduct(MapMessage mapMessage) throws JMSException {
        String outTradeNo = mapMessage.getString("outTradeNo");

        OrderInfo orderInfo = new OrderInfo();
        orderInfo.setOutTradeNo(outTradeNo);
        // PaymentInfo paymentInfoQuery = paymentService.getPaymentInfo(paymentInfo);
        // 检查该用户是否支付成功？
        boolean result = paymentService.checkPayment(orderInfo);
        System.out.println("支付结果："+result);
        if (!result){
            //  再次检查是否支付成功！
            // paymentService.sendDelayPaymentResult(outTradeNo,delaySec,checkCount-1);
            // 关闭过期订单
            OrderInfo orderInfoQuery = orderService.getOrderInfo(orderInfo);
            orderService.execExpiredOrder(orderInfoQuery);
        }
    }
}
