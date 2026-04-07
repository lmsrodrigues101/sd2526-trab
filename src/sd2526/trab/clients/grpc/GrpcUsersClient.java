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

    private static final java.util.concurrent.ConcurrentHashMap<String, ManagedChannel> channels = new java.util.concurrent.ConcurrentHashMap<>();
    private static Logger Log = Logger.getLogger(UsersRestServer.class.getName());
    private final ManagedChannel channel;
    private final GrpcUsersGrpc.GrpcUsersBlockingStub stub;

    public GrpcUsersClient(URI serverURI) {
        String target = serverURI.getHost() + "" + serverURI.getPort();
        this.channel = channels.computeIfAbsent(target, k -> ManagedChannelBuilder.forAddress(serverURI.getHost(), serverURI.getPort())
                .usePlaintext()
                .build());
        this.stub = GrpcUsersGrpc.newBlockingStub(channel);
    }

    @Override
    public Result<String> postUser(User user) {
        return grpcRetry(() ->
                stub.postUser(DataModelAdaptorUser.User_to_GrpcUser(user)).getUserAddress()
        );
    }

    @Override
    public Result<User> getUser(String name, String pwd) {
        return grpcRetry(() -> DataModelAdaptorUser.GrpcUser_to_User(
                stub.getUser(GetUserArgs.newBuilder().setName(name).setPwd(pwd).build()).getUser()
        ));
    }

    @Override
    public Result<User> updateUser(String name, String pwd, User info) {
        return grpcRetry(() -> DataModelAdaptorUser.GrpcUser_to_User(
                stub.updateUser(UpdateUserArgs.newBuilder()
                        .setName(name).setPwd(pwd)
                        .setInfo(DataModelAdaptorUser.User_to_GrpcUser(info))
                        .build()).getUser()
        ));
    }

    @Override
    public Result<User> deleteUser(String name, String pwd) {
        return grpcRetry(() -> DataModelAdaptorUser.GrpcUser_to_User(
                stub.deleteUser(DeleteUserArgs.newBuilder().setName(name).setPwd(pwd).build()).getUser()
        ));
    }

    @Override
    public Result<List<User>> searchUsers(String name, String pwd, String query) {
        return grpcRetry(() -> {
            Iterator<GrpcUser> res = stub.searchUsers(SearchUsersArgs.newBuilder()
                    .setName(name).setPwd(pwd).setQuery(query).build());
            List<User> ret = new ArrayList<>();
            while (res.hasNext()) {
                ret.add(DataModelAdaptorUser.GrpcUser_to_User(res.next()));
            }
            return ret;
        });
    }

    private <T> Result<T> grpcRetry(java.util.function.Supplier<T> call) {
        for (int i = 0; i < MAX_RETRIES; i++) {
            try {
                return Result.ok(call.get());
            } catch (StatusRuntimeException e) {
                // Se o servidor estiver em baixo (a iniciar ou com rede cortada), espera e tenta de novo
                if (e.getStatus().getCode() == io.grpc.Status.Code.UNAVAILABLE ||
                        e.getStatus().getCode() == io.grpc.Status.Code.DEADLINE_EXCEEDED) {
                    try { Thread.sleep(RETRY_SLEEP); } catch (InterruptedException ignored) {}
                    continue;
                }
                // Se for um erro real de negócio (ex: password errada), devolve logo
                return Result.error(statusToErrorCode(e.getStatus()));
            } catch (Exception e) {
                return Result.error(Result.ErrorCode.INTERNAL_ERROR);
            }
        }
        return Result.error(Result.ErrorCode.TIMEOUT);
    }
}