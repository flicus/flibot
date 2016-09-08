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
import org.schors.flibot.ZipStream;
import org.schors.vertx.telegram.bot.commands.CommandContext;
import org.telegram.telegrambots.api.methods.send.SendDocument;

import java.io.File;

public class DownloadZipCommand extends FlibotCommand {

    public DownloadZipCommand() {
        super("^/z");
    }

    @Override
    public void execute(String s, CommandContext commandContext) {
        String url = getCache().getIfPresent(Util.normalizeCmd(s));
        if (url != null) {
            downloadz(url, event -> {
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

    private void downloadz(String url, Handler<AsyncResult<Object>> handler) {
        getClient().get(url, res -> {
            if (res.statusCode() == 200) {
                try {
                    ZipStream zipStream = new ZipStream(res);
                    File book = File.createTempFile("flibot_" + Long.toHexString(System.currentTimeMillis()), null);
                    getBot().getVertx().fileSystem().open(book.getAbsolutePath(), new OpenOptions().setWrite(true), event -> {
                        if (event.succeeded()) {
                            Pump.pump(zipStream
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
}
