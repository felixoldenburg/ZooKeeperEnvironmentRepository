package com.foldenburg.jones;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("jones")
public class JonesConfiguration
{
    @Value("${spring.cloud.config.server.jones.zookeeper.connectionString}")
    private String zkConnectionString;

    @Value("${spring.cloud.config.server.jones.zookeeper.sessionTimeout:3600000}")
    private Integer sessionTimeout;

    @Value("${spring.cloud.config.server.jones.zookeeper.connectTimeout:10000}")
    private Integer connectTimeout;

    final ExponentialBackoffRetry DEFAULT_RETRY_POLICY = new ExponentialBackoffRetry(1000, 3);


    @Bean
    public JonesEnvironmentRepository jonesEnvironmentRepository(CuratorFramework curatorFramework)
    {
        return new JonesEnvironmentRepository(curatorFramework);
    }


    @Bean
    public CuratorFramework curatorFramework()
    {
        return CuratorFrameworkFactory.newClient(zkConnectionString, sessionTimeout, connectTimeout, DEFAULT_RETRY_POLICY);
    }
}
