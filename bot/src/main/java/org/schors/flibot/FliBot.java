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

import io.vertx.core.AbstractVerticle;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.entity.BufferedHttpEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.ssl.SSLContexts;
import org.apache.log4j.Logger;
import org.telegram.telegrambots.TelegramApiException;
import org.telegram.telegrambots.TelegramBotsApi;
import org.telegram.telegrambots.api.methods.send.SendDocument;
import org.telegram.telegrambots.api.methods.send.SendMessage;
import org.telegram.telegrambots.api.objects.Message;
import org.telegram.telegrambots.api.objects.Update;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;

import java.io.File;
import java.io.FileOutputStream;
import java.net.InetSocketAddress;
import java.util.List;


public class FliBot extends AbstractVerticle {

    //http://flibustahezeous3.onion/opds//search?searchType=authors&searchTerm=Толстой
    //http://flibustahezeous3.onion/search?searchType=books&searchTerm=криптономикон

    private static final String rootOPDS = "http://flibustahezeous3.onion";
    private static final String authorSearch = "/search?searchType=authors&searchTerm=%s";
    private static final String bookSearch = "/search?searchType=books&searchTerm=%s";

    private static final Logger log = Logger.getLogger(FliBot.class);

    private TelegramBotsApi telegram = new TelegramBotsApi();
    private HttpClientContext context = HttpClientContext.create();
    private CloseableHttpClient httpclient;

    @Override
    public void start() {

        final DBService db = DBService.createProxy(vertx, "db-service");


        InetSocketAddress socksaddr = new InetSocketAddress(config().getString("torhost"), Integer.parseInt(config().getString("torport")));
        context.setAttribute("socks.address", socksaddr);

        Registry<ConnectionSocketFactory> reg = RegistryBuilder.<ConnectionSocketFactory>create()
                .register("http", new MyConnectionSocketFactory())
                .register("https", new MySSLConnectionSocketFactory(SSLContexts.createSystemDefault())).build();
        PoolingHttpClientConnectionManager cm = new PoolingHttpClientConnectionManager(reg, new FakeDNSResolver());
        httpclient = HttpClients.custom().setConnectionManager(cm).build();

        try {
            telegram.registerBot(new TelegramLongPollingBot() {

                private Message sendReply(Update update, String res) {
                    Message result = null;
                    SendMessage message = new SendMessage()
                            .setChatId(String.valueOf(update.getMessage().getChatId()))
                            .setText(res)
                            .enableHtml(true);
                    try {
                        result = sendMessage(message);
                    } catch (TelegramApiException e) {
                        log.error(e, e);
                    }
                    return result;
                }

                private Message sendReply(Update update, SendMessage res) {
                    Message result = null;
                    res.setChatId(String.valueOf(update.getMessage().getChatId()));
                    try {
                        result = sendMessage(res);
                    } catch (TelegramApiException e) {
                        log.error(e, e);
                    }
                    return result;
                }

                private Message sendFile(Update update, SendDocument res) {
                    Message result = null;
                    res.setChatId(update.getMessage().getChatId().toString());
                    try {
                        result = sendDocument(res);
                    } catch (TelegramApiException e) {
                        log.error(e, e);
                    }
                    return result;
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
                        String cmd = update.getMessage().getText();
                        String userName = update.getMessage().getFrom().getUserName();
                        db.isRegisterdUser(userName, resgistationRes -> {
                            if (resgistationRes.succeeded() && resgistationRes.result().getBoolean("res")) {
                                if (cmd.startsWith("/a")) {
                                    getAuthor(cmd.substring(cmd.indexOf(" ") + 1).replaceAll(" ", "+"), event -> {
                                        if (event.succeeded()) {
                                            sendReply(update, (SendMessage) event.result());
                                        } else {
                                            sendReply(update, "Error happened :(");
                                        }
                                    });
                                } else if (cmd.startsWith("/b")) {
                                    getBook(cmd.substring(cmd.indexOf(" ") + 1).replaceAll(" ", "+"), event -> {
                                        if (event.succeeded()) sendReply(update, (SendMessage) event.result());
                                    });
                                } else if (cmd.startsWith("/c")) {
                                    getCmd(PageParser.toURL(cmd.replace("/c", "").split("@")[0], false), event -> {
                                        if (event.succeeded()) sendReply(update, (SendMessage) event.result());
                                    });
                                } else if (cmd.startsWith("/d")) {
                                    download(PageParser.toURL(cmd.replace("/d", "").split("@")[0], true), event -> {
                                        if (event.succeeded()) {
                                            sendFile(update, (SendDocument) event.result());
                                        } else {
                                            sendReply(update, "Error happened :(");
                                        }
                                    });
                                } else if (cmd.startsWith("/k")) {
                                    catalog(event -> {
                                        if (event.succeeded()) sendReply(update, (SendMessage) event.result());
                                    });
                                } else if (cmd.startsWith("/r")) {
                                    if (userName.equals(config().getString("admin"))) {
                                        db.registerUser(cmd.substring(cmd.indexOf(" ") + 1), res -> {
                                            sendReply(update, Boolean.toString(res.succeeded()));
                                        });
                                    }
                                } else if (cmd.startsWith("/u")) {
                                    if (userName.equals(config().getString("admin"))) {
                                        db.unregisterUser(cmd.substring(cmd.indexOf(" ") + 1), res -> {
                                            sendReply(update, Boolean.toString(res.succeeded()));
                                        });
                                    }
                                } else {
                                    getAuthor(cmd.substring(cmd.indexOf(" ") + 1), event -> {
                                        if (event.succeeded()) {
                                            sendReply(update, (SendMessage) event.result());
                                        } else {
                                            sendReply(update, "Error happened :(");
                                        }
                                    });
                                }
                            } else {
                                sendReply(update, "I do not talk to strangers");
                            }
                        });
                    }
                }
            });
        } catch (Exception e) {
            log.error(e, e);
        }
    }

    private void catalog(Handler<AsyncResult<Object>> handler) {
        vertx.executeBlocking(future -> {
            SendMessage res = doGenericRequest(rootOPDS + "/opds");
            future.complete(res);
        }, res -> {
            handler.handle(res);
        });
    }

    private void download(String url, Handler<AsyncResult<Object>> handler) {
        vertx.executeBlocking(future -> {
            HttpGet httpGet = new HttpGet(rootOPDS + url);
            try {
                CloseableHttpResponse response = httpclient.execute(httpGet, context);
                if (response.getStatusLine().getStatusCode() == 200) {
                    String fileName = "tmp";
                    if (url.contains("mobi")) {
                        String[] parts = url.split("/");
                        fileName = parts[parts.length - 2] + "." + parts[parts.length - 1];
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
    }

    private void getCmd(String cmd, Handler<AsyncResult<Object>> handler) {
        vertx.executeBlocking(future -> {
            SendMessage res = doGenericRequest(rootOPDS + cmd);
            future.complete(res);
        }, res -> {
            handler.handle(res);
        });
    }


    private void getAuthor(String author, Handler<AsyncResult<Object>> handler) {
        vertx.executeBlocking(future -> {
            SendMessage res = doGenericRequest(rootOPDS + "/opds" + String.format(authorSearch, author));
            future.complete(res);
        }, res -> {
            handler.handle(res);
        });
    }

    private void getBook(String book, Handler<AsyncResult<Object>> handler) {
        vertx.executeBlocking(future -> {
            SendMessage res = doGenericRequest(rootOPDS + "/opds" + String.format(bookSearch, book));
            future.complete(res);
        }, res -> {
            handler.handle(res);
        });
    }

    private SendMessage doGenericRequest(String url) {

        System.out.println(url);
        SendMessage sendMessage = new SendMessage();
        HttpGet httpGet = new HttpGet(url);
        try {
            CloseableHttpResponse response = httpclient.execute(httpGet, context);
            if (response.getStatusLine().getStatusCode() == 200) {
                StringBuilder sb = new StringBuilder();
                HttpEntity ht = response.getEntity();
                BufferedHttpEntity buf = new BufferedHttpEntity(ht);
                List<Entry> list = PageParser.parse(buf.getContent());
                list.stream().forEach(entry -> {
                    sb.append("<b>").append(entry.getTitle()).append("</b>");
                    if (entry.getAuthor() != null) {
                        sb.append(" (").append(entry.getAuthor()).append(")");
                    }
                    sb.append("\n");
                    entry.getLinks().stream()
                            .filter((l) -> "application/atom+xml;profile=opds-catalog".equals(l.getType()))
                            .forEach(link -> {
                                if (link.getTitle() != null) {
                                    sb.append(link.getTitle());
                                }
                                sb.append(" /c").append(PageParser.fromURL(link.getHref())).append("\n");
                            });
                    entry.getLinks().stream()
                            .filter(l -> "http://opds-spec.org/acquisition/open-access".equals(l.getRel()))
                            .forEach(link -> {
                                sb.append(link.getType().replace("application/", "")).append(" : /d").append(PageParser.fromURL(link.getHref())).append("\n");
                            });
                    sb.append("\n");
                });
                sendMessage.setText(sb.toString());
                sendMessage.enableHtml(true);
            }
        } catch (Exception e) {
            log.warn(e, e);
        }
        return sendMessage;
    }

}
