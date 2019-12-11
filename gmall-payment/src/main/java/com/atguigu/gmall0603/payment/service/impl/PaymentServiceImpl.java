package com.atguigu.gmall0603.payment.service.impl;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSON;
import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;
import com.alipay.api.request.AlipayTradeQueryRequest;
import com.alipay.api.request.AlipayTradeRefundRequest;
import com.alipay.api.response.AlipayTradeQueryResponse;
import com.alipay.api.response.AlipayTradeRefundResponse;
import com.atguigu.gmall0603.bean.OrderInfo;
import com.atguigu.gmall0603.bean.PaymentInfo;
import com.atguigu.gmall0603.bean.enums.OrderStatus;
import com.atguigu.gmall0603.bean.enums.PaymentStatus;
import com.atguigu.gmall0603.config.ActiveMQUtil;
import com.atguigu.gmall0603.payment.mapper.PaymentInfoMapper;
import com.atguigu.gmall0603.service.OrderService;
import com.atguigu.gmall0603.service.PaymentService;
import com.atguigu.gmall0603.util.HttpClient;
import com.github.wxpay.sdk.WXPayUtil;
import org.apache.activemq.ScheduledMessage;
import org.apache.activemq.command.ActiveMQMapMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import tk.mybatis.mapper.entity.Example;

import javax.jms.*;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Service
public class PaymentServiceImpl implements PaymentService {

    // 服务号Id
    @Value("${appid}")
    private String appid;
    // 商户号Id
    @Value("${partner}")
    private String mchId;
    // 密钥
    @Value("${partnerkey}")
    private String partnerkey;


    @Autowired
    private PaymentInfoMapper paymentInfoMapper;

    @Autowired
    private AlipayClient alipayClient;

    @Autowired
    private ActiveMQUtil activeMQUtil;

    @Reference
    private OrderService orderService;

    @Override
    public void savePaymentInfo(PaymentInfo paymentInfo) {
        paymentInfoMapper.insertSelective(paymentInfo);
    }

    @Override
    public PaymentInfo getPaymentInfo(PaymentInfo paymentInfoQuery) {
        // select * from paymentInfo where out_trade_no=?
        return paymentInfoMapper.selectOne(paymentInfoQuery);
    }

    @Override
    public void updatePaymentInfo(String out_trade_no, PaymentInfo paymentInfoUpd) {
        // update paymentInfo set PaymentStatus = PaymentStatus.PAID ,CallbackTime = new Date() where out_trade_no = ?
        Example example = new Example(PaymentInfo.class);
        example.createCriteria().andEqualTo("outTradeNo",out_trade_no);
        paymentInfoMapper.updateByExampleSelective(paymentInfoUpd,example);
    }

    @Override
    public boolean refund(String orderId) {
        // AlipayClient alipayClient = new DefaultAlipayClient("https://openapi.alipay.com/gateway.do","app_id","your private_key","json","GBK","alipay_public_key","RSA2");
        AlipayTradeRefundRequest request = new AlipayTradeRefundRequest();

        OrderInfo orderInfo = orderService.getOrderInfo(orderId);
        HashMap<String, Object> map = new HashMap<>();
        map.put("out_trade_no",orderInfo.getOutTradeNo());
        map.put("refund_amount",orderInfo.getTotalAmount());
        map.put("refund_reason","颜色浅了点");
        // out_request_no

        request.setBizContent(JSON.toJSONString(map));
        AlipayTradeRefundResponse response = null;
        try {
            response = alipayClient.execute(request);
        } catch (AlipayApiException e) {
            e.printStackTrace();
        }
        if(response.isSuccess()){
            System.out.println("调用成功");
            // 更新交易记录 ： 关闭
            PaymentInfo paymentInfo = new PaymentInfo();
            paymentInfo.setPaymentStatus(PaymentStatus.ClOSED);
            updatePaymentInfo(orderInfo.getOutTradeNo(), paymentInfo);
            return true;
        } else {
            System.out.println("调用失败");
            return false;
        }

    }

    @Override
    public Map createNative(String orderId, String totalAmount) {
        /*
            1.  封装参数 微信需要发送的是xml 格式 map --> xml
            2.  将xml 发送给微信支付的url：https://api.mch.weixin.qq.com/pay/unifiedorder
            3.  获取支付的结果 xml -- >map
         */
        HashMap<String, String> map = new HashMap<>();
        map.put("appid",appid);
        map.put("mch_id",mchId);
        map.put("nonce_str", WXPayUtil.generateNonceStr());
        map.put("body","冬天冷啊！买个大衣取取暖！");
        map.put("out_trade_no",orderId);
        map.put("total_fee",totalAmount);
        map.put("spbill_create_ip","127.0.0.1");
        map.put("notify_url","http://guli.free.idcfengye.com/wx/callback/notify");
        map.put("trade_type","NATIVE");

        // sign 当做key 传入进去 map --- >
        try {
            String xmlParam = WXPayUtil.generateSignedXml(map, partnerkey);

            // 发送请求
            HttpClient httpClient = new HttpClient("https://api.mch.weixin.qq.com/pay/unifiedorder");
            httpClient.setHttps(true);
            httpClient.setXmlParam(xmlParam);
            // 发送post 请求
            httpClient.post();
            // 获取结果
            String result  = httpClient.getContent();
            Map<String, String> resultMap  = WXPayUtil.xmlToMap(result);
            
            // 声明一个map 集合来保存支付结果得到的数据
            HashMap<Object, Object> returnMap = new HashMap<>();
            returnMap.put("code_url",resultMap.get("code_url"));
            returnMap.put("total_fee",totalAmount);
            returnMap.put("out_trade_no",orderId);

            return returnMap;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public void sendPaymentResult(PaymentInfo paymentInfo, String result) {
        // 获取连接
        Connection connection = activeMQUtil.getConnection();
        // 打开连接
        try {
            connection.start();
            // 创建session
            Session session = connection.createSession(true, Session.AUTO_ACKNOWLEDGE);
            // 创建队列
            Queue payment_result_queue = session.createQueue("PAYMENT_RESULT_QUEUE");
            // 创建消息提供者
            MessageProducer producer = session.createProducer(payment_result_queue);
            // orderId = ?  ,result = ?
            ActiveMQMapMessage activeMQMapMessage = new ActiveMQMapMessage();
            activeMQMapMessage.setString("orderId",paymentInfo.getOrderId());
            activeMQMapMessage.setString("result",result);
            producer.send(activeMQMapMessage);

            // 提交数据
            session.commit();

            producer.close();
            session.close();
            connection.close();

        } catch (JMSException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void sendPaymentResult(String orderId, String result) {

    }

    // 只做单独查询用户是否支付成功？
    @Override
    public boolean checkPayment(OrderInfo orderInfo) {
        //AlipayClient alipayClient = new DefaultAlipayClient("https://openapi.alipay.com/gateway.do","app_id","your private_key","json","GBK","alipay_public_key","RSA2");
        AlipayTradeQueryRequest request = new AlipayTradeQueryRequest();
        // 判断当前对象不能为null！
        if (orderInfo==null){
            return false;
        }

        // 设置查询的参数
        HashMap<String, String> map = new HashMap<>();
        map.put("out_trade_no",orderInfo.getOutTradeNo());
        request.setBizContent(JSON.toJSONString(map));

        AlipayTradeQueryResponse response = null;
        try {
            response = alipayClient.execute(request);
        } catch (AlipayApiException e) {
            e.printStackTrace();
        }
        if(response.isSuccess()){
            System.out.println("调用成功");
            // 得到交易状态！
            if ("TRADE_SUCCESS".equals(response.getTradeStatus())|| "TRADE_FINISHED".equals(response.getTradeStatus())){
                // 更新交易记录的状态！
                // 修改交易记录的状态！
                PaymentInfo paymentInfo = new PaymentInfo();
                paymentInfo.setPaymentStatus(PaymentStatus.PAID);
                paymentInfo.setCallbackTime(new Date());
                paymentInfo.setCallbackContent("延迟队列验签成功！");
                updatePaymentInfo(orderInfo.getOutTradeNo(),paymentInfo);

                // 发送消息给订单！
                System.out.println("支付成功！");
                return true;
            }
        } else {
            System.out.println("调用失败");
            return false;
        }
        return false;
    }

    @Override
    public void sendDelayPaymentResult(String outTradeNo, int delaySec, int checkCount) {
        // 创建工厂
        Connection connection = activeMQUtil.getConnection();
        try {
            connection.start();
            // 创建session
            Session session = connection.createSession(true, Session.SESSION_TRANSACTED);
            // 创建对象
            Queue payment_result_check_queue = session.createQueue("PAYMENT_RESULT_CHECK_QUEUE");
            // 创建消息提供者
            MessageProducer producer = session.createProducer(payment_result_check_queue);
            // 创建消息对象
            ActiveMQMapMessage activeMQMapMessage = new ActiveMQMapMessage();
            activeMQMapMessage.setString("outTradeNo",outTradeNo);
            activeMQMapMessage.setInt("delaySec",delaySec); // 每隔多久发送一次消息！
            activeMQMapMessage.setInt("checkCount",checkCount);
            // 开启延迟队列的参数设置
            activeMQMapMessage.setLongProperty(ScheduledMessage.AMQ_SCHEDULED_DELAY,delaySec*1000);
            // 发送消息
            producer.send(activeMQMapMessage);

            // 提交
            session.commit();
            // 关闭
            producer.close();
            session.close();
            connection.close();

        } catch (JMSException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void closePayment(String orderId) {
        Example example = new Example(PaymentInfo.class);
        example.createCriteria().andEqualTo("orderId",orderId);
        PaymentInfo paymentInfo = new PaymentInfo();
        paymentInfo.setPaymentStatus(PaymentStatus.ClOSED);
        paymentInfoMapper.updateByExampleSelective(paymentInfo,example);
    }
}
