package com.redhat.dsevosty;

import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.RemoteCacheManager;
import org.infinispan.client.hotrod.configuration.Configuration;
import org.infinispan.client.hotrod.configuration.ConfigurationBuilder;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;

public class DataGridVerticle<K, V> extends AbstractVerticle {

    private static final Logger  LOGGER                        = LoggerFactory.getLogger(DataGridVerticle.class);

    private static String        INFINISPAN_HOTROD_SERVER      = "infinispan.hotrod.server";
    private static String        INFINISPAN_HOTROD_SERVER_HOST = INFINISPAN_HOTROD_SERVER + ".host";
    private static String        INFINISPAN_HOTROD_SERVER_PORT = INFINISPAN_HOTROD_SERVER + ".port";

    protected RemoteCacheManager manager;
    protected RemoteCache<K, V>  cache;

    @Override
    public void start(Future<Void> startFuture) throws Exception {
        super.start(startFuture);
        vertx.<RemoteCache<K, V>> executeBlocking(future -> {
            Configuration managerConfig = getCacheManagerConfiguration();
            manager = new RemoteCacheManager(managerConfig);
            LOGGER.debug("Created RemoteCacheManger=" + manager);
            RemoteCache<K, V> newCache = manager.getCache(config().getString("cache-name"));
            LOGGER.debug("Got reference for RemoteCahe=" + cache);
            registerEndpointREST();
            future.complete(newCache);
        }, result -> {
            if (result.succeeded()) {
                cache = result.result();
                LOGGER.info("RemoteCacheManager=" + manager + " initialized, and RemoteCache=" + cache + " connected");
            } else {
                LOGGER.error("Error Connecting cache", result.cause());
                startFuture.fail(result.cause());
            }
        });

    }

    @Override
    public void stop(Future<Void> stopFuture) throws Exception {
        super.stop(stopFuture);
        if (manager != null) {
            manager.stopAsync().whenCompleteAsync((e, ex) -> {
                stopFuture.complete();
            });
        } else {
            stopFuture.complete();
        }
    }

    protected void registerEndpointREST() {
        //        if (config().getString("IS"))
        Router router = Router.router(vertx);
/*        
        // CORS support
        Set<String> allowHeaders = new HashSet<>();
        allowHeaders.add("x-requested-with");
        allowHeaders.add("Access-Control-Allow-Origin");
        allowHeaders.add("origin");
        allowHeaders.add("Content-Type");
        allowHeaders.add("accept");
        Set<HttpMethod> allowMethods = new HashSet<>();
        allowMethods.add(HttpMethod.GET);
        allowMethods.add(HttpMethod.POST);
        allowMethods.add(HttpMethod.DELETE);
        allowMethods.add(HttpMethod.PATCH);

        router.route().handler(CorsHandler.create("*")allowedHeaders(allowHeaders).allowedMethods(allowMethods));
        */
        router.route().handler(BodyHandler.create());
    }

    protected Configuration getCacheManagerConfiguration() {
        JsonObject vertxConfig = config();
        final String host = vertxConfig.getString(INFINISPAN_HOTROD_SERVER_HOST, "127.0.0.1");
        final int port = vertxConfig.getInteger(INFINISPAN_HOTROD_SERVER_PORT, 11222);
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Creating remote cache configuration for host=" + host + ", port=" + port);
        }
        Configuration config = new ConfigurationBuilder().addServer().host(host).port(port).build();
        return config;
    }
}
