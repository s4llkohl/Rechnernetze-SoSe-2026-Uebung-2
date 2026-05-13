import java.io.IOException;
import java.net.*;
import java.util.LinkedList;

public class TokenRing {

    // Konfigurationsparameter für Fehlertoleranz
    private static final int RECEIVE_TIMEOUT_MS = 10000;    // 10 Sekunden Timeout für Token-Empfang
    private static final int ACK_TIMEOUT_MS = 3000;         // 3 Sekunden Timeout für ACK
    private static final int MAX_RETRIES = 3;               // Maximale Wiederholungsversuche
    private static final int RETRY_DELAY_MS = 500;          // Verzögerung zwischen Versuchen
    private static final int TOKEN_DELAY_MS = 1000;         // Verzögerung vor Token-Weiterleitung

    /**
     * Sendet das Token an einen Endpoint und wartet auf ACK.
     * @return true wenn ACK empfangen, false bei Fehler/Timeout
     */
    private static boolean sendWithAck(DatagramSocket socket, Token token, Token.Endpoint target) {
        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try {
                // Token senden
                token.send(socket, target);

                // Auf ACK warten
                Token response = Token.receive(socket, ACK_TIMEOUT_MS);

                if (response != null && response.getMessageType() == Token.MessageType.ACK) {
                    System.out.printf("ACK received from %s:%d\n", target.ip(), target.port());
                    return true;
                } else if (response != null) {
                    // Unerwartete Nachricht empfangen (könnte ein anderes Token sein)
                    System.out.printf("Unexpected message received (type: %s), retrying...\n",
                            response.getMessageType());
                }

            } catch (IOException e) {
                System.out.printf("Send/receive error to %s:%d (attempt %d/%d): %s\n",
                        target.ip(), target.port(), attempt, MAX_RETRIES, e.getMessage());
            }

            // Warten vor erneutem Versuch
            if (attempt < MAX_RETRIES) {
                try {
                    Thread.sleep(RETRY_DELAY_MS);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    return false;
                }
            }
        }

        System.out.printf("Failed to reach %s:%d after %d attempts\n",
                target.ip(), target.port(), MAX_RETRIES);
        return false;
    }

    /**
     * Sendet ACK zurück an den Absender.
     */
    private static void sendAck(DatagramSocket socket, Token receivedToken,
                                String senderIp, int senderPort) {
        try {
            Token ack = Token.createAck(receivedToken.getSequence());
            ack.send(socket, senderIp, senderPort);
            System.out.printf("ACK sent to %s:%d\n", senderIp, senderPort);
        } catch (IOException e) {
            System.out.println("Failed to send ACK: " + e.getMessage());
        }
    }

    /**
     * Hauptschleife mit Fehlertoleranz.
     */
    private static void loop(DatagramSocket socket, String myIp, int myPort, boolean isFirst) {
        LinkedList<Token.Endpoint> candidates = new LinkedList<>();
        Token.Endpoint myEndpoint = new Token.Endpoint(myIp, myPort);

        if (isFirst) {
            candidates.add(myEndpoint);
        }

        while (true) {
            try {
                // Token empfangen mit Timeout
                Token rc = receiveTokenWithTimeout(socket, isFirst ? 0 : RECEIVE_TIMEOUT_MS);

                if (rc == null) {
                    // Timeout - Ring könnte unterbrochen sein
                    System.out.println("Timeout waiting for token. Ring may be broken.");

                    // Wenn wir der erste Knoten sind und Kandidaten haben,
                    // versuchen wir den Ring neu zu starten
                    if (isFirst && !candidates.isEmpty()) {
                        System.out.println("Attempting to restart ring as leader...");
                        Token newToken = new Token();
                        newToken.append(myEndpoint);
                        for (Token.Endpoint candidate : candidates) {
                            if (!candidate.equals(myEndpoint)) {
                                newToken.append(candidate);
                            }
                        }
                        candidates.clear();

                        if (newToken.length() > 1) {
                            Token.Endpoint next = newToken.poll();
                            newToken.append(next);
                            if (sendWithAck(socket, newToken, next)) {
                                continue;
                            }
                        }
                    }
                    continue;
                }

                // ACK-Nachrichten ignorieren (werden in sendWithAck verarbeitet)
                if (rc.getMessageType() == Token.MessageType.ACK) {
                    continue;
                }

                // Token-Informationen ausgeben
                System.out.printf("Token: seq=%d, #members=%d", rc.getSequence(), rc.length());
                for (Token.Endpoint endpoint : rc.getRing()) {
                    System.out.printf(" (%s, %d)", endpoint.ip(), endpoint.port());
                }
                System.out.println();

                // Einzelner Knoten im Token = neuer Kandidat will beitreten
                if (rc.length() == 1) {
                    Token.Endpoint newCandidate = rc.poll();
                    if (!candidates.contains(newCandidate) && !newCandidate.equals(myEndpoint)) {
                        candidates.add(newCandidate);
                        System.out.printf("New candidate added: %s:%d\n",
                                newCandidate.ip(), newCandidate.port());
                    }
                    if (!isFirst) {
                        continue;
                    }
                }

                isFirst = false;

                // Kandidaten zum Ring hinzufügen
                for (Token.Endpoint candidate : candidates) {
                    if (!containsEndpoint(rc, candidate)) {
                        rc.append(candidate);
                        System.out.printf("Added candidate to ring: %s:%d\n",
                                candidate.ip(), candidate.port());
                    }
                }
                candidates.clear();

                // Nächsten Knoten bestimmen und Token weiterleiten
                Token.Endpoint next = rc.poll();
                rc.append(next);
                rc.incrementSequence();

                // Verzögerung vor Weiterleitung
                Thread.sleep(TOKEN_DELAY_MS);

                // Token senden mit ACK-Bestätigung und Fehlerbehandlung
                boolean sent = false;
                while (!sent && rc.length() > 0) {
                    if (sendWithAck(socket, rc, next)) {
                        sent = true;
                    } else {
                        // Knoten ist ausgefallen - aus Ring entfernen
                        System.out.printf("Removing failed node: %s:%d\n", next.ip(), next.port());
                        rc.removeEndpoint(next);

                        if (rc.length() == 0) {
                            System.out.println("No more nodes in ring. Waiting for new connections...");
                            isFirst = true;
                            candidates.add(myEndpoint);
                            break;
                        }

                        // Nächsten Knoten versuchen
                        next = rc.poll();
                        rc.append(next);
                        System.out.printf("Trying next node: %s:%d\n", next.ip(), next.port());
                    }
                }

            } catch (IOException e) {
                System.out.println("Error receiving packet: " + e.getMessage());
            } catch (InterruptedException e) {
                System.out.println("Thread interrupted: " + e.getMessage());
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                System.out.println("Error: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    /**
     * Empfängt ein Token und sendet automatisch ACK zurück.
     */
    private static Token receiveTokenWithTimeout(DatagramSocket socket, int timeoutMs)
            throws IOException {
        int oldTimeout = socket.getSoTimeout();
        try {
            if (timeoutMs > 0) {
                socket.setSoTimeout(timeoutMs);
            }

            byte[] buf = new byte[4096];
            DatagramPacket packet = new DatagramPacket(buf, buf.length);
            socket.receive(packet);

            String json = new String(packet.getData(), 0, packet.getLength());
            Token token = Token.fromJSON(json);

            System.out.printf("Received %s from %s:%d\n", json,
                    packet.getAddress().getHostAddress(), packet.getPort());

            // ACK zurücksenden wenn es ein Token ist (kein ACK auf ACK)
            if (token.getMessageType() == Token.MessageType.TOKEN) {
                sendAck(socket, token,
                        packet.getAddress().getHostAddress(), packet.getPort());
            }

            return token;

        } catch (SocketTimeoutException e) {
            return null;
        } finally {
            socket.setSoTimeout(oldTimeout);
        }
    }

    /**
     * Prüft ob ein Endpoint bereits im Token enthalten ist.
     */
    private static boolean containsEndpoint(Token token, Token.Endpoint endpoint) {
        for (Token.Endpoint e : token.getRing()) {
            if (e.ip().equals(endpoint.ip()) && e.port() == endpoint.port()) {
                return true;
            }
        }
        return false;
    }

    public static void main(String[] args) {
        try (DatagramSocket socket = new DatagramSocket()) {
            // Eigene IP-Adresse ermitteln
            socket.connect(InetAddress.getByName("8.8.8.8"), 10002);
            String ip = socket.getLocalAddress().getHostAddress();
            socket.disconnect();
            int port = socket.getLocalPort();

            System.out.printf("UDP endpoint is (%s, %d)\n", ip, port);
            System.out.println("Fault-tolerant Token Ring started.");
            System.out.printf("Configuration: RECEIVE_TIMEOUT=%dms, ACK_TIMEOUT=%dms, MAX_RETRIES=%d\n",
                    RECEIVE_TIMEOUT_MS, ACK_TIMEOUT_MS, MAX_RETRIES);

            if (args.length == 0) {
                // Als Ring-Leader starten
                System.out.println("Starting as ring leader...");
                loop(socket, ip, port, true);
            } else if (args.length == 2) {
                // Einem bestehenden Ring beitreten
                System.out.printf("Joining ring via %s:%s...\n", args[0], args[1]);
                Token rc = new Token().append(ip, port);
                rc.send(socket, args[0], Integer.parseInt(args[1]));
                loop(socket, ip, port, false);
            } else {
                System.out.println("Usage: \"java TokenRing\" or \"java TokenRing <ip> <port>\"");
            }
        } catch (SocketException e) {
            System.out.println("Error creating socket: " + e.getMessage());
        } catch (UnknownHostException e) {
            System.out.println("Error while determining IP address: " + e.getMessage());
        } catch (IOException e) {
            System.out.println("IO error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
