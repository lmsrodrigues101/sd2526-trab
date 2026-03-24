package sd2526.trab.server;

import java.net.InetAddress;
import java.net.URI;
import java.util.logging.Logger;

import org.glassfish.jersey.jdkhttp.JdkHttpServerFactory;
import org.glassfish.jersey.server.ResourceConfig;

import io.grpc.Server;
import io.grpc.ServerBuilder;
import sd2526.trab.api.Discovery;
import sd2526.trab.api.java.Messages;
import sd2526.trab.server.java.JavaMessages;
import sd2526.trab.server.rest.RestMessagesResource;
import sd2526.trab.server.grpc.GrpcMessagesController;

public class MessagesServer {
    private static Logger Log = Logger.getLogger(MessagesServer.class.getName());

    // Portas diferentes do UsersServer para não haver conflito
    public static final int REST_PORT = 8081;
    public static final int GRPC_PORT = 9091;
    public static final String SERVICE_NAME = "Messages";

    public static void main(String[] args) throws Exception {
        // Ex: java MessagesServer fct
        String domain = args.length > 0 ? args[0] : "fct";
        String ip = InetAddress.getLocalHost().getHostAddress();

        // 1. A Lógica Única (Cérebro das Mensagens)
        // Nota: O JavaMessages usará a UsersClientFactory internamente para validar utilizadores
        Messages impl = new JavaMessages(domain);

        // 2. CONFIGURAÇÃO REST (Jersey)
        ResourceConfig config = new ResourceConfig();
        // Lembra-te de mudar o construtor do RestMessagesResource para aceitar 'impl'
        config.registerInstances(new RestMessagesResource(impl));

        String restURI = String.format("http://%s:%d/rest", ip, REST_PORT);
        JdkHttpServerFactory.createHttpServer(URI.create(restURI), config);
        Log.info(String.format("REST Messages Server running on: %s", restURI));

        // 3. CONFIGURAÇÃO gRPC
        GrpcMessagesController grpcController = new GrpcMessagesController(impl);
        Server grpcServer = ServerBuilder.forPort(GRPC_PORT)
                .addService(grpcController)
                .build();

        grpcServer.start();
        String grpcURI = String.format("grpc://%s:%d/grpc", ip, GRPC_PORT);
        Log.info(String.format("gRPC Messages Server running on: %s", grpcURI));

        // 4. DISCOVERY: Anunciar os dois endpoints para a Factory funcionar
        new Discovery(Discovery.DISCOVERY_ADDR, SERVICE_NAME, restURI, domain).start();
        new Discovery(Discovery.DISCOVERY_ADDR, SERVICE_NAME, grpcURI, domain).start();

        Log.info(String.format("%s Server ('%s') is ready (REST + gRPC).", SERVICE_NAME, domain));
    }
}