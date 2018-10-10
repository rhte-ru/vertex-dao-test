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

import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClient;
import io.vertx.core.json.JsonObject;
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
        DeploymentOptions options = new DeploymentOptions();
        options.setConfig(new JsonObject().put("cache-name", CACHE_NAME));
        vertx.deployVerticle(DataGridVerticle.class, options, context.asyncAssertSuccess());
        // context.async().awaitSuccess(20000);
    }

    @After
    public void teardDown(TestContext context) {
        vertx.close(context.asyncAssertSuccess());
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
        Async async = context.async();
        final String HTTP_HOST = "127.0.0.1";
        final int HTTP_PORT = 8080;
        final HttpClient http = vertx.createHttpClient();
        http.getNow(HTTP_PORT, HTTP_HOST, "/", response -> response.handler(body -> {
            LOGGER.info(body.toString());
        }));
        Async post = context.async();
        http.post(HTTP_PORT, HTTP_HOST, "/sdo").handler(response -> {
            final int statusCode = response.statusCode();
            LOGGER.info("StatusCode on post request for sdo=" + sdo + " is " + statusCode);
            context.assertEquals(statusCode, HttpResponseStatus.CREATED.code());
            response.bodyHandler(body -> {
                context.assertEquals(sdo, new SimpleDataObject(new JsonObject(body)));
                post.complete();
            });
        }).end(sdo.toJson().toString());
        post.awaitSuccess(1000);
        Async get = context.async();
        http.getNow(HTTP_PORT, HTTP_HOST, "/sdo/" + myId, response -> response.handler(body -> {
            LOGGER.info("StatusCode: " + response.statusCode() + "\nBody: " + body);
            context.assertEquals(response.statusCode(), HttpResponseStatus.OK.code());
            context.assertEquals(sdo, new SimpleDataObject(new JsonObject(body)));
            get.complete();
        }));
        get.awaitSuccess(1000);
        sdo.setOtherReference(null);
        Async update = context.async();
        http.put(HTTP_PORT, HTTP_HOST, "/sdo/" + myId).handler(response -> {
            final int statusCode = response.statusCode();
            LOGGER.info("StatusCode on update request for sdo=" + sdo + " is " + statusCode);
            context.assertEquals(statusCode, HttpResponseStatus.OK.code());
            response.bodyHandler(body -> {
                context.assertEquals(sdo, new SimpleDataObject(new JsonObject(body)));
                update.complete();
            });
        }).end(sdo.toJson().toString());
        update.awaitSuccess(1000);
        Async delete = context.async();
        http.delete(HTTP_PORT, HTTP_HOST, "/sdo/" + myId).handler(response -> {
            final int statusCode = response.statusCode();
            LOGGER.info("StatusCode on delete request for sdo=" + sdo + " is " + statusCode);
            context.assertEquals(statusCode, HttpResponseStatus.NO_CONTENT.code());
            delete.complete();
        }).end();
        delete.awaitSuccess(1000);
        Async get2 = context.async();
        http.getNow(HTTP_PORT, HTTP_HOST, "/sdo/" + myId, response -> {
            context.assertEquals(response.statusCode(), HttpResponseStatus.NOT_FOUND.code());
            get2.complete();
        });
        get2.awaitSuccess(1000);

        if (post.isCompleted() && get.isCompleted() && update.isCompleted() && delete.isCompleted()
                && get2.isCompleted()) {
            async.complete();
        }
        async.awaitSuccess(6000);
    }
}
