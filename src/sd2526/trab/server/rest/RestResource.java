package sd2526.trab.server.rest;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response.Status;
import sd2526.trab.api.java.Result;
import sd2526.trab.api.java.Result.ErrorCode;

public abstract class RestResource {

    /**
     * Traduz os erros do nosso Result.ErrorCode para códigos HTTP do Jersey (Status).
     */
    protected static Status errorCodeToStatus(ErrorCode error) {
        return switch (error) {
            case NOT_FOUND -> Status.NOT_FOUND;
            case CONFLICT -> Status.CONFLICT;
            case FORBIDDEN -> Status.FORBIDDEN;
            case NOT_IMPLEMENTED -> Status.NOT_IMPLEMENTED;
            case BAD_REQUEST -> Status.BAD_REQUEST;
            default -> Status.INTERNAL_SERVER_ERROR;
        };
    }

    /**
     * Avalia o Result: se estiver OK, devolve o valor. Se der erro, lança a exceção HTTP correspondente.
     */
    protected static <T> T unwrapResultOrThrow(Result<T> result) {
        if (result.isOK()) {
            return result.value();
        } else {
            var status = errorCodeToStatus(result.error());
            throw new WebApplicationException(status);
        }
    }
}