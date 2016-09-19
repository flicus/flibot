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
import io.vertx.core.AbstractVerticle;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.net.ProxyOptions;
import io.vertx.core.net.ProxyType;
import org.apache.log4j.Logger;
import org.schors.flibot.commands.*;
import org.schors.vertx.telegram.bot.LongPollingReceiver;
import org.schors.vertx.telegram.bot.TelegramBot;
import org.schors.vertx.telegram.bot.TelegramOptions;
import org.schors.vertx.telegram.bot.commands.CommandManager;
import org.telegram.telegrambots.api.methods.ActionType;
import org.telegram.telegrambots.api.methods.send.SendChatAction;
import org.telegram.telegrambots.api.methods.send.SendMessage;
import org.telegram.telegrambots.api.objects.Update;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class FliBot extends AbstractVerticle {

    //http://flibustahezeous3.onion/opds//search?searchType=authors&searchTerm=Толстой
    //http://flibustahezeous3.onion/search?searchType=books&searchTerm=криптономикон

    private static final String rootOPDStor = "flibustahezeous3.onion";
    private static final String rootOPDShttp = "flibusta.is";

    private static final Logger log = Logger.getLogger(FliBot.class);

    private TelegramBot bot;
    private HttpClient httpclient;
    private Storage db;
    private Cache<String, String> urlCache;
    private Map<String, Search> searches = new ConcurrentHashMap<>();
    private CommandManager cm;

    @Override
    public void start() {

        cm = new CommandManager()
                .addCommand(new GetAuthorCommand())
                .addCommand(new GetBookCommand())
                .addCommand(new CatalogCommand())
                .addCommand(new RegisterUserCommand())
                .addCommand(new UnregisterUserCommand())
                .addCommand(new DownloadCommand())
                .addCommand(new DownloadZipCommand())
                .addCommand(new GetCmdCommand())
                .setDefaultCommand(new DefaultCommand());

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
                            .setType(ProxyType.SOCKS5)
                            .setHost(config().getString("torhost"))
                            .setPort(Integer.valueOf(config().getString("torport"))));
        }
        httpclient = vertx.createHttpClient(httpOptions);

        TelegramOptions telegramOptions = new TelegramOptions()
                .setBotName(config().getString("name"))
                .setBotToken(config().getString("token"));

        bot = TelegramBot.create(vertx, telegramOptions, cm)
                .receiver(new LongPollingReceiver().onUpdate(update -> {
                    if (update.hasMessage() && update.getMessage().hasText()) {
                        sendBusy(update);
                        String text = update.getMessage().getText();
                        String userName = update.getMessage().getFrom().getUserName();
                        log.warn("onUpdate: " + text + ", " + userName);
                        db.isRegisteredUser(userName, registrationRes -> {
                            if (registrationRes.succeeded() && registrationRes.result().getBoolean("res")) {
                                cm.execute(text, cm.createContext(update));
                            } else {
                                sendReply(update, "I do not talk to strangers");
                            }
                        });
                    }
                }))
                .addFacility(Util.HTTP_CLIENT, httpclient)
                .addFacility(Util.CACHE, urlCache)
                .addFacility(Util.SEARCHES, searches)
                .addFacility(Util.DB, db)
                .addFacility(Util.CONFIG, config())
                .start();
    }

    private void sendReply(Update update, String res) {
        bot.sendMessage(new SendMessage()
                .setChatId(update.getMessage().getChatId())
                .setText(res)
                .enableHtml(true));
    }

    private void sendBusy(Update update) {
        bot.sendChatAction(new SendChatAction()
                .setChatId(update.getMessage().getChatId())
                .setAction(ActionType.UPLOADDOCUMENT));
    }
}
