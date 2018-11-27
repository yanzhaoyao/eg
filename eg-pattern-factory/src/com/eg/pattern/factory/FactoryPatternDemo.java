package com.eg.pattern.factory;

import com.eg.pattern.factory.factory.PhoneFactory;
import com.eg.pattern.factory.impl.Iphone;
import com.eg.pattern.factory.inface.IMobilePhone;

/**
 * @description
 * @auther Administrator
 * @date 2018/11/27 21:15
 */
public class FactoryPatternDemo {
    public static void main(String[] args) {
        PhoneFactory phoneFactory = new PhoneFactory();

        //获取iphone对象，并发布手机publish（）
        IMobilePhone iphone = phoneFactory.getIphone("iphone");
        iphone.publish();

        //获取huawei对象，并发布手机publish（）
        IMobilePhone huawei = phoneFactory.getIphone("huawei");
        huawei.publish();

        //获取huawei对象，并发布手机publish（）
        IMobilePhone vivo = phoneFactory.getIphone("vivo");
        vivo.publish();
    }
}
