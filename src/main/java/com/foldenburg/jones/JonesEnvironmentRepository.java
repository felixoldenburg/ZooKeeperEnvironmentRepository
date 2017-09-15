package com.foldenburg.jones;

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

//@ConfigurationProperties("spring.cloud.config.server.jones")
public class JonesEnvironmentRepository implements EnvironmentRepository, Ordered
{
    private static final Logger LOG = LoggerFactory.getLogger(JonesEnvironmentRepository.class);

    @Value("${spring.cloud.config.server.jones.order}")
    private int order = Ordered.LOWEST_PRECEDENCE;

    private final Gson gson;

    /**
     * The path structure is predefined by Jones
     */
    private final String ROOT_PATH = "/services/%s/";
    private final String NODE_MAPS = ROOT_PATH + "nodemaps";
    private final String CONF_DEFAULT = ROOT_PATH + "conf";

    // Mapping type for json deserialization
    Type MAP_TYPE = new TypeToken<Map<String, String>>()
    {
    }.getType();

    private CuratorFramework curator;


    public JonesEnvironmentRepository(CuratorFramework curator)
    {
        this.curator = curator;
        this.gson = new Gson();
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
            final String nodeMapPath = String.format(NODE_MAPS, service);
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
            environment.add(new PropertySource("JonesPropertySource", properties));
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
