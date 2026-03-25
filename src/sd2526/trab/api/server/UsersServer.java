package sd2526.trab.api.server;

import java.net.InetAddress;
import java.net.URI;
import java.util.logging.Logger;

import org.glassfish.jersey.jdkhttp.JdkHttpServerFactory;
import org.glassfish.jersey.server.ResourceConfig;

import io.grpc.Server;
import io.grpc.ServerBuilder;
import sd2526.trab.api.Discovery;
import sd2526.trab.api.java.Users;
import sd2526.trab.server.java.JavaUsers;
import sd2526.trab.server.rest.RestUsersResource;
import sd2526.trab.server.grpc.GrpcUsersController;

public class UsersServer {
    private static Logger Log = Logger.getLogger(UsersServer.class.getName());

    public static final int REST_PORT = 8080;
    public static final int GRPC_PORT = 9090;
    public static final String SERVICE_NAME = "Users";

    public static void main(String[] args) throws Exception {
        String domain = args.length > 0 ? args[0] : "fct";
        String ip = InetAddress.getLocalHost().getHostAddress();

        // 1. O "Cérebro" Único: Uma só instância da lógica para ambos os protocolos
        Users impl = new JavaUsers(domain);

        // 2. CONFIGURAÇÃO REST (Jersey)
        ResourceConfig config = new ResourceConfig();
        config.register(new RestUsersResource(impl));

        String restURI = String.format("http://%s:%d/rest", ip, REST_PORT);
        JdkHttpServerFactory.createHttpServer(URI.create(restURI), config);
        Log.info(String.format("REST Users Server running on: %s", restURI));

        // 3. CONFIGURAÇÃO gRPC
        GrpcUsersController grpcController = new GrpcUsersController(impl);
        Server grpcServer = ServerBuilder.forPort(GRPC_PORT)
                .addService(grpcController)
                .build();

        grpcServer.start();
        String grpcURI = String.format("grpc://%s:%d/grpc", ip, GRPC_PORT);
        Log.info(String.format("gRPC Users Server running on: %s", grpcURI));

        // 4. DISCOVERY: Anunciar ambos os endpoints
        new Discovery(Discovery.DISCOVERY_ADDR, SERVICE_NAME, restURI, domain).start();
        new Discovery(Discovery.DISCOVERY_ADDR, SERVICE_NAME, grpcURI, domain).start();

        Log.info(String.format("%s Server ('%s') is ready (REST + gRPC).", SERVICE_NAME, domain));
    }
}