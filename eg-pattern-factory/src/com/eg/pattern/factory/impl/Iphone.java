package com.eg.pattern.factory.impl;

import com.eg.pattern.factory.inface.IMobilePhone;

/**
 * @description
 * @auther Administrator
 * @date 2018/11/27 21:06
 */
public class Iphone implements IMobilePhone {
    @Override
    public void publish() {
        System.out.println("发布：Iphone XS");
    }
}
