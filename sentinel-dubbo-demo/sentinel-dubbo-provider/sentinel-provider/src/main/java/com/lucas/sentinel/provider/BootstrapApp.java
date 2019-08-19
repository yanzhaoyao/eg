package com.lucas.sentinel.provider;

import com.alibaba.csp.sentinel.slots.block.RuleConstant;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRule;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRuleManager;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.io.IOException;
import java.util.Collections;

/**
 * Hello world!
 */
public class BootstrapApp {
    public static void main(String[] args) throws IOException {
        initFlowRule();//初始化限流规则
        ApplicationContext applicationContext =
                new AnnotationConfigApplicationContext(ProviderConfig.class);
        ((AnnotationConfigApplicationContext) applicationContext).start();
        System.in.read();
    }

    private static void initFlowRule() {
        FlowRule flowRule = new FlowRule();
        //针对具体的方法限流
        flowRule.setResource("com.lucas.sentinel.api.SentinelService:sayHello(java.lang.String)");
        flowRule.setCount(10);//限流阈值 qps=10
        flowRule.setGrade(RuleConstant.FLOW_GRADE_QPS);//限流阈值类型（QPS 或并发线程数）
        flowRule.setLimitApp("springboot-study");//流控针对的调用来源，若为 default 则不区分调用来源
        // 流量控制手段（直接拒绝、Warm Up、匀速排队）
        flowRule.setControlBehavior(RuleConstant.CONTROL_BEHAVIOR_DEFAULT);
        FlowRuleManager.loadRules(Collections.singletonList(flowRule));
    }
}
