package sd2526.trab.clients;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.client.Invocation.Builder;
import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.client.Entity;
import java.util.logging.Logger;
import sd2526.trab.api.java.Result.ErrorCode;
import io.grpc.Status;

public class ClientOperations {
    private static Logger Log = Logger.getLogger(ClientOperations.class.getName());

    // FIX: Baixados os limites. 10 retries a 5s davam 50s.
    protected static final int MAX_RETRIES = 5;
    protected static final int READ_TIMEOUT = 10000;
    protected static final int RETRY_SLEEP = 1000;
    protected static final int CONNECT_TIMEOUT = 10000;

    public ClientOperations() {
    }

    public Response executeOperationPost(Builder req, Entity<?> e) {
        for (int i = 0; i < MAX_RETRIES; i++) {
            try {
                return req.post(e);
            } catch (ProcessingException x) {
                Log.info(x.getMessage());
                try {
                    Thread.sleep(RETRY_SLEEP);
                } catch (InterruptedException ie) {
                }
            } catch (Exception x) {
                x.printStackTrace();
            }
        }
        return null;
    }

    public Response executeOperationGet(Builder req) {
        for (int i = 0; i < MAX_RETRIES; i++) {
            try {
                return req.get();
            } catch (ProcessingException x) {
                Log.info(x.getMessage());
                try {
                    Thread.sleep(RETRY_SLEEP);
                } catch (InterruptedException e) {
                }
            } catch (Exception x) {
                x.printStackTrace();
            }
        }
        return null;
    }

    public Response executeOperationPut(Builder req, Entity<?> e) {
        for (int i = 0; i < MAX_RETRIES; i++) {
            try {
                return req.put(e);
            } catch (ProcessingException x) {
                Log.info(x.getMessage());
                try {
                    Thread.sleep(RETRY_SLEEP);
                } catch (InterruptedException ie) {
                }
            } catch (Exception x) {
                x.printStackTrace();
            }
        }
        return null;
    }

    public Response executeOperationDelete(Builder req) {
        for (int i = 0; i < MAX_RETRIES; i++) {
            try {
                return req.delete();
            } catch (ProcessingException x) {
                Log.info(x.getMessage());
                try {
                    Thread.sleep(RETRY_SLEEP);
                } catch (InterruptedException ie) {
                }
            } catch (Exception x) {
                x.printStackTrace();
            }
        }
        return null;
    }

    public static ErrorCode getErrorCodeFrom(int status) {
        return switch (status) {
            case 200, 204, 209 -> ErrorCode.OK; // FIX: Adicionado 204 (NO_CONTENT) que é vital para Deletes REST
            case 409 -> ErrorCode.CONFLICT;
            case 403 -> ErrorCode.FORBIDDEN;
            case 404 -> ErrorCode.NOT_FOUND;
            case 400 -> ErrorCode.BAD_REQUEST;
            case 500 -> ErrorCode.INTERNAL_ERROR;
            case 501 -> ErrorCode.NOT_IMPLEMENTED;
            default -> ErrorCode.INTERNAL_ERROR;
        };
    }

    public static ErrorCode statusToErrorCode(Status status) {
        return switch (status.getCode()) {
            case OK -> ErrorCode.OK;
            case NOT_FOUND -> ErrorCode.NOT_FOUND;
            case ALREADY_EXISTS -> ErrorCode.CONFLICT;
            case PERMISSION_DENIED -> ErrorCode.FORBIDDEN;
            case INVALID_ARGUMENT -> ErrorCode.BAD_REQUEST;
            case UNIMPLEMENTED -> ErrorCode.NOT_IMPLEMENTED;
            default -> ErrorCode.INTERNAL_ERROR;
        };
    }
}