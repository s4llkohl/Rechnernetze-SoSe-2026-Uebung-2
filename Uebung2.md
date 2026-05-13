# Übung 2

s4jazuck

## Aufgabe 1
tcp.payload matches "[a-z0-9]{8}"

http matches "[a-z0-9]{8}"

http.request.method == "GET" && http matches "[a-z0-9]{8}"

http.request.uri matches "[a-z0-9]{8}"

http.request.uri matches "[a-z0-9]{8}" && http.request.method == "GET"

Mit allen Filtern findet man das gesuchte Passwort ra6rgv7h. Aber man findet mit jedem dieser Filter auch andere Pakete.
Filter, die nur dieses Paket findet, habe ich nicht gefunden.
Mit der letzten Filterkombination sucht man am genausten, sodass eigentlich nur noch das Paket mit dem Passwort
infrage kommt. 

## Aufgabe 2
a) Die Ausgabe: 
```bash 
$ tracert 8.8.8.8

Routenverfolgung zu dns.google [8.8.8.8]
über maximal 30 Hops:

  1     5 ms     6 ms     2 ms  vwlanclients4.uni-trier.de [136.199.200.1]
  2     4 ms     2 ms     2 ms  opnsense1-transfer.uni-trier.de [136.199.1.99]
  3     6 ms     3 ms     2 ms  g-uni-tr-1.rlp-net.net [217.198.241.193]
  4     9 ms     8 ms     5 ms  g-uni-ko-1.rlp-net.net [217.198.247.149]
  5     8 ms     6 ms     6 ms  g-hbf-ko-1.rlp-net.net [217.198.246.105]
  6     6 ms     4 ms     4 ms  g-hbf-mz-2.rlp-net.net [217.198.240.21]
  7     6 ms     5 ms     4 ms  g-interxion-2.rlp-net.net [217.198.240.13]
  8     9 ms     7 ms     6 ms  g-interxion-4.rlp-net.net [217.198.246.250]
  9     *        7 ms     *     ipv4.de-cix.fra.de.as15169.google.com [80.81.192.108]
 10     6 ms     7 ms     6 ms  192.178.107.29
 11     7 ms     5 ms     6 ms  172.253.66.139
 12     7 ms     5 ms     6 ms  dns.google [8.8.8.8]

Ablaufverfolgung beendet.
```

Anhand der Ausgabe lässt sich sehen: Traceroute sendet Pakete mit steigender TTL. Dann reduziert jeder Router die TTl
um 1. Wenn TTl gleich null wird, sendet der Router eine Fehlermeldung zurück. 
Jeder Eintrag in der Ausgabe ist ein Router, also ein "Hop" auf dem Weg vom eigenem Rechner zu dem Ziel, in diesem
Fall Google.
Der * markiert Pakete, die keine Antwort bekommen haben

b) Die Ausgaben:  
``` bash
$ tracert www.anu.edu.au

Routenverfolgung zu terra-web.anu.edu.au [130.56.67.33]
über maximal 30 Hops:

  1    13 ms     2 ms     1 ms  vwlanclients4.uni-trier.de [136.199.200.1]
  2     3 ms     2 ms     6 ms  opnsense1-transfer.uni-trier.de [136.199.1.99]
  3     3 ms     2 ms     2 ms  g-uni-tr-1.rlp-net.net [217.198.241.193]
  4     5 ms     4 ms     4 ms  g-uni-ko-1.rlp-net.net [217.198.247.149]
  5     5 ms     4 ms     4 ms  g-hbf-ko-1.rlp-net.net [217.198.246.105]
  6    11 ms     6 ms     5 ms  g-hbf-mz-2.rlp-net.net [217.198.240.21]
  7     8 ms     5 ms     5 ms  g-interxion-2.rlp-net.net [217.198.240.13]
  8     9 ms     7 ms     4 ms  g-interxion-4.rlp-net.net [217.198.246.250]
  9     8 ms     6 ms     6 ms  decix-ae3.mpr1.fra4.de.zip.zayo.com [80.81.194.26]
 10   201 ms   204 ms   141 ms  ae12.cs1.fra6.de.eth.zayo.com [64.125.26.172]
 11   239 ms   206 ms   209 ms  ae1.cr1.ams17.nl.zip.zayo.com [64.125.24.14]
 12   236 ms   205 ms   205 ms  ae0.cr1.ams10.nl.zip.zayo.com [64.125.23.184]
 13   236 ms   206 ms   208 ms  ae10.cr1.man7.uk.eth.zayo.com [64.125.29.184]
 14   208 ms     *      142 ms  ae8.cr2.ewr14.us.zip.zayo.com [64.125.31.110]
 15     *      210 ms     *     ae7.cr1.ord8.us.zip.zayo.com [64.125.29.24]
 16     *        *        *     Zeitüberschreitung der Anforderung.
 17     *      160 ms   206 ms  ae12.cr1.sea1.us.zip.zayo.com [64.125.19.4]
 18     *        *        *     Zeitüberschreitung der Anforderung.
 19   143 ms   223 ms   189 ms  ae27.mpr1.sea1.us.zip.zayo.com [64.125.29.1]
 20   205 ms   207 ms   207 ms  64.125.193.130.i223.above.net [64.125.193.130]
 21   320 ms   311 ms   311 ms  et-10-0-5.170.pe1.brwy.nsw.aarnet.net.au [113.197.15.62]
 22   334 ms   312 ms   310 ms  et-0-3-0.pe1.actn.act.aarnet.net.au [113.197.15.11]
 23   334 ms   310 ms     *     138.44.172.29
 24   333 ms   310 ms   311 ms  vlan-2100-palo.anu.edu.au [150.203.201.33]
 25   329 ms   311 ms   310 ms  alumni.anu.edu.au [130.56.67.33]

Ablaufverfolgung beendet. 
```
Wenn man Traceroute zur Australian National University ausführt, ist die Route internationaler. Also es läuft über
internationale Netzwerke. Die Anzahl der Hops ist damit deutlich höher als bei lokalen Zielen, und die Latenzzeiten
steigen ebenfalls an, da die Daten über größere Entfernungen übertragen werden müssen.

c)
```bash
Routenverfolgung zu proxy-web.u-paris.fr [195.220.128.193]
über maximal 30 Hops:

  1     1 ms     1 ms     1 ms  vwlanclients4.uni-trier.de [136.199.200.1]
  2     2 ms     1 ms     1 ms  opnsense1-transfer.uni-trier.de [136.199.1.99]
  3     3 ms     2 ms     3 ms  g-uni-tr-1.rlp-net.net [217.198.241.193]
  4     5 ms     5 ms     3 ms  g-uni-ko-1.rlp-net.net [217.198.247.149]
  5     8 ms     5 ms     6 ms  g-hbf-ko-1.rlp-net.net [217.198.246.105]
  6     8 ms     4 ms     6 ms  g-hbf-mz-2.rlp-net.net [217.198.240.21]
  7    10 ms     7 ms     7 ms  g-interxion-2.rlp-net.net [217.198.240.13]
  8     6 ms     4 ms     4 ms  g-interxion-4.rlp-net.net [217.198.246.250]
  9     9 ms    10 ms     6 ms  et-0-1-12-561.core1.fra1.ix.f.man-da.net [82.195.78.5]
 10    10 ms     7 ms     6 ms  et-0-1-0-0.core1.fr5.eqx.f.man-da.net [82.195.80.4]
 11     6 ms     5 ms     5 ms  et-0-0-0-0.peer1.fr5.eqx.f.man-da.net [82.195.67.104]
 12     9 ms     7 ms     8 ms  as2603.frankfurt.megaport.com [62.69.146.103]
 13    19 ms    18 ms    16 ms  uk-hex.nordu.net [109.105.102.97]
 14    19 ms    19 ms    16 ms  ndn-gw.mx1.lon.uk.geant.net [109.105.102.98]
 15    25 ms    23 ms    23 ms  renater-ias-geant-gw.gen.ch.geant.net [83.97.89.13]
 16    37 ms    30 ms    27 ms  renater-ias-renater-gw.gen.ch.geant.net [83.97.89.14]
 17    31 ms    27 ms    30 ms  et-3-1-7-ren-nr-paris1-rtr-131.noc.renater.fr [193.51.180.166]
 18    30 ms    27 ms    27 ms  te0-0-0-11-ren-nr-odeon-rtr-091.noc.renater.fr [193.51.180.20]
 19    29 ms    26 ms    26 ms  xe-0-3-0-odeon-rtr-111.noc.renater.fr [193.51.180.156]
 20    30 ms    36 ms    27 ms  195.221.127.6
 21    36 ms    29 ms    29 ms  195.220.129.174
 22     *        *        *     Zeitüberschreitung der Anforderung.
 23     *        *        *     Zeitüberschreitung der Anforderung.
 24     *        *        *     Zeitüberschreitung der Anforderung.
 25     *        *        *     Zeitüberschreitung der Anforderung.
 26     *        *        *     Zeitüberschreitung der Anforderung.
 27     *        *        *     Zeitüberschreitung der Anforderung.
 28     *        *        *     Zeitüberschreitung der Anforderung.
 29     *        *        *     Zeitüberschreitung der Anforderung.
 30     *        *        *     Zeitüberschreitung der Anforderung.

Ablaufverfolgung beendet.
```
Die IP-Adresse gehört zu der Universität Paris. 
Der Weg geht von Trier -> Koblenz -> Mainz -> Frankfurt -> London -> Schweiz -> Paris

## Aufgabe 3
Scan der TCP-Ports: 
```bash
$ nmap -sS scanme.nmap.org
Starting Nmap 7.99 ( https://nmap.org ) at 2026-05-07 15:35 +0200
Nmap scan report for scanme.nmap.org (45.33.32.156)
Host is up (0.15s latency).
Other addresses for scanme.nmap.org (not scanned): 2600:3c01::f03c:91ff:fe18:bb2f
Not shown: 995 closed tcp ports (reset)
PORT      STATE    SERVICE
22/tcp    open     ssh
80/tcp    open     http
646/tcp   filtered ldp
9929/tcp  open     nping-echo
31337/tcp open     Elite

Nmap done: 1 IP address (1 host up) scanned in 5.64 seconds
```

Scan der UDP-Ports: 
```bash

```

Der Scan der TCP-Ports ist deutlich schneller als der UDP-Scan, da TCP eine Rückmeldung für jeden gescannten Port 
liefert. 
UDP ist verbindungslos, wodurch viele Ports nicht antworten. Das führt dazu, dass Nmap auf Timeouts wartet oder 
Fehlermeldungen erhält. Dies führt zu erheblich längeren Wartezeiten.

## Aufgabe 4

Falls ein Knoten ausfällt, könnte es passieren, dass weiterhin versucht wird ihn zu erreichen und der Ring "stoppt" an
diesem Knoten. 
Da UDP verwendet wird, könnte man ein ACK einbauen, damit Konten A weiß, dass Konten B das Paket empfangen hat oder
nicht. Falls kein ACK zurückkommt, kann der Knoten entfernt werden und an den nächsten Knoten gesendet werden. Somit
endet der Ring nicht, nur weil ein Knoten ausfällt.

Dafür habe ich eine Methode sendAck() hinzugefügt, die das ACK an den ursprünglichen Sender zurücksendet. Für diese
Methode werden allerdings IP-Adresse und Port des Senders benötigt, welche man aus dem Paket herauslesen kann. 
Außerdem muss eine entsprechende Methode eingefügt werden, damit der Sender auf das ACK wartet.
Zusätzlich kann man ein Timeout für den Socket setzen, damit ein receive() nicht lange blockieren kann. 
Sollte kein ACK ankommen, wird der entsprechende Knoten entfernt. 



