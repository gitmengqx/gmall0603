package com.atguigu.gmall0603.config;


import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD) // 该注解在方法上使用！
@Retention(RetentionPolicy.RUNTIME) // 表示注解的生命周期
public @interface LoginRequire {

    boolean autoRedirect() default true; // true 表示登录 默认登录！
}
