Übung 2: Linus Köbel

Aufgabe 1:
tcp.payload matches "[a-z0-9]{8}"
http matches "[a-z0-9]{8}"
http.request.method == "GET" && http matches "[a-z0-9]{8}"

Aufgabe 2: 
Der eigene Rechner schickt Packete raus wobei, die Packete haben unterschiedliche TTL gestezt von 1 bis n. Wobei 1 der nächste Hop ist und n der Zielrechner. 
TTL wird bei jedem packet um 1 reduziert und wenn TTL 0 ist, wird das Packet verworfen und eine ICMP-Meldung geschickt. Dadurch weiß mein Rechner welche Rechner auf dem Weg liegen unter der
Voraussetzung das ICMP-Antoworten aktiviert sind und Traceroute gibt das aus. Ansonsten weiß man nur das ein Rechner dazwischen liegt und Traceroute gibt * aus. 

b)
traceroute to terra-web.anu.edu.au (130.56.67.33), 64 hops max, 40 byte packets
 1  vwlanclients2 (136.199.244.1)  4.215 ms  4.976 ms  3.513 ms
 2  opnsense1-transfer (136.199.1.99)  3.710 ms  7.178 ms  3.449 ms
 3  g-uni-tr-1.rlp-net.net (217.198.241.193)  7.410 ms  7.857 ms  8.397 ms
 4  g-uni-ko-1.rlp-net.net (217.198.247.149)  8.156 ms  8.261 ms  10.728 ms
 5  g-hbf-ko-1.rlp-net.net (217.198.246.105)  7.062 ms  7.028 ms  6.795 ms
 6  g-hbf-mz-2.rlp-net.net (217.198.240.21)  9.217 ms  9.161 ms  9.812 ms
 7  g-interxion-2.rlp-net.net (217.198.240.13)  90.740 ms  7.986 ms  7.226 ms
 8  g-interxion-4.rlp-net.net (217.198.246.250)  6.977 ms  7.132 ms  7.170 ms
 9  decix-ae3.mpr1.fra4.de.zip.zayo.com (80.81.194.26)  7.149 ms  8.411 ms  8.455 ms
10  * * *
11  * * *
12  * * *
13  * * *
14  * * *
15  * * *
16  * * *
17  * * *
18  * * *
19  ae27.mpr1.sea1.us.zip.zayo.com (64.125.29.1)  149.630 ms  144.776 ms  144.256 ms
20  64.125.193.130.i223.above.net (64.125.193.130)  142.969 ms  142.602 ms  224.531 ms
21  et-10-0-5.170.pe1.brwy.nsw.aarnet.net.au (113.197.15.62)  297.934 ms  263.296 ms  356.830 ms
22  et-0-3-0.pe1.actn.act.aarnet.net.au (113.197.15.11)  313.806 ms  325.094 ms  291.590 ms
23  138.44.172.29 (138.44.172.29)  266.421 ms  362.732 ms  303.822 ms
24  vlan-2100-palo.anu.edu.au (150.203.201.33)  419.297 ms  268.465 ms  268.231 ms
25  alumni.anu.edu.au (130.56.67.33)  269.527 ms  309.583 ms  267.098 ms
26  * * -common-self-ip.anu.edu.au (130.56.66.249)  1110.367 ms !H
27  -common-self-ip.anu.edu.au (130.56.66.249)  286.550 ms !H *  2084.621 ms !H

c)
von Trier nach Koblenz nach Mainz nach Frankfurt nach Frankreich, dann sehr viel *

Aufgabe 3
der TCP Scan ist deutlich schneller, weil TCP durch den Three-Way-Handshake immer eine antwort schickt, falls der Port offen ist. Zusätzlich bietet nmap beim TCP Scan die Möglichkeit den 
Three-Way-Handshake nicht vollständig durchführen, sondern kann ihn verfrüht abbrechen, weil man ja nur herausfinden will ob der Port offen ist und nicht eine Verbindung aufbauen will. 
UDP-Scans sind sehr langsam, weil UDP im Gegensatz zu TCP nicht automatisch antwortet. Wenn Nmap bei einem UDP-Scan keine Antwort bekommt, weiß es nicht sicher, ob der Port offen ist, 
gefiltert wird oder das Paket verloren gegangen ist. Deshalb muss Nmap warten, bis ein Timeout abläuft, und versucht es teilweise erneut. Dadurch dauert der UDP-Scan deutlich länger als der TCP-Scan.

Aufgabe 4
Damit der Token Ring auch beim Ausfall eines Knotens weiter funktioniert, wird beim Weitergeben des Tokens geprüft, ob der nächste Knoten erreichbar ist. Dazu wird das Token nicht nur einmal gesendet, 
sondern bei einem Fehler mehrfach erneut übertragen. Wenn alle Sendeversuche fehlschlagen, wird der betroffene Knoten mit removeEndpoint aus dem Ring entfernt.
Anschließend wird das Token an den nächsten verbleibenden Knoten im Ring weitergegeben. Dadurch bleibt der Ring funktionsfähig, solange noch mindestens ein erreichbarer Knoten vorhanden ist. 
Zusätzlich wurde eine Funktion zum Entfernen von Knoten ergänzt, damit ausgefallene Teilnehmer dynamisch aus der Ringstruktur gelöscht werden können.
