package org.schors.flibot.checks;

import org.schors.flibot.Storage;
import org.schors.flibot.Util;
import org.schors.vertx.telegram.bot.commands.BotCheck;
import org.schors.vertx.telegram.bot.commands.Check;
import org.schors.vertx.telegram.bot.commands.CommandContext;

@BotCheck
public class UserCheck extends Check {

    private Storage getDB() {
        return (Storage) getBot().getFacility(Util.DB);
    }

    @Override
    public boolean execute(String text, CommandContext context) {
        boolean result = true;
        String userName = context.getUpdate().getMessage().getFrom().getUsername();
        if (!getDB().isRegisteredUser(userName)) {
            result = false;
            sendReply(context, "I do not talk to strangers");
        }

        return result;
    }
}
