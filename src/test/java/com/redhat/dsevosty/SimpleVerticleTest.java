package com.redhat.dsevosty;

import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(VertxUnitRunner.class)
public class SimpleVerticleTest {

    private Vertx     vertx;
    private WebClient webClient;

    @Before
    public void setUp(TestContext tc) {
        vertx = Vertx.vertx();
        vertx.deployVerticle(SimpleVerticle.class.getName(), tc.asyncAssertSuccess());
        webClient = WebClient.create(vertx);
    }

    @After
    public void tearDown(TestContext tc) {
        webClient.close();
        vertx.close(tc.asyncAssertSuccess());
    }

    @Test
    public void testVertexWebRootContext(TestContext tc) {
        webClient.get(8080, "localhost", "/").send(result -> {
            if (!result.succeeded()) {
                tc.asyncAssertFailure();
                return;
            }
            HttpResponse<Buffer> response = result.result();
            tc.assertEquals(response.statusCode(), HttpResponseStatus.OK.code());
            tc.assertTrue(response.bodyAsString().contains("Simplest page"));
        });
    }
}
