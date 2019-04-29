package com.redhat.summit2019;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.StaticHandler;

import static io.vertx.core.http.HttpHeaders.CONTENT_TYPE;

public class HttpApplication extends AbstractVerticle {

  static final String template = "Hello, %s!";

  @Override
  public void start(Future<Void> startFuture) {

    Future init = loadAdjectives().compose(v -> startHttpServer());
    init.setHandler(startFuture.completer());

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
    Future<Void> future = Future.future();
    vertx.fileSystem().readFile("adjectives.txt", res -> {
      if (res.succeeded()) {
        System.out.println(res.result());
        future.complete();
      } else {
        System.err.println("Oh oh ..." + res.cause());
        future.fail(res.cause());
      }
    });
    return future;
  }

  private void adjectiveHandler(RoutingContext rc) {

    JsonObject response = new JsonObject()
            .put("adjective", "clapper-clawed");

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
