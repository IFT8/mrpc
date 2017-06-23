#! /bin/bash

mvn clean install -P snapshots -N

cd mrpc-common && mvn clean install -P snapshots -Dmaven.test.skip=true -U
cd ..

cd mrpc-core && mvn clean install -P snapshots -Dmaven.test.skip=true -U
cd ..

cd mrpc-registry-zk && mvn clean install -P snapshots -Dmaven.test.skip=true -U
cd ..

cd mrpc-metric-influxdb && mvn clean install -P snapshots -Dmaven.test.skip=true -U
cd ..

cd mrpc-serialize && mvn clean install -P snapshots -N

cd mrpc-serialize-kryo && mvn clean install -P snapshots -Dmaven.test.skip=true -U
cd ../../

cd mrpc-spring-boot-starter && mvn clean install -P snapshots -Dmaven.test.skip=true -U
cd ..