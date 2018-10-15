package com.redhat.dsevosty;

import org.infinispan.Cache;
import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.RemoteCacheManager;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.manager.DefaultCacheManager;
import org.infinispan.manager.EmbeddedCacheManager;
import org.infinispan.server.hotrod.HotRodServer;
import org.infinispan.server.hotrod.configuration.HotRodServerConfigurationBuilder;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClient;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.junit5.Checkpoint;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.TimeUnit;


@ExtendWith(VertxExtension.class)
public class DataGridVerticleTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(DataGridVerticle.class);

    private static final String HOTROD_SERVER_HOST = "127.0.0.1";
    private static final int HOTROD_SERVER_PORT = 11222;
    // private static final String HTTP_SERVER_PORT = "http.port";
    private static final String CACHE_NAME = "sdo";

    private static final String HTTP_HOST = "127.0.0.1";
    private static final int HTTP_PORT = 8080;

    private static EmbeddedCacheManager cacheManager = null;
    // private HotRodServer hotrodServer = null;

    private static final String SDO_ID = SimpleDataObject.defaultId();

    private static final SimpleDataObject SDO = new SimpleDataObject(null, "name 1", null);

    @BeforeAll
    public static void setUp(Vertx vertx, VertxTestContext context) throws InterruptedException {

        // Checkpoint cacheManagerCheckpoint = context.checkpoint();
        // vertx.<EmbeddedCacheManager>executeBlocking(future -> {
            EmbeddedCacheManager cm = new DefaultCacheManager();
            LOGGER.debug("CacheManager: " + cm);
            cm.defineConfiguration(CACHE_NAME, new ConfigurationBuilder().build());
            Cache<String, SimpleDataObject> cache = cm.<String, SimpleDataObject>getCache(CACHE_NAME);
            LOGGER.debug("CACHE: " + cache + " got");
            cacheManager = cm;
            cache.put(SDO_ID, SDO);
            // future.complete(cm);
        // }, result -> {
        //     if (result.succeeded()) {
        //         cacheManager = result.result();
        //         cacheManagerCheckpoint.flag();
        //     } else {
        //         LOGGER.error("Error during create EmbeddedCacheManager", result.cause());
        //     }
        // });
        // cacheManagerCheckpoint. .awaitSuccess(10000);

        // Checkpoint hostrodServerCheckpoint = context.checkpoint();
        // vertx.<HotRodServer>executeBlocking(future -> {
            HotRodServer srv = new HotRodServer();
            HotRodServerConfigurationBuilder config = new HotRodServerConfigurationBuilder().host(HOTROD_SERVER_HOST)
                    .defaultCacheName(CACHE_NAME).port(HOTROD_SERVER_PORT);
            srv.start(config.build(), cacheManager);
            LOGGER.info("HotRod Server " + srv + " started");
        //     future.complete(srv);
        // }, result -> {
        //     if (result.succeeded()) {
        //         // hotrodServer = result.result();
        //         hostrodServerCheckpoint.flag();
        //     } else {
        //         LOGGER.error("Error during create HotRodServer", result.cause());
        //     }
        // });
        // hostrodServerAsync.awaitSuccess(10000);
        
        DeploymentOptions options = new DeploymentOptions();
        options.setConfig(new JsonObject().put("cache-name", CACHE_NAME));
        vertx.deployVerticle(DataGridVerticle.class, options, context.succeeding(ar -> context.completeNow()));
        context.awaitCompletion(5, TimeUnit.SECONDS);
    }

    @AfterAll
    public static void teardDown(Vertx vertx, VertxTestContext context) throws InterruptedException {
        vertx.close(context.succeeding(ar -> {
            context.completeNow();
        }));
        context.awaitCompletion(5, TimeUnit.SECONDS);
        // if (hotrodServer != null) {
        // hotrodServer.getTransport().stop();
        // hotrodServer.stop();
        //
        // if (cacheManager != null) {
        // cacheManager.stop();
        //
    }

    @DisplayName("Datagrid Cache Direct Test")
    @Test
    public void directCacheTest(Vertx vertx, VertxTestContext context) throws InterruptedException {
        final SimpleDataObject sdo = new SimpleDataObject(null, "name 123", null);
        sdo.setOtherReference(sdo.getId());
        org.infinispan.client.hotrod.configuration.ConfigurationBuilder builder = new org.infinispan.client.hotrod.configuration.ConfigurationBuilder();
        builder.addServer().host(HOTROD_SERVER_HOST).port(HOTROD_SERVER_PORT);
        RemoteCache<String, SimpleDataObject> cache = new RemoteCacheManager(builder.build()).getCache(CACHE_NAME);
        LOGGER.info("REMOTE CACHE: " + cache + " got");
        LOGGER.info("PUT OBJECT: " + cache.put(SDO_ID, sdo));

        SimpleDataObject sdo2 = cache.get(SDO_ID);
        LOGGER.info("GET OBJECT: " + sdo2.toJson().encodePrettily());
        assertThat(sdo2).isNotNull();
        assertThat(sdo2.getName()).isEqualTo(sdo.getName());
        // cache.remove(SDO_ID);
        // assertThat(cache.get(SDO_ID)).isNull();
        context.completeNow();
    }

    @Test
    public void testROOT(Vertx vertx, VertxTestContext context) {
        // Checkpoint async = context.checkpoint();
        final HttpClient http = vertx.createHttpClient();
        http.getNow(HTTP_PORT, HTTP_HOST, "/", response -> response.handler(body -> {
            LOGGER.info(body.toString());
            context.completeNow();
        }));
    }

    @Test
    public void createSDO(Vertx vertx, VertxTestContext context) {
        Checkpoint post = context.checkpoint();
        final HttpClient http = vertx.createHttpClient();
        final SimpleDataObject sdo = new SimpleDataObject(null, "name 2", null);
        http.post(HTTP_PORT, HTTP_HOST, "/sdo").handler(response -> {
            final int statusCode = response.statusCode();
            LOGGER.info("StatusCode on post request for sdo=" + sdo + " is " + statusCode);
            assertThat(statusCode).isEqualByComparingTo(HttpResponseStatus.CREATED.code());
            response.bodyHandler(body -> {
                assertThat(sdo).isEqualTo(new SimpleDataObject(new JsonObject(body)));
                post.flag();
            });
        }).end(sdo.toJson().toString());
        // post.awaitSuccess(1000);
        context.completeNow();
    }
    
    @Test
    public void getSDO(Vertx vertx, VertxTestContext context) {
        Checkpoint get = context.checkpoint();
        final HttpClient http = vertx.createHttpClient();
        http.getNow(HTTP_PORT, HTTP_HOST, "/sdo/" + SDO_ID, response -> response.handler(body -> {
            LOGGER.info("StatusCode: " + response.statusCode() + "\nBody: " + body);
            assertThat(response.statusCode()).isEqualByComparingTo(HttpResponseStatus.OK.code());
            assertThat(SDO).isEqualTo(new SimpleDataObject(new JsonObject(body)));
            get.flag();
        }));
        // get.awaitSuccess(1000);
        context.completeNow();
    }

    public void updateSDO(Vertx vertx, VertxTestContext context) {
        Checkpoint update = context.checkpoint();
        SDO.setOtherReference(null);
        final HttpClient http = vertx.createHttpClient();
        http.put(HTTP_PORT, HTTP_HOST, "/sdo/" + SDO_ID).handler(response -> {
            final int statusCode = response.statusCode();
            LOGGER.info("StatusCode on update request for sdo=" + SDO + " is " + statusCode);
            assertThat(statusCode).isEqualTo(HttpResponseStatus.OK.code());
            response.bodyHandler(body -> {
                assertThat(SDO).isEqualTo(new SimpleDataObject(new JsonObject(body)));
                update.flag();
            });
        }).end(SDO.toJson().toString());
        // update.awaitSuccess(1000);

        Checkpoint delete = context.checkpoint();
        http.delete(HTTP_PORT, HTTP_HOST, "/sdo/" + SDO_ID).handler(response -> {
            final int statusCode = response.statusCode();
            LOGGER.info("StatusCode on delete request for sdo=" + SDO + " is " + statusCode);
            assertThat(statusCode).isEqualTo(HttpResponseStatus.NO_CONTENT.code());
            delete.flag();
        }).end();
        // delete.awaitSuccess(1000);

        Checkpoint get2 = context.checkpoint();
        http.getNow(HTTP_PORT, HTTP_HOST, "/sdo/" + SDO_ID, response -> {
            assertThat(response.statusCode()).isEqualTo(HttpResponseStatus.NOT_FOUND.code());
            get2.flag();
            context.completeNow();
        });
        // get2.awaitSuccess(1000);

/*        if (post.isCompleted() && get.isCompleted() && update.isCompleted() && delete.isCompleted()
                && get2.isCompleted()) {
            async.complete();
        }
        */
        // async.awaitSuccess(6000);
        // context.completeNow();
    }
}
