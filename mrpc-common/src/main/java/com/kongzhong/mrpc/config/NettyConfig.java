package com.kongzhong.mrpc.config;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * netty服务端配置
 *
 * @author biezhi
 *         2017/4/28
 */
@Data
@AllArgsConstructor
public class NettyConfig {

    private int backlog;
    private boolean keepalive;

    private int lowWaterMark = 32 * 1024;
    private int highWaterMark = 64 * 1024;

    public NettyConfig(int backlog, boolean keepalive) {
        this.backlog = backlog;
        this.keepalive = keepalive;
    }

}
