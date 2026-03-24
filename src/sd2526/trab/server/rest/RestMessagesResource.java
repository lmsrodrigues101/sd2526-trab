package sd2526.trab.server.rest;

import java.util.List;

import jakarta.inject.Singleton;
import sd2526.trab.api.Message;
import sd2526.trab.api.java.Messages;
import sd2526.trab.api.rest.RestMessages;
import sd2526.trab.server.java.JavaMessages;

@Singleton
public class RestMessagesResource extends RestResource implements RestMessages {


    final Messages impl;

    public RestMessagesResource(String domain, String usersServerUri) {
        this.impl = new JavaMessages(domain, usersServerUri);
    }

    @Override
    public String postMessage(String pwd, Message msg){
        return unwrapResultOrThrow(impl.postMessage(pwd, msg));
    }

    @Override
    public Message getMessage(String name, String mid, String pwd) {
        return unwrapResultOrThrow(impl.getInboxMessage(name, mid, pwd));
    }

    @Override
    public List<String> getMessages(String name, String pwd, String query) {
        if (query == null || query.isEmpty()) {
            return unwrapResultOrThrow(impl.getAllInboxMessages(name, pwd));
        } else {
            return unwrapResultOrThrow(impl.searchInbox(name, pwd, query));
        }
    }

    @Override
    public void removeFromUserInbox(String name, String mid, String pwd) {
        unwrapResultOrThrow(impl.removeInboxMessage(name, mid, pwd));
    }

    @Override
    public void deleteMessage(String name, String mid, String pwd) {
        unwrapResultOrThrow(impl.deleteMessage(name, mid, pwd));
    }
}