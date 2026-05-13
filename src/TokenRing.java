import java.io.IOException;
import java.net.*;
import java.util.LinkedList;

public class TokenRing {

    private static final int RECEIVE_TIMEOUT_MS = 5000;
    private static final int MAX_RETRIES = 3;

    private static void loop(DatagramSocket socket, String ip, int port, boolean first) {
        LinkedList<Token.Endpoint> candidates = new LinkedList<>();
        if (first) {
            candidates.add(new Token.Endpoint(ip, port));
        }

        try {
            socket.setSoTimeout(RECEIVE_TIMEOUT_MS);
        } catch (SocketException e) {
            System.out.println("Konnte Timeout nicht setzen: " + e.getMessage());
        }

        while (true) {
            try {
                Token rc = Token.receive(socket);
                System.out.printf("Token: seq=%d, #members=%d", rc.getSequence(), rc.length());
                for (Token.Endpoint endpoint : rc.getRing()) {
                    System.out.printf(" (%s, %d)", endpoint.ip(), endpoint.port());
                }
                System.out.println();

                if (rc.length() == 1) {
                    candidates.add(rc.poll());
                    if (!first) {
                        continue;
                    }
                }

                first = false;

                for (Token.Endpoint candidate : candidates) {
                    rc.append(candidate);
                }
                candidates.clear();

                Token.Endpoint next = rc.poll();
                rc.append(next);
                rc.incrementSequence();

                Thread.sleep(1000);

                boolean sent = false;
                for (int i = 0; i < MAX_RETRIES && !sent; i++) {
                    try {
                        rc.send(socket, next);
                        sent = true;
                    } catch (IOException e) {
                        System.out.printf("Senden an %s:%d fehlgeschlagen – Versuch %d/%d\n",
                                next.ip(), next.port(), i + 1, MAX_RETRIES);
                        Thread.sleep(500);
                    }
                }

                if (!sent) {
                    System.out.printf("Knoten %s:%d wird aus dem Ring entfernt!\n", next.ip(), next.port());
                    rc.removeEndpoint(next);

                    if (rc.length() == 0) {
                        System.out.println("Keine weiteren Knoten im Ring. Beende.");
                        break;
                    }

                    Token.Endpoint fallback = rc.poll();
                    rc.append(fallback);
                    boolean fallbackSent = false;
                    for (int i = 0; i < MAX_RETRIES && !fallbackSent; i++) {
                        try {
                            rc.send(socket, fallback);
                            fallbackSent = true;
                        } catch (IOException e) {
                            System.out.printf("Fallback an %s:%d fehlgeschlagen – Versuch %d/%d\n",
                                    fallback.ip(), fallback.port(), i + 1, MAX_RETRIES);
                            Thread.sleep(500);
                        }
                    }
                    if (!fallbackSent) {
                        System.out.printf("Auch Fallback-Knoten %s:%d nicht erreichbar.\n",
                                fallback.ip(), fallback.port());
                        rc.removeEndpoint(fallback);
                    }
                    continue;
                }

            } catch (SocketTimeoutException e) {
                System.out.println("Timeout: Kein Token empfangen. Möglicherweise Token verloren.");

            } catch (IOException e) {
                System.out.println("Fehler beim Empfangen: " + e.getMessage());
            } catch (Exception e) {
                System.out.println("Fehler: " + e.getMessage());
            }
        }
    }

    public static void main(String[] args) {
        try (DatagramSocket socket = new DatagramSocket()) {
            socket.connect(InetAddress.getByName("8.8.8.8"), 10002);
            String ip = socket.getLocalAddress().getHostAddress();
            socket.disconnect();
            int port = socket.getLocalPort();
            System.out.printf("UDP-Endpunkt: (%s, %d)\n", ip, port);

            if (args.length == 0) {
                loop(socket, ip, port, true);
            } else if (args.length == 2) {
                Token rc = new Token().append(ip, port);
                rc.send(socket, args[0], Integer.parseInt(args[1]));
                loop(socket, ip, port, false);
            } else {
                System.out.println("Verwendung: \"java TokenRing\" oder \"java TokenRing <ip> <port>\"");
            }
        } catch (SocketException e) {
            System.out.println("Fehler beim Erstellen des Sockets: " + e.getMessage());
        } catch (UnknownHostException e) {
            System.out.println("Fehler bei IP-Ermittlung: " + e.getMessage());
        } catch (IOException e) {
            System.out.println("IO-Fehler: " + e.getMessage());
        }
    }
}
