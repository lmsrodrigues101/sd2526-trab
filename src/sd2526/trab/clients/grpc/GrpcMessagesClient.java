package sd2526.trab.clients.grpc;

import sd2526.trab.api.Message;
import sd2526.trab.api.java.Messages;
import sd2526.trab.api.java.Result;
import sd2526.trab.api.grpc.GrpcMessagesGrpc;
import sd2526.trab.api.grpc.DataModelAdaptorMessage;

import java.net.URI;
import java.util.List;
import java.util.logging.Logger;

public class GrpcMessagesClient extends GrpcClient implements Messages {

    private final GrpcMessagesGrpc.GrpcMessagesBlockingStub stub;

    public GrpcMessagesClient(URI serverURI) {
        super(serverURI.toString(), Logger.getLogger(GrpcMessagesClient.class.getName()));
        this.stub = GrpcMessagesGrpc.newBlockingStub(channel);
    }

    @Override
    public Result<String> postMessage(String pwd, Message msg) {
        return super.reTry(() -> {
            var grpcMsg = DataModelAdaptorMessage.Message_to_GrpcMessage(msg);

            var request = sd2526.trab.api.grpc.Messages.PostMessageArgs.newBuilder()
                    .setPwd(pwd)
                    .setMessage(grpcMsg)
                    .build();

            var response = stub.postMessage(request);
            return Result.ok(response.getMid());
        });
    }

    @Override
    public Result<Message> getInboxMessage(String name, String mid, String pwd) {
        return super.reTry(() -> {
            var request = sd2526.trab.api.grpc.Messages.GetInboxMessageArgs.newBuilder()
                    .setName(name)
                    .setMid(mid)
                    .setPwd(pwd)
                    .build();

            var response = stub.getInboxMessage(request);
            return Result.ok(DataModelAdaptorMessage.GrpcMessage_to_Message(response));
        });
    }

    @Override
    public Result<List<String>> getAllInboxMessages(String name, String pwd) {
        return super.reTry(() -> {
            var request = sd2526.trab.api.grpc.Messages.GetAllInboxMessagesArgs.newBuilder()
                    .setName(name)
                    .setPwd(pwd)
                    .build();

            var response = stub.getAllInboxMessages(request);
            return Result.ok(response.getMidsList());
        });
    }

    @Override
    public Result<Void> removeInboxMessage(String name, String mid, String pwd) {
        return super.reTry(() -> {
            var request = sd2526.trab.api.grpc.Messages.RemoveInboxMessageArgs.newBuilder()
                    .setName(name)
                    .setMid(mid)
                    .setPwd(pwd)
                    .build();

            stub.removeInboxMessage(request);
            return Result.ok();
        });
    }

    @Override
    public Result<Void> deleteMessage(String name, String mid, String pwd) {
        return super.reTry(() -> {
            var request = sd2526.trab.api.grpc.Messages.DeleteMessageArgs.newBuilder()
                    .setName(name)
                    .setMid(mid)
                    .setPwd(pwd)
                    .build();

            stub.deleteMessage(request);
            return Result.ok();
        });
    }

    @Override
    public Result<List<String>> searchInbox(String name, String pwd, String query) {
        return super.reTry(() -> {
            var request = sd2526.trab.api.grpc.Messages.SearchInboxArgs.newBuilder()
                    .setName(name)
                    .setPwd(pwd)
                    .setQuery(query)
                    .build();

            var response = stub.searchInbox(request);
            return Result.ok(response.getMidsList());
        });
    }
}