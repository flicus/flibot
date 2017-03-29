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
import org.schors.flibot.Search;
import org.schors.flibot.SendMessageList;
import org.schors.vertx.telegram.bot.api.methods.SendMessage;
import org.schors.vertx.telegram.bot.api.types.KeyboardButton;
import org.schors.vertx.telegram.bot.api.types.ReplyKeyboardMarkup;
import org.schors.vertx.telegram.bot.commands.CommandContext;

public class DefaultCommand extends FlibotCommand {

    public DefaultCommand() {
        super("no need in regexp");
    }

    @Override
    public void execute(String text, CommandContext context) {
        String userName = context.getUpdate().getMessage().getFrom().getUsername();
        Search search = getSearches().get(userName);
        if (search != null) {
            getSearches().remove(userName);
            switch (search.getSearchType()) {
                case AUTHOR: {
                    getAuthor(text.trim().replaceAll(" ", "+"), event -> {
                        if (event.succeeded()) {
                            sendReply(context, (SendMessageList) event.result());
                        } else {
                            sendReply(context, "Error happened :(");
                        }
                    });
                    break;
                }
                case BOOK: {
                    getBook(text.trim().replaceAll(" ", "+"), event -> {
                        if (event.succeeded()) {
                            sendReply(context, (SendMessageList) event.result());
                        } else {
                            sendReply(context, "Error happened :(");
                        }
                    });
                    break;
                }
            }
        } else {
            search = new Search();
            search.setToSearch(text.trim().replaceAll(" ", "+"));
            getSearches().put(userName, search);
            KeyboardButton authorButton = new KeyboardButton();
            authorButton.setText("/author");
            KeyboardButton bookButton = new KeyboardButton();
            bookButton.setText("/book");
            KeyboardButton[][] keyboardRows = new KeyboardButton[][]{{authorButton, bookButton}};
            ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
            keyboardMarkup.setKeyboard(keyboardRows);
            keyboardMarkup.setResizeKeyboard(true);
            keyboardMarkup.setSelective(true);
            SendMessage sendMessage = new SendMessage();
            sendMessage.setChatId(context.getUpdate().getMessage().getChatId());
            sendMessage.setReplyMarkup(keyboardMarkup);
            sendMessage.setText("What to search, author or book?");
            sendReply(context, sendMessage);
        }
    }

    private void getAuthor(String author, Handler<AsyncResult<Object>> handler) {
        doGenericRequest("/opds" + String.format(authorSearch, author), event -> handler.handle(event));
    }

    private void getBook(String book, Handler<AsyncResult<Object>> handler) {
        doGenericRequest("/opds" + String.format(bookSearch, book), event -> handler.handle(event));
    }
}
