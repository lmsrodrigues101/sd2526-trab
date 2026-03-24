package sd2526.trab.server.grpc;

import io.grpc.BindableService;
import io.grpc.ServerServiceDefinition;
import io.grpc.stub.StreamObserver;
import sd2526.trab.api.User;
import sd2526.trab.api.java.Users;
import sd2526.trab.api.java.Result;
import sd2526.trab.api.grpc.GrpcUsersGrpc;
import sd2526.trab.api.grpc.Users.*;
import sd2526.trab.api.grpc.GrpcUsersGrpc;
import sd2526.trab.api.grpc.DataModelAdaptorUser;

public class GrpcUsersController extends GrpcController implements GrpcUsersGrpc.AsyncService, BindableService {

    final Users impl;

    public GrpcUsersController(Users impl) {
        this.impl = impl;
    }

    @Override
    public ServerServiceDefinition bindService() {
        return GrpcUsersGrpc.bindService(this);
    }

    @Override
    public void postUser(GrpcUser user, StreamObserver<PostUserResult> responseObserver) {
        User javaUser = DataModelAdaptorUser.GrpcUser_to_User(user);
        super.toGrpcResult(responseObserver, impl.postUser(javaUser),
                (userAddress) -> PostUserResult.newBuilder().setUserAddress(userAddress).build());
    }

    @Override
    public void getUser(GetUserArgs request, StreamObserver<GetUserResult> responseObserver) {
        super.toGrpcResult(responseObserver, impl.getUser(request.getName(), request.getPwd()),
                (user) -> GetUserResult.newBuilder().setUser(DataModelAdaptorUser.User_to_GrpcUser(user)).build());
    }

    @Override
    public void updateUser(UpdateUserArgs request, StreamObserver<UpdateUserResult> responseObserver) {
        User userUpdate = DataModelAdaptorUser.GrpcUser_to_User(request.getInfo());

        super.toGrpcResult(responseObserver, impl.updateUser(request.getName(), request.getPwd(), userUpdate),
                (user) -> UpdateUserResult.newBuilder().setUser(DataModelAdaptorUser.User_to_GrpcUser(user)).build());
    }

    @Override
    public void deleteUser(DeleteUserArgs request, StreamObserver<DeleteUserResult> responseObserver) {
        super.toGrpcResult(responseObserver, impl.deleteUser(request.getName(), request.getPwd()),
                (user) -> DeleteUserResult.newBuilder().setUser(DataModelAdaptorUser.User_to_GrpcUser(user)).build());
    }

    @Override
    public void searchUsers(SearchUsersArgs request, StreamObserver<GrpcUser> responseObserver) {
        var res = impl.searchUsers(request.getName(), request.getPwd(), request.getQuery());

        if (res.isOK()) {
            for (User u : res.value()) {
                responseObserver.onNext(DataModelAdaptorUser.User_to_GrpcUser(u));
            }
            responseObserver.onCompleted();
        } else {
            responseObserver.onError(GrpcController.errorCodeToStatus(res.error()));
        }
    }



}
