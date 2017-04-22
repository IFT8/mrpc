package com.kongzhong.mrpc.client;

import com.google.common.reflect.AbstractInvocationHandler;
import com.kongzhong.mrpc.ha.Connections;
import com.kongzhong.mrpc.ha.loadblance.LoadBalance;
import com.kongzhong.mrpc.ha.loadblance.SimpleLoadBalance;
import com.kongzhong.mrpc.model.RpcRequest;
import com.kongzhong.mrpc.transport.SimpleClientHandler;
import com.kongzhong.mrpc.utils.StringUtils;

import java.lang.reflect.Method;

/**
 * 客户端代理
 *
 * @param <T>
 */
public class ClientProxy<T> extends AbstractInvocationHandler {

    /**
     * 负载均衡器
     */
    private LoadBalance loadBalance = new SimpleLoadBalance();

    @Override
    public Object handleInvocation(Object proxy, Method method, Object[] args) throws Throwable {
        RpcRequest request = new RpcRequest(StringUtils.getUUID(),
                method.getDeclaringClass().getName(), method.getName(),
                method.getParameterTypes(), args, method.getReturnType());

        SimpleClientHandler clientHandler = loadBalance.getClientHandler();
        while (!checkHeart(clientHandler)) {
            clientHandler = loadBalance.getClientHandler();
        }
        RpcFuture rpcFuture = clientHandler.sendRequest(request);
        return rpcFuture.get();
    }

    private boolean checkHeart(SimpleClientHandler clientHandler) {
        if (!clientHandler.getChannel().isActive()) {
            return false;
        }
        return true;
    }

}