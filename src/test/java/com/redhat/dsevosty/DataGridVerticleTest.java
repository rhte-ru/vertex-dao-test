package com.redhat.dsevosty;

import io.vertx.core.Vertx;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;

import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.manager.DefaultCacheManager;
import org.infinispan.manager.EmbeddedCacheManager;
import org.infinispan.server.hotrod.HotRodServer;
import org.infinispan.server.hotrod.configuration.HotRodServerConfigurationBuilder;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(VertxUnitRunner.class)
public class DataGridVerticleTest {

    private static final String  HOTROD_SERVER_HOST = "127.0.0.1";
    private static final int     HOTROD_SERVER_PORT = 11222;

    private EmbeddedCacheManager cacheManager;
    private HotRodServer         hotrodServer;
    private Vertx                vertx;

    private static EmbeddedCacheManager createcacheManager() {
        ConfigurationBuilder config = new ConfigurationBuilder();
        //    config.expiration().lifespan(5, TimeUnit.SECONDS);
        EmbeddedCacheManager cacheManager = new DefaultCacheManager(config.build());
        return cacheManager;
    }

    private static HotRodServer createServer(EmbeddedCacheManager cacheManager) {
        HotRodServer server = new HotRodServer();
        HotRodServerConfigurationBuilder config = new HotRodServerConfigurationBuilder().host(HOTROD_SERVER_HOST).port(HOTROD_SERVER_PORT);
        server.start(config.build(), cacheManager);
        return server;
    }

    @Before
    protected void setUp(TestContext context) {
        if (cacheManager == null) {
            cacheManager = createcacheManager();
        }
        if (hotrodServer == null) {
            hotrodServer = createServer(cacheManager);
        }
        vertx = Vertx.vertx();
        vertx.deployVerticle(DataGridVerticle.class.getName(), context.asyncAssertSuccess());
    }

    @After
    protected void teardDown(TestContext context) {
        vertx.close(context.asyncAssertSuccess());
        hotrodServer.stop();
        hotrodServer = null;
    }

    @Test
    public void testdataGridVerticle(TestContext context) {
    }
}
