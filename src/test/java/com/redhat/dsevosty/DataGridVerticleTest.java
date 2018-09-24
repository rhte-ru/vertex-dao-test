package com.redhat.dsevosty;

import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import io.vertx.ext.web.client.WebClient;


import java.io.IOException;
import java.net.ServerSocket;


import org.infinispan.Cache;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.configuration.cache.Configuration;
import org.infinispan.configuration.global.GlobalConfiguration;
import org.infinispan.configuration.global.GlobalConfigurationBuilder;
import org.infinispan.manager.DefaultCacheManager;
import org.infinispan.manager.EmbeddedCacheManager;
import org.infinispan.server.hotrod.HotRodServer;
import org.infinispan.server.hotrod.configuration.HotRodServerConfigurationBuilder;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

//@RunWith(VertxUnitRunner.class)
public class DataGridVerticleTest {

    private static final Logger  LOGGER             = LoggerFactory.getLogger(DataGridVerticle.class);

    private static final String  HOTROD_SERVER_HOST = "127.0.0.1";
    private static final int     HOTROD_SERVER_PORT = 11222;
    private static final String  HTTP_SERVER_PORT   = "http.port";
    private static final String  CACHE_NAME         = "sdo";

    private EmbeddedCacheManager cacheManager;
    private HotRodServer         hotrodServer;
    private Vertx                vertx;

    private String               myId;


    @Before
    public void setUp(TestContext context) {
        vertx = Vertx.vertx();
//        if (cacheManager == null) {
            vertx.<EmbeddedCacheManager>executeBlocking(future -> {
                Configuration config = new ConfigurationBuilder().build();
                EmbeddedCacheManager cm = new DefaultCacheManager(config);
                cacheManager.defineConfiguration(CACHE_NAME, new ConfigurationBuilder().build());
                Cache<String, SimpleDataObject> c = cacheManager.getCache(CACHE_NAME);
                LOGGER.info("Local cache " + c + " created");
                future.complete(cm);
            }, result -> {
               if (result.succeeded()) {
                 cacheManager = result.result();
                 vertx.<HotRodServer>executeBlocking(future2 -> {
                   HotRodServer server = new HotRodServer();
                   HotRodServerConfigurationBuilder config = new HotRodServerConfigurationBuilder().host(HOTROD_SERVER_HOST).port(HOTROD_SERVER_PORT);
                   server.start(config.build(), cacheManager);
                   LOGGER.info("HotRod Server " + server + " started");
                   future2.complete(server);
                 }, result2 -> {
                   if(result2.succeeded()) {
                     hotrodServer = result2.result();
//                      ServerSocket socket = new ServerSocket(0);
//                      final int httpServerPort = socket.getLocalPort();
//                      socket.close();
                      JsonObject json = new JsonObject().put(HTTP_SERVER_PORT, 8080).put("cache-name", CACHE_NAME);
                      DeploymentOptions options = new DeploymentOptions().setConfig(json);
                      vertx.deployVerticle(DataGridVerticle.class.getName(), options); 
                      context.asyncAssertSuccess();
                   } else {
                      LOGGER.error("Error during create HotRodServer", result2.cause());
                      context.asyncAssertFailure();
                   }
                 });
               } else {
                  LOGGER.error("Error during create EmbeddedCacheManager", result.cause());
                  context.asyncAssertFailure();
               }
            });
//        }

//        ServerSocket socket = new ServerSocket(0);
//        final int httpServerPort = socket.getLocalPort();
//        socket.close();
//        JsonObject json = new JsonObject().put(HTTP_SERVER_PORT, httpServerPort).put("cache-name", CACHE_NAME);
//        DeploymentOptions options = new DeploymentOptions().setConfig(json);
//        vertx.deployVerticle(DataGridVerticle.class.getName(), options);
        //        vertx.deployVerticle(DataGridVerticle.class.getName(), options, context.asyncAssertSuccess(did -> {
        //            vertxDeploymentId = did;
        //        }));
    }

    @After
    public void teardDown(TestContext context) {
        vertx.close(context.asyncAssertSuccess());
        if (hotrodServer != null) {
            hotrodServer.getTransport().stop();
            hotrodServer.stop();
        }
        if (cacheManager != null) {
            cacheManager.stop();
        }
        hotrodServer = null;
    }

//        @Test
//        public void dummy(TestContext context) {
//            final Async async = context.async();
//            async.complete();
//        }

//    @Test
    public void testDataGridVerticleAddSDO(TestContext context) {
        //        final Async async = context.async();
        final WebClient webClient = WebClient.create(vertx);
        final String name_1 = "name 1";
        final String ref_1 = "referrence 1";
        final SimpleDataObject sdo = new SimpleDataObject(null, name_1, ref_1);
        myId = sdo.getId();
//        final int httpPort = vertx.getOrCreateContext().config().getInteger(HTTP_SERVER_PORT);
        webClient.get(8080, HOTROD_SERVER_HOST, "/sdo")
                 .sendJsonObject(sdo.toJson(), context.asyncAssertSuccess(response -> {
                     context.assertEquals(HttpResponseStatus.CREATED.code(), response.statusCode());
                     final SimpleDataObject responseSDO = new SimpleDataObject(new JsonObject(response.body().toString()));
                     context.assertEquals(responseSDO.getName(), name_1);
                     context.assertEquals(responseSDO.getOtherReference(), ref_1);
                     context.assertEquals(responseSDO.getId(), sdo.getId());
                     context.async().complete();
                 }));
    }

//    @Test
    public void testDataGridVerticleGetSDO(TestContext context) {
        //        final Async async = context.async();
        final WebClient webClient = WebClient.create(vertx);
        webClient.get(vertx.getOrCreateContext().config().getInteger(HTTP_SERVER_PORT), HOTROD_SERVER_HOST, "/sdo" + myId)
                 .send(context.asyncAssertSuccess(response -> {
                     context.assertEquals(HttpResponseStatus.OK, response.statusCode());
                     final SimpleDataObject responseSDO = new SimpleDataObject(new JsonObject(response.body().toString()));
                     //                     context.assertEquals(responseSDO.getName(), name_1);
                     //                     context.assertEquals(responseSDO.getOtherReference(), ref_1);
                     context.assertEquals(responseSDO.getId(), myId);
                     context.async().complete();
                 }));
    }

}
