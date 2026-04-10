package sd2526.trab.server.messages.grpc;

import java.net.InetAddress;
import java.util.logging.Logger;

import io.grpc.*;
import sd2526.trab.server.Discovery;
import sd2526.trab.server.messages.java.JavaMessages;

public class MessagesGrpcServer {
    private static Logger Log = Logger.getLogger(MessagesGrpcServer.class.getName());

    public static final int PORT = 9091;
    private static final String GRPC_CTX = "/grpc";
    private static final String SERVICE = "Messages";
    private static final String SERVER_BASE_URI = "grpc://%s:%d%s";
    public static String DOMAIN;

    public static void main(String[] args) throws Exception {
        try {
            DOMAIN = getDomain(args);
            JavaMessages impl = new JavaMessages(DOMAIN);
            GrpcMessagesController stub = new GrpcMessagesController(impl);

            ServerCredentials cred = InsecureServerCredentials.create();

            Server server = Grpc.newServerBuilderForPort(PORT, cred).addService(stub).build();

            String serverURI = String.format(SERVER_BASE_URI, InetAddress.getLocalHost().getHostAddress(), PORT, GRPC_CTX);
            String discoveryName = String.format("%s@%s", SERVICE, DOMAIN);

            Discovery discovery = new Discovery(Discovery.DISCOVERY_ADDR, discoveryName, serverURI);
            discovery.start();
            Log.info(String.format("Messages gRPC Server ready @ %s (Domain: %s)\n", serverURI, DOMAIN));

            server.start().awaitTermination();
        }
        catch (Exception e) {
            Log.severe("Erro ao iniciar o servidor de Messages: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static String getDomain(String[] args) throws Exception {
        if (args.length > 0) return args[0];
        String hostname = InetAddress.getLocalHost().getHostName();
        return hostname.contains(".") ? hostname.substring(hostname.indexOf('.') + 1) : "localdomain";
    }
}