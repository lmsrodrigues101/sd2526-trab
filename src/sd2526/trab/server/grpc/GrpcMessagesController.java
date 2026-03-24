package sd2526.trab.server.grpc;

import com.google.protobuf.Empty;
import io.grpc.BindableService;
import io.grpc.ServerServiceDefinition;
import io.grpc.stub.StreamObserver;
import sd2526.trab.api.Message;
import sd2526.trab.api.grpc.DataModelAdaptorMessage;
import sd2526.trab.api.grpc.GrpcMessagesGrpc;
import sd2526.trab.api.grpc.Messages;
import sd2526.trab.server.java.JavaMessages;

public class GrpcMessagesController extends GrpcController implements GrpcMessagesGrpc.AsyncService, BindableService {
    private final JavaMessages impl;

    public GrpcMessagesController(sd2526.trab.api.java.Messages impl) {
        this.impl = (JavaMessages) impl;
    }

    @Override
    public ServerServiceDefinition bindService() {
        return GrpcMessagesGrpc.bindService(this);
    }

    @Override
    public void postMessage(Messages.PostMessageArgs request, StreamObserver<Messages.PostMessageResult> responseObserver) {
        Message javaMsg = DataModelAdaptorMessage.GrpcMessage_to_Message(request.getMessage());

        super.toGrpcResult(responseObserver, impl.postMessage(request.getPwd(), javaMsg),
                (mid) -> Messages.PostMessageResult.newBuilder().setMid(mid).build());
    }

    @Override
    public void getInboxMessage(Messages.GetInboxMessageArgs request, StreamObserver<Messages.GrpcMessage> responseObserver) {
        super.toGrpcResult(responseObserver, impl.getInboxMessage(request.getName(), request.getMid(), request.getPwd()),
                DataModelAdaptorMessage::Message_to_GrpcMessage);
    }

    @Override
    public void getAllInboxMessages(Messages.GetAllInboxMessagesArgs request, StreamObserver<Messages.GetAllInboxMessagesResult> responseObserver) {

        super.toGrpcResult(responseObserver, impl.getAllInboxMessages(request.getName(), request.getPwd()),
                (mids) -> Messages.GetAllInboxMessagesResult.newBuilder().addAllMids(mids).build());
    }

    @Override
    public void deleteMessage(Messages.DeleteMessageArgs request, StreamObserver<com.google.protobuf.Empty> responseObserver) {
        super.toGrpcResult(responseObserver, impl.deleteMessage(request.getName(), request.getMid(), request.getPwd()),
                (v) -> Empty.getDefaultInstance());
    }

    @Override
    public void removeInboxMessage(Messages.RemoveInboxMessageArgs request, StreamObserver<com.google.protobuf.Empty> responseObserver) {
        super.toGrpcResult(responseObserver, impl.removeInboxMessage(request.getName(), request.getMid(), request.getPwd()),
                (v) -> Empty.getDefaultInstance());
    }

    @Override
    public void searchInbox(Messages.SearchInboxArgs request, StreamObserver<Messages.SearchInboxResult> responseObserver) {
        super.toGrpcResult(responseObserver, impl.searchInbox(request.getName(), request.getPwd(), request.getQuery()),
                (mids) -> Messages.SearchInboxResult.newBuilder().addAllMids(mids).build());
    }
}

