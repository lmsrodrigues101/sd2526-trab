package sd2526.trab.api;

import java.io.IOException;
import java.net.*;
import java.util.logging.Logger;

/**
 * <p>
 * A class to perform service discovery, based on periodic service contact
 * endpoint announcements over multicast communication.
 * </p>
 *
 * <p>
 * Servers announce their *name* and contact *uri* at regular intervals. The
 * server actively collects received announcements.
 * </p>
 *
 * <p>
 * Service announcements have the following format:
 * </p>
 *
 * <p>
 * &lt;service-name-string&gt;&lt;delimiter-char&gt;&lt;service-uri-string&gt;
 * </p>
 */
public class Discovery {
    private static Logger Log = Logger.getLogger(Discovery.class.getName());

    static {
        // addresses some multicast issues on some TCP/IP stacks
        System.setProperty("java.net.preferIPv4Stack", "true");
        // summarizes the logging format
        System.setProperty("java.util.logging.SimpleFormatter.format", "%4$s: %5$s");
    }

    // The pre-aggreed multicast endpoint assigned to perform discovery.
    // Allowed IP Multicast range: 224.0.0.1 - 239.255.255.255
    static final public InetSocketAddress DISCOVERY_ADDR = new InetSocketAddress("226.226.226.226", 2266);
    static final int DISCOVERY_ANNOUNCE_PERIOD = 1000;
    static final int DISCOVERY_RETRY_TIMEOUT = 5000;
    static final int MAX_DATAGRAM_SIZE = 65536;

    // Used separate the two fields that make up a service announcement.
    private static final String DELIMITER = "\t";
    private static final String SIGN = "@";

    private final InetSocketAddress addr;
    private final String serviceName;
    private final String serviceURI;
    private final String domain;
    private final MulticastSocket ms;
    private final java.util.Map<String, java.util.Set<URI>> discoveredUris = new java.util.concurrent.ConcurrentHashMap<>();
    /**
     * @param serviceName the name of the service to announce
     * @param serviceURI  an uri string - representing the contact endpoint of the
     *                    service being announced
     * @param domain domain that the server belongs (ex: "fct", "fcsh",...). It's used to distinguish services from the
     *               same type in different domains.
     * @throws IOException
     * @throws UnknownHostException
     * @throws SocketException
     */
    public Discovery(InetSocketAddress addr, String serviceName, String serviceURI, String domain) throws SocketException, UnknownHostException, IOException {
        this.addr = addr;
        this.serviceName = serviceName;
        this.serviceURI = serviceURI;
        this.domain = domain;

        if (this.addr == null) {
            throw new RuntimeException("A multinet address has to be provided.");
        }

        this.ms = new MulticastSocket(addr.getPort());
        this.ms.joinGroup(addr, NetworkInterface.getByInetAddress(InetAddress.getLocalHost()));
    }

    public Discovery(InetSocketAddress addr) throws SocketException, UnknownHostException, IOException {
        this(addr, null, null, null);
    }

    /**
     * Starts sending service announcements at regular intervals...
     * @throws IOException
     */
    public void start() {
        //If this discovery instance was initialized with information about a service, start the thread that makes the
        //periodic announcement to the multicast address.
        if (this.serviceName != null && this.serviceURI != null) {

            Log.info(String.format("Starting Discovery announcements on: %s for: %s -> %s", addr, serviceName,
                    serviceURI));

            byte[] announceBytes = String.format("%s%s%s%s%s", serviceName, SIGN, domain, DELIMITER, serviceURI).getBytes();
            DatagramPacket announcePkt = new DatagramPacket(announceBytes, announceBytes.length, addr);

            try {
                // start thread to send periodic announcements
                Thread sender = new Thread(() -> {
                    for (;;) {
                        try {
                            ms.send(announcePkt);
                            Thread.sleep(DISCOVERY_ANNOUNCE_PERIOD);
                        } catch (Exception e) {
                            e.printStackTrace();
                            // do nothing
                        }
                    }
                });
                sender.setDaemon(true);
                sender.start();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        // start thread to collect announcements received from the network.
        Thread receiver = new Thread(() -> {
            DatagramPacket pkt = new DatagramPacket(new byte[MAX_DATAGRAM_SIZE], MAX_DATAGRAM_SIZE);
            for (;;) {
                try {
                    pkt.setLength(MAX_DATAGRAM_SIZE);
                    ms.receive(pkt);
                    String msg = new String(pkt.getData(), 0, pkt.getLength());
                    String[] msgElems = msg.split(DELIMITER);
                    if (msgElems.length == 2) { // periodic announcement
                        String name = msgElems[0];
                        String uriStr = msgElems[1];

                        try {
                            URI uri = new URI(uriStr);
                            discoveredUris.putIfAbsent(name, java.util.concurrent.ConcurrentHashMap.newKeySet());

                            boolean isNew = discoveredUris.get(name).add(uri);

                            if (isNew) {
                                synchronized (this) {
                                    this.notifyAll();
                                }
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                } catch (IOException e) {
                    // do nothing
                }
            }
        });
        receiver.setDaemon(true);
        receiver.start();
    }

    /**
     * Returns the known services.
     *
     * @param serviceName the name of the service being discovered
     * @param domain  - service's domain that we want to discover (ex: "fct", "fcm")
     * @return an array of URI with the service instances discovered.
     *
     */
    public URI[] knownUrisOf(String serviceName, String domain) {
        String key = serviceName + "@" + domain;
        discoveredUris.putIfAbsent(key, java.util.concurrent.ConcurrentHashMap.newKeySet());
        java.util.Set<URI> uris = discoveredUris.get(key);

        synchronized (this) {
            while (uris.isEmpty()) {
                try {
                    this.wait(1000); // Espera 1 segundo e volta a verificar
                } catch (InterruptedException e) {
                    // Fio de execução interrompido, não faz mal
                }
            }
        }

        return uris.toArray(new URI[0]);
    }

    // Main just for testing purposes
    public static void main(String[] args) throws Exception {
        Discovery discovery = new Discovery(DISCOVERY_ADDR, "test",
                "http://" + InetAddress.getLocalHost().getHostAddress(), "fct");
        discovery.start();
    }
}
