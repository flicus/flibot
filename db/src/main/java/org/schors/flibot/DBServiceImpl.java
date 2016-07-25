/*
 *
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
 *
 */

package org.schors.flibot;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import org.apache.log4j.Logger;

import java.sql.*;

public class DBServiceImpl implements DBService {

    private static final Logger log = Logger.getLogger(DBServiceImpl.class);
    private final String JDBC_DRIVER_NAME = "org.h2.Driver";
    private final String JDBC_URL = "jdbc:h2:./flibot";
    private final String JDBC_USER = "sa";
    private final String JDBC_PASSWORD = "";
    private Connection connection;
    private Vertx vertx;

    public DBServiceImpl(Vertx vertx, String adminName) {
        this.vertx = vertx;
        if (!isRegisterdUser(adminName)) {
            createTables(adminName);
        }
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

    @Override
    public void isRegisterdUser(String userName, Handler<AsyncResult<JsonObject>> handler) {
        vertx.executeBlocking(future -> {
            boolean r = isRegisterdUser(userName);
            future.complete(r);
        }, res -> {
            handler.handle(makeAsyncResult(new JsonObject().put("res", res.result()), res.cause(), res.succeeded()));
        });
    }

    @Override
    public void registerUser(String userName, Handler<AsyncResult<JsonObject>> handler) {
        vertx.executeBlocking(future -> {
            registerUser(userName);
            future.complete(true);
        }, res -> {
            handler.handle(makeAsyncResult(new JsonObject().put("res", res.result()), res.cause(), res.succeeded()));
        });
    }

    @Override
    public void unregisterUser(String userName, Handler<AsyncResult<JsonObject>> handler) {
        vertx.executeBlocking(future -> {
            unregisterUser(userName);
            future.complete(true);
        }, res -> {
            handler.handle(makeAsyncResult(new JsonObject().put("res", res.result()), res.cause(), res.succeeded()));
        });
    }

    private Connection getConnection() throws SQLException, ClassNotFoundException {
        if (connection == null) {
            Class.forName(JDBC_DRIVER_NAME);
            connection = DriverManager.getConnection(JDBC_URL, JDBC_USER, JDBC_PASSWORD);
        }
        return connection;
    }

    private void createTables(String admin) {

        try {
            Statement st = getConnection().createStatement();
            st.execute("drop table if EXISTS registered");
            st.execute("CREATE TABLE registered (name VARCHAR(255))");
            PreparedStatement ps = getConnection().prepareStatement("insert into registered values(?)");
            ps.setString(1, admin);
            ps.execute();
        } catch (Exception e) {
            log.error(e, e);
        }
    }

    public boolean isRegisterdUser(String userName) {
        boolean result = false;
        try {
            PreparedStatement ps = getConnection().prepareStatement("select 1 from registered where name=?");
            ps.setString(1, userName);
            ResultSet rs = ps.executeQuery();
            if (rs.first()) {
                result = true;
            }
        } catch (Exception e) {
            log.error(e, e);
        }
        return result;
    }

    public void registerUser(String username) {
        try {
            PreparedStatement ps = getConnection().prepareStatement("insert into registered values(?)");
            ps.setString(1, username);
            ps.execute();
        } catch (Exception e) {
            log.error(e, e);
        }
    }

    public void unregisterUser(String username) {
        try {
            PreparedStatement ps = getConnection().prepareStatement("delete from registered where name=?");
            ps.setString(1, username);
            ps.execute();
        } catch (Exception e) {
            log.error(e, e);
        }
    }

}
