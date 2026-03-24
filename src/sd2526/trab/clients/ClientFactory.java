package sd2526.trab.clients;

import java.net.URI;
import java.util.function.Function;
import sd2526.trab.api.Discovery;

public class ClientFactory<T> {
    private static final String REST = "/rest";
    private static final String GRPC = "/grpc";

    final String serviceName;
    final Function<URI, T> restClientFunc;
    final Function<URI, T> grpcClientFunc;

    public ClientFactory(String serviceName, Function<URI, T> restClientFunc, Function<URI, T> grpcClientFunc) {
        this.serviceName = serviceName;
        this.restClientFunc = restClientFunc;
        this.grpcClientFunc = grpcClientFunc;
    }

    public T get(String domain) {

        var sn = "%s@%s".formatted(serviceName, domain);
        URI uri = Discovery.knownUrisOf(sn, 1)[0];
        return newClient(uri);
    }

    private T newClient(URI serverURI) {
        var path = serverURI.getPath();

        if (path.endsWith(REST))
            return restClientFunc.apply(serverURI);

        if (path.endsWith(GRPC))
            return grpcClientFunc.apply(serverURI);

        throw new RuntimeException("Unknown service type: " + serverURI);
    }
}