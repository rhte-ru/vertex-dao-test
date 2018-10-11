package com.redhat.dsevosty;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.Vertx;
import io.vertx.junit5.Checkpoint;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;

@ExtendWith(VertxExtension.class)
public class SimpleVerticleTest {

    @BeforeEach
    public void setUp(Vertx vertx, VertxTestContext tc) throws InterruptedException {
        vertx.deployVerticle(SimpleVerticle.class.getName(), tc.succeeding(ar -> {
            tc.completeNow();
        }));
        tc.awaitCompletion(5, TimeUnit.SECONDS);
    }

    @AfterEach
    public void tearDown(Vertx vertx, VertxTestContext tc) throws InterruptedException {
        vertx.close(tc.succeeding( ar -> {
            tc.completeNow();
        }));
        tc.awaitCompletion(15, TimeUnit.SECONDS);
    }

    @Test
    public void testVertexWebRootContext(Vertx vertx, VertxTestContext tc) {
        Checkpoint async = tc.checkpoint();
        vertx.createHttpClient().getNow(8181, "localhost", "/", response -> response.handler(body -> {
            System.out.println("StatusCode: " + response.statusCode() + "\nBody: " + body);
            assertThat(response.statusCode()).isEqualTo(HttpResponseStatus.OK.code());
            assertThat(body.toString()).contains("Simplest page");
            async.flag();
        }));
    }
}
