# Sentinel整合Dubbo限流实战

## 创建provider项目

![image-20190819104544783](http://ww4.sinaimg.cn/large/006tNc79gy1g64skzk5egj30qw0rkacs.jpg)

### 添加jar依赖

```xml
<dependency>
  <artifactId>sentinel-api</artifactId>
  <groupId>com.lucas.sentinel</groupId>
  <version>1.0-SNAPSHOT</version>
</dependency>
<dependency>
  <groupId>org.apache.dubbo</groupId>
  <artifactId>dubbo</artifactId>
  <version>2.7.2</version>
</dependency>
<dependency>
  <groupId>org.slf4j</groupId>
  <artifactId>slf4j-api</artifactId>
  <version>1.7.25</version>
</dependency>
<dependency>
  <groupId>org.apache.curator</groupId>
  <artifactId>curator-framework</artifactId>
  <version>4.0.0</version>
</dependency>
<dependency>
  <groupId>org.apache.curator</groupId>
  <artifactId>curator-recipes</artifactId>
  <version>4.0.0</version>
</dependency>
```

### SentinelService

```java
public interface SentinelService {
  String sayHello(String name);
}
```

### SentinelServiceImpl

```java
@Service
public class SentinelServiceImpl implements SentinelService{
  
  @Override
  public String sayHello(String name) {
    System.out.println("begin execute sayHello:"+name);
    return "Hello World:"+name+"->timer:"+LocalDateTime.now();
  }
}
```

### ProviderConfig

```java
@Configuration
@DubboComponentScan("com.lucas.sentinel")
public class ProviderConfig {

    @Bean
    public ApplicationConfig applicationConfig() {
        ApplicationConfig config = new ApplicationConfig();
        config.setName("sentinel-web");
        config.setOwner("Lucas");
        return config;
    }

    @Bean
    public RegistryConfig registryConfig() {
        RegistryConfig registryConfig = new RegistryConfig();
        registryConfig.setAddress("zookeeper://192.168.0.102:2181");
        return registryConfig;
    }

    @Bean
    public ProtocolConfig protocolConfig() {
        ProtocolConfig protocolConfig = new ProtocolConfig();
        protocolConfig.setName("dubbo");
        protocolConfig.setPort(20880);
        return protocolConfig;
    }
}
```

### BootstrapApp

```java
public class BootstrapApp {
    public static void main(String[] args) throws IOException {
        ApplicationContext applicationContext =
                new AnnotationConfigApplicationContext(ProviderConfig.class);
        ((AnnotationConfigApplicationContext) applicationContext).start();
        System.in.read();
    }
}
```

## 创建SpringBoot的Consumer项目

### 添加jar依赖

```xml
<dependency>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-starter</artifactId>
</dependency>

<dependency>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-starter-test</artifactId>
  <scope>test</scope>
</dependency>

<dependency>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-starter-web</artifactId>
</dependency>
<dependency>
  <groupId>org.apache.dubbo</groupId>
  <artifactId>dubbo-spring-boot-starter</artifactId>
  <version>2.7.1</version>
</dependency>
<dependency>
  <groupId>org.apache.dubbo</groupId>
  <artifactId>dubbo</artifactId>
  <version>2.7.1</version>
</dependency>
<dependency>
  <groupId>com.lucas.sentinel</groupId>
  <artifactId>sentinel-api</artifactId>
  <version>1.0-SNAPSHOT</version>
</dependency>
<dependency>
  <!-- 
zookeeper 需要使用3.5+版本 -> curator 4.0+
zookeeper 需要使用3.4.*版本 -> curator 2.0+
采坑了-->
  <groupId>org.apache.curator</groupId>
  <artifactId>curator-recipes</artifactId>
  <version>4.0.0</version>
</dependency>
```

### SentinelDubboController

```java
@RestController
public class SentinelDubboController {

    @Reference
    SentinelService sentinelService;

    @GetMapping("/say")
    public String sayHello(){
        String result=sentinelService.sayHello("hello word");
        return result;
    }
}
```

### application.properties

```properties
dubbo.registry.address=zookeeper://192.168.0.102:2181
dubbo.application.name=sentinel-web
dubbo.scan.base-packages=com.lucas.sentinel.demo.study
```

## 现在启动BoostrapApp，启动Consumer，用jmeter测试

![image-20190819105614153](http://ww1.sinaimg.cn/large/006tNc79gy1g64svulc9dj31c00u00z8.jpg)

![image-20190819105644332](http://ww3.sinaimg.cn/large/006tNc79gy1g64swdrzwzj31c00u0qbd.jpg)

## 添加sentinel限流支持

### provider添加jar包依赖

```xml
<dependency>
  <groupId>com.alibaba.csp</groupId>
  <artifactId>sentinel-dubbo-adapter</artifactId>
  <version>1.6.2</version>
</dependency>
```

### 设置限流的基准

Service Provider 用于向外界提供服务，处理各个消费者的调用请求。为了保护 Provider 不被激增的流量拖垮影响稳定性，可以给 Provider 配置 QPS 模式的限流，这样当每秒的请求量超过设定的阈值时会自动拒绝多的请求。限流粒度可以是服务接口和服务方法两种粒度。若希望整个服务接口的 QPS 不超过一定数值，则可以为对应服务接口资源（resourceName 为接口全限定名）配置 QPS 阈值；若希望服务的某个方法的 QPS 不超过一定数值，则可以为对应服务方法资源（resourceName 为接口全限定名:方法签名）配置 QPS 阈值

```java
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
        flowRule.setResource("com.lucas.sentinel.api.SentinelService:sayHello(java.lang.String) ");
        flowRule.setCount(10);//限流阈值 qps=10
        flowRule.setGrade(RuleConstant.FLOW_GRADE_QPS);//限流阈值类型（QPS 或并发线程数）
        flowRule.setLimitApp("default");//流控针对的调用来源，若为 default 则不区分调用来源
        // 流量控制手段（直接拒绝、Warm Up、匀速排队）
        flowRule.setControlBehavior(RuleConstant.CONTROL_BEHAVIOR_DEFAULT);
        FlowRuleManager.loadRules(Collections.singletonList(flowRule));
    }
}
```

启动时加入 JVM 参数 `-Dcsp.sentinel.dashboard.server=localhost:8080` 指定控制台地址和端口

![image-20190819113449456](http://ww4.sinaimg.cn/large/006tNc79gy1g64tzzwmayj31520u0q9r.jpg)

这个是sentinel提供的一个web可视化页面，我们下载这个`sentinel-dashboard.jar`，下载地址https://github.com/alibaba/Sentinel/releases

![image-20190819110634450](http://ww4.sinaimg.cn/large/006tNc79gy1g64t6lty4hj31l20u0tge.jpg)

下载完成以后，直接用java -jar 启动,默认是8080端口

```shell
java -jar sentinel-dashboard-1.6.3.jar
```

启动之后的浏览器进入http://localhost:8080

![image-20190819113414277](http://ww2.sinaimg.cn/large/006tNc79gy1g64tze4lzoj31qg0u0wix.jpg)

默认的用户密码为sentinel/sentinel

进入之后sentinel首页如下图

![image-20190819111856025](http://ww1.sinaimg.cn/large/006tNc79gy1g64tjgzzb7j31sr0u0din.jpg)

因为还没有请求相应，没有数据

### 使用jemeter进行压测

100个qps

![image-20190819112140707](http://ww1.sinaimg.cn/large/006tNc79gy1g64tmbssylj316g0neq65.jpg)

![image-20190819114522244](http://ww3.sinaimg.cn/large/006tNc79gy1g64uazebnej31c00u0do5.jpg)

因为我们配置的限流阈值 qps=10

![image-20190819114621543](http://ww4.sinaimg.cn/large/006tNc79gy1g64uc0b3xrj31qo0u0dno.jpg)

## 参数解释

### LimitApp

很多场景下，根据调用方来限流也是非常重要的。比如有两个服务 A 和 B 都向 Service Provider 发起调用请求，我们希望只对来自服务 B 的请求进行限流，则可以设置限流规则的 limitApp 为服务 B 的名称。Sentinel Dubbo Adapter 会自动解析 Dubbo 消费者（调用方）的 application name 作为调用方名称（origin），在进行资源保护的时候都会带上调用方名称。若限流规则未配置调用方（default），则该限流规则对所有调用方生效。若限流规则配置了调用方则限流规则将仅对指定调用方生效。

注：Dubbo 默认通信不携带对端 application name 信息，因此需要开发者在调用端手动将 applicationname 置入 attachment 中，provider 端进行相应的解析。Sentinel Dubbo Adapter 实现了一个 Filter 用于自动从 consumer 端向 provider 端透传 application name。若调用端未引入 Sentinel Dubbo Adapter，又希望根据调用端限流，可以在调用端手动将 application name 置入 attachment 中，key 为dubboApplication

演示流程

1. 修改provider中限流规则：flowRule.setLimitApp("springboot-study");

2. 在consumer工程中，做如下处理。其中一个通过attachment传递了一个消费者的 application.name，另一个没有传，通过jemeter工具进行测试

```java
		@GetMapping("/say")
    public String sayHello(){
        RpcContext.getContext().setAttachment("dubboApplication","springbootstudy");
        String result=sentinelService.sayHello("Lucas");
        return result;
    }
    @GetMapping("/say2")
    public String say2Hello(){
        String result=sentinelService.sayHello("Lucas");
        return result;
    }
```

`/say`的压测，限流生效

![image-20190819115158588](http://ww1.sinaimg.cn/large/006tNc79gy1g64uhul7htj31c00u0472.jpg)

`/say2`的压测，限流未生效

![image-20190819115246776](http://ww2.sinaimg.cn/large/006tNc79gy1g64uip4mdqj31c00u0aim.jpg)

### ControlBehavior

当 QPS 超过某个阈值的时候，则采取措施进行流量控制。流量控制的手段包括以下几种：

`直接拒绝`、`Warm Up`、`匀速排队`。

- 直接拒绝（RuleConstant.CONTROL_BEHAVIOR_DEFAULT）方式是默认的流量控制方式，当QPS超过任意规则的阈值后，新的请求就会被立即拒绝，拒绝方式为抛出FlowException。这种方式适用于对系统处理能力确切已知的情况下，比如通过压测确定了系统的准确水位时

- Warm Up（RuleConstant.CONTROL_BEHAVIOR_WARM_UP）方式，即预热/冷启动方式，当系统长期处于低并发的情况下，流量突然增加到qps的最高峰值，可能会造成系统的瞬间流量过大把系统压垮。所以warmup，相当于处理请求的数量是缓慢增加，经过一段时间以后，到达系统处理请求个数的最大值

- 匀速排队（RuleConstant.CONTROL_BEHAVIOR_RATE_LIMITER）方式会严格控制请求通过的间隔时间，也即是让请求以均匀的速度通过，对应的是漏桶算法它的原理是，以固定的间隔时间让请求通过。当请求过来的时候，如果当前请求距离上个通过的请求通过的时间间隔不小于预设值，则让当前请求通过；否则，计算当前请求的预期通过时间，如果该请求的预期通过时间小于规则预设的 timeout 时间，则该请求会等待直到预设时间到来通过；反之，则马上抛出阻塞异常。

可以设置一个最长排队等待时间： flowRule.setMaxQueueingTimeMs(5 * 1000); // 最长排队等待时间：5s这种方式主要用于处理间隔性突发的流量，例如消息队列。想象一下这样的场景，在某一秒有大量的请求到来，而接下来的几秒则处于空闲状态，我们希望系统能够在接下来的空闲期间逐渐处理这些请求，而不是在第一秒直接拒绝多余的请求。

# 如何实现分布式限流

在前面的所有案例中，我们只是基于Sentinel的基本使用和单机限流的使用，假如有这样一个场景，我们现在把provider部署了10个集群，希望调用这个服务的api的总的qps是100，意味着每一台机器的qps是10，理想情况下总的qps就是100.但是实际上由于负载均衡策略的流量分发并不是非常均匀的，就会导致总的qps不足100时，就被限了。在这个场景中，仅仅依靠单机来实现总体流量的控制是有问题的。所以最好是能实现集群分布式限流。

## 架构图

要想使用集群流控功能，我们需要在应用端配置动态规则源，并通过 Sentinel 控制台实时进行推送。如下图所示：

![081911590257041](http://ww4.sinaimg.cn/large/006tNc79gy1g64v3wjbkxj30r90ewq4k.jpg)

## 搭建token-server

![image-20190819124250904](http://ww4.sinaimg.cn/large/006tNc79gy1g64vys0bjtj30li0k2769.jpg)

### Jar包依赖

```xml
				<dependency>
            <groupId>com.alibaba.csp</groupId>
            <artifactId>sentinel-cluster-server-default</artifactId>
            <version>1.6.3</version>
        </dependency>
        <dependency>
            <groupId>com.alibaba.csp</groupId>
            <artifactId>sentinel-transport-simple-http</artifactId>
            <version>1.6.3</version>
        </dependency>
        <dependency>
            <groupId>com.alibaba.csp</groupId>
            <artifactId>sentinel-datasource-nacos</artifactId>
            <version>1.6.3</version>
        </dependency>
        <dependency>
            <groupId>org.slf4j</groupId>
            <artifactId>slf4j-log4j12</artifactId>
            <version>1.7.25</version>
        </dependency>
```

### ClusterServer

```java
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
```

### DataSourceInitFunc

```java
public class DataSourceInitFunc implements InitFunc {
    //nacos 远程服务host
    private final String remoteAddress = "192.168.0.102";
    //nacos groupId
    private final String groupId = "SENTINEL_GROUP";
    //namespace不同，限流规则也不同
    private static final String FLOW_POSTFIX = "-flow-rules";

    @Override
    public void init() throws Exception {
        ClusterFlowRuleManager.setPropertySupplier(namespace -> {
            ReadableDataSource<String, List<FlowRule>> rds =
                    new NacosDataSource<>(remoteAddress, groupId,
                            namespace + FLOW_POSTFIX,
                            source -> JSON.parseObject(source, new
                                    TypeReference<List<FlowRule>>() {
                                    }));
            return rds.getProperty();
        });
    }
}
```

resource目录添加扩展点

/META-INF/services/com.alibaba.csp.sentinel.init.InitFunc = 自定义扩展点

添加log4j.properties文件

启动Sentinel dashboard

```java
java -Dserver.port=8080 -Dcsp.sentinel.dashboard.server=localhost:8080 -Dproject.name=sentinel-dashboard -jar sentinel-dashboard-1.6.3.jar
```

### 启动nacos以及增加配置

1. 启动nacos服务： nohup sh startup.sh -m standalone &

2. 增加限流配置

   ```json
   [
       {
           "resource":"com.lucas.sentinel.api.SentinelService:sayHello(java.lang.String)",//限流资源
           "grade":1,//限流模式：qps
           "count":10,//限流总阈值
           "clusterMode":true,
           "clusterConfig":{
               "flowId":111111,//全局唯一ID
               "thresholdType":1,//阈值模式，全局阈值
               "fallbackToLocalWhenFail":true //clinet连接失败或者通信失败时，是否退化到本地限流模式
           }
       }
   ]
   ```

3. ![image-20190819124842161](http://ww3.sinaimg.cn/large/006tNc79gy1g64w4vmti0j31w20u0wlv.jpg)

### 配置jvm参数

配置如下jvm启动参数，连接到sentinel dashboard

`-Dproject.name=App-Lucas -Dcsp.sentinel.dashboard.server=192.168.0.102:8080 -Dcsp.sentinel.log.use.pid=true`

- 服务启动之后，在$user.home$/logs/csp/ 可以找到sentinel-record.log.pid*.date文件，如果看到日志文件中获取到了远程服务的信息，说明token-server启动成功了

## Dubbo接入分布式限流

## jar包依赖

```xml
<dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter</artifactId>
        </dependency>

        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>

        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>

        <dependency>
            <artifactId>sentinel-api</artifactId>
            <groupId>com.lucas.sentinel</groupId>
            <version>1.0-SNAPSHOT</version>
        </dependency>
        <dependency>
            <groupId>org.apache.dubbo</groupId>
            <artifactId>dubbo</artifactId>
            <version>2.7.2</version>
        </dependency>
        <dependency>
            <groupId>org.slf4j</groupId>
            <artifactId>slf4j-api</artifactId>
            <version>1.7.25</version>
        </dependency>
        <dependency>
            <groupId>org.apache.curator</groupId>
            <artifactId>curator-framework</artifactId>
            <version>4.0.0</version>
        </dependency>
        <dependency>
            <groupId>org.apache.curator</groupId>
            <artifactId>curator-recipes</artifactId>
            <version>4.0.0</version>
        </dependency>

        <dependency>
            <groupId>com.alibaba.csp</groupId>
            <artifactId>sentinel-dubbo-adapter</artifactId>
            <version>1.6.3</version>
        </dependency>
        <dependency>
            <groupId>com.alibaba.csp</groupId>
            <artifactId>sentinel-transport-simple-http</artifactId>
            <version>1.6.3</version>
        </dependency>
        <dependency>
            <groupId>com.alibaba.csp</groupId>
            <artifactId>sentinel-dubbo-adapter</artifactId>
            <version>1.6.3</version>
        </dependency>
        <dependency>
            <groupId>com.alibaba.csp</groupId>
            <artifactId>sentinel-cluster-client-default</artifactId>
            <version>1.6.3</version>
        </dependency>
        <dependency>
            <groupId>com.alibaba.csp</groupId>
            <artifactId>sentinel-transport-simple-http</artifactId>
            <version>1.6.3</version>
        </dependency>
        <dependency>
            <groupId>com.alibaba.csp</groupId>
            <artifactId>sentinel-datasource-nacos</artifactId>
            <version>1.6.3</version>
        </dependency>
```

### 增加扩展点

扩展点需要在resources/META-INF/services/增加扩展的配置

com.alibaba.csp.sentinel.init.InitFunc = 自定义扩展点

```java
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
```

### 配置jvm参数

这里的project-name要包含在token-server中配置的namespace中，token server 会根据客户端对应的 namespace（默认为 project.name 定义的应用名）下的连接数来

计算总的阈值

`-Dproject.name=App-Lucas -Dcsp.sentinel.dashboard.server=192.168.0.102:8080 -Dcsp.sentinel.log.use.pid=true`

- 服务启动之后，在\$user.home​\$/logs/csp/ 可以找到sentinel-record.log.pid*.date文件，如果看到日

志文件中获取到了token-server的信息，说明连接成功了

### 演示集群限流

所谓集群限流，就是多个服务节点使用同一个限流规则。从而对多个节点的总流量进行限制，添加一个sentinel-server。同时运行两个程序

![image-20190819135143652](http://ww1.sinaimg.cn/large/006tNc79gy1g64xyjxkh0j31520u046d.jpg)

```shell
-Dserver.port=8082 -Dproject.name=App-Lucas -Dcsp.sentinel.dashboard.server=192.168.0.102:8080 -Dcsp.sentinel.log.use.pid=true -Ddubbo.protocol.port=20881
```

```shell
-Dserver.port=8081 -Dproject.name=App-Lucas -Dcsp.sentinel.dashboard.server=192.168.0.102:8080 -Dcsp.sentinel.log.use.pid=true
```

## 压测

使用jemeter创建1000个线程，进行压测，然后关注sentinel dashboard的变化。

![image-20190819135351799](http://ww1.sinaimg.cn/large/006tNc79gy1g64y0o5e3kj31c00u0gth.jpg)

![image-20190819135410759](http://ww2.sinaimg.cn/large/006tNc79gy1g64y0zua32j31vy0u07bn.jpg)

我们改一下nacos中心的配置，qps改为20，在压测一次

![image-20190819135524846](http://ww4.sinaimg.cn/large/006tNc79gy1g64y2ad5ioj31qs0u043w.jpg)

![image-20190819135549915](http://ww2.sinaimg.cn/large/006tNc79gy1g64y2quq2aj31c00u0jz9.jpg)

![image-20190819135657336](http://ww2.sinaimg.cn/large/006tNc79gy1g64y3vx4osj31w80u045k.jpg)

图上出现了21，可能是统计的误差。

