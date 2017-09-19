/*
 * Copyright 2013-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.github.felixoldenburg;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.Map;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.imps.CuratorFrameworkState;
import org.apache.curator.utils.CloseableUtils;
import org.apache.zookeeper.KeeperException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.config.environment.Environment;
import org.springframework.cloud.config.environment.PropertySource;
import org.springframework.cloud.config.server.environment.EnvironmentRepository;
import org.springframework.core.Ordered;

/**
 * A custom {@link EnvironmentRepository} which reads its configuration from ZooKeeper.
 * Configuration is stored hierarchically which allows for complex configuration setups via inheritance.
 * <p>
 * /services/<name>/conf        The actual configuration
 * /services/<name>/nodemaps    A json map, mapping from a keyword(association) to config view
 * /services/<name>/view        The effective config for name which includes all configuration from its ancestors
 * <p>
 * Nodes can inherit their configuration from ancestors.
 * Following a complete example for a service myservice with configuration for the environments prod, dev and qa.
 * Their are three arbitrary associations mapping to each of the environments
 * <p>
 * /services/myservice/nodemaps         -> {"live": "/services/myservice/conf/prod", "developer": "/services/myservice/conf/test/dev", "bob": "/services/myservice/conf/test/qa"}
 * /services/myservice/conf             -> JSON
 * /services/myservice/conf/prod        -> JSON
 * /services/myservice/conf/test        -> JSON
 * /services/myservice/conf/test/dev    -> JSON
 * /services/myservice/conf/test/qa     -> JSON
 * <p>
 * @author Felix Oldenburg
 */
public class ZooKeeperEnvironmentRepository implements EnvironmentRepository, Ordered
{
    private static final Logger LOG = LoggerFactory.getLogger(ZooKeeperEnvironmentRepository.class);

    @Value("${spring.cloud.config.server.jones.order}")
    private int order = Ordered.LOWEST_PRECEDENCE;

    private final Gson gson;

    private final static String DEFAULT_PREFIX = "/configuration";
    private final String rootPath;
    private final String nodeMapPath;

    // Mapping type for json deserialization of a String -> String map
    Type MAP_TYPE = new TypeToken<Map<String, String>>()
    {
    }.getType();

    private CuratorFramework curator;


    public ZooKeeperEnvironmentRepository(CuratorFramework curator)
    {
        this(curator, DEFAULT_PREFIX);
    }


    public ZooKeeperEnvironmentRepository(CuratorFramework curator, String prefix)
    {
        this.curator = curator;
        this.gson = new Gson();

        rootPath = prefix + "/%s/";
        nodeMapPath = rootPath + "nodemaps";
    }


    @PostConstruct
    private void setupZKConnection()
    {
        if (this.curator.getState() == CuratorFrameworkState.STARTED)
        {
            return;
        }

        try
        {
            this.curator.start();

            // Wait and check for connection
            if (!this.curator.getZookeeperClient().blockUntilConnectedOrTimedOut())
            {
                throw new ZooKeeperUnavailableException("No ZooKeeper available");
            }

        }
        catch (InterruptedException e) // NodeCache only throws "Exception"...
        {
            closeZKConnection();

            throw new RuntimeException("Error starting Jones", e);
        }
    }


    @PreDestroy
    private void closeZKConnection()
    {
        CloseableUtils.closeQuietly(curator);
    }


    private Map<String, String> getConfiguration(String service, String association)
    {
        try
        {
            // Get the config path for the service and association
            final String nodeMapPath = String.format(this.nodeMapPath, service);
            final byte[] nodeMapData = this.curator.getData().forPath(nodeMapPath);
            final Map<String, String> nodeMap = this.gson.fromJson(new String(nodeMapData), MAP_TYPE);
            final String configPath = nodeMap.get(association);

            if (configPath == null)
            {
                return null;
            }

            // Get actual configuration
            final byte[] configurationRaw = this.curator.getData().forPath(configPath);
            JsonElement configJson = this.gson.fromJson(new String(configurationRaw), JsonElement.class);

            return JsonHelper.flattenJson(configJson);
        }
        catch (KeeperException.NoNodeException e)
        {
            return null;
        }
        catch (Exception e)
        {
            LOG.warn(String.format("Couldn't get configuration for service %s with association %s", service, association), e);
            return null;
        }
    }


    @Override
    public Environment findOne(String application, String association, String label)
    {
        final Environment environment = new Environment(application, association);

        final Map<String, String> properties = getConfiguration(application, association);

        if (properties != null)
        {
            environment.add(new PropertySource("ZookeeperPropertySource", properties));
        }

        return environment;
    }


    public void setOrder(int order)
    {
        this.order = order;
    }


    @Override
    public int getOrder()
    {
        return order;
    }
}
