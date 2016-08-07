package org.schors.flibot;

import org.telegram.telegrambots.api.methods.send.SendMessage;

import java.util.ArrayList;
import java.util.List;

public class SendMessageList {
    private List<StringBuilder> list = new ArrayList<>();
    private int max = 2048;
    private StringBuilder current = new StringBuilder();

    public SendMessageList() {
    }

    public SendMessageList(int max) {
        this.max = max;
    }

    public SendMessageList append(String msg) {
        if ((current.length() + msg.length()) > max) {
            updateList();
        }
        current.append(msg);
        return this;
    }

    public List<SendMessage> getMessages() {
        updateList();
        List<SendMessage> res = new ArrayList<>();
        for (StringBuilder sb : list) {
            SendMessage sendMessage = new SendMessage();
            sendMessage.setText(sb.toString());
            sendMessage.enableHtml(true);
            res.add(sendMessage);
        }
        return res;
    }

    private void updateList() {
        list.add(current);
        current = new StringBuilder();
    }

}
