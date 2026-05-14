import java.io.IOException;
import java.net.*;
import java.util.LinkedList;

public class TokenRing {

    private static final int TOKEN_DELAY_MS = 1000;
    private static final int MIN_TIMEOUT_MS = 5000;
    private static final int TIMEOUT_PER_MEMBER_MS = 2500;

    private static void printToken(Token rc) {
        System.out.printf("Token: seq=%d, #members=%d", rc.getSequence(), rc.length());
        for (Token.Endpoint endpoint : rc.getRing()) {
            System.out.printf(" (%s, %d)", endpoint.ip(), endpoint.port());
        }
        System.out.println();
    }

    private static int timeoutFor(Token token) {
        return Math.max(MIN_TIMEOUT_MS, token.length() * TIMEOUT_PER_MEMBER_MS);
    }

    private static Token.Endpoint sendToNext(DatagramSocket socket, Token rc)
            throws IOException, InterruptedException {

        Token.Endpoint next = rc.poll();

        if (next == null) {
            return null;
        }

        rc.append(next);
        rc.incrementSequence();

        Thread.sleep(TOKEN_DELAY_MS);
        rc.send(socket, next);

        return next;
    }

    private static void loop(DatagramSocket socket, String ip, int port, boolean first) {
        LinkedList<Token.Endpoint> candidates = new LinkedList<>();

        if (first) {
            candidates.add(new Token.Endpoint(ip, port));
        }

        Token lastToken = null;
        Token.Endpoint lastNext = null;

        while (true) {
            try {
                Token rc = Token.receive(socket);
                printToken(rc);

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

                Token.Endpoint next = sendToNext(socket, rc);

                if (next != null) {
                    lastToken = rc;
                    lastNext = next;
                    socket.setSoTimeout(timeoutFor(rc));
                }
            }
            catch (SocketTimeoutException e) {
                if (lastToken == null || lastNext == null) {
                    continue;
                }

                System.out.printf(
                        "No token returned in time. Assuming node (%s, %d) failed.%n",
                        lastNext.ip(),
                        lastNext.port()
                );

                boolean removed = lastToken.removeEndpoint(lastNext);

                if (removed) {
                    System.out.printf(
                            "Removed failed node (%s, %d) from the ring.%n",
                            lastNext.ip(),
                            lastNext.port()
                    );
                }

                if (lastToken.length() <= 1) {
                    System.out.println("Only one node remains in the ring. Waiting for new nodes.");
                    lastNext = null;

                    try {
                        socket.setSoTimeout(0);
                    }
                    catch (SocketException ex) {
                        System.out.println("Could not reset socket timeout: " + ex.getMessage());
                    }

                    continue;
                }

                try {
                    Token.Endpoint next = sendToNext(socket, lastToken);

                    if (next != null) {
                        lastNext = next;
                        socket.setSoTimeout(timeoutFor(lastToken));
                    }
                }
                catch (IOException ex) {
                    System.out.println("Error while forwarding token after failure: " + ex.getMessage());
                }
                catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    System.out.println("Interrupted while forwarding token after failure.");
                    break;
                }
            }
            catch (IOException e) {
                System.out.println("Error receiving packet: " + e.getMessage());
            }
            catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.out.println("Interrupted.");
                break;
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

            System.out.printf("UDP endpoint is (%s, %d)%n", ip, port);

            if (args.length == 0) {
                loop(socket, ip, port, true);
            }
            else if (args.length == 2) {
                Token rc = new Token().append(ip, port);
                rc.send(socket, args[0], Integer.parseInt(args[1]));
                loop(socket, ip, port, false);
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
        }
    }
}
