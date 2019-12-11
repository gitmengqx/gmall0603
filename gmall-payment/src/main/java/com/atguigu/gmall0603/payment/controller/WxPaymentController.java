package com.atguigu.gmall0603.payment.controller;

import com.atguigu.gmall0603.service.PaymentService;
import com.atguigu.gmall0603.util.IdWorker;
import com.atguigu.gmall0603.util.StreamUtil;
import com.github.wxpay.sdk.WXPayUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Controller
public class WxPaymentController {

    @Autowired
    private PaymentService paymentService;

    // 密钥
    @Value("${partnerkey}")
    private String partnerkey;

    @RequestMapping("wx/submit")
    @ResponseBody
    public Map createNative(){
        // 用户生产的Id
        IdWorker idWorker = new IdWorker();
        long orderId = idWorker.nextId();
        // 生产微信支付的二维码
        Map map = paymentService.createNative(""+orderId,"1");

        System.out.println(map.get("code_url"));
        return map;

    }

    @RequestMapping("/wx/callback/notify")
    @ResponseBody
    public String wxNotify(HttpServletRequest request, HttpServletResponse response) throws Exception {
        //  1.  以数据流的形式获取到微信异步回调数据
        ServletInputStream inputStream = request.getInputStream();
        //  2.  将数据流对象转化为字符串
        String xmlString  = StreamUtil.inputStream2String(inputStream, "utf-8");
        //  3.  验证签名
        if (WXPayUtil.isSignatureValid(xmlString,partnerkey)){
            // 验签成功！
            Map<String, String> paramMap = WXPayUtil.xmlToMap(xmlString);
            String result_code = paramMap.get("result_code");
            if (result_code!=null && "SUCCESS".equals(result_code)){
                // 4.   更改交易状态，并发送通知给订单模块！

                //  5.  返回状态return_code，return_msg | 微信通讯方式是以 xml 形式
                HashMap<String, String> returnMap = new HashMap<>();
                returnMap.put("return_code","SUCCESS");
                returnMap.put("return_msg","OK");
                //  6.  生成发送的xml
                String returnXml  = WXPayUtil.mapToXml(returnMap);
                response.setContentType("text/xml");
                System.out.println("交易编号："+paramMap.get("out_trade_no")+"支付成功！");
                return returnXml;
            }
        }
        return null;
    }
}
