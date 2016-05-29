package org.schors.flibot;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.serviceproxy.ProxyHelper;

public class DBManager extends AbstractVerticle {

    private DBService dbService;

    @Override
    public void start(Future<Void> startFuture) throws Exception {
        try {
            dbService = new DBServiceImpl(vertx, config().getString("admin"));
            ProxyHelper.registerService(DBService.class, vertx, dbService, "db-service");
            startFuture.complete();
        } catch (Exception e) {
            startFuture.fail(e);
        }
    }


    @Override
    public void stop() throws Exception {
        super.stop();
    }
}
