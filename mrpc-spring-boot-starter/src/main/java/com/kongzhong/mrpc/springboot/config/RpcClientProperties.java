package com.kongzhong.mrpc.springboot.config;

import lombok.Data;
import lombok.ToString;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * mrpc客户端端配置
 *
 * @author biezhi
 *         2017/5/13
 */
@ConfigurationProperties("mrpc.client")
@Data
@ToString
public class RpcClientProperties {

    // 服务端传输协议，默认tcp
    private String transport;

    // 服务所属appId
    private String appId;

    // 直连服务地址
    private String directAddress;

    // 高可用策略
    private String haStrategy;

    // 负载均衡策略
    private String lbStrategy;

    // 序列化组件，默认kyro
    private String serialize;

    // 客户端连接超时时间，默认10秒
    private int waitTimeout = 10;

    // 客户端重试次数
    private int retryNumber = 3;

}