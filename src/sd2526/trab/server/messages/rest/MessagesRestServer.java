package sd2526.trab.server.messages.rest;

import java.net.InetAddress;
import java.net.URI;
import java.util.logging.Logger;
import org.glassfish.jersey.jdkhttp.JdkHttpServerFactory;
import org.glassfish.jersey.server.ResourceConfig;
import sd2526.trab.server.Discovery;


public class MessagesRestServer {
    private static Logger Log = Logger.getLogger(MessagesRestServer.class.getName());
    public static final int PORT = 8081;
    public static final String SERVICE_NAME = "Messages";
    private static final String URI_FMT = "http://%s:%d/rest";
    public static String DOMAIN;

    public static void main(String[] args) {
        try {
            DOMAIN = getDomain(args);
            String ip = InetAddress.getLocalHost().getHostAddress();
            int port = args.length > 1 ? Integer.parseInt(args[1]) : PORT;

            ResourceConfig config = new ResourceConfig();
            config.register(RestMessagesResource.class);

            String serverURI = String.format(URI_FMT, ip, port);
            JdkHttpServerFactory.createHttpServer(URI.create(serverURI), config);

            String discoveryName = String.format("%s@%s", SERVICE_NAME, DOMAIN);
            new Discovery(Discovery.DISCOVERY_ADDR, discoveryName, serverURI).start();

            Log.info(String.format("REST %s Server READY at %s (Domain: %s)\n", SERVICE_NAME, serverURI, DOMAIN));
        } catch (Exception e) {
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