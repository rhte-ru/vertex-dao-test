package com.redhat.dsevosty;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.Vertx;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;

// @RunWith(VertxUnitRunner.class)
public class SimpleVerticleTest {

    private Vertx vertx;

    @Before
    public void setUp(TestContext tc) {
        vertx = Vertx.vertx();
        vertx.deployVerticle(SimpleVerticle.class.getName(), tc.asyncAssertSuccess());
    }

    @After
    public void tearDown(TestContext tc) {
        vertx.close(tc.asyncAssertSuccess());
    }

    // @Test
    public void testVertexWebRootContext(TestContext tc) {
        Async async = tc.async();
        vertx.createHttpClient().getNow(8181, "localhost", "/", response -> response.handler(body -> {
            System.out.println("StatusCode: " + response.statusCode() + "\nBody: " + body);
            tc.assertEquals(response.statusCode(), HttpResponseStatus.OK.code());
            tc.assertTrue(body.toString().contains("Simplest page"));
            async.complete();
        }));
        async.awaitSuccess(10000);
    }
}
