package com.lucas.sentinel;

import com.alibaba.csp.sentinel.cluster.server.ClusterTokenServer;
import com.alibaba.csp.sentinel.cluster.server.SentinelDefaultTokenServer;
import com.alibaba.csp.sentinel.cluster.server.config.ClusterServerConfigManager;
import com.alibaba.csp.sentinel.cluster.server.config.ServerTransportConfig;

import java.util.Collections;

/**
 * TODO 描述
 *
 * @author zhaoyao.yan
 * @date 2019-08-19 12:16
 */
public class ClusterServer {
    public static void main(String[] args) throws Exception {
        ClusterTokenServer tokenServer = new SentinelDefaultTokenServer();
        ClusterServerConfigManager.loadGlobalTransportConfig(new
                ServerTransportConfig().
                setIdleSeconds(600).setPort(9999));
        ClusterServerConfigManager.loadServerNamespaceSet(Collections.singleton("App-Lucas"));
                tokenServer.start();
    }
}
