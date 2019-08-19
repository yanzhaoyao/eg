package com.lucas.sentinel.provider;


import com.lucas.sentinel.api.SentinelService;
import org.apache.dubbo.config.annotation.Service;

import java.time.LocalDateTime;

/**
 * TODO 描述
 *
 * @author zhaoyao.yan
 * @date 2019-08-13 11:10
 */
@Service
public class SentinelServiceImpl implements SentinelService {

    @Override
    public String sayHello(String name) {
        System.out.println("begin execute sayHello:" + name);
        return "Hello World:" + name + "->timer:" + LocalDateTime.now();
    }
}
