package com.redhat.dsevosty;

import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.RemoteCacheManager;
import org.infinispan.client.hotrod.configuration.Configuration;
import org.infinispan.client.hotrod.configuration.ConfigurationBuilder;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public class DataGridVerticle<K, V> extends AbstractVerticle {

    private static final Logger  LOGGER = LoggerFactory.getLogger(DataGridVerticle.class);

    protected RemoteCacheManager manager;
    protected RemoteCache<K, V>  cache;

    @Override
    public void start(Future<Void> startFuture) throws Exception {
        super.start(startFuture);
        vertx.<RemoteCache<K, V>> executeBlocking(future -> {
            Configuration managerConfig = getConfiguration();
            manager = new RemoteCacheManager(managerConfig);
            LOGGER.debug("Created RemoteCacheManger=" + manager);
            RemoteCache<K, V> newCache = manager.getCache(config().getString("cache-name"));
            LOGGER.debug("Got reference for RemoteCahe=" + cache);
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
        if (config().getString("IS"))
    }

    protected Configuration getConfiguration() {
        final String host = "127.0.0.1";
        final int port = 11222;
        LOGGER.debug("Creating remote cache configuration for host=" + host + ", port=" + port);
        Configuration config = new ConfigurationBuilder().addServer().host(host).port(port).build();
        return config;
    }
}
