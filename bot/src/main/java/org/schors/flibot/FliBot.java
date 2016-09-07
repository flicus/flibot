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

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.file.OpenOptions;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.net.ProxyOptions;
import io.vertx.core.net.ProxyType;
import io.vertx.core.streams.Pump;
import org.apache.log4j.Logger;
import org.schors.flibot.opds.Page;
import org.schors.flibot.opds.PageParser;
import org.schors.vertx.telegram.LongPollingReceiver;
import org.schors.vertx.telegram.TelegramBot;
import org.schors.vertx.telegram.TelegramOptions;
import org.telegram.telegrambots.api.methods.ActionType;
import org.telegram.telegrambots.api.methods.send.SendChatAction;
import org.telegram.telegrambots.api.methods.send.SendDocument;
import org.telegram.telegrambots.api.methods.send.SendMessage;
import org.telegram.telegrambots.api.objects.Message;
import org.telegram.telegrambots.api.objects.Update;
import org.telegram.telegrambots.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.api.objects.replykeyboard.buttons.KeyboardRow;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class FliBot extends AbstractVerticle {

    //http://flibustahezeous3.onion/opds//search?searchType=authors&searchTerm=Толстой
    //http://flibustahezeous3.onion/search?searchType=books&searchTerm=криптономикон

    private static final String rootOPDStor = "flibustahezeous3.onion";
    private static final String rootOPDShttp = "flibusta.is";
    private static final String authorSearch = "/search?searchType=authors&searchTerm=%s";
    private static final String bookSearch = "/search?searchType=books&searchTerm=%s";

    private static final Logger log = Logger.getLogger(FliBot.class);

    private TelegramBot bot;
    private HttpClient httpclient;
    private DBService db;
    private Cache<String, String> urlCache;
    private Map<String, Search> searches = new ConcurrentHashMap<>();
    private String rootOPDS;

    @Override
    public void start() {

        db = DBService.createProxy(vertx, "db-service");
        urlCache = CacheBuilder.newBuilder().maximumSize(1000).build();

        boolean usetor = config().getBoolean("usetor");

        HttpClientOptions httpOptions = new HttpClientOptions()
                .setTrustAll(true)
                .setIdleTimeout(50)
                .setMaxPoolSize(100)
                .setDefaultHost(usetor ? rootOPDStor : rootOPDShttp)
                .setDefaultPort(80)
                .setLogActivity(true);

        if (usetor) {
            rootOPDS = rootOPDStor;
            httpOptions.setProxyOptions(new ProxyOptions()
                    .setType(ProxyType.SOCKS5)
                    .setHost(config().getString("torhost"))
                    .setPort(Integer.valueOf(config().getString("torport"))));
        } else {
            rootOPDS = rootOPDShttp;
        }
        httpclient = vertx.createHttpClient(httpOptions);

        TelegramOptions telegramOptions = new TelegramOptions()
                .setBotName(config().getString("name"))
                .setBotToken(config().getString("token"));

        bot = TelegramBot.create(vertx, telegramOptions)
                .receiver(new LongPollingReceiver().onUpdate(update -> {
                    if (update.hasMessage() && update.getMessage().hasText()) {
                        sendBusy(update);
                        String cmd = update.getMessage().getText();
                        String userName = update.getMessage().getFrom().getUserName();
                        log.warn("onUpdate: " + cmd + ", " + userName);
                        db.isRegisterdUser(userName, registrationRes -> {
                            if (registrationRes.succeeded() && registrationRes.result().getBoolean("res")) {
                                if (cmd.startsWith("/author")) {
                                    Search search = searches.get(userName);
                                    if (search != null) {
                                        searches.remove(userName);
                                        getAuthor(search.getToSearch(), event -> {
                                            if (event.succeeded()) {
                                                sendReply(update, (SendMessageList) event.result());
                                            } else {
                                                sendReply(update, "Error happened :(");
                                            }
                                        });
                                    } else {
                                        search = new Search();
                                        search.setSearchType(SearchType.AUTHOR);
                                        searches.put(userName, search);
                                        sendReply(update, "Please enter the author name to search");
                                    }
                                } else if (cmd.startsWith("/book")) {
                                    Search search = searches.get(userName);
                                    if (search != null) {
                                        searches.remove(userName);
                                        getBook(search.getToSearch(), event -> {
                                            if (event.succeeded()) {
                                                sendReply(update, (SendMessageList) event.result());
                                            } else {
                                                sendReply(update, "Error happened :(");
                                            }
                                        });
                                    } else {
                                        search = new Search();
                                        search.setSearchType(SearchType.BOOK);
                                        searches.put(userName, search);
                                        sendReply(update, "Please enter the book name to search");
                                    }
                                } else if (cmd.startsWith("/c")) {
                                    String url = urlCache.getIfPresent(normalizeCmd(cmd));
                                    if (url != null) {
                                        getCmd(url, event -> {
                                            if (event.succeeded()) sendReply(update, (SendMessageList) event.result());
                                        });
                                    } else {
                                        sendReply(update, "Expired command");
                                    }
                                } else if (cmd.startsWith("/d")) {
                                    String url = urlCache.getIfPresent(normalizeCmd(cmd));
                                    if (url != null) {
                                        download(url, event -> {
                                            if (event.succeeded()) {
                                                sendFile(update, (SendDocument) event.result());
                                            } else {
                                                sendReply(update, "Error happened :(");
                                            }
                                        });
                                    } else {
                                        sendReply(update, "Expired command");
                                    }
                                } else if (cmd.startsWith("/z")) {
                                    String url = urlCache.getIfPresent(normalizeCmd(cmd));
                                    if (url != null) {
                                        downloadz(url, event -> {
                                            if (event.succeeded()) {
                                                sendFile(update, (SendDocument) event.result());
                                            } else {
                                                sendReply(update, "Error happened :(");
                                            }
                                        });
                                    } else {
                                        sendReply(update, "Expired command");
                                    }
                                } else if (cmd.startsWith("/k")) {
                                    catalog(event -> {
                                        if (event.succeeded()) sendReply(update, (SendMessageList) event.result());
                                    });
                                } else if (cmd.startsWith("/r")) {
                                    if (userName.equals(config().getString("admin"))) {
                                        db.registerUser(normalizeCmd(cmd), res -> {
                                            sendReply(update, Boolean.toString(res.succeeded()));
                                        });
                                    }
                                } else if (cmd.startsWith("/u")) {
                                    if (userName.equals(config().getString("admin"))) {
                                        db.unregisterUser(normalizeCmd(cmd), res -> {
                                            sendReply(update, Boolean.toString(res.succeeded()));
                                        });
                                    }
                                } else {
                                    Search search = searches.get(userName);
                                    if (search != null) {
                                        searches.remove(userName);
                                        switch (search.getSearchType()) {
                                            case AUTHOR: {
                                                getAuthor(cmd.trim().replaceAll(" ", "+"), event -> {
                                                    if (event.succeeded()) {
                                                        sendReply(update, (SendMessageList) event.result());
                                                    } else {
                                                        sendReply(update, "Error happened :(");
                                                    }
                                                });
                                                break;
                                            }
                                            case BOOK: {
                                                getBook(cmd.trim().replaceAll(" ", "+"), event -> {
                                                    if (event.succeeded()) {
                                                        sendReply(update, (SendMessageList) event.result());
                                                    } else {
                                                        sendReply(update, "Error happened :(");
                                                    }
                                                });
                                                break;
                                            }
                                        }
                                    } else {
                                        search = new Search();
                                        search.setToSearch(cmd.trim().replaceAll(" ", "+"));
                                        searches.put(userName, search);
                                        KeyboardButton authorButton = new KeyboardButton();
                                        authorButton.setText("/author");
                                        KeyboardButton bookButton = new KeyboardButton();
                                        bookButton.setText("/book");
                                        KeyboardRow keyboardRow = new KeyboardRow();
                                        keyboardRow.add(authorButton);
                                        keyboardRow.add(bookButton);
                                        List<KeyboardRow> keyboardRows = new ArrayList<KeyboardRow>();
                                        keyboardRows.add(keyboardRow);
                                        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
                                        keyboardMarkup.setKeyboard(keyboardRows);
                                        keyboardMarkup.setResizeKeyboard(true);
                                        keyboardMarkup.setSelective(true);
                                        SendMessage sendMessage = new SendMessage();
                                        sendMessage.setChatId(update.getMessage().getChatId());
                                        sendMessage.setReplyMarkup(keyboardMarkup);
                                        sendMessage.setText("What to search, author or book?");
                                        sendReply(update, sendMessage);
                                    }
                                }
                            } else {
                                sendReply(update, "I do not talk to strangers");
                            }
                        });
                    }
                }))
                .start();
    }

    private void downloadz(String url, Handler<AsyncResult<Object>> handler) {
        httpclient.get(url, res -> {
            if (res.statusCode() == 200) {
                try {
                    ZipStream zipStream = new ZipStream(res);
                    File book = File.createTempFile("flibot_" + Long.toHexString(System.currentTimeMillis()), null);
                    vertx.fileSystem().open(book.getAbsolutePath(), new OpenOptions().setWrite(true), event -> {
                        if (event.succeeded()) {
                            Pump.pump(zipStream
                                            .endHandler(done -> handler.handle(createResult(true, new SendDocument().setNewDocument(book).setCaption("book"), null)))
                                            .exceptionHandler(e -> handler.handle(createResult(false, null, e))),
                                    event.result())
                                    .start();
                        } else {
                            handler.handle(createResult(false, null, event.cause()));
                        }
                    });
                } catch (Exception e) {
                    handler.handle(createResult(false, null, e));
                }
            }
        }).exceptionHandler(e -> handler.handle(createResult(false, null, e)));
    }

    private void download(String url, Handler<AsyncResult<Object>> handler) {
        httpclient.get(url, res -> {
            if (res.statusCode() == 200) {
                try {
                    File book = File.createTempFile("flibot_" + Long.toHexString(System.currentTimeMillis()), null);
                    vertx.fileSystem().open(book.getAbsolutePath(), new OpenOptions().setWrite(true), event -> {
                        if (event.succeeded()) {
                            Pump.pump(res
                                            .endHandler(done -> handler.handle(createResult(true, new SendDocument().setNewDocument(book).setCaption("book"), null)))
                                            .exceptionHandler(e -> handler.handle(createResult(false, null, e))),
                                    event.result())
                                    .start();
                        } else {
                            handler.handle(createResult(false, null, event.cause()));
                        }
                    });
                } catch (Exception e) {
                    handler.handle(createResult(false, null, e));
                }
            }
        }).exceptionHandler(e -> handler.handle(createResult(false, null, e)));
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

    private void getCmd(String url, Handler<AsyncResult<Object>> handler) {
        doGenericRequest(rootOPDS + url, event -> handler.handle(event));
    }

    private void getAuthor(String author, Handler<AsyncResult<Object>> handler) {
        doGenericRequest(rootOPDS + "/opds" + String.format(authorSearch, author), event -> handler.handle(event));
    }

    private void getBook(String book, Handler<AsyncResult<Object>> handler) {
        doGenericRequest(rootOPDS + "/opds" + String.format(bookSearch, book), event -> handler.handle(event));
    }

    private void catalog(Handler<AsyncResult<Object>> handler) {
        doGenericRequest(rootOPDS + "/opds", event -> handler.handle(event));
    }

    private void doGenericRequest(String url, Handler<AsyncResult<Object>> handler) {
        SendMessageList result = new SendMessageList(4096);
        httpclient.get(url, event -> {
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
                                                urlCache.put(id, link.getHref());
                                                result.append(" /c").append(id).append("\n");
                                            });
                                    entry.getLinks().stream()
                                            .filter(l -> l.getRel() != null && l.getRel().contains("open-access"))
                                            .forEach(link -> {
                                                String type = link.getType().replace("application/", "");
                                                result.append(type);
                                                String id = Integer.toHexString(link.getHref().hashCode());
                                                urlCache.put(id, link.getHref());
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
                                            urlCache.put(id, lnk.getHref());
                                            result.append("next : /c").append(id).append("\n");
                                        });
                            } else {
                                result.append("Nothing found");
                            }
                            handler.handle(createResult(true, result, null));
                        })
                        .exceptionHandler(e -> {
                            handler.handle(createResult(false, null, e));
                        });
            } else handler.handle(createResult(false, null, new BotException(event.statusMessage())));
        });
    }

    private String normalizeCmd(String cmd) {
        return cmd.split("@")[0].substring(2).trim().replaceAll(" ", "+");
    }

    private void sendReply(Update update, String res) {
        bot.sendMessage(new SendMessage()
                .setChatId(update.getMessage().getChatId())
                .setText(res)
                .enableHtml(true));
    }

    private void sendReply(Update update, SendMessage res) {
        res.setChatId(update.getMessage().getChatId());
        bot.sendMessage(res);
    }

    private Message sendReply(Update update, SendMessageList res) {
        Message result = null;
        for (SendMessage sm : res.getMessages()) {
            sm.setChatId(update.getMessage().getChatId());
            bot.sendMessage(sm);
        }
        return result;
    }

    private Message sendFile(Update update, SendDocument res) {
        Message result = null;
        res.setChatId(update.getMessage().getChatId());
        bot.sendDocument(res);
        return result;
    }

    private void sendBusy(Update update) {
        bot.sendChatAction(new SendChatAction()
                .setChatId(update.getMessage().getChatId())
                .setAction(ActionType.UPLOADDOCUMENT));
    }

    private AsyncResult createResult(boolean success, Object result, Throwable e) {
        return new AsyncResult() {
            @Override
            public Object result() {
                return result;
            }

            @Override
            public Throwable cause() {
                return e;
            }

            @Override
            public boolean succeeded() {
                return success;
            }

            @Override
            public boolean failed() {
                return !success;
            }
        };
    }

}
