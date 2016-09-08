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

import org.schors.flibot.SendMessageList;
import org.schors.flibot.Util;
import org.schors.vertx.telegram.bot.commands.CommandContext;

public class GetCmdCommand extends FlibotCommand {

    public GetCmdCommand() {
        super("^/c");
    }

    @Override
    public void execute(String s, CommandContext commandContext) {
        String url = getCache().getIfPresent(Util.normalizeCmd(s));
        if (url != null) {
            doGenericRequest(url, event -> {
                if (event.succeeded()) {
                    sendReply(commandContext.getUpdate(), (SendMessageList) event.result());
                } else {
                    sendReply(commandContext.getUpdate(), "Error happened :(");
                }
            });
        } else {
            sendReply(commandContext.getUpdate(), "Expired command");
        }
    }
}
