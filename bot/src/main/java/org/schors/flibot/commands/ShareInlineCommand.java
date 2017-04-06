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
import org.schors.vertx.telegram.bot.api.methods.AnswerInlineQuery;
import org.schors.vertx.telegram.bot.api.methods.SendDocument;
import org.schors.vertx.telegram.bot.api.types.inline.InlineQueryResult;
import org.schors.vertx.telegram.bot.commands.BotCommand;
import org.schors.vertx.telegram.bot.commands.CommandContext;

import java.io.File;

@BotCommand(inline = "^share")
public class ShareInlineCommand extends FlibotCommand {

    @Override
    public void execute(CommandContext context, Handler<Boolean> handler) {
        String text = context.getUpdate().getInlineQuery().getQuery().split("_")[1];
        String url = getCache().getIfPresent(Util.normalizeCmd(text));

        if (url != null) {
            download(url, event -> {
                if (event.succeeded()) {
                    sendInlineAnswer(new AnswerInlineQuery().setResults(new InlineQueryResult[]{new}));
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

    private void download(String url, Handler<AsyncResult<Object>> handler) {
        getClient().get(url, res -> {
            if (res.statusCode() == 200) {
                try {
                    File book = File.createTempFile(fileNameParser.parse(url), null);
                    getBot().getVertx().fileSystem().open(book.getAbsolutePath(), new OpenOptions().setWrite(true), event -> {
                        if (event.succeeded()) {
                            Pump.pump(res
                                            .endHandler(done -> {
                                                event.result().close();
                                                handler.handle(Util.result(true, new SendDocument().setDocument(book.getAbsolutePath()).setCaption("book"), null));
                                            })
                                            .exceptionHandler(e -> handler.handle(Util.result(false, null, e))),
                                    event.result())
                                    .start();
                        } else {
                            handler.handle(Util.result(false, null, event.cause()));
                        }
                    });
                } catch (Exception e) {
                    handler.handle(Util.result(false, null, e));
                }
            }
        }).exceptionHandler(e -> handler.handle(Util.result(false, null, e)));
    }
}
