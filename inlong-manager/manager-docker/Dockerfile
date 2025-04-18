#
# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements. See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership. The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License. You may obtain a copy of the License at
#
#   http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing,
# software distributed under the License is distributed on an
# "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
# KIND, either express or implied.  See the License for the
# specific language governing permissions and limitations
# under the License.
#
FROM openjdk:8-jdk
RUN apt-get update \
    && apt-get install -y net-tools vim default-mysql-client \
    && rm -rf /var/lib/apt/lists/*
EXPOSE 8083
# profile and env virables
ENV ACTIVE_PROFILE=prod
ENV JDBC_URL=127.0.0.1:3306
ENV USERNAME=root
ENV PASSWORD=inlong
ENV ZK_URL=127.0.0.1:2181
ENV CLEANSING_ENABLE=false
ENV MANAGER_DBNAME=apache_inlong_manager
# support download plugins from remote address.
ENV PLUGINS_URL=default
# for flink-sort-plugin.properties
ENV FLINK_HOST=127.0.0.1
ENV FLINK_PORT=8081
ENV FLINK_PARALLELISM=1
# JVM env
ENV MANAGER_JVM_HEAP_OPTS="-XX:+UseContainerSupport -XX:InitialRAMPercentage=40.0 -XX:MaxRAMPercentage=80.0 -XX:-UseAdaptiveSizePolicy"
WORKDIR /opt/inlong-manager
# add manager tarball
ARG VERSION
ARG default_jdbc_connector_url=https://repo1.maven.org/maven2/mysql/mysql-connector-java/8.0.28/mysql-connector-java-8.0.28.jar
ADD target/manager-web-${VERSION}-bin.tar.gz /opt/inlong-manager
# add mysql connector
RUN wget -P lib/ ${default_jdbc_connector_url}
# add sort resource
ADD target/sort-dist-${VERSION}.jar /opt/inlong-sort/
RUN wget -P /opt/inlong-sort/connectors/ ${default_jdbc_connector_url}
ADD target/sort-connector-pulsar-v1.15-${VERSION}.jar /opt/inlong-sort/connectors/
ADD target/sort-connector-kafka-v1.15-${VERSION}.jar /opt/inlong-sort/connectors/
ADD target/sort-connector-mysql-cdc-v1.15-${VERSION}.jar /opt/inlong-sort/connectors/
# audit
ENV AUDIT_QUERY_URL=127.0.0.1:10080
ADD manager-docker.sh bin/
RUN chmod +x bin/manager-docker.sh
CMD ["bin/manager-docker.sh"]
