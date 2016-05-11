/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2016 schors
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package org.schors.eva;

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
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.filter.ElementFilter;
import org.jdom2.input.SAXBuilder;
import org.telegram.telegrambots.TelegramApiException;
import org.telegram.telegrambots.TelegramBotsApi;
import org.telegram.telegrambots.api.methods.send.SendMessage;
import org.telegram.telegrambots.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.api.objects.Message;
import org.telegram.telegrambots.api.objects.Update;
import org.telegram.telegrambots.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class FliBot extends AbstractVerticle {

    private static final String rootOPDS = "http://flibustahezeous3.onion/opds";
    private static final String authorSearch = "/search?searchType=authors&searchTerm=%s";
    private static final String bookSearch = "/search?searchType=books&searchTerm=%s";

    private static final Logger log = Logger.getLogger(FliBot.class);


    private TelegramBotsApi telegram = new TelegramBotsApi();
    private HttpClientContext context = HttpClientContext.create();
    private CloseableHttpClient httpclient;
    private ExecutorService executorService = Executors.newCachedThreadPool();

    @Override
    public void start() {

        InetSocketAddress socksaddr = new InetSocketAddress("127.0.0.1", 9150);
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
                            .setText(res);
//                    .enableHtml(true);
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
//                    .enableHtml(true);
                    try {
                        result = sendMessage(res);
                    } catch (TelegramApiException e) {
                        log.error(e, e);
                    }
                    return result;
                }

                @Override
                public String getBotUsername() {
                    return "evlampia_bot";
                }

                @Override
                public String getBotToken() {
                    return "219739200:AAHXCuDWJPoRhUAjFBXFmljVJhR2uVXdmwc";
                }

                @Override
                public void onUpdateReceived(Update update) {
                    if (update.hasMessage() && update.getMessage().hasText()) {
                        String cmd = update.getMessage().getText();

                        if (cmd.startsWith("/a")) {
                            getAuthor(cmd.substring(cmd.indexOf(" ") + 1), event -> {
                                if (event.succeeded()) {
                                    sendReply(update, event.result());
                                } else {
                                    sendReply(update, "Error happened :(");
                                }
                            });
                        } else if (cmd.startsWith("/b")) {
                            getBook(cmd.substring(cmd.indexOf(" ") + 1), event -> {
                                if (event.succeeded()) sendReply(update, event.result());
                            });
                        } else if (cmd.startsWith("/c")) {
                            getCmd(cmd.replace("/c", ""), event -> {
                                if (event.succeeded()) sendReply(update, event.result());
                            });
                        } else {
                            getAuthor(cmd.substring(cmd.indexOf(" ") + 1), event -> {
                                if (event.succeeded()) {
                                    sendReply(update, event.result());
                                } else {
                                    sendReply(update, "Error happened :(");
                                }
                            });
                        }
                    }
                }
            });
        } catch (Exception e) {
            log.error(e, e);
        }
    }

    private void getCmd(String cmd, Handler<AsyncResult<SendMessage>> handler) {
        executorService.submit(() -> {
            SendMessage res = doGenericRequest(rootOPDS + cmd);
            handler.handle(new AsyncResult<SendMessage>() {
                @Override
                public SendMessage result() {
                    return res;
                }

                @Override
                public Throwable cause() {
                    return null;
                }

                @Override
                public boolean succeeded() {
                    return true;
                }

                @Override
                public boolean failed() {
                    return false;
                }
            });

        });
    }


    private void getAuthor(String author, Handler<AsyncResult<SendMessage>> handler) {
        executorService.submit(() -> {
            SendMessage res = doGenericRequest(rootOPDS + String.format(authorSearch, author));
            handler.handle(new AsyncResult<SendMessage>() {
                @Override
                public SendMessage result() {
                    return res;
                }

                @Override
                public Throwable cause() {
                    return null;
                }

                @Override
                public boolean succeeded() {
                    return true;
                }

                @Override
                public boolean failed() {
                    return false;
                }
            });

        });
    }

    private void getBook(String book, Handler<AsyncResult<SendMessage>> handler) {
        executorService.submit(() -> {
            SendMessage res = doGenericRequest(rootOPDS + String.format(bookSearch, book));
            handler.handle(new AsyncResult<SendMessage>() {
                @Override
                public SendMessage result() {
                    return res;
                }

                @Override
                public Throwable cause() {
                    return null;
                }

                @Override
                public boolean succeeded() {
                    return true;
                }

                @Override
                public boolean failed() {
                    return false;
                }
            });

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
                SAXBuilder parser = new SAXBuilder();
                Document xmlDoc = parser.build(buf.getContent());
                List<Element> elements = xmlDoc.getRootElement().getContent(new ElementFilter("entry"));
                for (Element e : elements) {
                    Element title = e.getChild("title", e.getNamespace());
                    if (title != null) {
                        sb.append(title.getText())
                                .append(" /c").append(e.getChild("link", e.getNamespace()).getAttribute("href").getValue())
                                .append("\n");
//                        sb.append("<a href=\"")
//                                .append("/c").append(e.getChild("link", e.getNamespace()).getAttribute("href").getValue())
//                                .append("\">").append(title.getText()).append("</a>")
//                                .append("\n");
                    }
                }
                sendMessage.setText(sb.toString());
            }
        } catch (Exception e) {
            log.warn(e, e);
        }
        return sendMessage;
    }

}
