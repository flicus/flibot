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

import com.google.common.cache.Cache;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpClient;
import io.vertx.core.json.JsonObject;
import org.schors.flibot.*;
import org.schors.flibot.opds.Page;
import org.schors.flibot.opds.PageParser;
import org.schors.vertx.telegram.bot.commands.Command;
import org.telegram.telegrambots.api.methods.send.SendDocument;
import org.telegram.telegrambots.api.methods.send.SendMessage;
import org.telegram.telegrambots.api.objects.Message;
import org.telegram.telegrambots.api.objects.Update;

import java.util.Map;

public abstract class FlibotCommand extends Command {

    public static final String authorSearch = "/search?searchType=authors&searchTerm=%s";
    public static final String bookSearch = "/search?searchType=books&searchTerm=%s";

    public FlibotCommand(String regexp) {
        super(regexp);
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

    protected DBService getDB() {
        return (DBService) getBot().getFacility(Util.DB);
    }

    protected JsonObject getConfig() {
        return (JsonObject) getBot().getFacility(Util.CONFIG);
    }

    protected void sendReply(Update update, String res) {
        getBot().sendMessage(new SendMessage()
                .setChatId(update.getMessage().getChatId())
                .setText(res)
                .enableHtml(true));
    }

    protected void sendReply(Update update, SendMessage res) {
        res.setChatId(update.getMessage().getChatId());
        getBot().sendMessage(res);
    }

    protected Message sendReply(Update update, SendMessageList res) {
        Message result = null;
        for (SendMessage sm : res.getMessages()) {
            sm.setChatId(update.getMessage().getChatId());
            getBot().sendMessage(sm);
        }
        return result;
    }

    protected Message sendFile(Update update, SendDocument res) {
        Message result = null;
        res.setChatId(update.getMessage().getChatId());
        getBot().sendDocument(res);
        return result;
    }

    protected void doGenericRequest(String url, Handler<AsyncResult<Object>> handler) {
        SendMessageList result = new SendMessageList(4096);
        getClient().get(url, event -> {
            if (event.statusCode() == 200) {
                event
                        .bodyHandler(buffer -> {
                            Page page = PageParser.parse(new VertxBufferInputStream(buffer));
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
                            handler.handle(Util.createResult(true, result, null));
                        })
                        .exceptionHandler(e -> {
                            handler.handle(Util.createResult(false, null, e));
                        });
            } else handler.handle(Util.createResult(false, null, new BotException(event.statusMessage())));
        });
    }
}
