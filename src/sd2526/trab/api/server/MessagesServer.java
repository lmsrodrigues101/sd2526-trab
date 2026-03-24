package sd2526.trab.api.server;

import java.net.InetAddress;
import java.net.URI;
import java.util.logging.Logger;
import org.glassfish.jersey.jdkhttp.JdkHttpServerFactory;
import org.glassfish.jersey.server.ResourceConfig;
import sd2526.trab.api.Discovery;
import sd2526.trab.server.rest.RestMessagesResource;

public class MessagesServer {
    private static Logger Log = Logger.getLogger(MessagesServer.class.getName());

    public static void main(String[] args) throws Exception {
        if (args.length < 1) {
            System.out.println("Usage: java MessagesServer <domain>");
            return;
        }
        String domain = args[0];
        String ip = InetAddress.getLocalHost().getHostAddress();
        String serverURI = String.format("http://%s:8081/rest", ip);

        // 1. Iniciar o Discovery (Recetor)
        // Usamos o construtor simples porque este servidor só quer "ouvir" os outros
        Discovery discovery = new Discovery(Discovery.DISCOVERY_ADDR);
        discovery.start();

        // 2. Descobrir o servidor de Users no mesmo domínio
        Log.info("Waiting for Users service in domain: " + domain);
        URI[] uris = discovery.knownUrisOf("Users", domain);
        String usersServerUri = uris[0].toString();
        Log.info("Found Users Server at: " + usersServerUri);

        // 3. Iniciar o Servidor de Mensagens com o URI descoberto
        ResourceConfig config = new ResourceConfig();
        config.register(new RestMessagesResource(domain, usersServerUri));
        JdkHttpServerFactory.createHttpServer(URI.create(serverURI), config);

        // 4. (Opcional) O MessagesServer também se pode anunciar se quiseres
        // Discovery messagesDiscovery = new Discovery(Discovery.DISCOVERY_ADDR, "Messages", serverURI, domain);
        // messagesDiscovery.start();

        Log.info(String.format("Messages Server ready @ %s\n", serverURI));
    }
}