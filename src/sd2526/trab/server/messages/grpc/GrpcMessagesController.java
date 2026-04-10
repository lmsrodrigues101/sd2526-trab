package sd2526.trab.server.messages.grpc;

import com.google.protobuf.Empty;
import io.grpc.BindableService;
import io.grpc.ServerServiceDefinition;
import io.grpc.stub.StreamObserver;
import sd2526.trab.api.Message;
import sd2526.trab.api.java.Result;
import sd2526.trab.api.grpc.DataModelAdaptorMessage;
import sd2526.trab.server.util.GrpcController;
import sd2526.trab.api.grpc.GrpcMessagesGrpc;
import sd2526.trab.api.grpc.Messages;
import sd2526.trab.server.messages.java.JavaMessages;

import java.util.List;

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
        Result<String> res = impl.postMessage(request.getPwd(), DataModelAdaptorMessage.GrpcMessage_to_Message(request.getMessage()));

        if (!res.isOK()) {
            responseObserver.onError(errorCodeToStatus(res.error()));
        } else {
            responseObserver.onNext(Messages.PostMessageResult.newBuilder().setMid(res.value()).build());
            responseObserver.onCompleted();
        }
    }

    @Override
    public void getInboxMessage(Messages.GetInboxMessageArgs request, StreamObserver<Messages.GrpcMessage> responseObserver) {
            Result<Message> res = impl.getInboxMessage(request.getName(), request.getMid(), request.getPwd());

            if (!res.isOK()) {
                responseObserver.onError(errorCodeToStatus(res.error()));
            } else {
                responseObserver.onNext(DataModelAdaptorMessage.Message_to_GrpcMessage(res.value()));
                responseObserver.onCompleted();
            }
    }

    @Override
    public void getAllInboxMessages(Messages.GetAllInboxMessagesArgs request, StreamObserver<Messages.GetAllInboxMessagesResult> responseObserver) {
        Result<List<String>> res = impl.getAllInboxMessages(request.getName(), request.getPwd());

        if (!res.isOK()) {
            responseObserver.onError(errorCodeToStatus(res.error()));
        } else {
            responseObserver.onNext(Messages.GetAllInboxMessagesResult.newBuilder().addAllMids(res.value()).build());
            responseObserver.onCompleted();
        }
    }

    @Override
    public void deleteMessage(Messages.DeleteMessageArgs request, StreamObserver<com.google.protobuf.Empty> responseObserver) {
        Result<Void> res = impl.deleteMessage(request.getName(), request.getMid(), request.getPwd());
        if (!res.isOK()) {
            responseObserver.onError(errorCodeToStatus(res.error()));
        } else {
            responseObserver.onNext(Empty.getDefaultInstance());
            responseObserver.onCompleted();
        }
    }

    @Override
    public void removeInboxMessage(Messages.RemoveInboxMessageArgs request, StreamObserver<com.google.protobuf.Empty> responseObserver) {
        Result<Void> res = impl.removeInboxMessage(request.getName(), request.getMid(), request.getPwd());

        if (!res.isOK()) {
            responseObserver.onError(errorCodeToStatus(res.error()));
        } else {
            responseObserver.onNext(Empty.getDefaultInstance());
            responseObserver.onCompleted();
        }
    }

    @Override
    public void searchInbox(Messages.SearchInboxArgs request, StreamObserver<Messages.SearchInboxResult> responseObserver) {
        Result<List<String>> res = impl.searchInbox(request.getName(), request.getPwd(), request.getQuery());
        if (!res.isOK()) {
            responseObserver.onError(errorCodeToStatus(res.error()));
        } else {
            responseObserver.onNext(Messages.SearchInboxResult.newBuilder().addAllMids(res.value()).build());
            responseObserver.onCompleted();
        }
    }
}

