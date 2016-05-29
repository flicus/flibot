package org.schors.flibot;

import io.vertx.codegen.annotations.ProxyGen;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.serviceproxy.ProxyHelper;

@ProxyGen
public interface DBService {

//    static DBService create(Vertx vertx) {
//        return new DBServiceImpl(vertx);
//    }

    static DBService createProxy(Vertx vertx, String address) {
        return ProxyHelper.createProxy(DBService.class, vertx, address);
    }

    public void isRegisterdUser(String userName, Handler<AsyncResult<JsonObject>> handler);

    public void registerUser(String userName, Handler<AsyncResult<JsonObject>> handler);

    public void unregisterUser(String userName, Handler<AsyncResult<JsonObject>> handler);

}
