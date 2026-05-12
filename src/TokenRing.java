import java.io.IOException;
import java.net.*;
import java.util.LinkedList;

public class TokenRing {

    private static final int MAX_RETRIES = 3;
    private static final int ACK_TIMEOUT_MS = 1000;
    private static final int TOKEN_DELAY_MS = 1000;

    private static void printToken(Token token) {
        System.out.printf("Token: seq=%d, #members=%d", token.getSequence(), token.length());
        for (Token.Endpoint endpoint : token.getRing()) {
            System.out.printf(" (%s, %d)", endpoint.ip(), endpoint.port());
        }
        System.out.println();
    }

    private static boolean waitForAck(DatagramSocket socket, Token.Endpoint expectedSender, int sequence) throws IOException {
        int oldTimeout = socket.getSoTimeout();
        socket.setSoTimeout(ACK_TIMEOUT_MS);
        try {
            while (true) {
                Token.ReceivedToken message = Token.receiveMessage(socket);
                Token answer = message.token();
                Token.Endpoint sender = message.sender();

                if (answer.isAck()
                        && answer.getSequence() == sequence
                        && sender.equals(expectedSender)) {
                    System.out.printf("ACK for seq=%d from %s:%d received.\n",
                            sequence, sender.ip(), sender.port());
                    return true;
                }

                System.out.printf("Ignoring unexpected packet from %s:%d while waiting for ACK.\n",
                        sender.ip(), sender.port());
            }
        } catch (SocketTimeoutException e) {
            return false;
        } finally {
            socket.setSoTimeout(oldTimeout);
        }
    }

    private static boolean sendWithAck(DatagramSocket socket, Token token, Token.Endpoint next) throws InterruptedException {
        for (int i = 1; i <= MAX_RETRIES; i++) {
            try {
                token.send(socket, next);
                if (waitForAck(socket, next, token.getSequence())) {
                    return true;
                }
                System.out.printf("No ACK from %s:%d - try %d/%d\n",
                        next.ip(), next.port(), i, MAX_RETRIES);
            } catch (IOException e) {
                System.out.printf("Failed to send to %s:%d - try %d/%d: %s\n",
                        next.ip(), next.port(), i, MAX_RETRIES, e.getMessage());
            }
            Thread.sleep(500);
        }
        return false;
    }

    private static void forwardToken(DatagramSocket socket, Token token) throws InterruptedException {
        while (token.length() > 0) {
            Token.Endpoint next = token.poll();
            token.append(next);
            token.incrementSequence();
            Thread.sleep(TOKEN_DELAY_MS);

            if (sendWithAck(socket, token, next)) {
                return;
            }

            System.out.printf("Node %s:%d did not answer and is removed from the ring.\n",
                    next.ip(), next.port());
            token.removeEndpoint(next);
        }

        System.out.println("No more reachable nodes in the ring. Token will not be forwarded.");
    }

    private static void loop(DatagramSocket socket, String ip, int port, boolean first) {
        LinkedList<Token.Endpoint> candidates = new LinkedList<>();
        if (first) {
            candidates.add(new Token.Endpoint(ip, port));
        }

        while (true) {
            try {
                Token.ReceivedToken message = Token.receiveMessage(socket);
                Token token = message.token();

                if (token.isAck()) {
                    // Ein ACK gehört zu einer laufenden sendWithAck()-Phase. Wenn es hier ankommt,
                    // ist es verspätet und kann ignoriert werden.
                    continue;
                }

                // Empfang sofort bestätigen. So weiß der Vorgänger, dass dieser Knoten noch lebt.
                Token.sendAck(socket, message.sender(), token.getSequence());

                printToken(token);

                // Ein Token mit nur einem Eintrag wird als Beitrittsanfrage interpretiert.
                if (token.length() == 1) {
                    candidates.add(token.poll());
                    if (!first) {
                        continue;
                    }
                }

                first = false;

                for (Token.Endpoint candidate : candidates) {
                    if (!token.getRing().contains(candidate)) {
                        token.append(candidate);
                    }
                }
                candidates.clear();

                forwardToken(socket, token);
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
                loop(socket, ip, port, true);
            } else if (args.length == 2) {
                Token token = new Token().append(ip, port);
                token.send(socket, args[0], Integer.parseInt(args[1]));
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
        }
    }
}