package com.atguigu.gmall0603.config;

import com.alibaba.fastjson.JSON;
import com.atguigu.gmall0603.util.HttpClientUtil;
import io.jsonwebtoken.impl.Base64Codec;
import io.jsonwebtoken.impl.Base64UrlCodec;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Map;

@Component
public class AuthInterceptor extends HandlerInterceptorAdapter {
    // passport.atguigu.com/index?url=xxx
    // 进入控制器之前，执行
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        System.out.println("进入拦截器！");
        // 将用户登录时，返回的token 放入cookie 中！
        // https://www.jd.com/?newToken=eyJhbGciOiJIUzI1NiJ9.eyJuaWNrTmFtZSI6IkF0Z3VpZ3UiLCJ1c2VySWQiOiIxIn0.XzRrXwDhYywUAFn-ICLJ9t3Xwz7RHo1VVwZZGNdKaaQ
        String token = request.getParameter("newToken");
        // 获取到token 时，将token 放入cookie 中！
        if (token!=null){
            // 只有在登录成功之后，有?newToken=xxx 的时候，才会将token 放入cookie！
            CookieUtil.setCookie(request,response,"token",token,WebConst.COOKIE_MAXAGE,false);
        }
        // 用户在登录完成之后，访问其他业务模块时，item.gmall.com/36.html, list.gmall.com 是否还有token？
        // 有：将token 存放到cookie 中了！
        // 无：访问的url 控制器后面不会带有newToken=xxx 参数！
        if (token==null){
            token =  CookieUtil.getCookieValue(request,"token",false);
        }
        // 从token 中获取用户的昵称！
        if (token!=null){
            Map map = getUserInfoMap(token);
            // String userId = (String) map.get("userId");
            String nickName = (String) map.get("nickName");
            // 将用户昵称保存到作用域
            request.setAttribute("nickName",nickName);
        }

        // 获取方法上是否有该注解
        HandlerMethod handlerMethod = (HandlerMethod) handler;
        LoginRequire methodAnnotation = handlerMethod.getMethodAnnotation(LoginRequire.class);
        // 方法上有注解
        if (methodAnnotation!=null){
            // 做一个认证：
            // http://passport.atguigu.com/verify?token=eyJhbGciOiJIUzI1NiJ9.eyJuaWNrTmFtZSI6IkF0Z3VpZ3UiLCJ1c2VySWQiOiIxIn0.XzRrXwDhYywUAFn-ICLJ9t3Xwz7RHo1VVwZZGNdKaaQ&salt=192.168.67.1
            // 获取盐salt
            String salt = request.getHeader("X-forwarded-for");
            // 调用PassportController 中verify 方法，远程调用！
            String result = HttpClientUtil.doGet(WebConst.VERIFY_ADDRESS + "?token=" + token + "&salt=" + salt);
            if ("success".equals(result)){
                // 表示已经登录！
                Map userInfoMap = getUserInfoMap(token);
                String userId = (String) userInfoMap.get("userId");
                // 保存好用户Id
                request.setAttribute("userId",userId);
                return true;
            }else {
                // 没有登录，有一种情况，但是 autoRedirect() default true 则必须登录！跳转到登录页面！
                if (methodAnnotation.autoRedirect()){
                    // 先获取到访问的Url 是谁？
                    // 我们要访问 商品详情则必须登录 http://item.gmall.com/38.html
                    String requestURL = request.getRequestURL().toString();
                    System.out.println("requestURL:"+requestURL); // http://item.gmall.com/38.html ----> http%3A%2F%2Fitem.gmall.com%2F38.html
                    String encodeURL  = URLEncoder.encode(requestURL, "UTF-8"); // http%3A%2F%2Fitem.gmall.com%2F38.html
                    System.out.println("encodeURL:"+encodeURL); // http://item.gmall.com/38.html ----> http%3A%2F%2Fitem.gmall.com%2F38.html

                    // 最终重定向给浏览器 http://passport.atguigu.com/index?originUrl=http%3A%2F%2Fitem.gmall.com%2F38.html
                    response.sendRedirect(WebConst.LOGIN_ADDRESS+"?originUrl="+encodeURL);

                    return false;

                }
            }
        }
        return true;
    }

    private Map getUserInfoMap(String token) {
        // 获取用户昵称
        // token = eyJhbGciOiJIUzI1NiJ9.eyJuaWNrTmFtZSI6IkF0Z3VpZ3UiLCJ1c2VySWQiOiIxIn0.XzRrXwDhYywUAFn-ICLJ9t3Xwz7RHo1VVwZZGNdKaaQ
        // 从中间部分私有部分得到用户信息{userId,nickName}
        // 解密token 可以使用工具类，还可以使用base64编码
        // 获取token 中间部分，私有部分
        String tokenUserInfo  = StringUtils.substringBetween(token, ".");
        // tokenUserInfo=eyJuaWNrTmFtZSI6IkF0Z3VpZ3UiLCJ1c2VySWQiOiIxIn0
        // Base64Codec base64Codec = new Base64Codec();
        Base64UrlCodec base64UrlCodec = new Base64UrlCodec();
        byte[] bytes = base64UrlCodec.decode(tokenUserInfo);
        // bytes 数据格式，转换的时候，形式不一样！
        // 将bytes 变成字符串！{"nickName":"Atguigu","userId":"1"}
        String tokenJson = null;
        try {
            tokenJson = new String(bytes, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        // 转换为map
        Map map = JSON.parseObject(tokenJson, Map.class);

        return map;
    }

    // 进去控制器之后，返回视图之前
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception {

    }
    // 视图渲染完成之后！
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {

    }
}
