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

import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.json.JsonObject;
import org.apache.log4j.PropertyConfigurator;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

public class Main {

    public static void main(String[] args) throws IOException {

        String log4j = "log4j.properties";
        if (args.length > 1) log4j = args[1];
        PropertyConfigurator.configure(log4j);

        JsonObject config = new JsonObject();
        if (System.getenv("flibot") != null) {
            //get from env
            config.put("name", System.getenv("name"))
                    .put("token", System.getenv("token"))
                    .put("usetor", System.getenv("usetor"))
                    .put("torhost", System.getenv("torhost"))
                    .put("torport", System.getenv("torport"))
                    .put("admin", System.getenv("admin"));
        } else {
            //trying to read properties
            Properties p = new Properties();
            p.load(new FileInputStream("bot.ini"));
            config.put("name", p.getProperty("name"))
                    .put("token", p.getProperty("token"))
                    .put("usetor", p.getProperty("usetor"))
                    .put("torhost", p.getProperty("torhost"))
                    .put("torport", p.getProperty("torport"))
                    .put("admin", p.getProperty("admin"));
        }

        VertxOptions options = new VertxOptions().setWorkerPoolSize(40);
        Vertx vertx = Vertx.vertx(options);

        DeploymentOptions deploymentOptions = new DeploymentOptions().setInstances(1).setConfig(config);
        vertx.deployVerticle(new DBManager(), deploymentOptions, event -> {
            if (event.succeeded()) {
                vertx.deployVerticle(new FliBot(), deploymentOptions);
            } else {
                event.cause().printStackTrace();
                vertx.close();
            }
        });
    }
}
