/*
 *  The MIT License (MIT)
 *
 *  Copyright (c) 2016  schors
 *
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

package org.schors.flibot.commands;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.file.OpenOptions;
import io.vertx.core.streams.Pump;
import org.schors.flibot.Util;
import org.schors.vertx.telegram.bot.commands.CommandContext;
import org.telegram.telegrambots.api.methods.send.SendDocument;

import java.io.File;

public class DownloadCommand extends FlibotCommand {

    public DownloadCommand() {
        super("^/d");
    }

    @Override
    public void execute(String s, CommandContext commandContext) {
        String url = getCache().getIfPresent(Util.normalizeCmd(s));
        if (url != null) {
            download(url, event -> {
                if (event.succeeded()) {
                    sendFile(commandContext.getUpdate(), (SendDocument) event.result());
                } else {
                    sendReply(commandContext.getUpdate(), "Error happened :(");
                }
            });
        } else {
            sendReply(commandContext.getUpdate(), "Expired command");
        }
    }

    private void download(String url, Handler<AsyncResult<Object>> handler) {
        getClient().get(url, res -> {
            if (res.statusCode() == 200) {
                try {
                    File book = File.createTempFile("flibot_" + Long.toHexString(System.currentTimeMillis()), null);
                    getBot().getVertx().fileSystem().open(book.getAbsolutePath(), new OpenOptions().setWrite(true), event -> {
                        if (event.succeeded()) {
                            Pump.pump(res
                                            .endHandler(done -> handler.handle(Util.createResult(true, new SendDocument().setNewDocument(book).setCaption("book"), null)))
                                            .exceptionHandler(e -> handler.handle(Util.createResult(false, null, e))),
                                    event.result())
                                    .start();
                        } else {
                            handler.handle(Util.createResult(false, null, event.cause()));
                        }
                    });
                } catch (Exception e) {
                    handler.handle(Util.createResult(false, null, e));
                }
            }
        }).exceptionHandler(e -> handler.handle(Util.createResult(false, null, e)));
    }

    /*    private void download(String url, Handler<AsyncResult<Object>> handler) {
        vertx.executeBlocking(future -> {
            HttpGet httpGet = new HttpGet(rootOPDS + url);
            try {
                CloseableHttpResponse response = httpclient.execute(httpGet, context);
                if (response.getStatusLine().getStatusCode() == 200) {
                    String fileName = "tmp";
                    if (url.contains("mobi")) {
                        String[] parts = url.split("/");
                        fileName = parts[parts.length - 2] + "." + parts[parts.length - 1] + ".mobi";
                    } else if (url.contains("djvu")) {
                        String[] parts = url.split("/");
                        fileName = parts[parts.length - 2] + "." + parts[parts.length - 1] + ".djvu";
                    } else {
                        String[] parts = url.split("/");
                        fileName = parts[parts.length - 2] + "." + parts[parts.length - 1] + ".zip";
                    }
                    HttpEntity ht = response.getEntity();
                    BufferedHttpEntity buf = new BufferedHttpEntity(ht);
                    File book = File.createTempFile("flibot_" + Long.toHexString(System.currentTimeMillis()), null);
                    buf.writeTo(new FileOutputStream(book));
                    final SendDocument sendDocument = new SendDocument();
                    sendDocument.setNewDocument(book.getAbsolutePath(), fileName);
                    sendDocument.setCaption("book");
                    future.complete(sendDocument);
                }
            } catch (Exception e) {
                log.warn(e, e);
                future.fail(e);
            }
        }, res -> {
            handler.handle(res);
        });
    }*/
}
