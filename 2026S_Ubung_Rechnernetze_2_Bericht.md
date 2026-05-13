# Rechnernetze 2026-SS - Übungsblatt 02
`#19D9DA - s4joregn`

## Aufgabe 1 - Schnitzeljagd Wireshark


|No.| Result  |  Filter |   
|---|---|---| 
| 251  | https://www.lanwan.ninja/dzh20szq/  | Edit>Find>String>"password"  |   |   |
| 215  | https://www.lanwan.ninja/ra6rgv7h/  | http  |   |   |
| 343  | https://www.lanwan.ninja/nd7hark3/  | icmp  |   |   |
| Solution | Q29uZ3JhdHNfVGhpc19Jc190aGVfQm9udXMh||


## Aufgabe 2 - Traceroute
### a)
`Tracerout` ermöglicht den Pfad zu einer bestimmten IP-Adresse/Domain herauszufinden. Die Ausgabe von tracert gibt Zeilenweise  Informationen über den jeweiligen Netzwerkknoten und die Anfragedauer an. In jeder Zeile findet man die ID des jeweiligen Hops, sowie der Hostname mit IP-Adresse. Ebenso werden für jeden Hop drei Round-trip time (RTT) angegeben. Dies ist die Zeit von Anfrage zu Antwort (einmal im Kreis).

### b)
```
tracert www.anu.edu.au
```

```
Routenverfolgung zu terra-web.anu.edu.au [130.56.67.33]
über maximal 30 Hops:

  1     3 ms    29 ms     7 ms  vbackup.uni-trier.de [136.199.255.1]
  2    11 ms     3 ms     3 ms  opnsense1-transfer.uni-trier.de [136.199.1.99]
  3     6 ms     5 ms     5 ms  g-uni-tr-1.rlp-net.net [217.198.241.193]
  4    10 ms    32 ms     9 ms  g-uni-ko-1.rlp-net.net [217.198.247.149]
  5    19 ms     8 ms    38 ms  g-hbf-ko-1.rlp-net.net [217.198.246.105]
  6    10 ms    35 ms     9 ms  g-hbf-mz-2.rlp-net.net [217.198.240.21]
  7     *       88 ms     *     g-interxion-2.rlp-net.net [217.198.240.13]
  8    23 ms    38 ms    14 ms  g-interxion-4.rlp-net.net [217.198.246.250]
  9     9 ms     9 ms     8 ms  decix-ae3.mpr1.fra4.de.zip.zayo.com [80.81.194.26]
 10   151 ms   189 ms   508 ms  ae12.cs1.fra6.de.eth.zayo.com [64.125.26.172]
 11   144 ms   256 ms   202 ms  ae1.cr1.ams17.nl.zip.zayo.com [64.125.24.14]
 12   211 ms   201 ms   203 ms  ae0.cr1.ams10.nl.zip.zayo.com [64.125.23.184]
 13     *      212 ms     *     ae10.cr1.man7.uk.eth.zayo.com [64.125.29.184]
 14   255 ms   202 ms   199 ms  ae8.cr2.ewr14.us.zip.zayo.com [64.125.31.110]
 15   271 ms     *        *     ae7.cr1.ord8.us.zip.zayo.com [64.125.29.24]
 16   346 ms   203 ms     *     ae3.cr2.ord9.us.zip.zayo.com [64.125.30.77]
 17   257 ms   201 ms   301 ms  ae12.cr1.sea1.us.zip.zayo.com [64.125.19.4]
 18     *        *        *     Zeitüberschreitung der Anforderung.
 19   271 ms   199 ms   205 ms  ae27.mpr1.sea1.us.zip.zayo.com [64.125.29.1]
 20   281 ms   196 ms   195 ms  64.125.193.130.i223.above.net [64.125.193.130]
 21   376 ms   303 ms   302 ms  et-10-0-5.170.pe1.brwy.nsw.aarnet.net.au [113.197.15.62]
 22   394 ms   302 ms   301 ms  et-0-3-0.pe1.actn.act.aarnet.net.au [113.197.15.11]
 23   269 ms   391 ms   300 ms  138.44.172.29
 24   275 ms   501 ms   267 ms  vlan-2100-palo.anu.edu.au [150.203.201.33]
 25   277 ms   406 ms   301 ms  alumni.anu.edu.au [130.56.67.33]

Ablaufverfolgung beendet.
```

### c)
```
Hostname: proxy-web.u-paris.fr
IP Address: 195.220.128.193
Country:  France
Country Code: FR
Region: Ile-de-France
City: Paris
Postal Code: 75006
Latitude: 48.859078
Longitude: 2.293486
```




## Aufgabe 3 - Nmap

```
nmap -h
```
#### UDP Scan
```
nmap -sU scanme.nmap.org
```
Die UDP-Scans benötigen sehr viel Zeit - aber das Programm läuft. Wireshark mit Display Filter 
`ip.src == 45.33.32.156 and udp`
zeigt den Fortschritt an.

```
Nmap scan report for scanme.nmap.org (45.33.32.156)
Host is up (0.23s latency).
Not shown: 988 closed udp ports (port-unreach)
PORT     STATE         SERVICE
67/udp   open|filtered dhcps
68/udp   open|filtered dhcpc
123/udp  open          ntp
135/udp  open|filtered msrpc
137/udp  open|filtered netbios-ns
138/udp  open|filtered netbios-dgm
139/udp  open|filtered netbios-ssn
161/udp  open|filtered snmp
162/udp  open|filtered snmptrap
445/udp  open|filtered microsoft-ds
1900/udp open|filtered upnp
5353/udp open|filtered zeroconf

Nmap done: 1 IP address (1 host up) scanned in 1106.55 seconds
```
#### TCP Scan
```
nmap -sS scanme.nmap.org
```
```
Nmap scan report for scanme.nmap.org (45.33.32.156)
Host is up (0.28s latency).
Not shown: 995 closed tcp ports (reset)
PORT      STATE    SERVICE
22/tcp    open     ssh
80/tcp    filtered http
646/tcp   filtered ldp
9929/tcp  open     nping-echo
31337/tcp open     Elite

Nmap done: 1 IP address (1 host up) scanned in 10.49 seconds
```


### Geschwindigkeitsunterschiede

#### TCP-Scan (`nmap -sS`)
TCP-Scans sind schneller als UDP-Scans, da TCP-Scans direkt eine SYN-ACK-Antwort des Ziels erhalten, ohne eine vollständige Verbindung herzustellen.

#### UDP-Scan (`nmap -sU`)
Im Gegensatz dazu warten UDP-Scans auf eine Antwort vom Ziel, was mehr Zeit in Anspruch nimmt, da UDP-Anfragen nicht so schnell bestätigt werden können wie TCP-Anfragen.

### Möglichkeiten, den Scan schneller durchzuführen
- Mehrere Scans gleichzeitg ausführen.
- nur bestimmte Ports abfragen 
- Nmap-Optionen  `-T4` (Tempo erhöhen) oder `-F` (kurzer Scan)




## Aufgabe 4 - Programmieren
Überlegen Sie sich, was man am Token Ring verändern oder hinzufügen müsste, damit dieser auch beim Ausfall
eines Knotens im Ring noch funktioniert, und wie Sie das im Code umsetzen würden. Erläutern Sie Ihre Ideen in der
Textdatei.

### Ideen zur Umsetzung
1.  Jeder Knoten sendet einen Heartbeat zum Folgeknoten. Kommt von einem Knoten kein Heartbeat in einer bestimmten Zeit an, so wird davon ausgegangen, dass dieser nicht mehr erreichbar ist. Dieser Knoten wird dann aus der Liste der Endpoints entfernt. 
2. Unterschiedliche Pfade
4. Berechnen der zu erwarteten Dauer für einen Ring-Durchlauf, nach überschrittener Zeit neustarten.
5. FDDI
