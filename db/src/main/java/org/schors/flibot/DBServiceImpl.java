package org.schors.flibot;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;

import java.sql.*;

public class DBServiceImpl implements DBService {

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
            e.printStackTrace();
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

        }
        return result;
    }

    public void registerUser(String username) {
        try {
            PreparedStatement ps = getConnection().prepareStatement("insert into registered values(?)");
            ps.setString(1, username);
            ps.execute();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void unregisterUser(String username) {
        try {
            PreparedStatement ps = getConnection().prepareStatement("delete from registered where name=?");
            ps.setString(1, username);
            ps.execute();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
