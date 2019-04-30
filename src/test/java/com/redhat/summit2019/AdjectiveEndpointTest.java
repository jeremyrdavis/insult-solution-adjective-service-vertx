package com.redhat.summit2019;

import com.redhat.summit2019.model.Adjective;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.file.AsyncFile;
import io.vertx.core.file.OpenOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.core.parsetools.RecordParser;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import io.vertx.ext.web.client.WebClient;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.List;

import static junit.framework.TestCase.assertTrue;

@RunWith(VertxUnitRunner.class)
public class AdjectiveEndpointTest {

    private static final int PORT = 8081;

    private Vertx vertx;
    private WebClient client;

    private List<Adjective> adjectives;

    @Before
    public void before(TestContext context) {
        adjectives = new ArrayList<Adjective>();
        vertx = Vertx.vertx();
        vertx.exceptionHandler(context.exceptionHandler());
        vertx.deployVerticle(HttpApplication.class.getName(),
            new DeploymentOptions().setConfig(new JsonObject().put("http.port", PORT)),
            context.asyncAssertSuccess());
        client = WebClient.create(vertx);

        // Load the adjectives
        RecordParser recordParser = RecordParser.newDelimited("\n", bufferedLine -> {
            adjectives.add(new Adjective(bufferedLine.toString()));
        });

        AsyncFile asyncFile = vertx.fileSystem().openBlocking("adjectives.txt", new OpenOptions());

        asyncFile.handler(recordParser)
                .endHandler(v -> {
                    asyncFile.close();
                });
    }

    @After
    public void after(TestContext context) {
        vertx.close(context.asyncAssertSuccess());
    }

    @Test
    public void callAdjectiveEndpoint(TestContext context) {
        // Send a request and get a response
        Async async = context.async();
        client.get(PORT, "localhost", "/api/adjective")
            .send(resp -> {
                context.assertTrue(resp.succeeded());
                context.assertEquals(resp.result().statusCode(), 200);
                Adjective adjective = resp.result().bodyAsJson(Adjective.class);
                assertTrue(adjectives.contains(adjective));
                async.complete();
            });
    }
}
