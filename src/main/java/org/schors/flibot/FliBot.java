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

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import io.vertx.core.*;
import io.vertx.core.file.OpenOptions;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.net.ProxyOptions;
import io.vertx.core.net.ProxyType;
import io.vertx.core.streams.Pump;
import org.apache.log4j.Logger;
import org.telegram.telegrambots.ApiContextInitializer;
import org.telegram.telegrambots.bots.DefaultBotOptions;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.ApiContext;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.api.methods.ActionType;
import org.telegram.telegrambots.meta.api.methods.send.SendChatAction;
import org.telegram.telegrambots.meta.api.methods.send.SendDocument;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;


public class FliBot extends AbstractVerticle {

    //http://flibustahezeous3.onion/opds//search?searchType=authors&searchTerm=
    //http://flibustahezeous3.onion/search?searchType=books&searchTerm=

    private static final String rootOPDStor = "flibustahezeous3.onion";
    private static final String rootOPDShttp = "http://flibusta.is";
    private static final String authorSearch = "/search?searchType=authors&searchTerm=%s";
    private static final String bookSearch = "/search?searchType=books&searchTerm=%s";
    private static final File tmpdir = new File(System.getenv("java.io.tmpdir"));

    private static final Logger log = Logger.getLogger(FliBot.class);

    private TelegramBotsApi telegram;
    private HttpClient httpclient;
    private Storage db;
    private Cache<String, String> urlCache;
    private Map<String, Search> searches = new ConcurrentHashMap<>();
    private String rootOPDS;
    private FileNameParser fileNameParser = new FileNameParser();

    {
        fileNameParser
                .add(new FileNameParser.FileType("mobi") {
                    @Override
                    public String parse(String url) {
                        String[] parts = url.split("/");
                        return parts[parts.length - 2] + "." + parts[parts.length - 1];
                    }
                })
                .add(new FileNameParser.FileType("\\w+\\+zip") {
                    @Override
                    public String parse(String url) {
                        String[] parts = url.split("/");
                        return parts[parts.length - 2] + "." + parts[parts.length - 1] + ".zip";
                    }
                })
                .add(new FileNameParser.FileType("djvu") {
                    @Override
                    public String parse(String url) {
                        String[] parts = url.split("/");
                        return parts[parts.length - 1] + ".djvu";
                    }
                })
                .add(new FileNameParser.FileType("pdf") {
                    @Override
                    public String parse(String url) {
                        String[] parts = url.split("/");
                        return parts[parts.length - 1] + ".pdf";
                    }
                })
                .add(new FileNameParser.FileType("doc") {
                    @Override
                    public String parse(String url) {
                        String[] parts = url.split("/");
                        return parts[parts.length - 1] + ".doc";
                    }
                })
                .add(new FileNameParser.FileType("\\w+\\+rar") {
                    @Override
                    public String parse(String url) {
                        String[] parts = url.split("/");
                        return parts[parts.length - 2] + "." + parts[parts.length - 1] + ".rar";
                    }
                })
                .add(new FileNameParser.FileType("fb2") {
                    @Override
                    public String parse(String url) {
                        String[] parts = url.split("/");
                        return parts[parts.length - 2] + "." + parts[parts.length - 1] + ".zip";
                    }
                })
        ;
    }

    @Override
    public void start() {

        ApiContextInitializer.init();
        DefaultBotOptions botOptions = ApiContext.getInstance(DefaultBotOptions.class);
        telegram = new TelegramBotsApi();
        db = new Storage(vertx, config().getString("admin"));
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
            httpOptions
                    .setProxyOptions(new ProxyOptions()
                            .setType(ProxyType.SOCKS4)
                            .setHost(config().getString("torhost"))
                            .setPort(Integer.valueOf(config().getString("torport"))));
        }
        httpclient = vertx.createHttpClient(httpOptions);

        try {
            telegram.registerBot(new TelegramLongPollingBot() {

                private void sendReply(Update update, String res) {
                    Message result = null;
                    SendMessage message = new SendMessage()
                            .setChatId(String.valueOf(update.getMessage().getChatId()))
                            .setText(res)
                            .enableHtml(true);
                    try {
                        result = execute(message);
                    } catch (TelegramApiException e) {
                        log.error(e, e);
                    }
                }

                private void sendReply(Update update, SendMessage res) {
                    Message result = null;
                    res.setChatId(String.valueOf(update.getMessage().getChatId()));
                    try {
                        result = execute(res);
                    } catch (TelegramApiException e) {
                        log.error(e, e);
                    }
//                    return result;
                }

                private Message sendReply(Update update, SendMessageList res) {
                    Message result = null;
                    for (SendMessage sm : res.getMessages()) {
                        if (sm.getText() != null && sm.getText().length() > 0) {
                            sm.setChatId(String.valueOf(update.getMessage().getChatId()));
                            try {
                                result = execute(sm);
                            } catch (TelegramApiException e) {
                                log.error(e, e);
                            }
                        }
                    }
                    return result;
                }

                private Message sendFile(Update update, SendDocument res) {
                    Message result = null;
                    res.setChatId(update.getMessage().getChatId().toString());
                    try {
                        result = execute(res);
                    } catch (TelegramApiException e) {
                        log.error(e, e);
                    }
                    return result;
                }

                private void sendBusy(Update update) {
                    SendChatAction sca = new SendChatAction();
                    sca.setChatId(update.getMessage().getChatId().toString());
                    sca.setAction(ActionType.UPLOADDOCUMENT);
                    try {
                        execute(sca);
                    } catch (TelegramApiException e) {
                        log.error(e, e);
                    }
                }

                @Override
                public String getBotUsername() {
                    return config().getString("name");
                }

                @Override
                public String getBotToken() {
                    return config().getString("token");
                }

                @Override
                public void onUpdateReceived(Update update) {
                    if (update.hasMessage() && update.getMessage().hasText()) {
                        sendBusy(update);
                        String cmd = update.getMessage().getText();
                        String userName = update.getMessage().getFrom().getUserName();
                        log.info("onUpdate: " + cmd + ", " + userName);
                        if (db.isRegisteredUser(userName)) {
                            if (cmd.startsWith("/author")) {
                                Search search = searches.get(userName);
                                log.info("onAuthorSearch: " + search);
                                if (search != null) {
                                    searches.remove(userName);
                                    getAuthor(search.getToSearch(), event -> {
                                        if (event.succeeded()) {
                                            sendReply(update, (SendMessageList) event.result());
                                        } else {
                                            sendReply(update, "Error happened :(");
                                            log.warn("onError: " + event.cause().getMessage());
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
                                log.info("onBookSearch: " + search);
                                if (search != null) {
                                    searches.remove(userName);
                                    getBook(search.getToSearch(), event -> {
                                        if (event.succeeded()) {
                                            sendReply(update, (SendMessageList) event.result());
                                        } else {
                                            sendReply(update, "Error happened :(");
                                            log.warn("onError: " + event.cause().getMessage());
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
                                        if (event.succeeded()) {
                                            sendReply(update, (SendMessageList) event.result());
                                        } else {
                                            sendReply(update, "Error happened :(");
                                            log.warn("onError: " + event.cause().getMessage());
                                        }
                                    });
                                } else {
                                    sendReply(update, "Expired command");
                                }
                            } else if (cmd.startsWith("/d")) {
                                String url = urlCache.getIfPresent(normalizeCmd(cmd));
                                if (url != null) {
                                    download(url, event -> {
                                        if (event.succeeded()) {
                                            log.info("Sending file");
                                            sendFile(update, (SendDocument) event.result());
                                        } else {
                                            sendReply(update, "Error happened :(");
                                            log.warn("onError: " + event.cause().getMessage());
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
                                            log.warn("onError: " + event.cause().getMessage());
                                        }
                                    });
                                } else {
                                    sendReply(update, "Expired command");
                                }
                            } else if (cmd.startsWith("/k")) {
                                catalog(event -> {
                                    if (event.succeeded()) {
                                        sendReply(update, (SendMessageList) event.result());
                                    } else {
                                        sendReply(update, "Error happened :(");
                                        log.warn("onError: " + event.cause().getMessage());
                                    }
                                });
                            } else if (cmd.startsWith("/r")) {
                                if (userName.equals(config().getString("admin"))) {
                                    db.registerUser(normalizeCmd(cmd));
                                }
                            } else if (cmd.startsWith("/u")) {
                                if (userName.equals(config().getString("admin"))) {
                                    db.unregisterUser(normalizeCmd(cmd));
                                }
                            } else {
                                Search search = searches.get(userName);
                                log.info("onSearch: " + search);
                                if (search != null) {
                                    searches.remove(userName);
                                    switch (search.getSearchType()) {
                                        case AUTHOR: {
                                            getAuthor(cmd.trim().replaceAll(" ", "+"), event -> {
                                                if (event.succeeded()) {
                                                    sendReply(update, (SendMessageList) event.result());
                                                } else {
                                                    sendReply(update, "Error happened :(");
                                                    log.warn("onError: " + event.cause().getMessage());
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
                                                    log.warn("onError: " + event.cause().getMessage());
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
                                    sendMessage.setChatId(update.getMessage().getChatId().toString());
                                    sendMessage.setReplyMarkup(keyboardMarkup);
                                    sendMessage.setText("What to search, author or book?");
                                    sendReply(update, sendMessage);
                                }
                            }
                        } else {
                            sendReply(update, "I do not talk to strangers");
                        }
                    }
                }
            });
        } catch (Exception e) {
            log.error(e, e);
        }
    }

    private void downloadz(String url, Handler<AsyncResult<Object>> handler) {

        log.info("Start downloading zip");
        httpclient.get(url, res -> {
            if (res.statusCode() == 200) {
                try {
                    log.info("Downloaded");
                    File book = File.createTempFile("flibot_" + Long.toHexString(System.currentTimeMillis()), null);
                    vertx.fileSystem().open(book.getAbsolutePath(), new OpenOptions().setWrite(true), event -> {
                        if (event.succeeded()) {
                            Pump.pump(res
                                            .endHandler(done -> {
                                                Promise unzipFuture = Promise.promise();
                                                Promise flushFuture = Promise.promise();
                                                event.result().flush(flushFuture);
                                                flushFuture.future()
                                                        .compose(o -> {
                                                            Promise closeFuture = Promise.promise();
                                                            event.result().close(closeFuture);
                                                            return closeFuture.future();
                                                        })
                                                        .compose(o -> {
                                                            vertx.executeBlocking(future -> {
                                                                try {
                                                                    log.info("Start unzip");
                                                                    ZipInputStream zip = new ZipInputStream(new FileInputStream(book));
                                                                    ZipEntry entry = zip.getNextEntry();
                                                                    log.info(String.format("TMP: %s, ENTRY: %s", tmpdir, entry));
//                                                        File book2 = File.createTempFile("fbunzip_" + Long.toHexString(System.currentTimeMillis()), null);
                                                                    File book2 = new File(tmpdir.getAbsolutePath() + "/" + entry.getName());
                                                                    if (book2.exists()) {
                                                                        try {
                                                                            book2.delete();
                                                                        } catch (Exception e) {
                                                                            log.warn("Unable to delete: " + book2.getName());
                                                                        }
                                                                    }
                                                                    byte[] buffer = new byte[2048];
                                                                    FileOutputStream fileOutputStream = new FileOutputStream(book2);
                                                                    int len = 0;
                                                                    while ((len = zip.read(buffer)) > 0) {
                                                                        fileOutputStream.write(buffer, 0, len);
                                                                    }
                                                                    fileOutputStream.close();
                                                                    zip.close();
                                                                    final SendDocument sendDocument = new SendDocument();
                                                                    sendDocument.setDocument(book2);
                                                                    sendDocument.setCaption(book2.getName());
                                                                    future.complete(sendDocument);
                                                                } catch (Exception e) {
                                                                    log.info("Exception on unzip", e);
                                                                    future.fail(e);
                                                                }
                                                            }, unzipFuture);
                                                        }, unzipFuture.future());
                                                handler.handle(unzipFuture.future());
//                                                unzipFuture.setHandler(handler);
                                            })
                                            .exceptionHandler(e -> {
                                                log.info("Pump eception: ", e);
                                                handler.handle(Future.failedFuture(e));

                                            }),
                                    event.result())
                                    .start();
                        } else {
                            log.info("Error on file open: ", event.cause());
                            handler.handle(Future.failedFuture(event.cause()));
                        }
                    });
                } catch (Exception e) {
                    log.info("Exception after download: ", e);
                    handler.handle(Future.failedFuture(e));
                }
            }
        }).exceptionHandler(event -> {
            log.info("Exception on download: ", event.getCause());
            handler.handle(Future.failedFuture(event.getCause()));
        }).setFollowRedirects(true).end();
    }

    private void download(String url, Handler<AsyncResult<Object>> handler) {
        log.info("Download: " + url);

        httpclient.get("gist.githubusercontent.com", "/flicus/dc1fe51afff2809a848f0ed8d6d1e558/raw/a63ec2e0bdc626226e0a997b783bd63318369910/TelegramOptions.java", res -> {
//        httpclient.get(url, res -> {
            log.info(String.format("onDownload: RC=%d, MSG=%s", res.statusCode(), res.statusMessage()));
            if (res.statusCode() == 200) {
                try {
                    String fileName = fileNameParser.parse(url);
//                    File book = File.createTempFile(fileName, null);
                    File book = new File(tmpdir.getAbsolutePath() + "/" + "t.java");
                    vertx.fileSystem().open(book.getAbsolutePath(), new OpenOptions().setWrite(true), event -> {
                        if (event.succeeded()) {
                            log.info("file opened");
                            Pump.pump(res
                                            .endHandler(done -> {
                                                log.info("pump done");
                                                event.result().close();
//                                                book.renameTo(new File(book.getParent() + "/" + fileName));
                                                handler.handle(Future.succeededFuture(
                                                        new SendDocument()
                                                                .setDocument(book)
                                                                .setCaption(fileName)
                                                ));
                                            })
                                            .exceptionHandler(e -> {
                                                log.warn(e, e);
                                                handler.handle(Future.failedFuture(e));
                                            }),
                                    event.result())
                                    .start();
                        } else {
                            log.warn(event.cause(), event.cause());
                            handler.handle(Future.failedFuture(event.cause()));
                        }
                    });
                } catch (Exception e) {
                    log.warn(e, e);
                    handler.handle(Future.failedFuture(e));
                }
            } else {
                log.warn("Unsuccesfull HTTP req: " + res.statusCode());
                handler.handle(Future.failedFuture("Error on request: " + res.statusCode()));
            }
        }).exceptionHandler(e -> {
            log.warn(e, e);
            handler.handle(Future.failedFuture(e));
        }).setFollowRedirects(true).end();
    }

    private void catalog(Handler<AsyncResult<Object>> handler) {
        doGenericRequest("/opds", handler);
    }

    private void getCmd(String url, Handler<AsyncResult<Object>> handler) {
        doGenericRequest(url, handler);
    }

    private void getAuthor(String author, Handler<AsyncResult<Object>> handler) {
        doGenericRequest("/opds" + String.format(authorSearch, author), handler);
    }

    private void getBook(String book, Handler<AsyncResult<Object>> handler) {
        doGenericRequest("/opds" + String.format(bookSearch, book), handler);
    }

    protected void doGenericRequest(String url, Handler<AsyncResult<Object>> handler) {
        SendMessageList result = new SendMessageList(4096);
        log.info("URL: " + url);
        httpclient.get(url, event -> {
            log.info(String.format("onGenericReq: RC=%d, MSG=%s", event.statusCode(), event.statusMessage()));
            if (event.statusCode() == 200) {
                event
                        .bodyHandler(buffer -> {
                            log.info("onBodyHandler");
                            Page page = PageParser.parse(new ByteArrayInputStream(buffer.getBytes()));
                            log.info("Page: " + page);
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
                            handler.handle(Future.succeededFuture(result));
                        })
                        .exceptionHandler(e -> {
                            handler.handle(Future.failedFuture(e));
                        });
            } else {
                log.warn("Unsuccesfull HTTP req: " + event.statusCode());
                handler.handle(Future.failedFuture(event.statusMessage()));
            }
        }).exceptionHandler(e -> {
            handler.handle(Future.failedFuture(e));
        }).setFollowRedirects(true).end();
    }


    private String normalizeCmd(String cmd) {
        return cmd.split("@")[0].substring(2).trim().replaceAll(" ", "+");
    }

}
