package com.github.felixoldenburg;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.util.Map;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.RetryNTimes;
import org.apache.curator.test.TestingServer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.config.environment.Environment;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class JonesEnvironmentRepositoryTest
{
    private static final Logger LOG = LoggerFactory.getLogger(JonesEnvironmentRepositoryTest.class);

    TestingServer server = null;
    CuratorFramework curator = null;
    final RetryNTimes retryPolicy = new RetryNTimes(0, 100);
    private JonesEnvironmentRepository dut;


    @Before
    public void before() throws Exception
    {
        server = new TestingServer(27015, true);
        server.start();

        curator = CuratorFrameworkFactory.newClient(server.getConnectString(), retryPolicy);
        curator.start();

        dut = new JonesEnvironmentRepository(curator);

        // Test env
        final JsonObject testJson = new JsonObject();
        testJson.addProperty("username", "test-anna");
        setupJonesConfig("myapplication", "development", testJson);

        // Prod env
        final JsonObject liveJson = new JsonObject();
        liveJson.addProperty("username", "production-bob");
        setupJonesConfig("myapplication", "production", liveJson);
    }


    private void setupJonesConfig(String application, String association, JsonObject config) throws Exception
    {
        final String applicationPath = "/services/" + application;
        final String confPath = applicationPath + "/" + association + "/view";

        curator.create().creatingParentsIfNeeded().forPath(confPath, config.toString().getBytes());

        final String nodemapPath = applicationPath + "/nodemaps";
        if (curator.checkExists().forPath(nodemapPath) == null)
        {
            final JsonObject confPathMapping = new JsonObject();
            confPathMapping.addProperty(association, confPath);

            curator.create().orSetData().creatingParentsIfNeeded().forPath(
                nodemapPath, confPathMapping.toString().getBytes());
        }
        else // Update exisiting nodemap config
        {
            final String json = new String(curator.getData().forPath(nodemapPath));
            final JsonObject nodemapConf = new JsonParser().parse(json).getAsJsonObject();
            nodemapConf.addProperty(association, confPath);
            curator.setData().forPath(nodemapPath, nodemapConf.toString().getBytes());
        }
    }


    @After
    public void teardown() throws IOException
    {
        server.close();
        curator.close();
    }

    @Test
    public void findEnvironment() throws Exception
    {
        final Environment env = dut.findOne("myapplication", "development", null);

        assertNotNull(env);
        assertEquals("myapplication", env.getName());
        assertArrayEquals(new String[]{"development"}, env.getProfiles());
        assertNotNull(env.getPropertySources());
        assertEquals(1, env.getPropertySources().size());
    }

    @Test
    public void getProperties() throws Exception
    {
        final Environment productionEnv = dut.findOne("myapplication", "production", null);
        final Map<String, String> prodProps = (Map<String, String>) productionEnv.getPropertySources().get(0).getSource();
        assertEquals("production-bob", prodProps.get("username"));

        final Environment developmentEnv = dut.findOne("myapplication", "development", null);
        final Map<String, String> testProps = (Map<String, String>) developmentEnv.getPropertySources().get(0).getSource();

        assertEquals("test-anna", testProps.get("username"));
    }
}
