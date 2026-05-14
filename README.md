# TokenRingUDP

## Description

This Java project demonstrates the basic usage of the UDP protocol by establishing a
token ring network between multiple nodes. The ring is formed dynamically with a first
node serving as the ring leader. The leader is responsible for sending the first token
the moment a second node connects. Any subsequent node can join the ring by connecting
to the leader or any other node that is already part of the ring. A token is forwarded by a ring node
1 second after receiving it. Every time a node receives a token, it prints a message
to the console including the sequence number, the current size of the ring, and the IP
addresses and port number of all nodes in the ring.

## Usage

The implementation has been compiled against Java 21, although no special features are
used that would prevent it from running on older versions. The `Jackson` package is used
for JSON serialization and deserialization. To ease the process of running the program,
a *fatjar* has been created that includes all dependencies. The jar file `TokenRingUDP.jar`
has been added to the root of the repository.

---

# Übung 2 – Majd Abass

## Aufgabe 1: Wireshark-Schnitzeljagd

Ich habe die Datei `wshunt1_1.pcapng` in Wireshark geöffnet und mit verschiedenen Display-Filtern nach Zeichenfolgen gesucht, die aus genau acht Kleinbuchstaben und/oder Zahlen bestehen.

### Erste gültige Zeichenfolge

Zuerst habe ich HTTP-Pakete betrachtet.

Verwendeter Display-Filter:

```wireshark
http
```

Dabei wurde die Zeichenfolge gefunden:

```text
dzh20szq
```

Diese Zeichenfolge wurde auf der Webseite getestet:

```text
https://www.lanwan.ninja/dzh20szq
```

Die Webseite war gültig und zeigte den nächsten Hinweis:

```text
One HTTP error is different.
```

### Zweite gültige Zeichenfolge

Aufgrund des Hinweises habe ich nach HTTP-Fehlern gesucht.

Verwendeter Display-Filter:

```wireshark
http.response.code >= 400
```

Dabei fiel ein HTTP-Fehler besonders auf:

```text
HTTP/1.1 401 ra6rgv7h
```

Die gefundene Zeichenfolge war:

```text
ra6rgv7h
```

Zur genaueren Anzeige können auch folgende Filter verwendet werden:

```wireshark
frame.number == 493
```

```wireshark
frame contains "ra6rgv7h"
```

Die Zeichenfolge wurde auf der Webseite getestet:

```text
https://www.lanwan.ninja/ra6rgv7h
```

Die Webseite war gültig und führte zum nächsten Hinweis.

### Dritte und finale gültige Zeichenfolge

Danach habe ich ICMP-Pakete untersucht.

Verwendeter Display-Filter:

```wireshark
icmp
```

Dadurch wurden zwei ICMP-Pakete angezeigt. In Paket 342 befindet sich im Datenbereich die Zeichenfolge:

```text
nd7hark3
```

Zur genaueren Anzeige können auch folgende Filter verwendet werden:

```wireshark
frame.number == 342
```

```wireshark
icmp && frame contains "nd7hark3"
```

Die Zeichenfolge wurde auf der Webseite getestet:

```text
https://www.lanwan.ninja/nd7hark3
```

Die Webseite war gültig und bestätigte, dass die dritte und finale Herausforderung gelöst wurde.

### Zusammenfassung der gefundenen gültigen Zeichenfolgen

```text
dzh20szq
ra6rgv7h
nd7hark3
```

### Zusammenfassung der verwendeten Display-Filter

```wireshark
http
http.response.code >= 400
frame.number == 493
frame contains "ra6rgv7h"
icmp
frame.number == 342
icmp && frame contains "nd7hark3"
```
The ring leader can be started by just running `java -jar TokenRingUDP.jar`. The
leader then prints its IP address and port number to the console. Any subsequent
node can join by running `java -jar TokenRingUDP.jar <ip> <port>` for any `<ip>`
and `<port`> of a node already part of the ring.


## Aufgabe 2: Traceroute

### a) Erklärung der Funktionsweise von Traceroute

Traceroute zeigt die Zwischenstationen zwischen dem eigenen Rechner und einem Zielrechner an. Dafür werden Pakete mit unterschiedlichen TTL-Werten verschickt.

TTL bedeutet `Time To Live`. Der TTL-Wert gibt an, wie viele Router bzw. Hops ein Paket maximal passieren darf. Jeder Router auf dem Weg verringert den TTL-Wert um 1. Wenn der TTL-Wert bei einem Router 0 erreicht, wird das Paket verworfen. Der Router schickt dann normalerweise eine ICMP-Antwort zurück.

Traceroute nutzt dieses Verhalten aus:

- Zuerst wird ein Paket mit TTL = 1 gesendet. Dieses Paket erreicht nur den ersten Router.
- Danach wird ein Paket mit TTL = 2 gesendet. Dieses Paket erreicht den zweiten Router.
- Danach wird TTL = 3, TTL = 4 usw. verwendet.

So kann Traceroute Schritt für Schritt herausfinden, welche Router auf dem Weg zum Ziel liegen.

In der Ausgabe steht jede Zeile für einen Hop. Die drei Zeitwerte pro Zeile sind drei Messungen der Antwortzeit zu diesem Hop. Wenn ein Stern `*` angezeigt wird, kam für diese Messung keine Antwort zurück. Das kann durch Paketverlust, Firewalls oder Router-Konfigurationen passieren.

Als Beispiel habe ich `tracert 8.8.8.8` ausgeführt:

```text
Tracing route to dns.google [8.8.8.8]
over a maximum of 30 hops:

  1     3 ms     3 ms     4 ms  o2.box [192.168.1.1]
  2    15 ms    35 ms    15 ms  lo1.0006.acln.02.fra.de.net.telefonica.de [62.52.193.30]
  3    14 ms    14 ms    14 ms  lag38.0002.cord.02.fra.de.net.telefonica.de [62.53.12.110]
  4    14 ms    15 ms    16 ms  lag2.0001.corp.02.fra.de.net.telefonica.de [62.53.8.189]
  5    15 ms    29 ms    15 ms  72.14.213.80
  6    14 ms    14 ms    14 ms  142.251.65.69
  7   185 ms    17 ms    14 ms  172.253.66.137
  8    14 ms    14 ms    14 ms  dns.google [8.8.8.8]

Trace complete.
```

Man sieht hier, dass der erste Hop mein lokaler Router `o2.box` ist. Danach folgen mehrere Router im Netz von Telefónica in Frankfurt. Anschließend geht der Weg in Richtung Google. Das Ziel `dns.google [8.8.8.8]` wird nach 8 Hops erreicht.

### b) Traceroute zu `www.anu.edu.au`

Befehl:

```powershell
tracert www.anu.edu.au
```

Ausgabe:

```text
Tracing route to terra-web.anu.edu.au [130.56.67.33]
over a maximum of 30 hops:

  1     3 ms     3 ms     3 ms  o2.box [192.168.1.1]
  2    15 ms    15 ms    14 ms  lo1.0006.acln.02.fra.de.net.telefonica.de [62.52.193.30]
  3    14 ms    14 ms    14 ms  lag38.0002.cord.02.fra.de.net.telefonica.de [62.53.12.110]
  4    16 ms    14 ms    14 ms  lag2.0002.corp.02.fra.de.net.telefonica.de [62.53.9.53]
  5    24 ms    17 ms    14 ms  de-cix-ae10.mcs1.fra6.de.zip.zayo.com [80.81.192.255]
  6   150 ms   151 ms   150 ms  ae11.cr1.fra6.de.zip.zayo.com [64.125.20.124]
  7   150 ms   150 ms   150 ms  ae1.cr1.ams17.nl.zip.zayo.com [64.125.24.14]
  8   150 ms   150 ms   150 ms  ae0.cr1.ams10.nl.zip.zayo.com [64.125.23.184]
  9   150 ms   150 ms     *     ae10.cr1.man7.uk.eth.zayo.com [64.125.29.184]
 10   150 ms     *      150 ms  ae8.cr2.ewr14.us.zip.zayo.com [64.125.31.110]
 11     *        *        *     Request timed out.
 12     *        *        *     Request timed out.
 13     *      149 ms   150 ms  ae12.cr1.sea1.us.zip.zayo.com [64.125.19.4]
 14     *        *        *     Request timed out.
 15   150 ms   149 ms   150 ms  ae27.mpr1.sea1.us.zip.zayo.com [64.125.29.1]
 16   154 ms   150 ms   150 ms  64.125.193.130.i223.above.net [64.125.193.130]
 17   287 ms   287 ms   287 ms  et-10-0-5.170.pe1.brwy.nsw.aarnet.net.au [113.197.15.62]
 18   311 ms   290 ms   290 ms  et-0-3-0.pe1.actn.act.aarnet.net.au [113.197.15.11]
 19   290 ms   290 ms   290 ms  138.44.172.29
 20   291 ms   290 ms   290 ms  vlan-2100-palo.anu.edu.au [150.203.201.33]
 21   291 ms   291 ms   291 ms  policyforum.net [130.56.67.33]

Trace complete.
```

Bei `www.anu.edu.au` wird als Ziel `terra-web.anu.edu.au [130.56.67.33]` angezeigt. Der Weg ist deutlich länger als bei `8.8.8.8` und erreicht das Ziel erst nach 21 Hops.

Am Anfang geht der Weg über meinen lokalen Router und Telefónica in Frankfurt. Danach geht der Verkehr über Zayo, unter anderem über Frankfurt, Amsterdam, Manchester, Newark und Seattle. Ab Hop 17 sieht man `aarnet.net.au`. AARNet ist das australische Forschungs- und Bildungsnetz. Danach geht der Weg weiter in das Netz der Australian National University.

Auffällig ist, dass ab Hop 6 die Zeiten auf ungefähr 150 ms steigen. Später, ab dem australischen Teil der Route, steigen sie auf ungefähr 287 bis 311 ms. Das ist plausibel, weil Australien geographisch sehr weit von Deutschland entfernt ist.

Außerdem gibt es mehrere Stellen mit `*` oder `Request timed out`. Das bedeutet, dass einzelne Router nicht auf die Traceroute-Anfragen geantwortet haben. Trotzdem ist der Weg nicht abgebrochen, weil spätere Hops und schließlich das Ziel erreicht wurden.

### c) Traceroute zu `195.220.128.193`

Befehl:

```powershell
tracert 195.220.128.193
```

Ausgabe:

```text
Tracing route to proxy-web.u-paris.fr [195.220.128.193]
over a maximum of 30 hops:

  1     4 ms     3 ms     3 ms  o2.box [192.168.1.1]
  2    15 ms    15 ms    15 ms  lo1.0006.acln.02.fra.de.net.telefonica.de [62.52.193.30]
  3    15 ms    15 ms    14 ms  lag38.0001.cord.02.fra.de.net.telefonica.de [62.53.12.72]
  4    14 ms    14 ms    14 ms  lag0-0.0001.prrx.09.fra.de.net.telefonica.de [62.53.5.73]
  5    14 ms    14 ms    14 ms  as2603.frankfurt.megaport.com [62.69.146.103]
  6    25 ms    24 ms    24 ms  uk-hex.nordu.net [109.105.102.97]
  7    25 ms    25 ms    25 ms  ndn-gw.mx1.lon.uk.geant.net [109.105.102.98]
  8    31 ms    40 ms    31 ms  renater-ias-geant-gw.gen.ch.geant.net [83.97.89.13]
  9    40 ms    41 ms    41 ms  renater-ias-renater-gw.gen.ch.geant.net [83.97.89.14]
 10    42 ms    42 ms    44 ms  et-3-1-7-ren-nr-paris1-rtr-131.noc.renater.fr [193.51.180.166]
 11    43 ms    43 ms    42 ms  te0-0-0-12-ren-nr-odeon-rtr-091.noc.renater.fr [193.55.204.2]
 12    42 ms    42 ms    42 ms  xe-0-2-0-1-odeon-rtr-111-re0.noc.renater.fr [193.51.186.101]
 13  1063 ms    40 ms    39 ms  195.221.127.6
 14    42 ms    42 ms    42 ms  195.220.129.174
 15     *        *        *     Request timed out.
 16     *        *        *     Request timed out.
 17     *        *        *     Request timed out.
 18     *        *        *     Request timed out.
 19     *        *        *     Request timed out.
 20     *        *        *     Request timed out.
 21     *        *        *     Request timed out.
 22     *        *        *     Request timed out.
 23     *        *        *     Request timed out.
 24     *        *        *     Request timed out.
 25     *        *        *     Request timed out.
 26     *        *        *     Request timed out.
 27     *        *        *     Request timed out.
 28     *        *        *     Request timed out.
 29     *        *        *     Request timed out.
 30     *        *        *     Request timed out.

Trace complete.
```

Als Ziel wird `proxy-web.u-paris.fr [195.220.128.193]` angezeigt. Die Zieladresse gehört also zu `u-paris.fr`, also zur Universität Paris.

Der Weg verläuft zuerst über mein lokales Netzwerk:

```text
o2.box [192.168.1.1]
```

Danach geht der Weg über Telefónica in Frankfurt:

```text
fra.de.net.telefonica.de
```

Anschließend geht der Weg über Megaport in Frankfurt:

```text
as2603.frankfurt.megaport.com
```

Danach führt die Route über NORDUnet und GÉANT. GÉANT ist ein europäisches Forschungs- und Bildungsnetz. Danach sieht man RENATER:

```text
renater-ias-geant-gw.gen.ch.geant.net
renater-ias-renater-gw.gen.ch.geant.net
```

RENATER ist das französische Forschungs- und Bildungsnetz. Ab Hop 10 sieht man Router mit `paris1` und `odeon` im Namen:

```text
et-3-1-7-ren-nr-paris1-rtr-131.noc.renater.fr
te0-0-0-12-ren-nr-odeon-rtr-091.noc.renater.fr
xe-0-2-0-1-odeon-rtr-111-re0.noc.renater.fr
```

Das deutet darauf hin, dass der Weg nach Paris führt.

Geographische Einordnung:

- Start: eigener Rechner / lokaler Router
- Deutschland: Telefónica-Netz in Frankfurt
- Deutschland: Megaport Frankfurt
- Europa: NORDUnet / GÉANT
- Frankreich: RENATER
- Frankreich / Paris: RENATER-Router mit `paris1` und `odeon`
- Zielorganisation: Universität Paris / `u-paris.fr`

Ab Hop 15 erscheinen nur noch `Request timed out`. Trotzdem wurde vorher schon klar, dass die Route bis in das Netz von RENATER in Paris führt. Das Zielsystem oder spätere Router antworten wahrscheinlich nicht auf Traceroute-Anfragen. Das bedeutet nicht automatisch, dass der Server nicht erreichbar ist, sondern nur, dass keine Traceroute-Antwort zurückkommt.



## Aufgabe 3: Nmap

Für diese Aufgabe habe ich Nmap installiert und die Seite `scanme.nmap.org` mit TCP- und UDP-Scans untersucht.

Zuerst habe ich überprüft, ob Nmap korrekt installiert ist:

```powershell
nmap --version
```

Ausgabe:

```text
Nmap version 7.99 ( https://nmap.org )
Platform: i686-pc-windows-windows
Compiled with: nmap-liblua-5.4.8 openssl-3.0.16 nmap-libssh2-1.11.1 nmap-libz-1.3.2 nmap-libpcre2-10.47 Npcap-1.87 nmap-libdnet-1.18.0 ipv6
Compiled without:
Available nsock engines: iocp poll select
```

### TCP SYN Scan

Befehl:

```powershell
nmap -sS scanme.nmap.org
```

Ausgabe:

```text
Starting Nmap 7.99 ( https://nmap.org ) at 2026-05-14 17:23 +0200
Nmap scan report for scanme.nmap.org (45.33.32.156)
Host is up (0.18s latency).
Other addresses for scanme.nmap.org (not scanned): 2600:3c01::f03c:91ff:fe18:bb2f
Not shown: 996 closed tcp ports (reset)
PORT      STATE SERVICE
22/tcp    open  ssh
80/tcp    open  http
9929/tcp  open  nping-echo
31337/tcp open  Elite

Nmap done: 1 IP address (1 host up) scanned in 4.46 seconds
```

Der TCP SYN Scan war sehr schnell. Er dauerte nur `4.46 Sekunden`.

Gefundene offene TCP-Ports:

```text
22/tcp    open  ssh
80/tcp    open  http
9929/tcp  open  nping-echo
31337/tcp open  Elite
```

### UDP Scan

Befehl:

```powershell
nmap -sU scanme.nmap.org
```

Ausgabe:

```text
Starting Nmap 7.99 ( https://nmap.org ) at 2026-05-14 17:23 +0200
Nmap scan report for scanme.nmap.org (45.33.32.156)
Host is up (0.18s latency).
Other addresses for scanme.nmap.org (not scanned): 2600:3c01::f03c:91ff:fe18:bb2f
Not shown: 998 closed udp ports (port-unreach)
PORT    STATE         SERVICE
68/udp  open|filtered dhcpc
123/udp open          ntp

Nmap done: 1 IP address (1 host up) scanned in 1011.43 seconds
```

Der UDP Scan war deutlich langsamer. Er dauerte `1011.43 Sekunden`, also ungefähr `16 Minuten und 51 Sekunden`.

Gefundene UDP-Ports:

```text
68/udp  open|filtered dhcpc
123/udp open          ntp
```

### Warum ist der TCP Scan wesentlich schneller?

Der TCP SYN Scan ist schneller, weil TCP ein verbindungsorientiertes Protokoll ist. Bei einem SYN Scan sendet Nmap ein TCP-SYN-Paket an einen Port.

Wenn der Port offen ist, antwortet der Zielrechner normalerweise mit `SYN/ACK`.

Wenn der Port geschlossen ist, antwortet der Zielrechner normalerweise mit `RST`.

Dadurch bekommt Nmap bei TCP meistens schnell eine eindeutige Antwort. In meiner Ausgabe sieht man auch:

```text
Not shown: 996 closed tcp ports (reset)
```

Das bedeutet, dass viele geschlossene TCP-Ports schnell durch TCP-Reset-Antworten erkannt wurden.

### Warum ist der UDP Scan wesentlich langsamer?

UDP ist verbindungslos. Es gibt keinen Verbindungsaufbau wie bei TCP und auch keine SYN/SYN-ACK/RST-Logik.

Wenn Nmap ein UDP-Paket an einen offenen UDP-Port sendet, muss der Dienst nicht unbedingt antworten. Wenn ein UDP-Port geschlossen ist, kann eine ICMP-Fehlermeldung zurückkommen, zum Beispiel `port unreachable`.

Wenn aber keine Antwort kommt, ist das für Nmap nicht eindeutig. Der Port könnte offen sein, gefiltert sein oder das Paket bzw. die Antwort könnte verloren gegangen sein. Deshalb muss Nmap bei UDP länger warten und teilweise erneut versuchen.

Das sieht man auch an diesem Ergebnis:

```text
68/udp open|filtered dhcpc
```

`open|filtered` bedeutet, dass Nmap nicht sicher unterscheiden konnte, ob der Port offen oder gefiltert ist.

Dadurch entstehen beim UDP Scan viele Wartezeiten und Timeouts. Deshalb dauerte der UDP Scan in meinem Test `1011.43 Sekunden`, während der TCP SYN Scan nur `4.46 Sekunden` dauerte.

### Möglichkeiten, den Scan schneller durchzuführen

Man kann den Scan schneller machen, indem man weniger Ports scannt oder aggressivere Timing-Optionen verwendet.

Beispiele:

```powershell
nmap -sU -F scanme.nmap.org
```

Dieser Befehl verwendet den Fast-Scan-Modus und prüft weniger Ports.

```powershell
nmap -sU --top-ports 20 scanme.nmap.org
```

Dieser Befehl prüft nur die 20 häufigsten UDP-Ports.

```powershell
nmap -sU -p 53,67,68,123,161 scanme.nmap.org
```

Dieser Befehl prüft nur ausgewählte UDP-Ports.

```powershell
nmap -sU -T4 scanme.nmap.org
```

Dieser Befehl verwendet ein schnelleres Timing.

```powershell
nmap -sU -n scanme.nmap.org
```

Dieser Befehl deaktiviert DNS-Auflösung und kann dadurch Zeit sparen.

Man muss dabei beachten, dass schnellere Scans unvollständiger oder ungenauer sein können. Wenn man weniger Ports prüft oder kürzere Wartezeiten verwendet, kann man offene Ports übersehen.

## Aufgabe 4: Programmieren

### Idee

Der ursprüngliche Token Ring funktioniert nur, solange alle Knoten im Ring aktiv bleiben. Wenn ein Knoten ausfällt, kann das Token an diesen Knoten gesendet werden und danach verschwindet es. Dann kommt das Token bei den anderen Knoten nicht mehr an und der Ring bleibt stehen.

Damit der Ring auch beim Ausfall eines Knotens weiter funktionieren kann, muss ein Knoten erkennen können, dass das Token nicht mehr zurückkommt. Danach muss der ausgefallene Knoten aus der Ringliste entfernt werden. Anschließend kann das Token an den nächsten noch aktiven Knoten weitergegeben werden.

### Problem bei UDP

Das Programm verwendet UDP. UDP ist verbindungslos. Deshalb bedeutet ein erfolgreicher `send`-Aufruf nicht, dass der Empfänger wirklich aktiv ist oder das Paket erhalten hat.

Deshalb reicht es nicht aus, nur beim Senden auf eine Exception zu warten. Ein UDP-Paket kann ohne Fehler gesendet werden, obwohl der Zielknoten nicht mehr läuft.

### Umsetzung

Für die Absicherung habe ich folgende Idee umgesetzt:

1. Nach dem Senden des Tokens wird gespeichert, welches Token zuletzt gesendet wurde.
2. Außerdem wird gespeichert, an welchen Knoten das Token gesendet wurde.
3. Danach wird ein Timeout auf dem Socket gesetzt.
4. Wenn innerhalb der erwarteten Zeit kein Token zurückkommt, wird angenommen, dass der nächste Knoten ausgefallen ist.
5. Dieser Knoten wird mit `removeEndpoint(...)` aus der Ringliste entfernt.
6. Danach wird das Token an den nächsten verbleibenden Knoten weitergeleitet.

In `Token.java` wurde dafür eine Methode verwendet, die einen Endpoint aus der Queue entfernt:

```java
public boolean removeEndpoint(Endpoint endpoint) {
    return ring.remove(endpoint);
}
```

In `TokenRing.java` wird nach dem Senden des Tokens ein Timeout gesetzt:

```java
socket.setSoTimeout(timeoutFor(rc));
```

Wenn kein Token rechtzeitig zurückkommt, wird eine `SocketTimeoutException` ausgelöst. Dann wird der zuletzt verwendete Empfänger als ausgefallen betrachtet:

```java
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

    // Danach wird der Ring ohne den ausgefallenen Knoten fortgesetzt.
}
```

Wenn nach dem Entfernen noch mehrere Knoten übrig sind, wird das Token an den nächsten Knoten weitergeleitet:

```java
Token.Endpoint next = sendToNext(socket, lastToken);

if (next != null) {
    lastNext = next;
    socket.setSoTimeout(timeoutFor(lastToken));
}
```

### Ergebnis

Durch diese Änderung bleibt der Token Ring nicht dauerhaft stehen, wenn ein Knoten ausfällt. Stattdessen wird nach einem Timeout angenommen, dass der zuletzt angesprochene Knoten nicht mehr erreichbar ist. Dieser Knoten wird aus der Ringliste entfernt und das Token wird an den nächsten Knoten weitergegeben.

Dadurch kann der Ring mit den verbleibenden Knoten weiterlaufen.

### Einschränkungen

Diese Lösung ist eine einfache Absicherung. Sie erkennt Ausfälle über Timeouts. Das bedeutet, dass ein Knoten auch dann fälschlicherweise als ausgefallen erkannt werden könnte, wenn das Netzwerk nur sehr langsam ist oder ein UDP-Paket verloren geht.

Eine noch bessere Lösung wäre ein eigenes ACK- oder Heartbeat-System. Dann würde ein Knoten nach dem Empfang des Tokens eine Bestätigung zurückschicken. Wenn diese Bestätigung ausbleibt, könnte der Sender den Knoten gezielter als ausgefallen betrachten.
