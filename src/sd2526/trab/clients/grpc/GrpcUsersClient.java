package sd2526.trab.clients.grpc;

import sd2526.trab.api.User;
import sd2526.trab.api.grpc.DataModelAdaptorUser;
import sd2526.trab.api.grpc.GrpcUsersGrpc;
import sd2526.trab.api.java.Result;
import sd2526.trab.api.java.Users;


import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class GrpcUsersClient extends GrpcClient implements Users {
    private final GrpcUsersGrpc.GrpcUsersBlockingStub stub;

    public GrpcUsersClient(URI serverURI) {
        super(serverURI.toString(), Logger.getLogger(GrpcUsersClient.class.getName()));
        this.stub = GrpcUsersGrpc.newBlockingStub(channel);
    }

    @Override
    public Result<String> postUser(User user) {
        return super.reTry(() -> {
            var res = stub.postUser(DataModelAdaptorUser.User_to_GrpcUser(user));
            return Result.ok(res.getUserAddress());
        });
    }

    @Override
    public Result<User> getUser(String name, String pwd) {
        return super.reTry(() -> {
            var res = stub.getUser(sd2526.trab.api.grpc.Users.GetUserArgs.newBuilder().setName(name).setPwd(pwd).build());
            return Result.ok(DataModelAdaptorUser.GrpcUser_to_User(res.getUser()));
        });
    }

    @Override
    public Result<User> updateUser(String name, String pwd, User info) {
        return super.reTry(() -> {
            var args = sd2526.trab.api.grpc.Users.UpdateUserArgs.newBuilder()
                    .setName(name)
                    .setPwd(pwd)
                    .setInfo(DataModelAdaptorUser.User_to_GrpcUser(info))
                    .build();
            var res = stub.updateUser(args);
            return Result.ok(DataModelAdaptorUser.GrpcUser_to_User(res.getUser()));
        });
    }

    @Override
    public Result<User> deleteUser(String name, String pwd) {
        return super.reTry(() -> {
            var res = stub.deleteUser(sd2526.trab.api.grpc.Users.DeleteUserArgs.newBuilder().setName(name).setPwd(pwd).build());
            return Result.ok(DataModelAdaptorUser.GrpcUser_to_User(res.getUser()));
        });
    }

    @Override
    public Result<List<User>> searchUsers(String name, String pwd, String query) {
        return super.reTry(() -> {
            var args = sd2526.trab.api.grpc.Users.SearchUsersArgs.newBuilder().setName(name).setPwd(pwd).setQuery(query).build();
            var iterator = stub.searchUsers(args);
            List<User> list = new ArrayList<>();
            iterator.forEachRemaining(gu -> list.add(DataModelAdaptorUser.GrpcUser_to_User(gu)));
            return Result.ok(list);
        });
    }
}
