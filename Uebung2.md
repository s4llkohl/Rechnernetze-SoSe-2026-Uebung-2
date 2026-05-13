ÜBUNGSBLATT 2
Aufgabe 1

Display-Filter: data-text-lines matches "[a-z0-9]{8}"
-> ra6rgv7h

Display-Filter: icmp
-> nd7hark3

final solution code is Q29uZ3JhdHNfVGhpc19Jc190aGVfQm9udXMh

Aufgabe 2

(a)
Traceroute baut sich den gesamten Pfad zum Ziel also Hop für Hop auf. Jeder Hop ist ein Router, der das Paket weiterleitet.

(b)
Beim Traceroute zur Australian National University sieht man zuerst die Router des eigenen Netzwerks und des deutschen Telekom‑Backbones. Ab einem bestimmten Punkt antworten internationale Router jedoch nicht mehr auf ICMP‑Time‑Exceeded‑Pakete, sodass Traceroute nur Sternchen (* * *) zeigt. Der tatsächliche Pfad führt weiter über weltweite Backbone‑Provider bis nach Australien, wird aber aufgrund von ICMP‑Filtern nicht vollständig sichtbar.

(c)
Frankfurt -> Paris -> Marseille -> Lyon -> Paris -> Odeon ->  Renater -> danach nur noch * * *
Nach Geo IP Tool befindet sich die IP-Adresse beim Eiffelturm und gehört zu der Organisation Renater, ein Forschungsnetzwerk

Aufgabe 3

Der TCP‑Scan (nmap -sS) ist deutlich schneller als der UDP‑Scan (nmap -sU), weil TCP bei jedem Port sofort eine eindeutige Antwort liefert. Nmap kann dadurch ohne Verzögerung entscheiden, wie der Port reagiert. UDP dagegen ist verbindungslos und antwortet oft überhaupt nicht. Nmap muss deshalb lange Timeouts abwarten und mehrere Wiederholungen senden, bevor es entscheiden kann, ob ein Port offen, geschlossen oder gefiltert ist. Diese fehlenden Rückmeldungen machen UDP‑Scans grundsätzlich viel langsamer.
Beschleunigen lässt sich ein Scan durch höhere Timing‑Profile wie -T4 oder -T5, weniger Wiederholungen (--max-retries), kürzere Timeouts oder die Einschränkung auf bestimmte Ports (-p).

Aufgabe 4

Neben dem primären Vorwärtsring wird ein sekundärer Rückwärtsring eingeführt. Jeder Knoten kennt sowohl seinen Nachfolger (im Vorwärtsring) als auch seinen Vorgänger (im Rückwärtsring). Fällt ein Knoten aus, übernimmt der Rückwärtsring die Verbindung: Der Vorgänger des ausgefallenen Knotens verbindet sich direkt mit dessen Nachfolger und überbrückt so den Ausfall. Der Ring wird durch einen logischen Rückwärtsring abgesichert. Jeder Knoten hält eine gespiegelte Kopie der aktuellen Ringreihenfolge. Fällt ein Knoten aus, wird mithilfe des Rückwärtsrings der nächste erreichbare Vorgänger ermittelt und der Token direkt an diesen weitergeleitet. Dadurch überbrückt der Ring den Ausfall selbstständig, ohne dass eine zentrale Instanz nötig ist.

