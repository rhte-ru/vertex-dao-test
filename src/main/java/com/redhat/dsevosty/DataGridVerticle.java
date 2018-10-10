package com.redhat.dsevosty;

import java.util.Set;
import java.util.HashSet;

import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.RemoteCacheManager;
import org.infinispan.client.hotrod.configuration.Configuration;
import org.infinispan.client.hotrod.configuration.ConfigurationBuilder;

import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.CorsHandler;

public class DataGridVerticle<K, V> extends AbstractVerticle {

    private static final Logger LOGGER = LoggerFactory.getLogger(DataGridVerticle.class);

    public static String INFINISPAN_HOTROD_SERVER = "infinispan.hotrod.server";
    public static String INFINISPAN_HOTROD_SERVER_HOST = INFINISPAN_HOTROD_SERVER + ".host";
    public static String INFINISPAN_HOTROD_SERVER_PORT = INFINISPAN_HOTROD_SERVER + ".port";

    public static String HTTP_SERVER = "http.server";
    public static String HTTP_SERVER_HOST = HTTP_SERVER + ".host";
    public static String HTTP_SERVER_PORT = HTTP_SERVER + ".port";

    protected RemoteCacheManager manager;
    protected RemoteCache<String, SimpleDataObject> cache;

    @Override
    public void start(Future<Void> startFuture) throws Exception {
        // final String logFactory =
        // System.getProperty("vertx.logger-delegate-factory-class-name");
        // if (logFactory == null || logFactory.equals("")) {
        // System.setProperty("vertx.logger-delegate-factory-class-name",
        // "io.vertx.core.logging.Log4j2LogDelegateFactory");
        // }
        LOGGER.info("Vertx uses LOGGER: " + LOGGER + ", LoggerDelegate is " + LOGGER.getDelegate());
        LOGGER.debug("DEBUG: Vertx uses LOGGER: " + LOGGER + ", LoggerDelegate is " + LOGGER.getDelegate());
        vertx.<RemoteCache<String, SimpleDataObject>>executeBlocking(future -> {
            Configuration managerConfig = getCacheManagerConfiguration();
            manager = new RemoteCacheManager(managerConfig);
            LOGGER.info("Created RemoteCacheManger=" + manager);
            final String cacheName = config().getString("cache-name");
            LOGGER.debug("Trying to get cache: " + cacheName);
            RemoteCache<String, SimpleDataObject> newCache = manager.getCache(cacheName);
            LOGGER.debug("Got reference for RemoteCahe=" + newCache);
            future.complete(newCache);
        }, result -> {
            if (result.succeeded()) {
                cache = result.result();
                LOGGER.info("RemoteCacheManager=" + manager + " initialized, and RemoteCache=" + cache + " connected");
                registerEndpointREST(startFuture);
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

    protected void registerEndpointREST(Future<Void> startFuture) {
        // if (config().getString("IS"))
        Router router = Router.router(vertx);
        // CORS support
        Set<String> allowHeaders = new HashSet<String>();
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
        allowMethods.add(HttpMethod.PUT);

        router.route().handler(CorsHandler.create("*").allowedHeaders(allowHeaders).allowedMethods(allowMethods));

        router.route().handler(BodyHandler.create());
        router.route("/").handler(this::getROOT);
        router.get("/sdo/:id").handler(this::getSDO);
        router.post("/sdo").handler(this::addSDO);
        router.put("/sdo/:id").handler(this::updateSDO);
        router.patch("/sdo/:id").handler(this::updateSDO);
        router.delete("/sdo/:id").handler(this::removeSDO);

        JsonObject vertxConfig = config();
        final String host = vertxConfig.getString(HTTP_SERVER_HOST, "127.0.0.1");
        final int port = vertxConfig.getInteger(HTTP_SERVER_PORT, 8080);
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Creating HTTP server for host=" + host + ", port=" + port);
        }
        vertx.createHttpServer().requestHandler(router::accept).listen(port, host, ar -> {
            if (ar.succeeded()) {
                LOGGER.debug("Vert.x HTTP Server started: " + ar.result());
                startFuture.complete();
            } else {
                LOGGER.error("Error while start Vert.x HTTP Server", ar.cause());
                startFuture.fail(ar.cause());
            }
        });
    }

    private void sendError(RoutingContext rc, String id, Throwable th) {
        final String msg = "Error occured while looking key " + id + " in cache " + cache.getName();
        LOGGER.error(msg, th);
        rc.fail(HttpResponseStatus.INTERNAL_SERVER_ERROR.code());
        rc.fail(th);
    }

    protected void getSDO(RoutingContext rc) {
        final String id = rc.request().getParam("id");
        LOGGER.debug("GET id: " + id);
        cache.getAsync(id).whenComplete((result, th) -> {
            LOGGER.info("Cache get request for id=" + id + " completed");
            if (th != null) {
                sendError(rc, id, th);
                return;
            }
            final HttpServerResponse response = rc.response();
            if (result == null) {
                LOGGER.debug("Object Not found for id=" + id);
                response.setStatusCode(HttpResponseStatus.NOT_FOUND.code()).end();
            } else {
                LOGGER.debug("Object for id=" + id + " is : " + result.toJson().encode());
                response.setStatusCode(HttpResponseStatus.OK.code()).putHeader("context-type", "application/json");
                response.end(result.toJson().encode());
            }
            LOGGER.debug("Flushing response");
        });
    }

    protected void addSDO(RoutingContext rc) {
        JsonObject o = rc.getBodyAsJson();
        LOGGER.debug("POST reuest for OBJECT: " + o);
        SimpleDataObject sdo = new SimpleDataObject(o);
        final String id = sdo.getId();
        cache.putAsync(id, sdo).whenComplete((result, th) -> {
            LOGGER.info("Cache put request for id=" + id + " completed with result: " + result);
            if (th != null) {
                sendError(rc, id, th);
                return;
            }
            rc.response().setStatusCode(HttpResponseStatus.CREATED.code()).putHeader("content-type", "application/json")
                    .end(cache.get(id).toJson().toString());
        });
    }

    protected void updateSDO(RoutingContext rc) {
        final String id = rc.request().getParam("id");
        JsonObject o = rc.getBodyAsJson();
        LOGGER.debug("UPDATE request for OBJECT: " + o + " with id: " + id);
        SimpleDataObject sdo = new SimpleDataObject(o);
        cache.replaceAsync(id, sdo).whenComplete((result, th) -> {
            LOGGER.info("Cache update request for id=" + id + " completed with result: " + result);
            if (th != null) {
                sendError(rc, id, th);
                return;
            }
            rc.response().setStatusCode(HttpResponseStatus.OK.code()).putHeader("content-type", "application/json")
                    .end(cache.get(id).toJson().toString());
        });
    }

    protected void removeSDO(RoutingContext rc) {
        final String id = rc.request().getParam("id");
        LOGGER.debug("DELETE request for id: " + id);
        cache.removeAsync(id).whenCompleteAsync((result, th) -> {
            LOGGER.info("Cache delete request for id=" + id + " completed with result: " + result);
            if (th != null) {
                sendError(rc, id, th);
                return;
            }
            rc.response().setStatusCode(HttpResponseStatus.NO_CONTENT.code()).end();
        });
    }

    protected void getROOT(RoutingContext rc) {
        rc.response().putHeader("rc-type", "text/html").setStatusCode(HttpResponseStatus.OK.code())
                .end("<html><body>OK/</body></html>\n");

    }

    protected SimpleDataObject getSimpleDataObject(String id) {
        return cache.get(id);
    }

    protected SimpleDataObject createSimpleDataObject(SimpleDataObject dto) {
        // https://docs.oracle.com/javase/8/docs/api/java/util/concurrent/ConcurrentMap.html#putIfAbsent-K-V-
        return cache.putIfAbsent(dto.getId(), dto);
    }

    protected SimpleDataObject updateSimpleDataObject(SimpleDataObject dto) {
        return cache.replace(dto.getId(), dto);
    }

    protected boolean removeSimpleDataObject(SimpleDataObject dto) {
        return cache.remove(dto.getId(), dto);
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
