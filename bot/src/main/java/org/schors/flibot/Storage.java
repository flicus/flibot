/*
 *  The MIT License (MIT)
 *
 *  Copyright (c) 2016 schors
 *   Permission is hereby granted, free of charge, to any person obtaining a copy
 *  of this software and associated documentation files (the "Software"), to deal
 *  in the Software without restriction, including without limitation the rights
 *  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the Software is
 *  furnished to do so, subject to the following conditions:
 *
 *   The above copyright notice and this permission notice shall be included in all
 *  copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 *  SOFTWARE.
 */

package org.schors.flibot;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.jdbc.JDBCClient;

import java.util.ArrayList;
import java.util.List;

public class Storage {

    private JDBCClient client;

    public Storage(Vertx vertx, String admin) {
        JsonObject config = new JsonObject()
                .put("url", "jdbc:h2:./flibot")
                .put("driver_class", "org.h2.Driver")
                .put("user", "sa")
                .put("password", "");

        client = JDBCClient.createShared(vertx, config);
        isRegisteredUser(admin, event -> {
            if (event.failed()) {
                createTables(admin);
            }
        });
    }

    public static AsyncResult<JsonObject> makeAsyncResult(final JsonObject result, final Throwable cause, final boolean success) {
        return new AsyncResult<JsonObject>() {
            @Override
            public JsonObject result() {
                return result;
            }

            @Override
            public Throwable cause() {
                return cause;
            }

            @Override
            public boolean succeeded() {
                return success;
            }

            @Override
            public boolean failed() {
                return !success;
            }
        };
    }

    public void isRegisteredUser(String userName, Handler<AsyncResult<JsonObject>> handler) {
        client.getConnection(event -> {
            if (event.succeeded()) {
                event.result().queryWithParams("select 1 from registered where name=?", new JsonArray().add(userName), res -> {
                    if (res.succeeded() && res.result().getNumRows() > 0) {
                        handler.handle(makeAsyncResult(null, res.cause(), res.succeeded()));
                    } else {
                        handler.handle(makeAsyncResult(null, null, false));
                    }
                });
            } else handler.handle(makeAsyncResult(null, null, false));
        });
    }

    public void registerUser(String userName, Handler<AsyncResult<JsonObject>> handler) {
        client.getConnection(event -> {
            if (event.succeeded()) {
                event.result().updateWithParams("insert into registered values(?)", new JsonArray().add(userName), res -> {
                    handler.handle(makeAsyncResult(null, res.cause(), res.succeeded()));
                });
            } else handler.handle(makeAsyncResult(null, null, false));
        });
    }

    public void unregisterUser(String userName, Handler<AsyncResult<JsonObject>> handler) {
        client.getConnection(event -> {
            if (event.succeeded()) {
                event.result().updateWithParams("delete from registered where name=?", new JsonArray().add(userName), res -> {
                    handler.handle(makeAsyncResult(null, res.cause(), res.succeeded()));
                });
            } else handler.handle(makeAsyncResult(null, null, false));
        });
    }

    private void createTables(String admin) {
        client.getConnection(event -> {
            if (event.succeeded()) {
                List<String> batch = new ArrayList<>();
                batch.add("drop table if EXISTS registered");
                batch.add("CREATE TABLE registered (name VARCHAR(255))");
                event.result().batch(batch, res -> {
                    if (res.succeeded()) {
                        event.result().updateWithParams("insert into registered values(?)", new JsonArray().add(admin), event1 -> {
                            //do nothing, maybe check updated rows
                        });
                    }
                });
            }
        });
    }
}
