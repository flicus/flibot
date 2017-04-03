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

package org.schors.flibot.commands;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.file.OpenOptions;
import io.vertx.core.streams.Pump;
import org.schors.flibot.Util;
import org.schors.flibot.VxZipInputStream;
import org.schors.vertx.telegram.bot.api.methods.SendDocument;
import org.schors.vertx.telegram.bot.commands.BotCommand;
import org.schors.vertx.telegram.bot.commands.CommandContext;

import java.io.File;
import java.io.IOException;

@BotCommand(regexp = "^/z")
public class DownloadZipCommand extends FlibotCommand {

    public DownloadZipCommand() {

    }

    @Override
    public void execute(CommandContext context, Handler<Boolean> handler) {
        String text = context.getUpdate().getMessage().getText();
        String url = getCache().getIfPresent(Util.normalizeCmd(text));
        if (url != null) {
            downloadz(url, event -> {
                if (event.succeeded()) {
                    sendFile(context, (SendDocument) event.result());
                    handler.handle(Boolean.TRUE);
                } else {
                    sendReply(context, "Error happened :(");
                    handler.handle(Boolean.FALSE);
                }
            });
        } else {
            sendReply(context, "Expired command");
            handler.handle(Boolean.FALSE);
        }
    }

    private void downloadz(String url, Handler<AsyncResult<Object>> handler) {
        getClient().get(url, res -> {
            if (res.statusCode() == 200) {
                try {
                    VxZipInputStream zipStream = new VxZipInputStream(res);
                    zipStream.exceptionHandler(e -> handler.handle(Util.result(false, null, e)));
                    zipStream.getNextEntry(entry -> {
                        if (entry.succeeded()) {
                            File book = null;
                            try {
                                book = File.createTempFile(entry.result().getName(), null);
                            } catch (IOException e) {
                                handler.handle(Util.result(false, null, e));
                                return;
                            }
                            final File finalBook = book;
                            getBot().getVertx().fileSystem().open(book.getAbsolutePath(), new OpenOptions().setWrite(true), output -> {
                                if (output.succeeded()) {
                                    zipStream.endHandler(done -> handler.handle(Util.result(true, new SendDocument().setDocument(finalBook.getAbsolutePath()).setCaption("book"), null)));
                                    Pump.pump(zipStream, output.result()).start();
                                } else {
                                    handler.handle(Util.result(false, null, output.cause()));
                                }
                            });
                        } else {
                            handler.handle(Util.result(false, null, entry.cause()));
                        }
                    });
                } catch (Exception e) {
                    handler.handle(Util.result(false, null, e));
                }
            }
        }).exceptionHandler(e -> {
            handler.handle(Util.result(false, null, e));
        });
    }
}
