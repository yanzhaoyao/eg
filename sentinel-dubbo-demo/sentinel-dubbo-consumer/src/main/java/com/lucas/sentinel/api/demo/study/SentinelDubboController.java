package com.lucas.sentinel.api.demo.study;

import com.lucas.sentinel.api.SentinelService;
import org.apache.dubbo.config.annotation.Reference;
import org.apache.dubbo.rpc.RpcContext;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * TODO 描述
 *
 * @author zhaoyao.yan
 * @date 2019-08-13 11:27
 */
@RestController
public class SentinelDubboController {

    @Reference
    SentinelService sentinelService;

    @GetMapping("/say")
    public String sayHello(){
//        RpcContext.getContext().setAttachment("dubboApplication","springboot-study");
        String result=sentinelService.sayHello("Lucas");
        return result;
    }
//    @GetMapping("/say2")
//    public String say2Hello(){
//        String result=sentinelService.sayHello("Lucas");
//        return result;
//    }
}
