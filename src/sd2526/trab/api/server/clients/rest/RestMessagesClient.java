package sd2526.trab.api.server.clients.rest; // Ajusta o pacote conforme a tua estrutura

import java.net.URI;
import java.util.List;
import java.util.logging.Logger;

import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.GenericType;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.ClientProperties;

import sd2526.trab.api.Message;
import sd2526.trab.api.java.Messages;
import sd2526.trab.api.java.Result;
import sd2526.trab.api.java.Result.ErrorCode;
import sd2526.trab.api.rest.RestMessages;

public class RestMessagesClient implements Messages {

    private static Logger Log = Logger.getLogger(RestMessagesClient.class.getName());

    protected static final int READ_TIMEOUT = 5000;
    protected static final int CONNECT_TIMEOUT = 5000;
    protected static final int MAX_RETRIES = 3;
    protected static final int RETRY_SLEEP = 5000;

    final URI serverURI;
    final Client client;
    final ClientConfig config;
    final WebTarget target;

    public RestMessagesClient(URI serverURI) {
        this.serverURI = serverURI;
        this.config = new ClientConfig();

        config.property(ClientProperties.READ_TIMEOUT, READ_TIMEOUT);
        config.property(ClientProperties.CONNECT_TIMEOUT, CONNECT_TIMEOUT);

        this.client = ClientBuilder.newClient(config);
        this.target = client.target(serverURI).path(RestMessages.PATH);
    }

    @Override
    public Result<String> postMessage(String pwd, Message msg) {
        for (int i = 0; i < MAX_RETRIES; i++) {
            try {
                Response r = target.queryParam(RestMessages.PWD, pwd)
                        .request()
                        .accept(MediaType.APPLICATION_JSON)
                        .post(Entity.entity(msg, MediaType.APPLICATION_JSON));

                int status = r.getStatus();
                if (status != Status.OK.getStatusCode())
                    return Result.error(getErrorCodeFrom(status));
                else
                    return Result.ok(r.readEntity(String.class));

            } catch (ProcessingException x) {
                Log.info(x.getMessage());
                try { Thread.sleep(RETRY_SLEEP); } catch (InterruptedException e) {}
            } catch (Exception x) {
                x.printStackTrace();
            }
        }
        return Result.error(ErrorCode.TIMEOUT);
    }

    @Override
    public Result<Message> getInboxMessage(String name, String mid, String pwd) {
        for (int i = 0; i < MAX_RETRIES; i++) {
            try {
                Response r = target.path(RestMessages.MBOX).path(name).path(mid)
                        .queryParam(RestMessages.PWD, pwd)
                        .request()
                        .accept(MediaType.APPLICATION_JSON)
                        .get();

                int status = r.getStatus();
                if (status != Status.OK.getStatusCode())
                    return Result.error(getErrorCodeFrom(status));
                else
                    return Result.ok(r.readEntity(Message.class));

            } catch (ProcessingException x) {
                Log.info(x.getMessage());
                try { Thread.sleep(RETRY_SLEEP); } catch (InterruptedException e) {}
            } catch (Exception x) {
                x.printStackTrace();
            }
        }
        return Result.error(ErrorCode.TIMEOUT);
    }

    @Override
    public Result<List<String>> getAllInboxMessages(String name, String pwd) {
        // it's the same as searchInbox just the query empty
        return searchInbox(name, pwd, "");
    }

    @Override
    public Result<List<String>> searchInbox(String name, String pwd, String query) {
        for (int i = 0; i < MAX_RETRIES; i++) {
            try {
                Response r = target.path(RestMessages.MBOX).path(name)
                        .queryParam(RestMessages.PWD, pwd)
                        .queryParam(RestMessages.QUERY, query)
                        .request()
                        .accept(MediaType.APPLICATION_JSON)
                        .get();

                int status = r.getStatus();
                if (status != Status.OK.getStatusCode())
                    return Result.error(getErrorCodeFrom(status));
                else
                    return Result.ok(r.readEntity(new GenericType<List<String>>() {}));

            } catch (ProcessingException x) {
                Log.info(x.getMessage());
                try { Thread.sleep(RETRY_SLEEP); } catch (InterruptedException e) {}
            } catch (Exception x) {
                x.printStackTrace();
            }
        }
        return Result.error(ErrorCode.TIMEOUT);
    }

    @Override
    public Result<Void> removeInboxMessage(String name, String mid, String pwd) {
        for (int i = 0; i < MAX_RETRIES; i++) {
            try {
                Response r = target.path(RestMessages.MBOX).path(name).path(mid)
                        .queryParam(RestMessages.PWD, pwd)
                        .request()
                        .delete();

                int status = r.getStatus();
                if (status != Status.NO_CONTENT.getStatusCode() && status != Status.OK.getStatusCode())
                    return Result.error(getErrorCodeFrom(status));
                else
                    return Result.ok();

            } catch (ProcessingException x) {
                Log.info(x.getMessage());
                try { Thread.sleep(RETRY_SLEEP); } catch (InterruptedException e) {}
            } catch (Exception x) {
                x.printStackTrace();
            }
        }
        return Result.error(ErrorCode.TIMEOUT);
    }

    @Override
    public Result<Void> deleteMessage(String name, String mid, String pwd) {
        for (int i = 0; i < MAX_RETRIES; i++) {
            try {
                Response r = target.path(name).path(mid)
                        .queryParam(RestMessages.PWD, pwd)
                        .request()
                        .delete();

                int status = r.getStatus();
                if (status != Status.NO_CONTENT.getStatusCode() && status != Status.OK.getStatusCode())
                    return Result.error(getErrorCodeFrom(status));
                else
                    return Result.ok();

            } catch (ProcessingException x) {
                Log.info(x.getMessage());
                try { Thread.sleep(RETRY_SLEEP); } catch (InterruptedException e) {}
            } catch (Exception x) {
                x.printStackTrace();
            }
        }
        return Result.error(ErrorCode.TIMEOUT);
    }

    public static ErrorCode getErrorCodeFrom(int status) {
        return switch (status) {
            case 200, 204 -> ErrorCode.OK;
            case 409 -> ErrorCode.CONFLICT;
            case 403 -> ErrorCode.FORBIDDEN;
            case 404 -> ErrorCode.NOT_FOUND;
            case 400 -> ErrorCode.BAD_REQUEST;
            case 500 -> ErrorCode.INTERNAL_ERROR;
            case 501 -> ErrorCode.NOT_IMPLEMENTED;
            default -> ErrorCode.INTERNAL_ERROR;
        };
    }
}
