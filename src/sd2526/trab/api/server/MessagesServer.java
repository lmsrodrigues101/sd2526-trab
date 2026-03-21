package sd2526.trab.api.server; // Ajusta para o teu pacote

import java.net.InetAddress;
import java.net.URI;
import java.util.logging.Logger;

import org.glassfish.jersey.jdkhttp.JdkHttpServerFactory;
import org.glassfish.jersey.server.ResourceConfig;

import sd2526.trab.api.server.resources.MessagesResource;

public class MessagesServer {

    private static Logger Log = Logger.getLogger(MessagesServer.class.getName());

    static {
        System.setProperty("java.net.preferIPv4Stack", "true");
        System.setProperty("java.util.logging.SimpleFormatter.format", "%4$s: %5$s\n");
    }

    public static final int PORT = 8081; // Porta diferente para não chocar com os Users!
    public static final String SERVICE = "Messages";
    private static final String SERVER_URI_FMT = "http://%s:%s/rest";

    public static void main(String[] args) {
        try {
            String domain = args.length > 0 ? args[0] : "fct";

            // Para testes locais, dizemos ao MessagesServer onde está o UsersServer
            String usersServerUri = "http://localhost:8080/rest";

            ResourceConfig config = new ResourceConfig();
            // Registamos o recurso passando o domínio E o URI do servidor de users
            config.register(new MessagesResource(domain, usersServerUri));

            String ip = InetAddress.getLocalHost().getHostAddress();
            String serverURI = String.format(SERVER_URI_FMT, ip, PORT);

            JdkHttpServerFactory.createHttpServer(URI.create(serverURI), config);
            Log.info(String.format("%s Server ready @ %s (Domain: %s)\n", SERVICE, serverURI, domain));

        } catch( Exception e) {
            Log.severe(e.getMessage());
        }
    }
}