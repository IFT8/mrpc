package com.kongzhong.mrpc.demo.helloworld;

import com.kongzhong.mrpc.client.RpcClient;
import com.kongzhong.mrpc.demo.service.UserService;

/**
 * @author biezhi
 *         2017/4/19
 */
public class ClientApplication {
    public static void main(String[] args) {
        RpcClient rpcClient = new RpcClient("127.0.0.1:5066");

        UserService userService = rpcClient.getProxyBean(UserService.class);
        System.out.println(userService);
        System.out.println(userService.add(10, 20));
        System.out.println(userService.add(20, 20));
        System.out.println(userService.add(30, 20));
        System.out.println(userService.add(40, 20));
        System.out.println(userService.add(50, 20));
        rpcClient.stop();
    }
}
