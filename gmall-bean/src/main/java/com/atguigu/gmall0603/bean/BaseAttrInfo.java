package com.atguigu.gmall0603.bean;

import lombok.Data;

import javax.persistence.*;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@Data
public class BaseAttrInfo implements Serializable{
    @Id
    @Column
    @GeneratedValue(strategy = GenerationType.IDENTITY) // mysql = GenerationType.IDENTITY 表示获取主键自增！ oracle = GenerationType.AUTO
    private String id;
    @Column
    private String attrName;
    @Column
    private String catalog3Id;

    @Transient // 表示数据库表中没有的字段，但是业务需要使用！ attrValueList 在页面封装好了！
    private List<BaseAttrValue> attrValueList;


    public static void main(String[] args) {
        ArrayList<String> stringArrayList = new ArrayList<>();
        stringArrayList.add("1");
        stringArrayList.add("2");
        stringArrayList.add("3");
        stringArrayList.add("4");

//        for (String s : stringArrayList) {
//            if ("3".equals(s)){
//                stringArrayList.remove(s);
//            }
//        }

        for (Iterator<String> iterator = stringArrayList.iterator(); iterator.hasNext(); ) {
            String s = iterator.next();
            System.out.println(s);
            if ("4".equals(s)){
                stringArrayList.remove(s);
            }
        }

//        for (int i = 0; i < stringArrayList.size(); i++) {
//            String s = stringArrayList.get(i);
//            if ("3".equals(s)){
//                stringArrayList.remove(s);
//            }
//        }
        System.out.println(stringArrayList);
    }
}
