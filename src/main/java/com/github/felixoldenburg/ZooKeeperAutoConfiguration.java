package com.github.felixoldenburg;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("zookeeper")
public class ZooKeeperAutoConfiguration
{
    @Value("${spring.cloud.config.server.zookeeper.connectionString}")
    private String zkConnectionString;

    @Value("${spring.cloud.config.server.zookeeper.sessionTimeout:3600000}")
    private Integer sessionTimeout;

    @Value("${spring.cloud.config.server.zookeeper.connectTimeout:10000}")
    private Integer connectTimeout;

    final ExponentialBackoffRetry DEFAULT_RETRY_POLICY = new ExponentialBackoffRetry(1000, 3);


    @Bean
    @ConditionalOnMissingBean(ZooKeeperEnvironmentRepository.class)
    public ZooKeeperEnvironmentRepository jonesEnvironmentRepository(CuratorFramework curatorFramework)
    {
        return new ZooKeeperEnvironmentRepository(curatorFramework);
    }


    @Bean
    @ConditionalOnMissingBean(CuratorFramework.class)
    public CuratorFramework curatorFramework()
    {
        return CuratorFrameworkFactory.newClient(zkConnectionString, sessionTimeout, connectTimeout, DEFAULT_RETRY_POLICY);
    }
}
