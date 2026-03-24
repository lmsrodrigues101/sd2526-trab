package sd2526.trab.clients;

import java.net.URI;
import sd2526.trab.api.java.Users;
import sd2526.trab.api.Discovery;
import sd2526.trab.clients.rest.RestUsersClient;
import sd2526.trab.clients.grpc.GrpcUsersClient;

public class UsersClientFactory {
    private static final String REST = "/rest";
    private static final String GRPC = "/grpc";
    private static final String DOMAIN_DELIMITER = "@";

    static public Users get(String domain) {
        var sn = "%s%s%s".formatted(Users.SERVICE_NAME, DOMAIN_DELIMITER, domain);
        return newClient(Discovery.knownUrisOf(sn, 1)[0]);
    }

    static private Users newClient(URI serverURI) {
        var path = serverURI.getPath();

        if (path.endsWith(REST))
            return new RestUsersClient(serverURI);
        if (path.endsWith(GRPC))
            return new GrpcUsersClient(serverURI);

        throw new RuntimeException("Unknown service type..." + serverURI);
    }
}