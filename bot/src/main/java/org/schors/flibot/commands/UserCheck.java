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
import org.schors.vertx.telegram.bot.commands.BotCommand;
import org.schors.vertx.telegram.bot.commands.CommandContext;

@BotCommand(isPreExecute = true)
public class UserCheck extends FlibotCommand {

//    private Storage getDB() {
//        return (Storage) getBot().getFacility(Util.DB);
//    }

    @Override
    public void execute(CommandContext context, Handler<Boolean> handler) {
        log.warn("## User check command executing: " + context.getUpdate());
        String userName = context.getUpdate().getMessage().getFrom().getUsername();
        if (!getDB().isRegisteredUser(userName)) {
            sendReply(context, "I do not talk to strangers");
            handler.handle(Boolean.FALSE);
        } else handler.handle(Boolean.TRUE);
    }
}
