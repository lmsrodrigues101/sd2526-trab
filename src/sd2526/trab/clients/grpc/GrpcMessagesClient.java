package sd2526.trab.clients.grpc;

import java.net.URI;
import java.util.List;
import java.util.function.Supplier;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;
import io.grpc.Status;

import sd2526.trab.api.Message;
import sd2526.trab.api.grpc.Messages;
import sd2526.trab.api.java.Result;
import sd2526.trab.api.java.Result.ErrorCode;
import sd2526.trab.clients.MessagesClient;
import sd2526.trab.server.util.DataModelAdaptorMessage;

import sd2526.trab.server.grpc.generated_java.GrpcMessagesGrpc;
import sd2526.trab.api.grpc.Messages.PostMessageArgs;
import sd2526.trab.api.grpc.Messages.GetInboxMessageArgs;
import sd2526.trab.api.grpc.Messages.GetAllInboxMessagesArgs;
import sd2526.trab.api.grpc.Messages.DeleteMessageArgs;
import sd2526.trab.api.grpc.Messages.SearchInboxArgs;

public class GrpcMessagesClient extends MessagesClient {

    private final ManagedChannel channel;
    private final GrpcMessagesGrpc.GrpcMessagesBlockingStub stub;

    public GrpcMessagesClient(URI serverURI) {
        this.channel = ManagedChannelBuilder.forAddress(serverURI.getHost(), serverURI.getPort())
                .usePlaintext()
                .build();
        this.stub = GrpcMessagesGrpc.newBlockingStub(channel);
    }

    @Override
    public Result<String> postMessage(String pwd, Message msg) {
        try {
            Messages.PostMessageResult res = stub.postMessage(PostMessageArgs.newBuilder()
                    .setPwd(pwd)
                    .setMessage(DataModelAdaptorMessage.Message_to_GrpcMessage(msg))
                    .build());
            return Result.ok(res.getMid());
        } catch (StatusRuntimeException sre) {
            return Result.error(statusToErrorCode(sre.getStatus()));
        }
    }

    @Override
    public Result<Message> getInboxMessage(String name, String mid, String pwd) {
        try {
            Messages.GrpcMessage res = stub.getInboxMessage(GetInboxMessageArgs.newBuilder()
                    .setName(name)
                    .setMid(mid)
                    .setPwd(pwd)
                    .build());
            return Result.ok(DataModelAdaptorMessage.GrpcMessage_to_Message(res));
        } catch (StatusRuntimeException sre) {
            return Result.error(statusToErrorCode(sre.getStatus()));
        }
    }

    @Override
    public Result<List<String>> getAllInboxMessages(String name, String pwd) {
        try {
            Messages.GetAllInboxMessagesResult res = stub.getAllInboxMessages(GetAllInboxMessagesArgs.newBuilder()
                    .setName(name)
                    .setPwd(pwd)
                    .build());
            return Result.ok(res.getMidsList());
        } catch (StatusRuntimeException sre) {
            return Result.error(statusToErrorCode(sre.getStatus()));
        }
    }

    @Override
    public Result<List<String>> searchInbox(String name, String pwd, String query) {
        try {
            Messages.SearchInboxResult res = stub.searchInbox(SearchInboxArgs.newBuilder()
                    .setName(name)
                    .setPwd(pwd)
                    .setQuery(query)
                    .build());
            return Result.ok(res.getMidsList());
        } catch (StatusRuntimeException sre) {
            return Result.error(statusToErrorCode(sre.getStatus()));
        }
    }

    @Override
    public Result<Void> removeInboxMessage(String name, String mid, String pwd) {
        try {
            stub.removeInboxMessage(Messages.RemoveInboxMessageArgs.newBuilder()
                    .setName(name)
                    .setMid(mid)
                    .setPwd(pwd)
                    .build());
            return Result.ok(null);
        } catch (StatusRuntimeException sre) {
            return Result.error(statusToErrorCode(sre.getStatus()));
        }
    }

    @Override
    public Result<Void> deleteMessage(String name, String mid, String pwd) {
        try {
            stub.deleteMessage(DeleteMessageArgs.newBuilder()
                    .setName(name)
                    .setMid(mid)
                    .setPwd(pwd)
                    .build());
            return Result.ok(null);
        } catch (StatusRuntimeException sre) {
            return Result.error(statusToErrorCode(sre.getStatus()));
        }
    }

}