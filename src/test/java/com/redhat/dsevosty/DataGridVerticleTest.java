package com.redhat.dsevosty;

import org.infinispan.Cache;
import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.RemoteCacheManager;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.manager.DefaultCacheManager;
import org.infinispan.manager.EmbeddedCacheManager;
import org.infinispan.server.hotrod.HotRodServer;
import org.infinispan.server.hotrod.configuration.HotRodServerConfigurationBuilder;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import io.vertx.core.Vertx;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;

@RunWith(VertxUnitRunner.class)
public class DataGridVerticleTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(DataGridVerticle.class);

    private static final String HOTROD_SERVER_HOST = "127.0.0.1";
    private static final int HOTROD_SERVER_PORT = 11222;
    // private static final String HTTP_SERVER_PORT = "http.port";
    private static final String CACHE_NAME = "sdo";

    private Vertx vertx;

    @Before
    public void setUp(TestContext context) {
        vertx = Vertx.vertx();
        Async cacheManagerAsync = context.async();
        Async hostrodServerAsync = context.async();
        vertx.<EmbeddedCacheManager>executeBlocking(future -> {
            EmbeddedCacheManager cm = new DefaultCacheManager();
            LOGGER.debug("CacheManager: " + cm);
            cm.defineConfiguration(CACHE_NAME, new ConfigurationBuilder().build());
            Cache<String, SimpleDataObject> cache = cm.<String, SimpleDataObject>getCache(CACHE_NAME);
            LOGGER.debug("CACHE: " + cache + " got");
            future.complete(cm);
        }, result -> {
            if (result.succeeded()) {
                context.put("CACHE_MANAGER", result.result());
                cacheManagerAsync.complete();
            } else {
                LOGGER.error("Error during create EmbeddedCacheManager", result.cause());
            }
        });

        cacheManagerAsync.awaitSuccess(10000);

        vertx.<HotRodServer>executeBlocking(future -> {
            HotRodServer server = new HotRodServer();
            HotRodServerConfigurationBuilder config = new HotRodServerConfigurationBuilder().host(HOTROD_SERVER_HOST)
                    .defaultCacheName(CACHE_NAME).port(HOTROD_SERVER_PORT);
            server.start(config.build(), context.get("CACHE_MANAGER"));
            LOGGER.info("HotRod Server " + server + " started");
            future.complete(server);
        }, result -> {
            if (result.succeeded()) {
                context.put("HOTROD", result.result());
                hostrodServerAsync.complete();
            } else {
                LOGGER.error("Error during create HotRodServer", result.cause());
            }
        });
        hostrodServerAsync.awaitSuccess(10000);
    }

    @After
    public void teardDown(TestContext context) {
        // vertx.close(context.asyncAssertSuccess());
        // if (hotrodServer != null) {
        // hotrodServer.getTransport().stop();
        // hotrodServer.stop();
        //
        // if (cacheManager != null) {
        // cacheManager.stop();
        //
    }

    @Test
    public void testSDO(TestContext context) {
        String myId;
        SimpleDataObject sdo = new SimpleDataObject(null, "name 1", null);
        sdo.setOtherReference(sdo.getId());
        myId = sdo.getId();
        LOGGER.debug("MY SDO ID: " + myId);
        org.infinispan.client.hotrod.configuration.ConfigurationBuilder builder = new org.infinispan.client.hotrod.configuration.ConfigurationBuilder();
        builder.addServer().host(HOTROD_SERVER_HOST).port(HOTROD_SERVER_PORT);
        RemoteCache<String, SimpleDataObject> cache = new RemoteCacheManager(builder.build()).getCache(CACHE_NAME);
        LOGGER.info("REMOTE CACHE: " + cache + " got");
        LOGGER.info("PUT OBJECT: " + cache.put(myId, sdo));
        SimpleDataObject sdo2 = cache.get(myId);
        LOGGER.info("GET OBJECT: " + sdo2.toJson().encodePrettily());
        context.assertNotNull(sdo2);
        context.assertEquals(sdo2.getName(), sdo.getName());
        cache.remove(myId);
        context.assertNull(cache.get(myId));
    }

}
