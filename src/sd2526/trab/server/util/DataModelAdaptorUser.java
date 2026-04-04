package sd2526.trab.server.util;

import sd2526.trab.api.User;
import sd2526.trab.api.grpc.Users.GrpcUser;
import sd2526.trab.api.grpc.Users.GrpcUser.Builder;

public class DataModelAdaptorUser {

    public static User GrpcUser_to_User(GrpcUser gu) {
        String name = (gu.getName() == null || gu.getName().isEmpty()) ? null : gu.getName();
        String pwd = (gu.getPwd() == null || gu.getPwd().isEmpty()) ? null : gu.getPwd();
        String displayName = (gu.getDisplayName() == null || gu.getDisplayName().isEmpty()) ? null : gu.getDisplayName();
        String domain = (gu.getDomain() == null || gu.getDomain().isEmpty()) ? null : gu.getDomain();

        return new User(name, pwd, displayName, domain);
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