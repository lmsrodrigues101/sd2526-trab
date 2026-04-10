package sd2526.trab.server.util;

import io.grpc.Status;
import sd2526.trab.api.java.Result.ErrorCode;

public class GrpcController {

    protected static Throwable errorCodeToStatus(ErrorCode error) {
        var status = switch (error) {
            case NOT_FOUND -> Status.NOT_FOUND;
            case CONFLICT -> Status.ALREADY_EXISTS;
            case FORBIDDEN -> Status.PERMISSION_DENIED;
            case NOT_IMPLEMENTED -> Status.UNIMPLEMENTED;
            case BAD_REQUEST -> Status.INVALID_ARGUMENT;
            default -> Status.INTERNAL;
        };
        return status.asException();
    }
}