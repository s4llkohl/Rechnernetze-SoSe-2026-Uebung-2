import java.io.IOException;
import java.net.*;
import java.util.LinkedList;


public class TokenRing {

    private static final int MAX_RETRIES = 3;
    private static final long PROCESSING_DELAY_MS = 1000;

    private static void pause(long millis) throws InterruptedException {
        Thread.sleep(millis);
    }

    private static boolean sendWithRetries(DatagramSocket socket, Token token, Token.Endpoint next)
            throws InterruptedException {
        for (int i = 0; i < MAX_RETRIES; i++) {
            try {
                token.send(socket, next);
                return true;
            } catch (IOException e) {
                System.out.printf("Failed to sent to %s:%d – Take: %d/%d\n",
                        next.ip(), next.port(), i + 1, MAX_RETRIES);
                Thread.sleep(500);
            }
        }
        return false;
    }

    private static boolean forwardToken(DatagramSocket socket, Token token, Token.Endpoint self)
            throws InterruptedException {
        int failures = 0;
        while (token.length() > 1 && failures < token.length()) {
            Token.Endpoint next = token.nextEndpoint(self);
            if (next == null) {
                return false;
            }
            if (sendWithRetries(socket, token, next)) {
                return true;
            }

            System.out.printf("Token %s:%d removed!\n", next.ip(), next.port());
            token.removeEndpoint(next);
            token.flipDirection();
            failures++;
        }
        return false;
    }

    private static void loop(DatagramSocket socket, String ip, int port, boolean first){
        LinkedList<Token.Endpoint> candidates = new LinkedList<>();
        Token.Endpoint self = new Token.Endpoint(ip, port);
        if (first) {
            candidates.add(self);
        }
        while (true) {
            try {
                Token rc = Token.receive(socket);
                System.out.printf("Token: seq=%d, #members=%d, direction=%s", rc.getSequence(), rc.length(), rc.getDirection());
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
                if (rc.indexOf(self) < 0) {
                    rc.append(self);
                }
                rc.incrementSequence();
                pause(PROCESSING_DELAY_MS);
                boolean sent = forwardToken(socket, rc, self);

                if (!sent) {
                    if (rc.length() == 0) {
                        System.out.println("No more Tokens found.");
                        break;
                    }
                    System.out.println("Unable to reach any neighbor in either direction.");
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
            e.printStackTrace();
        }
    }
}