package com.atguigu.gmall0603.cart.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.atguigu.gmall0603.bean.CartInfo;
import com.atguigu.gmall0603.bean.SkuInfo;
import com.atguigu.gmall0603.config.CookieUtil;
import com.atguigu.gmall0603.config.LoginRequire;
import com.atguigu.gmall0603.service.CartService;
import com.atguigu.gmall0603.service.ManageService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.awt.image.RescaleOp;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Controller
public class CartController {

    @Reference
    private CartService cartService;

    @Reference
    private ManageService manageService;

    @RequestMapping("addToCart")
    @LoginRequire(autoRedirect = false)
    public String addToCart(HttpServletRequest request, HttpServletResponse response){
        // 如何获取userId
        String userId = (String) request.getAttribute("userId");
        String skuId = request.getParameter("skuId");
        String skuNum = request.getParameter("skuNum");

        if (userId==null){
            // 根本没有登录过！
            // 起一个临时的用户Id，并将临时的userId 保存到cookie中！
            // 表示未登录时，已经添加过一次购物车！
            userId = CookieUtil.getCookieValue(request,"my-userId",false);
            // 表示未登录时，从未添加过购物车！
            if (userId==null){
                userId = UUID.randomUUID().toString().replace("-","");
                // 保存到cookie
                CookieUtil.setCookie(request, response,"my-userId",userId,60*60*24*7,false);
            }
        }
        cartService.addToCart(skuId,userId,Integer.parseInt(skuNum));

        // 保存商品的数量
        request.setAttribute("skuNum",skuNum);
        SkuInfo skuInfo = manageService.getSkuInfo(skuId);

        request.setAttribute("skuInfo",skuInfo);
        return "success";
    }
    // http://cart.gmall.com/cartList
    @RequestMapping("cartList")
    @LoginRequire(autoRedirect = false)
    public String cartList(HttpServletRequest request){
        List<CartInfo> cartInfoList = null;
        // 获取用户Id
        String userId = (String) request.getAttribute("userId");
        // 已经登录
        if (userId!=null){

            /*
             1. 准备合并购物车
             2. 获取未登录的购物车数据
             3. 如果未登录购物车中有数据，则进行合并 合并的条件：skuId 相同 则数量相加，合并完成之后，删除未登录的数据！
             4. 如果未登录购物车没有数据，则直接显示已登录的数据
              */
            // 未登录：临时用户Id 存储在cookie 中！
            String userTempId  = CookieUtil.getCookieValue(request, "my-userId", false);
            // 声明一个集合来存储未登录购物车数据
            List<CartInfo> cartInfoArrayList = new ArrayList<>();
            if (userTempId!=null){
                cartInfoArrayList = cartService.getCartList(userTempId);
                // 判断购物车集合中是否有数据！
                if (cartInfoArrayList!=null && cartInfoArrayList.size()>0){
                    // 如果未登录购物车中有数据，则进行合并 合并的条件：skuId 相同
                    cartInfoList = cartService.mergeToCartList(cartInfoArrayList,userId);
                    // 删除未登录购物车数据
                    cartService.deleteCartList(userTempId);
                }
            }
            // 如果未登录购物车中没用数据！
            if (userTempId==null || (cartInfoArrayList==null || cartInfoArrayList.size()==0)){
                // 根据什么查询？userId
                cartInfoList = cartService.getCartList(userId);
            }
        }else {
            // 未登录：临时用户Id 存储在cookie 中！
            String userTempId  = CookieUtil.getCookieValue(request, "my-userId", false);
            if (userTempId!=null){
                cartInfoList = cartService.getCartList(userTempId);
            }
        }
        request.setAttribute("cartInfoList",cartInfoList);
        return "cartList";
    }
    // http://cart.gmall.com/checkCart
    @RequestMapping("checkCart")
    @ResponseBody
    @LoginRequire(autoRedirect = false)
    public void checkCart(HttpServletRequest request){
        // 获取用户Id
        String userId = (String) request.getAttribute("userId");
        String isChecked = request.getParameter("isChecked");
        String skuId = request.getParameter("skuId");
        // update cartInfo set isChecked=? where  skuId = ? and userId=？
        if (userId==null){
            // 未登录
            userId = CookieUtil.getCookieValue(request, "my-userId", false);
        }
        // 调用更新方法
        cartService.checkCart(userId,isChecked,skuId);

    }

    //  http://cart.gmall.com/toTrade
    @RequestMapping("toTrade")
    @LoginRequire
    public String toTrade(HttpServletRequest request){
        // 获取用户Id
        String userId = (String) request.getAttribute("userId");
        List<CartInfo> cartInfoList = null;
        // 对商品的勾选状态进行合并
        // 未登录：临时用户Id 存储在cookie 中！
        String userTempId  = CookieUtil.getCookieValue(request, "my-userId", false);
        if (userTempId!=null){
            // 未登录数据
            cartInfoList = cartService.getCartList(userTempId);
            if (cartInfoList!=null && cartInfoList.size()>0){
                List<CartInfo> mergeToCartList = cartService.mergeToCartList(cartInfoList, userId);
                // 删除未登录数据
                cartService.deleteCartList(userTempId);
            }

        }
        // 重定向到订单页面！
        return "redirect://trade.gmall.com/trade";
    }

}
