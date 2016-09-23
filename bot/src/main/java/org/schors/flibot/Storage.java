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

import io.vertx.core.Handler;
import io.vertx.core.Vertx;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.HashSet;
import java.util.Set;

public class Storage {

    private Vertx vertx;
    private Set<String> users = new HashSet<>();

    public Storage(Vertx vertx, String admin) {
        this.vertx = vertx;
        load(event -> {
            if (!users.contains(admin)) {
                users.clear();
                users.add(admin);
                save();
            }
        });
    }

    public boolean isRegisteredUser(String userName) {
        return users.contains(userName);
    }

    public void registerUser(String userName) {
        if (!users.contains(userName)) {
            users.add(userName);
            save();
        }
    }

    public void unregisterUser(String userName) {
        if (users.contains(userName)) {
            users.remove(userName);
            save();
        }
    }

    private void load(Handler handler) {
        vertx.executeBlocking(future -> {
            try {
                BufferedReader r = new BufferedReader(new FileReader("flibot.dat"));
                while (r.ready()) {
                    users.add(r.readLine());
                }
                r.close();
                future.complete();
            } catch (Exception e) {
                future.fail(e);
            }
        }, res -> {
            handler.handle(null);
        });
    }

    private void save() {
        vertx.executeBlocking(future -> {
            try {
                BufferedWriter w = new BufferedWriter(new FileWriter("flibot.dat"));
                for (String s : users) {
                    w.write(s);
                    w.newLine();
                }
                w.flush();
                w.close();
            } catch (Exception e) {
                future.fail(e);
            }
        }, res -> {
            //do nothing
        });
    }
}
