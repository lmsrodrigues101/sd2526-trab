package sd2526.trab.clients;

import java.net.InetAddress;
import java.util.logging.Logger;

import io.grpc.*;
import sd2526.trab.server.Discovery;
import sd2526.trab.server.grpc.GrpcUsersController;
import sd2526.trab.server.java.JavaUsers;

public class UsersGrpcServer {
    private static Logger Log = Logger.getLogger(UsersGrpcServer.class.getName());

    public static final int PORT = 9090;
    private static final String GRPC_CTX = "/grpc";
    private static final String SERVICE = "Users";
    private static final String SERVER_BASE_URI = "grpc://%s:%d%s";
    public static String DOMAIN;

    public static void main(String[] args) throws Exception {
        try {
            DOMAIN = getDomain(args);

            GrpcUsersController stub = new GrpcUsersController();
            ServerCredentials cred = InsecureServerCredentials.create();

            Server server = Grpc.newServerBuilderForPort(PORT, cred).addService(stub).build();

            String serverURI = String.format(SERVER_BASE_URI, InetAddress.getLocalHost().getHostAddress(), PORT, GRPC_CTX);
            String discoveryName = String.format("%s@%s", SERVICE, DOMAIN);

            Discovery discovery = new Discovery(Discovery.DISCOVERY_ADDR, discoveryName, serverURI);
            discovery.start();
            Log.info(String.format("Users gRPC Server ready @ %s\n", serverURI));
            server.start().awaitTermination();
        }
        catch (Exception e) {
            Log.severe("Erro ao iniciar o servidor: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static String getDomain(String[] args) throws Exception {
        if (args.length > 0) return args[0];
        String hostname = InetAddress.getLocalHost().getHostName();
        return hostname.contains(".") ? hostname.substring(hostname.indexOf('.') + 1) : "localdomain";
    }
}