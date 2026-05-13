import java.io.IOException;
import java.net.*;
import java.util.LinkedList;

public class TokenRing {

    /**
     * Main loop for each node in the token ring.
     *
     * The ring now maintains TWO ordered lists:
     *   - forwardRing  : the primary ring (token travels forward)
     *   - backwardRing : a reverse copy used as fallback if the next node fails
     *
     * When a node cannot be reached (after maxRetries), it is removed from both
     * rings and the token is forwarded to the node AFTER the failed one, so the
     * ring heals itself automatically.
     */
    private static void loop(DatagramSocket socket, String ip, int port, boolean first) {
        LinkedList<Token.Endpoint> candidates = new LinkedList<>();

        if (first) {
            candidates.add(new Token.Endpoint(ip, port));
        }

        // Secondary backward ring: mirrors the forward ring in reverse order.
        // Each node keeps track of the full ring order so it can skip a failed node.
        LinkedList<Token.Endpoint> backwardRing = new LinkedList<>();

        while (true) {
            try {
                Token rc = Token.receive(socket);

                System.out.printf("Token: seq=%d, #members=%d", rc.getSequence(), rc.length());
                for (Token.Endpoint endpoint : rc.getRing()) {
                    System.out.printf(" (%s, %d)", endpoint.ip(), endpoint.port());
                }
                System.out.println();

                // --- Join phase: collect candidates until ring has >1 member ---
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

                // Update the local backward ring mirror (reverse of current forward ring)
                backwardRing.clear();
                for (Token.Endpoint ep : rc.getRing()) {
                    backwardRing.addFirst(ep); // reverse order = backward ring
                }

                // --- Normal forwarding ---
                Token.Endpoint next = rc.poll();
                rc.append(next); // put it at the back (ring stays intact)
                rc.incrementSequence();
                Thread.sleep(1000);

                boolean sent = false;
                int maxRetries = 3;

                for (int i = 0; i < maxRetries && !sent; i++) {
                    try {
                        rc.send(socket, next);
                        sent = true;
                    } catch (IOException e) {
                        System.out.printf("Failed to send to %s:%d – Attempt: %d/%d\n",
                                next.ip(), next.port(), i + 1, maxRetries);
                        Thread.sleep(500);
                    }
                }

                // --- Failure recovery via backward ring ---
                if (!sent) {
                    System.out.printf("Node %s:%d is unreachable – removing from ring.\n",
                            next.ip(), next.port());

                    rc.removeEndpoint(next);

                    if (rc.length() == 0) {
                        System.out.println("Ring is empty. Shutting down.");
                        break;
                    }

                    // Find the fallback node using the backward ring:
                    // The backward ring is the reverse of the forward ring.
                    // We look for the predecessor of the failed node in the forward ring,
                    // which is the successor in the backward ring – i.e. the next reachable node
                    // we should try forwarding to.
                    Token.Endpoint fallback = findFallback(backwardRing, next, rc);

                    if (fallback != null) {
                        System.out.printf(
                                "Backward ring fallback: forwarding token to %s:%d instead.\n",
                                fallback.ip(), fallback.port());
                        boolean fallbackSent = false;
                        for (int i = 0; i < maxRetries && !fallbackSent; i++) {
                            try {
                                rc.send(socket, fallback);
                                fallbackSent = true;
                            } catch (IOException e) {
                                System.out.printf(
                                        "Fallback send to %s:%d also failed – Attempt: %d/%d\n",
                                        fallback.ip(), fallback.port(), i + 1, maxRetries);
                                Thread.sleep(500);
                            }
                        }
                        if (!fallbackSent) {
                            System.out.printf("Fallback node %s:%d also unreachable.\n",
                                    fallback.ip(), fallback.port());
                            // The next loop iteration will try the next node in line.
                        }
                    } else {
                        System.out.println("No fallback node found via backward ring.");
                    }
                }

            } catch (IOException e) {
                System.out.println("Error receiving packet: " + e.getMessage());
            } catch (Exception e) {
                System.out.println("Error: " + e.getMessage());
            }
        }
    }

    /**
     * Finds a fallback node using the backward ring when 'failed' is unreachable.
     *
     * Strategy: walk the backward ring starting from the position of the failed
     * node. The first node we encounter that is still present in the active ring
     * (rc) is our fallback – it is the predecessor of the failed node in the
     * forward direction, and therefore the next logical hop after the gap.
     *
     * @param backwardRing  reverse-ordered snapshot of the ring taken this round
     * @param failed        the node that could not be reached
     * @param rc            the current token (contains the surviving ring members)
     * @return              a reachable fallback endpoint, or null if none found
     */
    private static Token.Endpoint findFallback(
            LinkedList<Token.Endpoint> backwardRing,
            Token.Endpoint failed,
            Token rc) {

        boolean foundFailed = false;
        for (Token.Endpoint ep : backwardRing) {
            if (ep.ip().equals(failed.ip()) && ep.port() == failed.port()) {
                foundFailed = true;
                continue; // skip the failed node itself
            }
            if (foundFailed) {
                // Check whether this candidate is still alive in the ring
                for (Token.Endpoint alive : rc.getRing()) {
                    if (alive.ip().equals(ep.ip()) && alive.port() == ep.port()) {
                        return alive; // first reachable predecessor → use as fallback
                    }
                }
            }
        }
        return null;
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
        }
    }
}