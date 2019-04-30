package com.redhat.summit2019;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.redhat.summit2019.model.Adjective;
import io.reactivex.Flowable;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.file.AsyncFile;
import io.vertx.core.file.OpenOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.core.parsetools.RecordParser;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.StaticHandler;
import io.vertx.reactivex.FlowableHelper;
import io.vertx.reactivex.RxHelper;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Observable;
import java.util.Random;

import static io.vertx.core.http.HttpHeaders.CONTENT_TYPE;

public class HttpApplication extends AbstractVerticle {

  static final String template = "Hello, %s!";

  List<Adjective> adjectives;

  @Override
  public void start(Future<Void> startFuture) {

    Future init = loadAdjectives().compose(v -> startHttpServer()).setHandler(startFuture.completer());

  }

  private Future<Void> startHttpServer(){
    Future<Void> future = Future.future();

    // Create a router object.
    Router router = Router.router(vertx);

    router.get("/api/greeting").handler(this::greeting);
    router.get("/api/adjective").handler(this::adjectiveHandler);
    router.get("/*").handler(StaticHandler.create());

    // Create the HTTP server and pass the "accept" method to the request handler.
    vertx
      .createHttpServer()
      .requestHandler(router)
      .listen(
              // Retrieve the port from the configuration, default to 8080.
              config().getInteger("http.port", 8080), ar -> {
                if (ar.succeeded()) {
                  System.out.println("Server started on port " + ar.result().actualPort());
                  future.complete();
                }else{
                  future.fail(ar.cause());
                }
              });

    return future;
  }

  private Future<Void> loadAdjectives() {

    if (adjectives == null) {
      adjectives = new ArrayList<>();
    }

    Future<Void> future = Future.future();

    try {
      InputStream is = this.getClass().getClassLoader().getResourceAsStream("adjectives.txt");
      if (is != null) {
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        reader.lines()
                .forEach(adj -> adjectives.add(new Adjective(adj.trim())));
      }
      future.complete();
    } catch (Exception e) {
      e.printStackTrace();
      future.fail(e.getCause());
    }

    return future;
  }

  private void adjectiveHandler(RoutingContext rc) {

    Adjective adjective = adjectives.get(new Random().nextInt(adjectives.size()));
    JsonObject response = new JsonObject()
            .put("adjective", adjective.getAdjective());

    rc.response()
            .putHeader(CONTENT_TYPE, "application/json; charset=utf-8")
            .end(response.encodePrettily());
  }

  private void greeting(RoutingContext rc) {
    String name = rc.request().getParam("name");
    if (name == null) {
      name = "World";
    }

    JsonObject response = new JsonObject()
        .put("content", String.format(template, name));

    rc.response()
        .putHeader(CONTENT_TYPE, "application/json; charset=utf-8")
        .end(response.encodePrettily());
  }
}
