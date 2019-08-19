package com.lucas.sentinel.provider;

import com.alibaba.csp.sentinel.cluster.ClusterStateManager;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.IOException;

@SpringBootApplication
public class SentinelProviderApplication {

    public static void main(String[] args) throws IOException {
        //表示当前的节点是集群客户端
        ClusterStateManager.applyState(ClusterStateManager.CLUSTER_CLIENT);
        SpringApplication.run(SentinelProviderApplication.class, args);
        System.in.read();
    }
}
