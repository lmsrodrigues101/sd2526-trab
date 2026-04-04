package sd2526.trab.clients.grpc;

import java.net.URI;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;
import io.grpc.LoadBalancerRegistry;
import io.grpc.internal.PickFirstLoadBalancerProvider;
import sd2526.trab.api.User;
import sd2526.trab.api.grpc.Users;
import sd2526.trab.api.java.Result;
import sd2526.trab.clients.UsersClient;
import sd2526.trab.clients.UsersRestServer;
import sd2526.trab.server.util.DataModelAdaptorUser;
import sd2526.trab.server.grpc.generated_java.GrpcUsersGrpc;
import sd2526.trab.api.grpc.Users.GrpcUser;
import sd2526.trab.api.grpc.Users.GetUserArgs;
import sd2526.trab.api.grpc.Users.UpdateUserArgs;
import sd2526.trab.api.grpc.Users.DeleteUserArgs;
import sd2526.trab.api.grpc.Users.SearchUsersArgs;
import sd2526.trab.api.grpc.Users.PostUserResult;

public class GrpcUsersClient extends UsersClient {

    static {
        LoadBalancerRegistry.getDefaultRegistry().register(new PickFirstLoadBalancerProvider());
    }

    private static Logger Log = Logger.getLogger(UsersRestServer.class.getName());
    private final ManagedChannel channel;
    private final GrpcUsersGrpc.GrpcUsersBlockingStub stub;

    public GrpcUsersClient(URI serverURI) {
        this.channel = ManagedChannelBuilder.forAddress(serverURI.getHost(), serverURI.getPort())
                .usePlaintext()
                .build();
        this.stub = GrpcUsersGrpc.newBlockingStub(channel);
    }

    @Override
    public Result<String> postUser(User user) {
        try {
            PostUserResult res = stub.postUser(DataModelAdaptorUser.User_to_GrpcUser(user));

            return Result.ok(res.getUserAddress());
        } catch (StatusRuntimeException sre) {
            return Result.error( statusToErrorCode(sre.getStatus()));
        }
    }

    @Override
    public Result<User> getUser(String name, String pwd) {
        try {
            Users.GetUserResult res = stub.getUser(GetUserArgs.newBuilder()
                    .setName(name)
                    .setPwd(pwd)
                    .build());

            return Result.ok(DataModelAdaptorUser.GrpcUser_to_User(res.getUser()));
        } catch (StatusRuntimeException sre) {
            return Result.error( statusToErrorCode(sre.getStatus()));
        }
    }

    @Override
    public Result<User> updateUser(String name, String pwd, User info) {
        try {
            Users.UpdateUserResult res = stub.updateUser(UpdateUserArgs.newBuilder()
                    .setName(name)
                    .setPwd(pwd)
                    .setInfo(DataModelAdaptorUser.User_to_GrpcUser(info))
                    .build());

            return Result.ok(DataModelAdaptorUser.GrpcUser_to_User(res.getUser()));
        } catch (StatusRuntimeException sre) {
            return Result.error( statusToErrorCode(sre.getStatus()));
        }
    }

    @Override
    public Result<User> deleteUser(String name, String pwd) {
        try {
            Users.DeleteUserResult res = stub.deleteUser(DeleteUserArgs.newBuilder()
                    .setName(name)
                    .setPwd(pwd)
                    .build());

            return Result.ok(DataModelAdaptorUser.GrpcUser_to_User(res.getUser()));
        } catch (StatusRuntimeException sre) {
            return Result.error( statusToErrorCode(sre.getStatus()));
        }
    }

    @Override
    public Result<List<User>> searchUsers(String name, String pwd, String query) {
        try {
            Iterator<GrpcUser> res = stub.searchUsers(SearchUsersArgs.newBuilder()
                    .setName(name)
                    .setPwd(pwd)
                    .setQuery(query)
                    .build());

            List<User> ret = new ArrayList<User>();
            while(res.hasNext()) {
                ret.add(DataModelAdaptorUser.GrpcUser_to_User(res.next()));
            }
            return Result.ok(ret);
        } catch (StatusRuntimeException sre) {
            return Result.error( statusToErrorCode(sre.getStatus()));
        }
    }


}