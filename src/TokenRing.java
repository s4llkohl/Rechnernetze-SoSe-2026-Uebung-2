import java.io.IOException;
import java.net.*;
import java.util.LinkedList;


public class TokenRing {

    private static void loop(DatagramSocket socket, String ip, int port, boolean first){
        LinkedList<Token.Endpoint> candidates = new LinkedList<>();
        if (first) {
            candidates.add(new Token.Endpoint(ip, port));
        }
        while (true) {
            try {
                socket.setSoTimeout(5000); // 5 Sekunden Timeout für das Empfangen des Tokens
                Token rc;
                try {
                    rc = Token.receive(socket);
                } catch (SocketTimeoutException e) {
                    System.out.println("Timeout: No Token received. Regenerating Token...");
                    rc = new Token().append(ip, port);
                }
                System.out.printf("Token: seq=%d, #members=%d", rc.getSequence(), rc.length());
                for (Token.Endpoint endpoint : rc.getRing()) {
                    System.out.printf(" (%s, %d)", endpoint.ip(), endpoint.port());
                }
                System.out.println();

                // Eigenen Endpunkt bestimmen (wir sind immer der, der gerade empfangen hat,
                // aber im Ring stehen wir irgendwo).
                // Da der Sender uns das Token geschickt hat, stehen wir bereits im Ring.
                // In der ursprünglichen Logik wurden neue Kandidaten hinzugefügt.
                if (rc.length() == 1 && candidates.isEmpty()) {
                    // Falls wir alleine sind und noch keine Kandidaten haben
                    // (Sollte eigentlich nicht passieren, wenn wir im Ring sind)
                }

                if (!candidates.isEmpty()) {
                    for (Token.Endpoint candidate : candidates) {
                        // Prüfen, ob der Kandidat bereits im Ring ist
                        boolean exists = false;
                        for (Token.Endpoint e : rc.getRing()) {
                            if (e.ip().equals(candidate.ip()) && e.port() == candidate.port()) {
                                exists = true;
                                break;
                            }
                        }
                        if (!exists) {
                            rc.append(candidate);
                        }
                    }
                    candidates.clear();
                }

                first = false;

                // Um Nachfolger zu bestimmen, müssen wir uns selbst im Ring finden
                LinkedList<Token.Endpoint> ringList = (LinkedList<Token.Endpoint>) rc.getRing();
                int myIndex = -1;
                for (int i = 0; i < ringList.size(); i++) {
                    Token.Endpoint e = ringList.get(i);
                    if (e.ip().equals(ip) && e.port() == port) {
                        myIndex = i;
                        break;
                    }
                }

                if (myIndex == -1) {
                    // Wir sind nicht im Ring? Dann fügen wir uns hinzu.
                    rc.append(new Token.Endpoint(ip, port));
                    ringList = (LinkedList<Token.Endpoint>) rc.getRing();
                    myIndex = ringList.size() - 1;
                }

                if (rc.length() <= 1) {
                    System.out.println("Waiting for more members...");
                    // Wenn wir allein sind, können wir nicht sinnvoll weitergeben
                    // Wir behalten das Token und warten auf neue Teilnehmer
                    // In dieser Implementierung fügen wir uns selbst hinzu, falls nicht vorhanden
                    // und warten in der receive() Methode auf ein Token (das evtl. von einem neuen Teilnehmer kommt)
                    // ODER wir senden es an uns selbst nach einer Pause.
                    Thread.sleep(1000);
                    if (rc.length() == 1) {
                        // Falls wir alleine sind, einfach weitermachen (nächste receive() Runde)
                        // Wenn wir das Token an uns selbst senden, triggern wir receive() sofort wieder
                        rc.send(socket, new Token.Endpoint(ip, port));
                        continue;
                    }
                }

                // Berechne rechten und linken Nachbarn
                int nextRightIndex = (myIndex + 1) % ringList.size();
                int nextLeftIndex = (myIndex - 1 + ringList.size()) % ringList.size();

                Token.Endpoint next_right = ringList.get(nextRightIndex);
                Token.Endpoint next_left = ringList.get(nextLeftIndex);

                rc.incrementSequence();
                Thread.sleep(1000);

                boolean sent = false;
                // Zuerst rechts versuchen
                try {
                    rc.send(socket, next_right);
                    sent = true;
                } catch (IOException sendEx) {
                    System.out.printf("Failed to send to right neighbor %s:%d. Error: %s\n",
                            next_right.ip(), next_right.port(), sendEx.getMessage());

                    // Optional: ausgefallenen Knoten entfernen
                    rc.removeEndpoint(next_right);
                    System.out.printf("Removed node %s:%d from ring.\n", next_right.ip(), next_right.port());

                    if (rc.length() > 0) {
                        // Nach Entfernen neuen linken Nachbarn berechnen (falls nötig)
                        ringList = (LinkedList<Token.Endpoint>) rc.getRing();
                        // Wir müssen myIndex neu finden, da sich die Liste geändert hat
                        myIndex = -1;
                        for (int i = 0; i < ringList.size(); i++) {
                            Token.Endpoint endpoint = ringList.get(i);
                            if (endpoint.ip().equals(ip) && endpoint.port() == port) {
                                myIndex = i;
                                break;
                            }
                        }
                        if (myIndex != -1) {
                            nextLeftIndex = (myIndex - 1 + ringList.size()) % ringList.size();
                            next_left = ringList.get(nextLeftIndex);

                            System.out.printf("Trying left neighbor %s:%d...\n", next_left.ip(), next_left.port());
                            try {
                                rc.send(socket, next_left);
                                sent = true;
                            } catch (IOException sendEx2) {
                                System.out.printf("Failed to send to left neighbor %s:%d as well.\n",
                                        next_left.ip(), next_left.port());
                            }
                        }
                    }
                }

                if (!sent && rc.length() > 1) {
                     System.out.println("Could not send token to any neighbor.");
                }

            }
            catch (IOException e) {
                System.out.println("Error receiving packet: " + e.getMessage());
            }
            catch (Exception e) {
                System.out.println("Error: " + e.getMessage());
            }
        }
    }

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