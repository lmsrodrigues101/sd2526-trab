package sd2526.trab.server.grpc;

import java.util.function.Function;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import sd2526.trab.api.java.Result;
import sd2526.trab.api.java.Result.ErrorCode;

public abstract class GrpcController {

    protected static Throwable errorCodeToStatus(ErrorCode error) {
        var status = switch (error) {
            case NOT_FOUND -> Status.NOT_FOUND;
            case CONFLICT -> Status.ALREADY_EXISTS;
            case FORBIDDEN -> Status.PERMISSION_DENIED;
            case NOT_IMPLEMENTED -> Status.UNIMPLEMENTED;
            case BAD_REQUEST -> Status.INVALID_ARGUMENT;
            default -> Status.INTERNAL;
        };
        return status.withDescription(error.toString()).asException();
    }

    protected <T, R> void toGrpcResult(StreamObserver<R> responseObserver, Result<T> result, Function<T, R> mapper) {
        if (result.isOK()) {
            R reply = mapper.apply(result.value());
            if (reply != null) {
                responseObserver.onNext(reply);
            }
            responseObserver.onCompleted();
        } else {
            responseObserver.onError(errorCodeToStatus(result.error()));
        }
    }
}