package sd2526.trab.clients.grpc;

import java.util.function.Supplier;
import java.util.logging.Logger;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;
import jakarta.ws.rs.ProcessingException;
import sd2526.trab.api.java.Result;
import sd2526.trab.api.java.Result.ErrorCode;


public class GrpcClient {
    protected static final int MAX_RETRIES = 3; // [cite: 468]
    protected static final long RETRY_SLEEP = 1000; // [cite: 468]

    protected final ManagedChannel channel;
    protected final Logger Log;

    public GrpcClient(String serverURI, Logger logger) {
        this.Log = logger;
        // Assume que o URI vem no formato "grpc://host:port"
        String[] parts = serverURI.replace("grpc://", "").split(":");
        String host = parts[0];
        int port = Integer.parseInt(parts[1]);

        this.channel = ManagedChannelBuilder.forAddress(host, port)
                .usePlaintext() // Para desenvolvimento local sem SSL
                .build();
    }

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
