package com.lucas.sentinel.provider;

import com.alibaba.csp.sentinel.cluster.client.config.ClusterClientAssignConfig;
import com.alibaba.csp.sentinel.cluster.client.config.ClusterClientConfig;
import com.alibaba.csp.sentinel.cluster.client.config.ClusterClientConfigManager;
import com.alibaba.csp.sentinel.datasource.ReadableDataSource;
import com.alibaba.csp.sentinel.datasource.nacos.NacosDataSource;
import com.alibaba.csp.sentinel.init.InitFunc;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRule;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRuleManager;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;

import java.util.List;

/**
 * TODO 描述
 *
 * @author zhaoyao.yan
 * @date 2019-08-19 12:57
 */
public class DataSourceInitFunc implements InitFunc {
    private static final String CLUSTER_SERVER_HOST = "localhost";//token-server的ip
    private static final int CLUSTER_SERVER_PORT = 8999;//token-server 端口
    private static final int REQUEST_TIME_OUT = 200000; //请求超时时间
    private static final String APP_NAME = "App-Lucas";
    private static final String REMOTE_ADDRESS = "192.168.0.102"; //nacos服务的ip
    private static final String GROUP_ID = "SENTINEL_GROUP";//group id
    private static final String FLOW_POSTFIX = "-flow-rules";//限流规则后缀

    @Override
    public void init() throws Exception {
        loadClusterClientConfig();
        registerClusterFlowRuleProperty();
    }

    //通过硬编码的方式，配置连接到token-server服务的地址,{这种在实际使用过程中不建议，后续可以基于动态配置源改造}
    public static void loadClusterClientConfig() {
        ClusterClientAssignConfig assignConfig = new
                ClusterClientAssignConfig();
        assignConfig.setServerHost(CLUSTER_SERVER_HOST);
        assignConfig.setServerPort(CLUSTER_SERVER_PORT);
        ClusterClientConfigManager.applyNewAssignConfig(assignConfig);
        ClusterClientConfig clientConfig = new ClusterClientConfig();
        clientConfig.setRequestTimeout(REQUEST_TIME_OUT); //token-client请求token-server获取令牌的超时时间
        ClusterClientConfigManager.applyNewConfig(clientConfig);
    }

    /**
     * 注册动态规则Property
     * 当client与Server连接中断，退化为本地限流时需要用到的该规则
     * 该配置为必选项，客户端会从nacos上加载限流规则，请求tokenserver时，会戴上要check的规则id
     * {这里的动态数据源，我们稍后会专门讲到}
     */
    private static void registerClusterFlowRuleProperty() {
        // 使用 Nacos 数据源作为配置中心，需要在 REMOTE_ADDRESS 上启动一个 Nacos 的服务
        ReadableDataSource<String, List<FlowRule>> ds = new
                NacosDataSource<List<FlowRule>>(REMOTE_ADDRESS, GROUP_ID, APP_NAME + FLOW_POSTFIX,
                source -> JSON.parseObject(source, new
                        TypeReference<List<FlowRule>>() {
                        }));
        // 为集群客户端注册动态规则源
        FlowRuleManager.register2Property(ds.getProperty());
    }
}
