package com.atguigu.gmall0603.passport.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.atguigu.gmall0603.bean.UserInfo;
import com.atguigu.gmall0603.passport.config.JwtUtil;
import com.atguigu.gmall0603.service.UserService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

@Controller
public class PassportController {

    @Value("${token.key}")
    private String key;

    @Reference
    private UserService userService;
    // http://localhost:8087/index?originUrl=https%3A%2F%2Fwww.jd.com%2F
    @RequestMapping("index")
    public String index(HttpServletRequest request){
        String originUrl = request.getParameter("originUrl");
        request.setAttribute("originUrl",originUrl);
        return "index";
    }

    // springMVC 对象传值！
    @RequestMapping("login")
    @ResponseBody
    public String login(HttpServletRequest request, UserInfo userInfo){
//        String loginName = request.getParameter("loginName");
        System.out.println("进入控制器！");
       UserInfo info = userService.login(userInfo);

       if (info!=null){
           // 获取服务器的IP地址
           String salt = request.getHeader("X-forwarded-for");
           // 返回token
           // String key = "atguigu";
           HashMap<String, Object> map = new HashMap<>();
           map.put("userId",info.getId());
           map.put("nickName",info.getNickName());
           // String salt = "192.168.67.224"; 获取服务器的IP地址。
           String token = JwtUtil.encode(key, map, salt);
           System.out.println("token:"+token);
           return token;
       }else {
           return "fail";
       }
    }

    // http://passport.atguigu.com/verify?token=xxx&salt=xxx
    @RequestMapping("verify")
    @ResponseBody
    public String verify(HttpServletRequest request){

        String token = request.getParameter("token");
        String salt = request.getParameter("salt");
        // 解密
        Map<String, Object> map = JwtUtil.decode(token, key, salt);
        if (map!=null && map.size()>0){
            // 通过解密能够得到用户信息
            String userId = (String) map.get("userId");

            // 通过userId 获取缓存中userInfo的数据
            UserInfo info = userService.verify(userId);
            if (info!=null){
                // 缓存中有数据，则表示认证成功！
                return "success";
            }
        }
        return "fail";
    }
}
