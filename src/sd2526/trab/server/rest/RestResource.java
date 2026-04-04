package sd2526.trab.server.rest;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response.Status;
import sd2526.trab.api.java.Result;

public class RestResource {

    private static Status errorCodeToStatus( Result.ErrorCode error ) {
        return switch( error) {
            case NOT_FOUND -> Status.NOT_FOUND;
            case CONFLICT -> Status.CONFLICT;
            case FORBIDDEN -> Status.FORBIDDEN;
            case NOT_IMPLEMENTED -> Status.NOT_IMPLEMENTED;
            case BAD_REQUEST -> Status.BAD_REQUEST;
            default -> Status.INTERNAL_SERVER_ERROR;
        };
    }

    protected static <T> T unwrapResultOrThrow( Result<T> result ) {
        if( result.isOK() )
            return result.value();
        else
            throw new WebApplicationException( errorCodeToStatus( result.error() ) );
    }
}