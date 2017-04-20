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

import io.vertx.core.Handler;
import org.schors.flibot.Util;
import org.schors.vertx.telegram.bot.api.methods.AnswerInlineQuery;
import org.schors.vertx.telegram.bot.api.types.InputTextMessageContent;
import org.schors.vertx.telegram.bot.api.types.inline.InlineQueryResult;
import org.schors.vertx.telegram.bot.api.types.inline.InlineQueryResultArticle;
import org.schors.vertx.telegram.bot.api.util.ParseMode;
import org.schors.vertx.telegram.bot.commands.BotCommand;
import org.schors.vertx.telegram.bot.commands.CommandContext;

@BotCommand(inline = "^share")
public class ShareInlineCommand extends FlibotCommand {

    @Override
    public void execute(CommandContext context, Handler<Boolean> handler) {
        String text = context.getUpdate().getInlineQuery().getQuery().split("_")[1];
        String url = getCache().getIfPresent(Util.normalizeCmd(text));

        if (url != null) {
            sendInlineAnswer(context, new AnswerInlineQuery()
                    .setResults(
                            new InlineQueryResult[]{
                                    new InlineQueryResultArticle()
                                            .setId(Long.toHexString(System.currentTimeMillis()))
                                            .setTitle("title")
                                            .setInputMessageContent(
                                            new InputTextMessageContent()
                                                    .setDisableWebPagePreview(true)
                                                    .setParseMode(ParseMode.html)
                                                    .setMessageText("text"))}));
            handler.handle(Boolean.TRUE);
        } else {
            sendReply(context, "Expired command");
            handler.handle(Boolean.FALSE);
        }
    }
}
