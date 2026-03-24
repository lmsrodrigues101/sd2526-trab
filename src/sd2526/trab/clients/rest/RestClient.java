package sd2526.trab.clients.rest;

import java.net.URI;
import java.util.function.Supplier;

import io.grpc.Status;
import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.GenericType;
import jakarta.ws.rs.core.Response;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.ClientProperties;

import sd2526.trab.api.java.Result;

import sd2526.trab.api.java.Result.ErrorCode;

public class RestClient {
    private static final int MAX_RETRIES = 3;
    private static final long RETRY_SLEEP = 5000;
    private static final int READ_TIMEOUT = 5000;
    private static final int CONNECT_TIMEOUT = 5000;

    protected final URI serverURI;
    protected final Client client;
    protected final ClientConfig config;
    protected WebTarget target;

    public RestClient(URI serverURI) {
        this.serverURI = serverURI;
        this.config = new ClientConfig();

        config.property(ClientProperties.READ_TIMEOUT, READ_TIMEOUT);
        config.property(ClientProperties.CONNECT_TIMEOUT, CONNECT_TIMEOUT);

        this.client = ClientBuilder.newClient(config);
        this.target = client.target(serverURI);
    }

    /**
     * Executa um pedido REST e tenta novamente em caso de falha de comunicação (ProcessingException).
     */
    protected <T> Result<T> reTry(Supplier<Result<T>> func) {
        for (int i = 0; i < MAX_RETRIES; i++) {
            try {
                return func.get();
            } catch (ProcessingException x) {
                try {
                    Thread.sleep(RETRY_SLEEP);
                } catch (InterruptedException ignored) {
                }
            } catch (Exception x) {
                return Result.error(ErrorCode.INTERNAL_ERROR);
            }
        }
        return Result.error(ErrorCode.TIMEOUT);
    }

    protected <T> Result<T> processResponse(Response r, Class<T> entityType) {
        try {
            var status = r.getStatusInfo().toEnum();
            if (status == Response.Status.OK && r.hasEntity()) {
                return Result.ok(r.readEntity(entityType));
            } else
                if( status == Response.Status.NO_CONTENT)
                    return Result.ok();
            return Result.error(getErrorCodeFrom(status.getStatusCode()));
        } finally {
            r.close();
        }
    }

    protected <T> Result<T> processResponse(Response r, GenericType<T> entityType) {
        try {
            var status = r.getStatusInfo().toEnum();
            if (status == Response.Status.OK && r.hasEntity()) {
                return Result.ok(r.readEntity(entityType));
            } else
            if( status == Response.Status.NO_CONTENT)
                return Result.ok();
            return Result.error(getErrorCodeFrom(status.getStatusCode()));
        } finally {
            r.close();
        }
    }

    /**
     * Traduz o status HTTP devolvido para o nosso Result.ErrorCode interno.
     */
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