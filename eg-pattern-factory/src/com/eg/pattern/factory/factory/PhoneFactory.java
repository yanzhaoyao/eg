package com.eg.pattern.factory.factory;

import com.eg.pattern.factory.impl.HUAWEI;
import com.eg.pattern.factory.impl.Iphone;
import com.eg.pattern.factory.impl.Vivo;
import com.eg.pattern.factory.inface.IMobilePhone;

/**
 * @description 手机工厂类
 * @auther Administrator
 * @date 2018/11/27 21:10
 */
public class PhoneFactory {

    public IMobilePhone getIphone(String trademaek){
        if(trademaek == null){
            return null;
        }
        if(trademaek.equalsIgnoreCase("iphone")){
            return new Iphone();
        }
        if(trademaek.equalsIgnoreCase("huawei")){
            return new HUAWEI();
        }
        if(trademaek.equalsIgnoreCase("vivo")){
            return new Vivo();
        }
        return null;
    }

}
