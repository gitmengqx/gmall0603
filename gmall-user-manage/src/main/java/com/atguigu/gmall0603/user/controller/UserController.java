package com.atguigu.gmall0603.user.controller;

import com.atguigu.gmall0603.bean.UserInfo;
import com.atguigu.gmall0603.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sound.midi.Soundbank;
import java.util.List;

@RestController
public class UserController {

    @Autowired
    private UserService userService;
    @RequestMapping("findAll")
    public List<UserInfo> findAll(){
        return userService.findAll();
    }


}
