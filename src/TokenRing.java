import java.io.IOException;
import java.net.*;
import java.util.LinkedList;


public class TokenRing {

    private static final int RECEIVE_TIMEOUT_MS = 5000;
    private static final int RETRY_DELAY_MS = 500;
    private static final int MAX_RETRIES = 3;

    private static void loop(DatagramSocket socket, String ip, int port, boolean isLeader) {
        final boolean wasLeader = isLeader;
        LinkedList<Token.Endpoint> candidates = new LinkedList<>();
        if (isLeader) {
            candidates.add(new Token.Endpoint(ip, port));
        }

        try {
            socket.setSoTimeout(RECEIVE_TIMEOUT_MS);
        } catch (SocketException e) {
            System.out.println("Warning: could not set receive timeout: " + e.getMessage());
        }

        Token lastToken = null;

        while (true) {
            try {
                Token rc = Token.receive(socket);
                lastToken = rc;

                System.out.printf("Token: seq=%d, #members=%d", rc.getSequence(), rc.length());
                for (Token.Endpoint endpoint : rc.getRing()) {
                    System.out.printf(" (%s, %d)", endpoint.ip(), endpoint.port());
                }
                System.out.println();

                if (rc.length() == 1) {
                    candidates.add(rc.poll());
                    if (!isLeader) {
                        continue;
                    }
                }
                isLeader = false;
                for (Token.Endpoint candidate : candidates) {
                    rc.append(candidate);
                }
                candidates.clear();

                Token.Endpoint next = rc.poll();
                rc.append(next);
                rc.incrementSequence();
                Thread.sleep(1000);

                boolean sent = false;
                while (!sent) {
                    int retries = 0;
                    while (retries < MAX_RETRIES && !sent) {
                        try {
                            rc.send(socket, next);
                            sent = true;
                        } catch (IOException e) {
                            System.out.printf("Failed to send to %s:%d – Attempt %d/%d\n",
                                    next.ip(), next.port(), retries + 1, MAX_RETRIES);
                            retries++;
                            Thread.sleep(RETRY_DELAY_MS);
                        }
                    }
                    if (!sent) {
                        System.out.printf("Node %s:%d removed from ring.\n", next.ip(), next.port());
                        rc.removeEndpoint(next);
                        if (rc.length() == 0) {
                            System.out.println("Ring is empty, shutting down.");
                            return;
                        }
                        next = rc.poll();
                        rc.append(next);
                    }
                }

            } catch (SocketTimeoutException e) {
                System.out.println("Timeout: no token received, token may be lost.");
                if (wasLeader && lastToken != null) {
                    System.out.println("Regenerating token as original ring leader...");
                    try {
                        Token regenerated = Token.fromJSON(lastToken.toJSON());
                        regenerated.incrementSequence();
                        Token.Endpoint first = regenerated.first();
                        if (first != null) {
                            regenerated.send(socket, first);
                            System.out.println("Token regenerated.");
                        }
                    } catch (Exception ex) {
                        System.out.println("Could not regenerate token: " + ex.getMessage());
                    }
                }
            } catch (IOException e) {
                System.out.println("Error receiving packet: " + e.getMessage());
            } catch (Exception e) {
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