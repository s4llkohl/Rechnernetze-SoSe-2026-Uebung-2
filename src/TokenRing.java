import java.io.IOException;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.LinkedList;


public class TokenRing {

    private static final int FORWARD_RETRIES = 3;
    private static final int ACK_TIMEOUT_MS = 600;

    private static String ackPayload(int sequence) {
        return "ACK:" + sequence;
    }

    private static boolean isAckPacket(String payload) {
        return payload.startsWith("ACK:");
    }

    private static void sendAck(DatagramSocket socket, Token.Endpoint destination, int sequence) throws IOException {
        byte[] bytes = ackPayload(sequence).getBytes(StandardCharsets.UTF_8);
        InetAddress address = InetAddress.getByName(destination.ip());
        DatagramPacket packet = new DatagramPacket(bytes, bytes.length, address, destination.port());
        socket.send(packet);
    }

    private static boolean waitForAck(DatagramSocket socket, Token.Endpoint expectedSender, int expectedSequence)
            throws IOException {
        int previousTimeout = socket.getSoTimeout();
        socket.setSoTimeout(ACK_TIMEOUT_MS);
        try {
            byte[] buf = new byte[4096];
            DatagramPacket packet = new DatagramPacket(buf, buf.length);
            while (true) {
                socket.receive(packet);
                String payload = new String(packet.getData(), 0, packet.getLength(), StandardCharsets.UTF_8);
                if (!isAckPacket(payload)) {
                    // This packet is not an ACK and will be handled in the next receive cycle.
                    continue;
                }
                String expectedAck = ackPayload(expectedSequence);
                String senderIp = packet.getAddress().getHostAddress();
                int senderPort = packet.getPort();
                boolean senderMatches = expectedSender.ip().equals(senderIp) && expectedSender.port() == senderPort;
                if (senderMatches && expectedAck.equals(payload)) {
                    return true;
                }
            }
        }
        catch (SocketTimeoutException e) {
            return false;
        }
        finally {
            socket.setSoTimeout(previousTimeout);
        }
    }

    private static Token.ReceivedToken receiveNextToken(DatagramSocket socket) throws IOException {
        while (true) {
            byte[] buf = new byte[4096];
            DatagramPacket packet = new DatagramPacket(buf, buf.length);
            socket.receive(packet);
            String payload = new String(packet.getData(), 0, packet.getLength(), StandardCharsets.UTF_8);
            if (isAckPacket(payload)) {
                continue;
            }
            Token token = Token.fromJSON(payload);
            String senderIp = packet.getAddress().getHostAddress();
            int senderPort = packet.getPort();
            System.out.printf("Received %s from %s:%d\n", payload, senderIp, senderPort);
            return new Token.ReceivedToken(token, new Token.Endpoint(senderIp, senderPort));
        }
    }

    private static boolean forwardToNextHealthyNode(DatagramSocket socket, Token token) throws IOException, InterruptedException {
        int remainingCandidates = token.length();
        while (remainingCandidates > 0 && token.length() > 0) {
            Token.Endpoint next = token.poll();
            token.append(next);
            boolean acknowledged = false;

            for (int attempt = 1; attempt <= FORWARD_RETRIES; attempt++) {
                token.send(socket, next);
                acknowledged = waitForAck(socket, next, token.getSequence());
                if (acknowledged) {
                    return true;
                }
                System.out.printf("No ACK from %s:%d (attempt %d/%d)\n",
                        next.ip(), next.port(), attempt, FORWARD_RETRIES);
                Thread.sleep(250);
            }

            System.out.printf("Token endpoint %s:%d removed after missing ACKs.\n", next.ip(), next.port());
            token.removeEndpoint(next);
            remainingCandidates--;
        }
        return false;
    }

    private static void loop(DatagramSocket socket, String ip, int port, boolean first){
        LinkedList<Token.Endpoint> candidates = new LinkedList<>();
        if (first) {
            candidates.add(new Token.Endpoint(ip, port));
        }
        while (true) {
            try {
                Token.ReceivedToken received = receiveNextToken(socket);
                Token rc = received.token();
                sendAck(socket, received.sender(), rc.getSequence());
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
                boolean delivered = forwardToNextHealthyNode(socket, rc);
                if (!delivered && rc.length() == 0) {
                    System.out.println("No reachable token endpoints left.");
                    break;
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