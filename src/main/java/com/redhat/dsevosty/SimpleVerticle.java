package com.redhat.dsevosty;

import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;

public class SimpleVerticle extends AbstractVerticle {

    @Override
    public void start(Future<Void> future) {

        Router router = Router.router(vertx);
        router.route().handler(BodyHandler.create());
        router.route("/").handler(rc -> {
            mainHandler(rc);
        });
        vertx.createHttpServer().requestHandler(router::accept).listen(8181, result -> {
            if (result.succeeded()) {
                future.complete();
            } else {
                future.fail(result.cause());
            }
        });
    }

    private void mainHandler(RoutingContext rc) {
        rc.response().putHeader("context-type", "text/html").setStatusCode(HttpResponseStatus.OK.code())
                .end("<html><body><h1>My first Vert.x</h1><p>Simplest page</p></body></html>\n");

    }

    /*
     * public static void main(String[] args) { Vertx vertx = Vertx.vertx();
     * vertx.deployVerticle(SimpleVerticle.class.getName()); }
     */
}
