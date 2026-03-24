package sd2526.trab.api.grpc;

import sd2526.trab.api.User;
import sd2526.trab.api.grpc.Users.GrpcUser;
import sd2526.trab.api.grpc.Users.GrpcUser.Builder;

public class DataModelAdaptorUser {

    public static User GrpcUser_to_User(GrpcUser from) {
        return new User(
                from.getName(),
                from.hasPwd() ? from.getPwd() : null,
                from.hasDisplayName() ? from.getDisplayName() : null,
                from.hasDomain() ? from.getDomain() : null
        );
    }

    public static GrpcUser User_to_GrpcUser(User from) {
        Builder b = GrpcUser.newBuilder();

        if (from.getName() != null)
            b.setName(from.getName());

        if (from.getPwd() != null)
            b.setPwd(from.getPwd());

        if (from.getDisplayName() != null)
            b.setDisplayName(from.getDisplayName());

        if (from.getDomain() != null)
            b.setDomain(from.getDomain());

        return b.build();
    }
}