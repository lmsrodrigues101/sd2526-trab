package sd2526.trab.api.server;

import java.net.InetAddress;
import java.net.URI;
import java.util.logging.Logger;
import org.glassfish.jersey.jdkhttp.JdkHttpServerFactory;
import org.glassfish.jersey.server.ResourceConfig;
import sd2526.trab.api.Discovery;
import sd2526.trab.server.rest.RestUsersResource;

public class UsersServer {
    private static Logger Log = Logger.getLogger(UsersServer.class.getName());

    public static void main(String[] args) throws Exception {
        if (args.length < 1) {
            System.out.println("Usage: java UsersServer <domain>");
            return;
        }
        String domain = args[0];
        String ip = InetAddress.getLocalHost().getHostAddress();
        String serverURI = String.format("http://%s:8080/rest", ip);

        // 1. Configurar o Recurso REST
        ResourceConfig config = new ResourceConfig();
        config.register(new RestUsersResource(domain));
        JdkHttpServerFactory.createHttpServer(URI.create(serverURI), config);

        // 2. Iniciar o Discovery (Anunciador)
        // Usamos o construtor completo para este servidor se anunciar na rede
        Discovery discovery = new Discovery(Discovery.DISCOVERY_ADDR, "Users", serverURI, domain);
        discovery.start(); // Isto inicia a thread de anúncios e a de receção

        Log.info(String.format("Users Server ready @ %s (Domain: %s)\n", serverURI, domain));
    }
}