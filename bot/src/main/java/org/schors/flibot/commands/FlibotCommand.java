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

import com.google.common.cache.Cache;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpClient;
import io.vertx.core.json.JsonObject;
import org.schors.flibot.*;
import org.schors.flibot.opds.Page;
import org.schors.flibot.opds.PageParser;
import org.schors.vertx.telegram.bot.api.methods.SendChatAction;
import org.schors.vertx.telegram.bot.api.methods.SendDocument;
import org.schors.vertx.telegram.bot.api.methods.SendMessage;
import org.schors.vertx.telegram.bot.api.types.Action;
import org.schors.vertx.telegram.bot.api.types.Update;
import org.schors.vertx.telegram.bot.api.util.ParseMode;
import org.schors.vertx.telegram.bot.commands.Command;
import org.schors.vertx.telegram.bot.commands.CommandContext;

import java.io.ByteArrayInputStream;
import java.util.Map;

public abstract class FlibotCommand extends Command {

    public static final String authorSearch = "/search?searchType=authors&searchTerm=%s";
    public static final String bookSearch = "/search?searchType=books&searchTerm=%s";

    public FlibotCommand() {
    }

    protected HttpClient getClient() {
        return (HttpClient) getBot().getFacility(Util.HTTP_CLIENT);
    }

    protected Cache<String, String> getCache() {
        return (Cache<String, String>) getBot().getFacility(Util.CACHE);
    }

    protected Map<String, Search> getSearches() {
        return (Map<String, Search>) getBot().getFacility(Util.SEARCHES);
    }

    protected Storage getDB() {
        return (Storage) getBot().getFacility(Util.DB);
    }

    protected JsonObject getConfig() {
        return (JsonObject) getBot().getFacility(Util.CONFIG);
    }

    protected void sendReply(CommandContext context, String res) {
        getBot().sendMessage(new SendMessage()
                .setChatId(context.getUpdate().getMessage().getChatId())
                .setText(res)
                .setParseMode(ParseMode.html));
    }

    protected void sendReply(CommandContext context, SendMessage res) {
        res.setChatId(context.getUpdate().getMessage().getChatId());
        getBot().sendMessage(res);
    }

    protected void sendReply(CommandContext context, SendMessageList res) {
        for (SendMessage sm : res.getMessages()) {
            sm.setChatId(context.getUpdate().getMessage().getChatId());
            getBot().sendMessage(sm);
        }
    }

    protected void sendFile(CommandContext context, SendDocument res) {
        getBot().sendDocument(res
                .setChatId(context.getUpdate().getMessage().getChatId())
//                .setReplyMarkup(new InlineKeyboardMarkup(new InlineKeyboardButton[][]{{new InlineKeyboardButton()
//                        .setText("Share")
//                        .setSwitchInlineQuery("share_" + context.get("text"))}}))
        );
    }

//    protected void sendInlineAnswer(CommandContext context, AnswerInlineQuery query) {
//        getBot().sendAnswerInlineQuery(query);
//    }

    protected void sendBusy(Update update) {
        getBot().sendChatAction(new SendChatAction()
                .setChatId(update.getMessage().getChatId())
                .setAction(Action.UPLOADDOCUMENT));
    }

    protected void doGenericRequest(String url, Handler<AsyncResult<Object>> handler) {
        SendMessageList result = new SendMessageList(4096);
        getClient().get(url, event -> {
            if (event.statusCode() == 200) {
                event
                        .bodyHandler(buffer -> {
//                            Page page = PageParser.parse(new VertxBufferInputStream(buffer));
                            Page page = PageParser.parse(new ByteArrayInputStream(buffer.getBytes()));
                            if (page.getEntries() != null && page.getEntries().size() > 0) {
                                if (page.getTitle() != null) {
                                    result.append("<b>").append(page.getTitle()).append("</b>\n");
                                }
                                page.getEntries().stream().forEach(entry -> {
                                    result.append("<b>").append(entry.getTitle()).append("</b>");
                                    if (entry.getAuthor() != null) {
                                        result.append(" (").append(entry.getAuthor()).append(")");
                                    }
                                    result.append("\n");
                                    entry.getLinks().stream()
                                            .filter((l) -> l.getType() != null && l.getType().toLowerCase().contains("opds-catalog"))
                                            .forEach(link -> {
                                                if (link.getTitle() != null) {
                                                    result.append(link.getTitle());
                                                }
                                                String id = Integer.toHexString(link.getHref().hashCode());
                                                getCache().put(id, link.getHref());
                                                result.append(" /c").append(id).append("\n");
                                            });
                                    entry.getLinks().stream()
                                            .filter(l -> l.getRel() != null && l.getRel().contains("open-access"))
                                            .forEach(link -> {
                                                String type = link.getType().replace("application/", "");
                                                result.append(type);
                                                String id = Integer.toHexString(link.getHref().hashCode());
                                                getCache().put(id, link.getHref());
                                                result.append(" : /d").append(id).append("\n");
                                                if ("fb2+zip".equals(type)) {
                                                    result.append("fb2").append(" : /z").append(id).append("\n");
                                                }
                                            });
                                    result.append("\n");
                                });
                                page.getLinks().stream()
                                        .filter((l) -> l.getRel().equals("next"))
                                        .forEach(lnk -> {
                                            String id = Integer.toHexString(lnk.getHref().hashCode());
                                            getCache().put(id, lnk.getHref());
                                            result.append("next : /c").append(id).append("\n");
                                        });
                            } else {
                                result.append("Nothing found");
                            }
                            handler.handle(Util.result(true, result, null));
                        })
                        .exceptionHandler(e -> {
                            handler.handle(Util.result(false, null, e));
                        });
            } else handler.handle(Util.result(false, null, new BotException(event.statusMessage())));
        }).exceptionHandler(e -> {
            handler.handle(Util.result(false, null, new BotException(e)));
        }).end();
    }
}
