package sd2526.trab.server.grpc;

import io.grpc.BindableService;
import io.grpc.ServerServiceDefinition;
import io.grpc.stub.StreamObserver;
import sd2526.trab.api.User;
import sd2526.trab.api.java.Result;
import sd2526.trab.api.java.Users;
import sd2526.trab.clients.UsersGrpcServer;
import sd2526.trab.clients.UsersRestServer;
import sd2526.trab.server.grpc.generated_java.GrpcUsersGrpc;
import sd2526.trab.api.grpc.Users.*;
import sd2526.trab.server.java.JavaUsers;
import sd2526.trab.server.util.DataModelAdaptorUser;

import java.util.List;
import java.util.logging.Logger;

public class GrpcUsersController extends GrpcController implements GrpcUsersGrpc.AsyncService, BindableService {

    final Users impl;
    private static Logger Log = Logger.getLogger(UsersRestServer.class.getName());

    public GrpcUsersController() {
        this.impl = new JavaUsers(UsersGrpcServer.DOMAIN);
    }

    @Override
    public ServerServiceDefinition bindService() {
        return GrpcUsersGrpc.bindService(this);
    }

    @Override
    public void postUser(GrpcUser user, StreamObserver<PostUserResult> responseObserver) {
        Log.info("Create User: " + user);

        Result<String> res = impl.postUser(DataModelAdaptorUser.GrpcUser_to_User(user));
        if( ! res.isOK() )
            responseObserver.onError(errorCodeToStatus(res.error()));
        else {
            responseObserver.onNext(PostUserResult.newBuilder().setUserAddress(res.value()).build());
            responseObserver.onCompleted();
        }
    }

    @Override
    public void getUser(GetUserArgs request, StreamObserver<GetUserResult> responseObserver) {
        Log.info("Get User: " + request);

        Result<User> res = impl.getUser(request.getName(), request.getPwd());
        if( ! res.isOK() )
            responseObserver.onError(errorCodeToStatus(res.error()));
        else {
            responseObserver.onNext( GetUserResult.newBuilder().setUser(DataModelAdaptorUser.User_to_GrpcUser(res.value())).build() );
            responseObserver.onCompleted();
        }
    }

    @Override
    public void updateUser(UpdateUserArgs request, StreamObserver<UpdateUserResult> responseObserver) {
        Log.info("Update User: " + request);
        Result<User> res = impl.updateUser(request.getName(), request.getPwd(),
                DataModelAdaptorUser.GrpcUser_to_User(request.getInfo()));
        if( ! res.isOK() )
            responseObserver.onError(errorCodeToStatus(res.error()));
        else {
            responseObserver.onNext( UpdateUserResult.newBuilder().setUser(DataModelAdaptorUser.User_to_GrpcUser(res.value())).build());
            responseObserver.onCompleted();
        }
    }

    @Override
    public void deleteUser(DeleteUserArgs request, StreamObserver<DeleteUserResult> responseObserver) {
        Log.info("Delete User: " + request);

        Result<User> res = impl.deleteUser(request.getName(), request.getPwd());

        if ( ! res.isOK() )
            responseObserver.onError(errorCodeToStatus(res.error()));
        else {
            responseObserver.onNext( DeleteUserResult.newBuilder().setUser(DataModelAdaptorUser.User_to_GrpcUser(res.value())).build());
            responseObserver.onCompleted();
        }
    }

    @Override
    public void searchUsers(SearchUsersArgs request, StreamObserver<GrpcUser> responseObserver) {
        Log.info("Search Users: " + request);

        Result<List<User>> res = impl.searchUsers(request.getName(),request.getPwd(),request.getQuery());

        if( ! res.isOK() )
            responseObserver.onError(errorCodeToStatus(res.error()));
        else {
            for(User u: res.value()) {
                responseObserver.onNext( DataModelAdaptorUser.User_to_GrpcUser(u));
            }
            responseObserver.onCompleted();
        }
    }



}
