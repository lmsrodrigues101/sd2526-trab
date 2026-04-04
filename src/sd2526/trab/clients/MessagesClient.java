package sd2526.trab.clients;

import java.util.List;
import sd2526.trab.api.Message;
import sd2526.trab.api.java.Messages;
import sd2526.trab.api.java.Result;

public abstract class MessagesClient extends ClientOperations implements Messages {

    abstract public Result<String> postMessage(String pwd, Message msg);

    abstract public Result<Message> getInboxMessage(String name, String mid, String pwd);

    abstract public Result<List<String>> getAllInboxMessages(String name, String pwd);

    abstract public Result<List<String>> searchInbox(String name, String pwd, String query);

    abstract public Result<Void> removeInboxMessage(String name, String mid, String pwd);

    abstract public Result<Void> deleteMessage(String name, String mid, String pwd);
}