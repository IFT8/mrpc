package com.kongzhong.mrpc.registry;

import com.github.zkclient.IZkClient;
import com.github.zkclient.ZkClient;
import com.kongzhong.mrpc.exception.RpcException;
import com.kongzhong.mrpc.model.ServiceBean;
import lombok.extern.slf4j.Slf4j;

/**
 * 服务注册
 */
@Slf4j
public class ZookeeperServiceRegistry implements ServiceRegistry {

    private IZkClient zkClient;

    public ZookeeperServiceRegistry(String zkAddr) {
        zkClient = new ZkClient(zkAddr);
    }

    @Override
    public void register(ServiceBean serviceBean) throws RpcException {
//        if (null == serverAddr || null == appId) {
//            this.serverAddr = StringUtils.isNotEmpty(ServerCommonConfig.me().getElasticIp()) ?
//                    ServerCommonConfig.me().getElasticIp() : ServerCommonConfig.me().getAddress();
//            this.appId = ServerCommonConfig.me().getAppId();
//        }
        if (null == serviceBean) {
            throw new RpcException("Service bean not is null");
        }
        removeNode(serviceBean);
        createNode(serviceBean);
    }

    @Override
    public void unregister(ServiceBean serviceBean) throws RpcException {
        if (null == serviceBean) {
            throw new RpcException("Service bean not is null");
        }
        removeNode(serviceBean);
    }

    private void removeNode(ServiceBean serviceBean) {
        String appId = serviceBean.getAppId();
        String node = serviceBean.getServiceName();
        String serverAddr = serviceBean.getAddress();

        // node path = rootPath + appId + node + address
        String path = Constant.ZK_ROOT + "/" + appId + "/" + node + "/" + serverAddr;
        if (zkClient.exists(path)) {
            if (!zkClient.delete(path)) {
                log.warn("Delete node [{}] fail", path);
            }
        }
    }

    private void createNode(ServiceBean serviceBean) {
        String appId = serviceBean.getAppId();
        String node = serviceBean.getServiceName();
        String serverAddr = serviceBean.getAddress();

        // node path = rootPath + appId + node + address
        String path = Constant.ZK_ROOT + "/" + appId + "/" + node;
        if (!zkClient.exists(path)) {
            zkClient.createPersistent(path, true);
        }

        log.debug("Create node [{}]", path);
        zkClient.createEphemeral(path + "/" + serverAddr, "".getBytes());
    }

}