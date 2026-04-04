package sd2526.trab.clients.grpc;

import java.net.URI;
import java.util.function.Supplier;
import java.util.logging.Logger;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;
import sd2526.trab.api.java.Result;
import sd2526.trab.api.java.Result.ErrorCode;


public class GrpcClient {
    protected static final int MAX_RETRIES = 3; // [cite: 468]
    protected static final long RETRY_SLEEP = 1000; // [cite: 468]

    protected final ManagedChannel channel;
    protected final Logger Log;

    public GrpcClient(URI serverURI, Logger logger) {
        this.Log = logger;
        String host = serverURI.getHost();
        int port = serverURI.getPort();

        // Se o getPort() devolver -1 (não especificado), podes definir um default
        if (port == -1) {
            port = 9090;
        }

        this.channel = ManagedChannelBuilder.forAddress(host, port)
                .usePlaintext() // Para desenvolvimento local sem SSL
                .build();
    }

    protected <T> Result<T> reTry(Supplier<Result<T>> func) {
        for (int i = 0; i < MAX_RETRIES; i++) {
            try {
                return func.get();
            } catch (StatusRuntimeException e) {
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

}



