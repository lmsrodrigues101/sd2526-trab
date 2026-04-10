package sd2526.trab.clients.messages.grpc;

import java.net.URI;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;

import sd2526.trab.api.Message;
import sd2526.trab.api.grpc.Messages;
import sd2526.trab.api.java.Result;
import sd2526.trab.api.java.Result.ErrorCode;
import sd2526.trab.clients.messages.java.MessagesClient;
import sd2526.trab.api.grpc.DataModelAdaptorMessage;

import sd2526.trab.api.grpc.GrpcMessagesGrpc;
import sd2526.trab.api.grpc.Messages.PostMessageArgs;
import sd2526.trab.api.grpc.Messages.GetInboxMessageArgs;
import sd2526.trab.api.grpc.Messages.GetAllInboxMessagesArgs;
import sd2526.trab.api.grpc.Messages.DeleteMessageArgs;
import sd2526.trab.api.grpc.Messages.SearchInboxArgs;

public class GrpcMessagesClient extends MessagesClient {

    private static final ConcurrentHashMap<String, ManagedChannel> channels = new ConcurrentHashMap<>();

    private final ManagedChannel channel;
    private final GrpcMessagesGrpc.GrpcMessagesBlockingStub stub;

    public GrpcMessagesClient(URI serverURI) {
        String target = serverURI.getHost() + ":" + serverURI.getPort();
        this.channel = channels.computeIfAbsent(target, k ->
                ManagedChannelBuilder.forAddress(serverURI.getHost(), serverURI.getPort())
                        .usePlaintext()
                        .build()
        );
        this.stub = GrpcMessagesGrpc.newBlockingStub(this.channel);
    }

    @Override
    public Result<String> postMessage(String pwd, Message msg) {
        return grpcRetry(() ->
                stub.postMessage(PostMessageArgs.newBuilder()
                        .setPwd(pwd)
                        .setMessage(DataModelAdaptorMessage.Message_to_GrpcMessage(msg))
                        .build()).getMid()
        );
    }

    @Override
    public Result<Message> getInboxMessage(String name, String mid, String pwd) {
        return grpcRetry(() -> DataModelAdaptorMessage.GrpcMessage_to_Message(
                stub.getInboxMessage(GetInboxMessageArgs.newBuilder()
                        .setName(name).setMid(mid).setPwd(pwd).build())
        ));
    }

    @Override
    public Result<List<String>> getAllInboxMessages(String name, String pwd) {
        return grpcRetry(() ->
                stub.getAllInboxMessages(GetAllInboxMessagesArgs.newBuilder()
                        .setName(name).setPwd(pwd).build()).getMidsList()
        );
    }

    @Override
    public Result<List<String>> searchInbox(String name, String pwd, String query) {
        return grpcRetry(() ->
                stub.searchInbox(SearchInboxArgs.newBuilder()
                        .setName(name).setPwd(pwd).setQuery(query).build()).getMidsList()
        );
    }

    @Override
    public Result<Void> removeInboxMessage(String name, String mid, String pwd) {
        return grpcRetry(() -> {
            stub.removeInboxMessage(Messages.RemoveInboxMessageArgs.newBuilder()
                    .setName(name).setMid(mid).setPwd(pwd).build());
            return null;
        });
    }

    @Override
    public Result<Void> deleteMessage(String name, String mid, String pwd) {
        return grpcRetry(() -> {
            stub.deleteMessage(DeleteMessageArgs.newBuilder()
                    .setName(name).setMid(mid).setPwd(pwd).build());
            return null;
        });
    }


    private <T> Result<T> grpcRetry(java.util.function.Supplier<T> call) {
        for (int i = 0; i < MAX_RETRIES; i++) {
            try {
                return Result.ok(call.get());
            } catch (StatusRuntimeException e) {
                if (e.getStatus().getCode() == io.grpc.Status.Code.UNAVAILABLE ||
                        e.getStatus().getCode() == io.grpc.Status.Code.DEADLINE_EXCEEDED) {
                    try { Thread.sleep(RETRY_SLEEP); } catch (InterruptedException ignored) {}
                    continue;
                }
                return Result.error(statusToErrorCode(e.getStatus()));
            } catch (Exception e) {
                return Result.error(ErrorCode.INTERNAL_ERROR);
            }
        }
        return Result.error(ErrorCode.TIMEOUT);
    }
}