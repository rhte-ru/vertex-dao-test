package com.redhat.dsevosty;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.ServerSocket;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.server.hotrod.configuration.HotRodServerConfigurationBuilder;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
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

@ExtendWith(VertxExtension.class)
public class DataGridVerticleTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(DataGridVerticle.class);

    private static final String HOTROD_SERVER_HOST = "127.0.0.1";
    private static final int HOTROD_SERVER_PORT = 11222;
    private static final String PUBLIC_CONTEXT_NAME = "sdo";

    private static final String HTTP_HOST = "127.0.0.1";
    private static final int HTTP_PORT = 8080;

    private static final UUID SDO_ID = SimpleDataObject.defaultId();
    private static final SimpleDataObject SDO = new SimpleDataObject(SDO_ID, "name 1", SDO_ID);

    private static int httpPort = 0;
    private static InfinispanLocalHotrodServer<UUID, AbstractDataObject> server;
    private HttpClient http;

    @BeforeAll
    public static void setUp(Vertx vertx, VertxTestContext context) throws InterruptedException {
        HotRodServerConfigurationBuilder config = new HotRodServerConfigurationBuilder().host(HOTROD_SERVER_HOST)
                .defaultCacheName(PUBLIC_CONTEXT_NAME).port(HOTROD_SERVER_PORT);
        server = new InfinispanLocalHotrodServer<UUID, AbstractDataObject>(new ConfigurationBuilder().build(),
                config.build());
        server.getCache().put(SDO_ID, SDO);
        DeploymentOptions options = new DeploymentOptions();
        JsonObject vertxConfig = new JsonObject();
        vertxConfig.put(DataGridVerticle.INFINISPAN_HOTROD_SERVER_HOST, HOTROD_SERVER_HOST);
        vertxConfig.put(DataGridVerticle.INFINISPAN_HOTROD_SERVER_PORT, HOTROD_SERVER_PORT);
        vertxConfig.put(DataGridVerticle.PUBLIC_CONTEXT_NAME, PUBLIC_CONTEXT_NAME);
        try {
            ServerSocket socket = new ServerSocket(httpPort);
            httpPort = socket.getLocalPort();
            socket.close();
        } catch (Exception e) {
            httpPort = HTTP_PORT;
        }
        vertxConfig.put(DataGridVerticle.INFINISPAN_HOTROD_SERVER_HOST, HOTROD_SERVER_HOST);
        vertxConfig.put(DataGridVerticle.VERTX_HTTP_SERVER_ENABLED, true);
        vertxConfig.put(DataGridVerticle.VERTX_HTTP_SERVER_PORT, httpPort);
        LOGGER.info("Configuring to run Vert.x HTTP Server on port: " + httpPort);
        options.setConfig(vertxConfig);
        vertx.deployVerticle(SimpleDataObjectDataGridVerticle.class, options,
                context.succeeding(ar -> context.completeNow()));
        // vertx.deployVerticle(SimpleDataObjectDataGridVerticle.class, options, ar -> {
        // if (ar.failed()) {
        // LOGGER.error("Error while deploying Verticle", ar.cause());
        // context.failNow(ar.cause());
        // }
        // if (ar.succeeded()) {
        // LOGGER.info("HotRod Server={} initialized", server);
        // context.completeNow();
        // }
        // });
        // context.awaitCompletion(5, TimeUnit.MINUTES);
    }

    @AfterAll
    public static void teardDown(Vertx vertx, VertxTestContext context) throws InterruptedException {
        vertx.close(context.succeeding(ar -> {
            if (server != null) {
                server.stop();
            }
            context.completeNow();
        }));
        context.awaitCompletion(15, TimeUnit.SECONDS);
    }

    // @Test
    // public void directCacheTest(Vertx vertx, VertxTestContext context) throws InterruptedException {
    //     SimpleDataObject sdo = new SimpleDataObject();
    //     sdo.setOtherReference(sdo.getId());
    //     org.infinispan.client.hotrod.configuration.ConfigurationBuilder builder = new org.infinispan.client.hotrod.configuration.ConfigurationBuilder();
    //     builder.addServer().host(HOTROD_SERVER_HOST).port(HOTROD_SERVER_PORT);
    //     RemoteCacheManager rmc = new RemoteCacheManager(builder.build());
    //     rmc.start();
    //     RemoteCache<Object, SimpleDataObject> cache = rmc.getCache(PUBLIC_CONTEXT_NAME);
    //     LOGGER.info("GOT Remote Cache: " + cache);
    //     cache.put(SDO_ID, sdo);
    //     SimpleDataObject sdo2 = cache.get(SDO_ID);
    //     LOGGER.info("GET OBJECT: " + sdo2.toJson().toString());
    //     assertThat(sdo2).isNotNull();
    //     assertThat(sdo2.getName()).isEqualTo(sdo.getName());
    //     context.completeNow();
    // }

    @BeforeEach
    public void before(Vertx vertx, VertxTestContext context) {
        http = vertx.createHttpClient();
        LOGGER.trace("BeforeEach created http client {}", http);
        context.completeNow();
    }

    @AfterEach
    public void after(Vertx vertx, VertxTestContext context) {
        LOGGER.trace("AfterEach closing http client {}", http);
        http.close();
        context.completeNow();
    }

    @Test
    public void testROOT(Vertx vertx, VertxTestContext context) throws InterruptedException {
        final String path = "/";
        LOGGER.trace("tetsROOT: HTTP CALL: {}", path);
        http.getNow(httpPort, HTTP_HOST, path, response -> response.handler(body -> {
            LOGGER.info(body.toString());
            context.completeNow();
        }));
        context.awaitCompletion(2, TimeUnit.SECONDS);
    }

    @Test
    public void createSDO(Vertx vertx, VertxTestContext context) throws InterruptedException {
        final String path = "/sdo";
        LOGGER.trace("createSDO: HTTP CALL: {}", path);
        Checkpoint post = context.checkpoint();
        final SimpleDataObject sdo = new SimpleDataObject(SimpleDataObject.defaultId(), "name 2", null);
        http.post(httpPort, HTTP_HOST, path).handler(response -> {
            final int statusCode = response.statusCode();
            LOGGER.info("StatusCode on post request for sdo=" + sdo + " is " + statusCode);
            assertThat(statusCode).isEqualByComparingTo(HttpResponseStatus.CREATED.code());
            response.bodyHandler(body -> {
                LOGGER.info(body);
                assertThat(sdo).isEqualTo(new SimpleDataObject(new JsonObject(body)));
                post.flag();
            });
        }).end(sdo.toJson().toString());
        context.awaitCompletion(2, TimeUnit.SECONDS);
    }

    @Test
    public void getSDO(Vertx vertx, VertxTestContext context) throws InterruptedException {
        final String path = "/sdo/" + SDO_ID;
        LOGGER.trace("getSDO: HTTP CALL: {}", path);
        Checkpoint get = context.checkpoint();
        http.getNow(httpPort, HTTP_HOST, path, response -> response.handler(body -> {
            LOGGER.info("StatusCode: " + response.statusCode() + "\nBody: " + body);
            assertThat(response.statusCode()).isEqualByComparingTo(HttpResponseStatus.OK.code());
            assertThat(SDO).isEqualTo(new SimpleDataObject(new JsonObject(body)));
            get.flag();
        }));
        context.awaitCompletion(2, TimeUnit.SECONDS);
    }

    @Test
    public void updateSDO(Vertx vertx, VertxTestContext context) throws InterruptedException {
        final String path = "/sdo/" + SDO_ID;
        LOGGER.trace("updateSDO: HTTP CALL: {}", path);
        Checkpoint update = context.checkpoint();
        SDO.setOtherReference(null);
        SDO.setName("Updated Name");
        http.put(httpPort, HTTP_HOST, path).handler(response -> {
            final int statusCode = response.statusCode();
            LOGGER.info("StatusCode on update request for sdo=" + SDO + " is " + statusCode);
            assertThat(statusCode).isEqualTo(HttpResponseStatus.OK.code());
            response.bodyHandler(body -> {
                assertThat(SDO).isEqualTo(new SimpleDataObject(new JsonObject(body)));
                update.flag();
            });
        }).end(SDO.toJson().toString());
        context.awaitCompletion(2, TimeUnit.SECONDS);
    }

    @Test
    public void removeSDO(Vertx vertx, VertxTestContext context) throws InterruptedException {
        final String path = "/sdo/" + SDO_ID;
        LOGGER.trace("removeSDO: HTTP CALL: {}", path);
        Checkpoint delete = context.checkpoint();
        http.delete(httpPort, HTTP_HOST, path).handler(response -> {
            final int statusCode = response.statusCode();
            LOGGER.info("StatusCode on delete request for sdo=" + SDO + " is " + statusCode);
            assertThat(statusCode).isEqualTo(HttpResponseStatus.NO_CONTENT.code());
            delete.flag();
        }).end();
        // delete.awaitSuccess(1000);

        Checkpoint get2 = context.checkpoint();
        http.getNow(httpPort, HTTP_HOST, path, response -> {
            assertThat(response.statusCode()).isEqualTo(HttpResponseStatus.NOT_FOUND.code());
            get2.flag();
        });
        context.awaitCompletion(2, TimeUnit.SECONDS);

    }
}
