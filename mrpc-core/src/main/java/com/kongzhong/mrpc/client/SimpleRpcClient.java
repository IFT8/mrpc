package com.kongzhong.mrpc.client;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.reflect.Reflection;
import com.kongzhong.mrpc.client.cluster.Connections;
import com.kongzhong.mrpc.client.cluster.HaStrategy;
import com.kongzhong.mrpc.client.cluster.ha.FailFastHaStrategy;
import com.kongzhong.mrpc.client.cluster.ha.FailOverHaStrategy;
import com.kongzhong.mrpc.client.proxy.SimpleClientProxy;
import com.kongzhong.mrpc.config.ClientCommonConfig;
import com.kongzhong.mrpc.enums.HaStrategyEnum;
import com.kongzhong.mrpc.enums.LbStrategyEnum;
import com.kongzhong.mrpc.enums.TransportEnum;
import com.kongzhong.mrpc.exception.RpcException;
import com.kongzhong.mrpc.exception.SystemException;
import com.kongzhong.mrpc.interceptor.RpcClientInteceptor;
import com.kongzhong.mrpc.model.ClientBean;
import com.kongzhong.mrpc.registry.ServiceDiscovery;
import com.kongzhong.mrpc.serialize.RpcSerialize;
import com.kongzhong.mrpc.utils.ReflectUtils;
import com.kongzhong.mrpc.utils.StringUtils;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * RPC客户端抽象实现
 *
 * @author biezhi
 *         2017/4/25
 */
@NoArgsConstructor
@Slf4j
public abstract class SimpleRpcClient {

    /**
     * 序列化类型，默认protostuff
     */
    @Setter
    protected String serialize;

    /**
     * 传输协议，默认tcp协议
     */
    @Setter
    protected String transport;

    /**
     * 客户端是否已经初始化
     */
    protected boolean isInit;

    /**
     * 负载均衡策略，默认轮询
     */
    @Setter
    protected String lbStrategy;

    /**
     * 高可用策略，默认failover
     */
    @Setter
    protected String haStrategy;

    /**
     * 服务注册实例
     */
    protected Map<String, ServiceDiscovery> serviceDiscoveryMap = Maps.newHashMap();

    /**
     * 客户端直连地址列表
     */
    protected Map<String, List<ClientBean>> directAddressList = Maps.newHashMap();

    /**
     * appId
     */
    @Setter
    protected String appId;

    /**
     * 是否使用注册中心
     */
    protected boolean usedRegistry;

    /**
     * 直连地址，开发时可配置，当配置了直连则不会走注册中心
     */
    @Setter
    protected String directAddress;

    /**
     * 引用类名
     */
    @Setter
    protected List<ClientBean> referers = Lists.newArrayList();

    /**
     * 客户端拦截器列表
     */
    protected List<RpcClientInteceptor> inteceptors = Lists.newArrayList();

    /***
     * 动态代理,获得代理后的对象
     *
     * @param rpcInterface
     * @param <T>
     * @return
     */
    public <T> T getProxyReferer(Class<T> rpcInterface) {
        if (!isInit) {
            try {
                this.init();
            } catch (Exception e) {
                log.error("RPC client init error", e);
            }
        }
        if (StringUtils.isNotEmpty(directAddress)) {
            this.directConnect(directAddress, rpcInterface);
        }
        return this.getProxyBean(rpcInterface);
    }

    /**
     * 获取一个Class的代理对象
     *
     * @param rpcInterface
     * @param <T>
     * @return
     */
    protected <T> T getProxyBean(Class<T> rpcInterface) {
        return (T) Reflection.newProxy(rpcInterface, new SimpleClientProxy<T>(inteceptors));
    }

    /**
     * 获取服务使用的注册中心
     *
     * @param serviceBean
     * @return
     */
    protected ServiceDiscovery getDiscovery(ClientBean clientBean) {
        ServiceDiscovery serviceDiscovery = serviceDiscoveryMap.get(clientBean.getRegistry());
        return serviceDiscovery;
    }

    protected void init() throws RpcException {
        Connections connections = Connections.me();
        if (null == serialize) serialize = "kyro";
        if (null == transport) transport = "tcp";
        if (null == lbStrategy) lbStrategy = LbStrategyEnum.ROUND.name();
        if (null == haStrategy) haStrategy = HaStrategyEnum.FAILOVER.name();

        RpcSerialize rpcSerialize = null;
        if (serialize.equalsIgnoreCase("kyro")) {
            rpcSerialize = ReflectUtils.newInstance("com.kongzhong.mrpc.serialize.KyroSerialize", RpcSerialize.class);
        }
        if (serialize.equalsIgnoreCase("protostuff")) {
            rpcSerialize = ReflectUtils.newInstance("com.kongzhong.mrpc.serialize.ProtostuffSerialize", RpcSerialize.class);
        }

        HaStrategy haStrategy = null;
        if (this.haStrategy.equalsIgnoreCase(HaStrategyEnum.FAILOVER.name())) {
            haStrategy = new FailOverHaStrategy();
        }
        if (this.haStrategy.equalsIgnoreCase(HaStrategyEnum.FAILFAST.name())) {
            haStrategy = new FailFastHaStrategy();
        }

        LbStrategyEnum lbStrategyEnum = LbStrategyEnum.valueOf(this.lbStrategy.toUpperCase());
        TransportEnum transportEnum = TransportEnum.valueOf(this.transport.toUpperCase());

        ClientCommonConfig.me().setAppId(appId);
        ClientCommonConfig.me().setRpcSerialize(rpcSerialize);
        ClientCommonConfig.me().setHaStrategy(haStrategy);
        ClientCommonConfig.me().setLbStrategy(lbStrategyEnum);
        ClientCommonConfig.me().setTransport(transportEnum);
        isInit = true;
    }

    /**
     * 直连
     *
     * @param directUrl
     * @param rpcInterface
     */
    protected void directConnect() {
        Map<String, Set<String>> mappings = Maps.newHashMap();
        directAddressList.forEach((directAddress, clientBeans) -> {
            Set<String> serviceNames = clientBeans.stream().map(clientBean -> clientBean.getServiceName()).collect(Collectors.toSet());
            mappings.put(directAddress, serviceNames);
        });
        Connections.me().updateNodes(mappings);
    }

    private void directConnect(String directAddress, Class<?> rpcInterface) {
        Map<String, Set<String>> mappings = Maps.newHashMap();
        mappings.put(directAddress, Sets.newHashSet(rpcInterface.getName()));
        Connections.me().updateNodes(mappings);
    }

    /**
     * 绑定多个客户端引用服务
     *
     * @param interfaces 接口名
     */
    public void bindReferer(Class<?>... interfaces) {
        if (null != interfaces) {
            Stream.of(interfaces).forEach(type -> referers.add(new ClientBean(type)));
        }
    }

    public void setDefaultDiscovery(ServiceDiscovery serviceDiscovery) {
        serviceDiscoveryMap.put("default", serviceDiscovery);
    }

    /**
     * 绑定多个客户端引用服务
     *
     * @param interfaces 接口名
     */
    public void bindReferer(String... interfaces) {
        if (null != interfaces) {
            Stream.of(interfaces).forEach(type -> referers.add(new ClientBean(ReflectUtils.from(type))));
        }
    }

    /**
     * 添加一个客户端拦截器
     *
     * @param inteceptor
     */
    public void addInterceptor(RpcClientInteceptor inteceptor) {
        if (null == inteceptor) {
            throw new IllegalArgumentException("RpcClientInteceptor not is null");
        }
        log.info("Add interceptor [{}]", inteceptor.toString());
        this.inteceptors.add(inteceptor);
    }

    /**
     * 初始化客户端引用
     *
     * @param clientBean
     * @param beanFactory
     */
    protected void initReferer(ClientBean clientBean, ConfigurableListableBeanFactory beanFactory) {
        String serviceName = clientBean.getServiceName();
        Class<?> serviceClass = clientBean.getServiceClass();
        try {
            Object object = this.getProxyBean(serviceClass);
            if (null != beanFactory) {
                beanFactory.registerSingleton(serviceName, object);
            }
            if (usedRegistry) {
                // 服务发现
                ServiceDiscovery serviceDiscovery = this.getDiscovery(clientBean);
                if (null == serviceDiscovery) {
                    throw new SystemException(String.format("Client referer [%s] not found registry [%s]", serviceName, clientBean.getRegistry()));
                }
                serviceDiscovery.discover();
            } else {
                String directAddress = StringUtils.isNotEmpty(clientBean.getDirectAddress()) ? clientBean.getDirectAddress() : this.directAddress;
                if (StringUtils.isNotEmpty(directAddress)) {
                    log.debug("Service [{}] direct to [{}]", serviceName, directAddress);

                    List<ClientBean> directUrlServices = directAddressList.getOrDefault(directAddress, new ArrayList<>());
                    directUrlServices.add(clientBean);
                    directAddressList.put(directAddress, directUrlServices);
                }
            }
            log.info("Bind rpc service [{}]", serviceName);
        } catch (Exception e) {
            log.warn("Bind rpc service [{}] error", serviceName, e);
        }

    }

    /**
     * 停止客户端，释放资源
     */
    public void stop() {
        Connections.me().shutdown();
    }

}