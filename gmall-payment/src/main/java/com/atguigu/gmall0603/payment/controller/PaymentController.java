package com.atguigu.gmall0603.payment.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.fastjson.JSON;
import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;
import com.alipay.api.internal.util.AlipaySignature;
import com.alipay.api.request.AlipayTradePagePayRequest;
import com.atguigu.gmall0603.bean.OrderInfo;
import com.atguigu.gmall0603.bean.PaymentInfo;
import com.atguigu.gmall0603.bean.enums.PaymentStatus;
import com.atguigu.gmall0603.config.LoginRequire;
import com.atguigu.gmall0603.payment.config.AlipayConfig;
import com.atguigu.gmall0603.service.OrderService;
import com.atguigu.gmall0603.service.PaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static redis.clients.jedis.Protocol.CHARSET;

@Controller
public class PaymentController {

    @Reference
    OrderService orderService;

    @Autowired
    private AlipayClient alipayClient;

    @Autowired
    private PaymentService paymentService;
    // http://guli.free.idcfengye.com/index?orderId=120
    // http://payment.gmall.com/index?orderId=120
    @RequestMapping("index")
    @LoginRequire
    public String index(HttpServletRequest request){

        String orderId = request.getParameter("orderId");
        // 根据orderId 查询总金额
        OrderInfo orderInfo = orderService.getOrderInfo(orderId);
        // orderInfo.sumTotalAmount();
        // 保存数据
        request.setAttribute("orderId",orderId);
        request.setAttribute("totalAmount",orderInfo.getTotalAmount());
        return "index";
    }
    // http://payment.gmall.com/alipay/submit
    @RequestMapping("alipay/submit")
    @ResponseBody // 第一个作用 返回json 字符串！第二个作用将返回值直接输入到页面！
    public String aliPay(HttpServletRequest request, HttpServletResponse response){
        String orderId = request.getParameter("orderId");
        OrderInfo orderInfo = orderService.getOrderInfo(orderId);
        // 保存交易记录

        PaymentInfo paymentInfo = new PaymentInfo();
        paymentInfo.setCreateTime(new Date());
        paymentInfo.setOrderId(orderId);
        paymentInfo.setOutTradeNo(orderInfo.getOutTradeNo());
        paymentInfo.setPaymentStatus(PaymentStatus.UNPAID);
        paymentInfo.setSubject("冬天买大衣");
        paymentInfo.setTotalAmount(orderInfo.getTotalAmount());

        paymentService.savePaymentInfo(paymentInfo);
        // 生产二维码
        // @Bean
        // AlipayClient alipayClient = new DefaultAlipayClient("https://openapi.alipay.com/gateway.do", APP_ID, APP_PRIVATE_KEY, FORMAT, CHARSET, ALIPAY_PUBLIC_KEY, SIGN_TYPE); //获得初始化的AlipayClient
        AlipayTradePagePayRequest alipayRequest = new AlipayTradePagePayRequest();//创建API对应的request
        // 同步回调
        // https://www.domain.com/CallBack/return_url?out_trade_no=ATGUIGU157499259135929&version=1.0&app_id=2018020102122556&charset=utf-8&sign_type=RSA2&trade_no=2019112922001465910548235454&auth_app_id=2018020102122556&timestamp=2019-11-29%2011:31:51&seller_id=2088921750292524&method=alipay.trade.page.pay.return&total_amount=0.01&sign=e9iElZyViBkF2o9RlgWq2VjnST8khZ8mXaXhispkQDws9AytNRFGoTMM/GxGWYkA4XGyZfSRm+p2aZVHD3EKpyaOeaLPiBOgdtwAHTImkWCWVsCQpZOuKf296YMWOmDjEw744YvLHOx4j5nu84UWWaWZa7Xv1aknKZK238BddjPqrLs+KULhrJKVgwdkKsGnIQDt4Yc9/ne5WUV15jfb8i2G3Q2eM/bcLJh/TLv7Uu1woKTz5soENM8rqr8vkZpiVHOJg9xoZEpTCEjiEoJHGN49LNdyFqYAW0ISwUrOCGnnEW4iLAB6rUlYc7VFqCKE8qY0Xgx5g6z9Dbb/69NYHw==
        // return_payment_url=http://payment.gmall.com/alipay/callback/return
        alipayRequest.setReturnUrl(AlipayConfig.return_payment_url);
        // 异步回调
        alipayRequest.setNotifyUrl(AlipayConfig.notify_payment_url);//在公共参数中设置回跳和通知地址
        // 参数

        // 声明一个map 集合
        HashMap<String, Object> map = new HashMap<>();
        map.put("out_trade_no",orderInfo.getOutTradeNo());
        map.put("product_code","FAST_INSTANT_TRADE_PAY");
        map.put("total_amount",orderInfo.getTotalAmount());
        map.put("subject","冬天买大衣");

        alipayRequest.setBizContent(JSON.toJSONString(map));

        String form="";
        try {
            form = alipayClient.pageExecute(alipayRequest).getBody(); //调用SDK生成表单
        } catch (AlipayApiException e) {
            e.printStackTrace();
        }
        response.setContentType("text/html;charset=" + CHARSET);

        // 发送延迟队列
        paymentService.sendDelayPaymentResult(paymentInfo.getOutTradeNo(),15,3);
        return form;
    }

    // 异步回调：alipay/callback/return
    @RequestMapping("alipay/callback/return")
    public String callBack(){
        // 同步回调给用户展示信息
        return "redirect:"+AlipayConfig.return_order_url;
    }

    // 异步回调：http://60.205.215.91/alipay/callback/notify 必须使用内网穿透
    // http://guli.free.idcfengye.com/alipay/callback/notify
    @RequestMapping("alipay/callback/notify")
    @ResponseBody
    public String alipayNotify(@RequestParam Map<String,String> paramMap, HttpServletRequest request){
        System.out.println("回来了！");
//        Map<String, String> paramsMap = ... //将异步通知中收到的所有参数都存放到map中
        boolean signVerified = false; //调用SDK验证签名
        try {
            signVerified = AlipaySignature.rsaCheckV1(paramMap, AlipayConfig.alipay_public_key, CHARSET, AlipayConfig.sign_type);
        } catch (AlipayApiException e) {
            e.printStackTrace();
        }

        // 交易状态
        String trade_status = paramMap.get("trade_status");
        String out_trade_no = paramMap.get("out_trade_no");
        // true
        if(signVerified){
            // TODO 验签成功后，按照支付结果异步通知中的描述，对支付结果中的业务内容进行二次校验，校验成功后在response中返回success并继续商户自身业务处理，校验失败返回failure
            if ("TRADE_SUCCESS".equals(trade_status) || "TRADE_FINISHED".equals(trade_status)){
                // 但是，如果交易记录表中 PAID 或者 CLOSE  获取交易记录中的支付状态 通过outTradeNo来查询数据
                PaymentInfo paymentInfoQuery = new PaymentInfo();
                paymentInfoQuery.setOutTradeNo(out_trade_no);
                // select * from paymentInfo where out_trade_no=?
                PaymentInfo paymentInfo = paymentService.getPaymentInfo(paymentInfoQuery);
                if (paymentInfo.getPaymentStatus()==PaymentStatus.PAID || paymentInfo.getPaymentStatus()==PaymentStatus.ClOSED){
                    return "failure";
                }
                // 正常的支付成功，我们应该更新交易记录状态
                PaymentInfo paymentInfoUpd = new PaymentInfo();
                // update paymentInfo set PaymentStatus = PaymentStatus.PAID ,CallbackTime = new Date() where out_trade_no = ?
                paymentInfoUpd.setPaymentStatus(PaymentStatus.PAID);
                paymentInfoUpd.setCallbackTime(new Date());
                paymentInfoUpd.setCallbackContent(paramMap.toString());
                paymentService.updatePaymentInfo(out_trade_no,paymentInfoUpd);
                // 表示交易成功！

                // 后续更新订单状态！ 使用消息队列！
                paymentService.sendPaymentResult(paymentInfo,"success");
                return "success";
            }

        }else{
            // TODO 验签失败则记录异常日志，并在response中返回failure.
            return "failure";
        }
        return "failure";
    }

    // 发起退款！订单编号 http://payment.gmall.com/refund?orderId=118
    @RequestMapping("refund")
    @ResponseBody
    public String refund(String orderId,HttpServletRequest request){
        // 调用退款接口
        boolean flag = paymentService.refund(orderId);

        return ""+flag;
    }
    // http://payment.gmall.com/sendPaymentResult?orderId=115&result=success
    @RequestMapping("sendPaymentResult")
    @ResponseBody
    public String sendPaymentResult(PaymentInfo paymentInfo,String result){
        paymentService.sendPaymentResult(paymentInfo,result);
        return "OK";
    }

    // http://payment.gmall.com/queryPaymentResult?orderId=xxx?
    @RequestMapping("queryPaymentResult")
    @ResponseBody
    public String queryPaymentResult(OrderInfo orderInfo){
        // 根据orderId 查询paymentInfo 对象
        OrderInfo orderInfoQuery = orderService.getOrderInfo(orderInfo);
        boolean res = paymentService.checkPayment(orderInfoQuery);
        return ""+res;
    }
}
