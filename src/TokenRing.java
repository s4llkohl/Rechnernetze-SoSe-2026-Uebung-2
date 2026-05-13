import javax.xml.crypto.Data;
import java.io.IOException;
import java.net.*;
import java.util.LinkedList;


public class TokenRing {

//    private static void loop(DatagramSocket socket, String ip, int port, boolean first){
//        LinkedList<Token.Endpoint> candidates = new LinkedList<>();
//        if (first) {
//            candidates.add(new Token.Endpoint(ip, port));
//        }
//        while (true) {
//            try {
//                Token rc = Token.receive(socket);
//                System.out.printf("Token: seq=%d, #members=%d", rc.getSequence(), rc.length());
//                for (Token.Endpoint endpoint : rc.getRing()) {
//                    System.out.printf(" (%s, %d)", endpoint.ip(), endpoint.port());
//                }
//                System.out.println();
//                if (rc.length() == 1) {
//                    candidates.add(rc.poll());
//                    if (!first) {
//                        continue;
//                    }
//                }
//                first = false;
//                for (Token.Endpoint candidate : candidates) {
//                    rc.append(candidate);
//                }
//                candidates.clear();
//                Token.Endpoint next = rc.poll();
//                rc.append(next);
//                rc.incrementSequence();
//                Thread.sleep(1000);
//                rc.send(socket, next);
//
//                boolean sent = false;
//                int maxRetries = 3;
//                for (int i = 0; i < maxRetries && !sent; i++) {
//                    try {
//                        rc.send(socket, next);
//                        sent = true;
//                    } catch (IOException e) {
//                        System.out.printf("Failed to sent to %s:%d – Take: %d/%d\n",
//                                next.ip(), next.port(), i + 1, maxRetries);
//                        Thread.sleep(500);
//                    }
//                }
//                //Hier wird eine maximale Anzahl von Versuchen definiert, um einen bestimmten Knoten zu erreichen.
//                //Wenn es scheitert, soll der entsprechende Endpoint aus der Queue im nächsten Schritt gelöscht werde
//                if (!sent) {
//                    System.out.printf("Token %s:%d removed!\n", next.ip(), next.port());
//                    rc.removeEndpoint(next);
//
//                    if (rc.length() == 0) {
//                        System.out.println("No more Tokens found.");
//                        break;
//                    }
//                    continue;
//                }
//
//            }
//            catch (IOException e) {
//                System.out.println("Error receiving packet: " + e.getMessage());
//            }
//            catch (Exception e) {
//                System.out.println("Error: " + e.getMessage());
//            }
//        }
//    }

    public static boolean trySend(DatagramSocket socket, Token rc, Token.Endpoint next, int n){
        return true;
    }

    //==========Durch KI vorgeschlagene Änderung
    private static void loop(DatagramSocket socket, String ip, int port, boolean first) {
        LinkedList<Token.Endpoint> candidates = new LinkedList<>();
        if (first) {
            candidates.add(new Token.Endpoint(ip, port));
        }

        while (true) {
            try {
                Token rc = Token.receive(socket);
                System.out.printf("Token erhalten: seq=%d, Richtung=%s\n",
                        rc.getSequence(), rc.isReverse() ? "Rückwärts" : "Vorwärts");

                // Neue Teilnehmer hinzufügen
                for (Token.Endpoint candidate : candidates) {
                    rc.append(candidate);
                }
                candidates.clear();

                // Bestimmung des nächsten Knotens basierend auf der Richtung
                Token.Endpoint next;
                LinkedList<Token.Endpoint> ringList = (LinkedList<Token.Endpoint>) rc.getRing();

                int currentIndex = -1;
                for (int i = 0; i < ringList.size(); i++) {
                    if (ringList.get(i).ip().equals(ip) && ringList.get(i).port() == port) {
                        currentIndex = i;
                        break;
                    }
                }

                // Berechne Nachbarn (Modulo sorgt für den Ringschluss)
                if (rc.isReverse()) {
                    // Rückwärts: (index - 1 + size) % size
                    int nextIdx = (currentIndex - 1 + ringList.size()) % ringList.size();
                    next = ringList.get(nextIdx);
                } else {
                    // Vorwärts: (index + 1) % size
                    int nextIdx = (currentIndex + 1) % ringList.size();
                    next = ringList.get(nextIdx);
                }

                rc.incrementSequence();
                Thread.sleep(1000);

                // Sende-Logik mit Fehlerbehandlung
                boolean sent = trySend(socket, rc, next, 3);

                if (!sent) {
                    System.out.printf("Knoten %s:%d nicht erreichbar. Versuche Gegenrichtung...\n", next.ip(), next.port());
                    // Hier greift die Redundanz:
                    rc.removeEndpoint(next);
                    rc.flip(); // Richtung umkehren, um den defekten Knoten zu umgehen

                    // Neuen Nachbarn in Gegenrichtung suchen
                    // (Logik analog zu oben, nur mit geflipptem rc.isReverse())
                    // ...
                }

            } catch (Exception e) {
                System.out.println("Fehler: " + e.getMessage());
            }
        }
    }
    //==========

    public static void main(String[] args) {
        try (DatagramSocket socket = new DatagramSocket()) {
            socket.connect(InetAddress.getByName("8.8.8.8"), 10002);
            String ip = socket.getLocalAddress().getHostAddress();
            socket.disconnect();
            int port = socket.getLocalPort();
            System.out.printf("UDP endpoint is (%s, %d)\n", ip, port);
            if (args.length == 0) {
                loop(socket,ip,port,true);
            }
            else if (args.length == 2) {
                Token rc = new Token().append(ip,port);
                rc.send(socket,args[0],Integer.parseInt(args[1]));
                loop(socket,ip,port,false);
            }
            else {
                System.out.println("Usage: \"java TokenRing\" or \"java TokenRing <ip> <port>\"");
            }
        }
        catch (SocketException e) {
            System.out.println("Error creating socket: " + e.getMessage());
        }
        catch (UnknownHostException e) {
            System.out.println("Error while determining IP address: " + e.getMessage());
        }
        catch (IOException e) {
            System.out.println("IO error: " + e.getMessage());
            System.out.println(e.getStackTrace());
        }
    }
}