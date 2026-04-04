package sd2526.trab.server;

import java.net.URI;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import sd2526.trab.clients.MessagesClient;
import sd2526.trab.clients.UsersClient;
import sd2526.trab.clients.grpc.GrpcMessagesClient;
import sd2526.trab.clients.grpc.GrpcUsersClient;
import sd2526.trab.clients.rest.RestUsersClient;
import sd2526.trab.clients.rest.RestMessagesClient;
// Se já tiveres os gRPC, importa-os aqui:
// import sd2526.trab.clients.grpc.GrpcUsersClient;
// import sd2526.trab.clients.grpc.GrpcMessagesClient;

public class ServiceFactory {
    private static ServiceFactory instance;

    private final Map<String, URI> latestUris;

    private ServiceFactory() {
        latestUris = new ConcurrentHashMap<>();
    }

    public void addService(String serviceName, URI serviceUri) {
        latestUris.put(serviceName, serviceUri);
    }

    public URI getLatestUri(String serviceName) {
        URI uri = latestUris.get(serviceName);
        int retries = 0;
        while (uri == null && retries < 10) {
            try { Thread.sleep(500); } catch (InterruptedException e) {}
            uri = latestUris.get(serviceName);
            retries++;
        }
        return uri;
    }

    public UsersClient getUsersClient(String domain) {
        String fullName = "Users@" + domain;
        URI userUri = getLatestUri(fullName);
        if (userUri != null) {
            if (userUri.toString().endsWith("rest")) {
                return new RestUsersClient(userUri);
            } else {
                return new GrpcUsersClient(userUri);
            }
        }
        return null;
    }

    public MessagesClient getMessagesClient(String domain) {
        String fullName = "Messages@" + domain;
        URI messagesUri = getLatestUri(fullName);
        if (messagesUri != null) {
            if (messagesUri.toString().endsWith("rest")) {
                return new RestMessagesClient(messagesUri);
            } else {
                return new GrpcMessagesClient(messagesUri);
            }
        }
        return null;
    }

    synchronized public static ServiceFactory getInstance() {
        if (instance == null) {
            instance = new ServiceFactory();
        }
        return instance;
    }
}