# Konzept v2 – Koordinations-Website zur Bibelverteilung

*Stand: 02.07.2026 · ersetzt das Konzept v1 („Freiwilligen-App") · Arbeitsdokument für Projektleitung und Entwicklung*

---

## 0. Was sich gegenüber v1 grundlegend ändert

Das Konzept v1 war als offline-fähige Mobile-App für den Feldeinsatz gedacht und hat damit das schwierigste Problem (Offline-Synchronisation) an den Anfang gestellt, obwohl es erst im Feldeinsatz gebraucht wird. Version 2 dreht die Reihenfolge um und richtet sich nach der tatsächlichen Nutzung:

| | v1 | v2 |
|---|---|---|
| Erste Ausbaustufe | Mobile-App, offline-first | **Website (Demonstrator), rein im Browser** |
| Anmeldung | Rollen-/Rechtekonzept ab Start | **V1 ohne Login; Verteiler als Sitzungs-Namen; später Bibel TV Login** |
| Gebietszuteilung | Polygon „zeichnen" (unspezifiziert) | **Lasso: Bereiche mit der Maus umfahren, Gebäude + Straßen werden automatisch abgeleitet** |
| Statusmodell | 5 Logistik-Status | **2 Dimensionen (Zuteilung × Ergebnis) → 4 Anzeige-Kategorien** |
| Fortschritt | Dashboard-Funktion | **KPI-Leiste, immer sichtbar am oberen Rand** |
| Offline-Sync | Kern der Architektur | **verschoben auf Stufe 3 (Feld-App); Ereignisprotokoll wird aber ab Tag 1 vorbereitet** |

**Unverändert übernommen aus v1** (das waren die richtigen Entscheidungen):

- Datensparsamkeit als Architektur: keine Bewohner, keine Namen an Adressen, kein „abgelehnt"-Status (Art.-9-Risiko), keine Standortverfolgung der Freiwilligen.
- Gebäudedaten aus OpenStreetMap (Geofabrik), später ergänzt um amtliche Länder-Gebäudereferenzen.
- Append-only-Ereignisprotokoll statt überschreibbarem Status (trägt Undo in V1 und Offline-Sync in Stufe 3).
- Selbst gehostete Kartenkacheln (keine Drittabflüsse).

---

## 1. Zwei Nutzungssituationen — der Schlüssel zum Stufenplan

Das v1-Konzept hat zwei sehr verschiedene Situationen vermischt. Sie sauber zu trennen macht das Projekt planbar:

| Situation | Wer, wo | Netz | Gerät | Kernfunktionen |
|---|---|---|---|---|
| **Planung & Koordination** | Organisatoren am Schreibtisch | vorhanden | Desktop/Laptop, Maus | Gebiete umfahren und zuteilen, Fortschritt sehen, KPI-Überblick |
| **Feldeinsatz** | Freiwillige an der Haustür | oft nicht vorhanden | Smartphone | Haus antippen, Status setzen, offline arbeiten |

Alle neuen Anforderungen (Lasso-Auswahl, Sitzungs-Namen, KPI-Leiste) gehören zur **Planungssituation** — und die braucht kein Offline und keine Synchronisationslogik. Deshalb ist „Website zuerst" nicht nur eine pragmatische Reihenfolge, sondern die architektonisch richtige: Die erste Ausbaustufe kommt vollständig ohne die schwierigen Teile aus.

### Ausbaustufen

1. **Stufe 1 — Demonstrator-Website (jetzt).** Läuft komplett im Browser, ohne Backend, ohne Login. Ein Pilotgebiet (entschieden: **Bonn-Bad Godesberg**, siehe Abschnitt 12) mit echten OSM-Gebäuden. Alle fünf neuen Anforderungen sind hier voll enthalten. Zweck: das Konzept intern vorführen, Bedienung erproben, Entscheidungen absichern.
2. **Stufe 2 — Geteilter Stand (Backend).** Kleines Backend mit PostGIS; mehrere Browser sehen denselben Stand; Zugang zunächst über einen Aktions-Link/Code statt Login. Bundesweite Gebäudebasis.
3. **Stufe 3 — Login + Feldeinsatz.** Anmeldung über den Bibel TV Login (siehe 9.1), die Website wird als PWA offline-fähig fürs Smartphone; erst wenn die PWA im Feld nicht ausreicht, folgt eine native App. Hier greift die Offline-Sync-Architektur aus Konzept v1, Abschnitt 6 — sie bleibt als Referenz gültig.

Der Übergang zwischen den Stufen ist ein **Ausbau, kein Neubau**: Das Ereignisprotokoll (Abschnitt 7) ist von Anfang an das Datenformat; in Stufe 1 lebt es im Browser, in Stufe 2 wandert es auf den Server, in Stufe 3 wird es synchronisiert.

---

## 2. Statusmodell — zwei Dimensionen statt einer Liste

Die Anforderung nennt vier Zustände pro Haus: *nicht markiert / zur persönlichen Verteilung markiert / schon verteilt / persönlich gesprochen*. Dahinter stecken bei genauem Hinsehen **zwei unabhängige Fragen**:

1. **Zuteilung** (Planung): Ist das Haus jemandem zur persönlichen Verteilung zugeteilt — und wem?
2. **Ergebnis** (Durchführung): Was ist an diesem Haus passiert — nichts, Bibel verteilt (übergeben/eingeworfen), oder verteilt **mit** persönlichem Gespräch?

Diese Trennung ist wichtig, weil sonst Information verloren geht: Würde „verteilt" die Zuteilung überschreiben, wüsste man hinterher nicht mehr, wer das Gebiet hatte. Und ein Haus kann verteilt werden, ohne je zugeteilt gewesen zu sein (spontane Aktion) — auch das muss das Modell abbilden.

### 2.1 Die zwei Felder

| Feld | Werte | Bedeutung |
|---|---|---|
| `zuteilung` | *leer* oder `verteiler_id` | wem das Haus zur persönlichen Verteilung zugeordnet ist |
| `ergebnis` | `offen` · `verteilt` · `gesprochen` | was am Haus passiert ist; `gesprochen` schließt die Übergabe ein |

Ergänzend ein Ausnahme-Kennzeichen `nicht_zustellbar` (Abriss, Baustelle, kein Briefkasten): Solche Gebäude fallen aus der Zählbasis heraus, statt eine der vier Kategorien zu verfälschen.

### 2.2 Die vier Anzeige-Kategorien

Aus den zwei Feldern ergeben sich genau die vier gewünschten Kategorien — **eindeutig und überschneidungsfrei**, d. h. jedes Haus ist zu jedem Zeitpunkt in genau einer Kategorie und die vier Zahlen summieren sich immer zur Gesamtzahl:

| Kategorie | Bedingung | Kartenfarbe |
|---|---|---|
| **Unerreicht** | `ergebnis = offen` und keine Zuteilung | Grau |
| **Zugeteilt** | `ergebnis = offen` und Zuteilung vorhanden | Blau |
| **Verteilt** | `ergebnis = verteilt` | Grün |
| **Persönlich gesprochen** | `ergebnis = gesprochen` | Gold |

Rangfolge fürs spätere Zusammenführen (Stufe 3): `gesprochen` > `verteilt` > `offen`. Ein höherer Zustand wird durch einen niedrigeren nie stillschweigend überschrieben; Korrekturen laufen ausschließlich über explizites Zurücknehmen (Undo im Ereignisprotokoll).

### 2.3 Datenschutz-Anmerkung zu „persönlich gesprochen"

Der Status hält fest, dass an einem **Ort** ein Gespräch stattgefunden hat — nicht mit wem, nicht worüber, nicht mit welchem Ausgang. Das bleibt eine logistische Aussage, solange zwei Regeln gelten, die im Datenmodell fest verankert werden:

1. **Kein Freitextfeld** am Haus (keine Gesprächsnotizen, keine Eindrücke). Was nicht existiert, kann nicht befüllt werden.
2. **Kein „abgelehnt"** — unverändert aus v1: Die Ablehnung einer Bibel adressscharf zu speichern, kann eine Aussage über die weltanschauliche Haltung der Bewohner sein (Art. 9 DSGVO). Wie mit Ablehnungen im Feldeinsatz praktisch umgegangen wird (Haus abschließen ohne Übergabe), ist eine Stufe-3-Entscheidung (Abschnitt 12).

---

## 3. KPI-Leiste — der ständige Überblick

Am oberen Rand steht immer, über der Karte fixiert, der Stand der geladenen Aktion:

```
UNERREICHT 1.240   |   ZUGETEILT 380   |   VERTEILT 512   |   GESPROCHEN 87        Gesamt 2.219 · 27 % erreicht
```

- Die vier Zahlen entsprechen 1:1 den vier Kategorien und Kartenfarben (Legende = KPI-Leiste).
- Sie aktualisieren sich sofort bei jeder Aktion — ein Haus wird zugeteilt, die Zahlen springen um.
- „Erreicht" = Verteilt + Gesprochen. Optional als Fortschrittsbalken.
- Bezugsraum ist das geladene Aktionsgebiet (Stufe 1: das Pilotgebiet). Später filterbar (nur mein Gebiet / nur Team X), die Gesamtleiste bleibt aber immer sichtbar.

**Wichtig für die Kommunikation:** Gezählt werden **Häuser** (adressierte Gebäude), nicht Haushalte. Ein Mehrfamilienhaus mit zwölf Wohnungen ist in der Zählung *ein* Haus. Für die Aussage „X Haushalte erreicht" braucht es später eine Schätzung der Wohneinheiten pro Gebäude (`unit_count_hint` aus v1) — als bewusste Erweiterung, nicht als stille Vermischung.

---

## 4. Gebiete mit der Maus umfahren (Lasso-Zuteilung)

Der zentrale neue Arbeitsschritt der Koordination:

1. **Umfahren:** Die Nutzerin malt mit gedrückter Maustaste einfach eine Linie um den Bereich — **sobald die Linie sich selbst kreuzt, ist das umschlossene Gebiet ausgewählt**; Loslassen ohne Kreuzung schließt die Linie automatisch (nachsichtiger Rückfall). Kreuzungen, deren Schleife kein Haus enthält, werden ignoriert (kein Auslösen durch versehentliche Mini-Kringel). Alternativ: Klick-Polygon Punkt für Punkt, praktischer bei großen Gebieten. Auf Touch-Geräten funktioniert dasselbe mit dem Finger.
2. **Auswahl:** Alle Gebäude, deren **Mittelpunkt** in der Fläche liegt, werden ausgewählt und hervorgehoben. Die Mittelpunkt-Regel verhindert, dass Randgebäude in zwei Gebieten landen.
3. **Bestätigen:** Ein Panel zeigt die Zusammenfassung — Anzahl Gebäude und die **automatisch abgeleitete Straßenliste** mit Hausnummernbereichen („Musterstraße 1–41, Lindenweg 2–18a"). Straßen werden aus den Adressen der ausgewählten Gebäude gruppiert; niemand muss sie von Hand zuordnen.
4. **Zuteilen:** Das Gebiet bekommt einen Namen (Vorschlag „Gebiet 3", änderbar) und wird per Auswahl einem Verteiler-Namen zugeordnet (Abschnitt 5). Alle enthaltenen, noch offenen Häuser wechseln in „Zugeteilt".

### Regeln, die früh festgelegt sein müssen

- **Gebäude sind die zugeteilte Einheit, Straßen nur Anzeige.** Eine Straße kann durch mehrere Gebiete laufen; über die Hausnummernbereiche bleibt trotzdem eindeutig, wer welchen Abschnitt hat. Wer versucht, ganze Straßen als Einheit zu vergeben, baut sich Konflikte ein.
- **Überschneidungen:** Liegt im umfahrenen Bereich ein bereits zugeteiltes Haus, wird es standardmäßig **nicht** übernommen, sondern schraffiert angezeigt — mit der Option „in dieses Gebiet verschieben". Kein stilles Umhängen.
- **Mitgliederliste wird eingefroren:** Die Gebäudeliste eines Gebiets wird bei der Zuteilung festgeschrieben. Ein späteres Update der Gebäudedaten verändert bestehende Zuteilungen nicht; das Polygon bleibt nur für Darstellung und Bearbeitung erhalten.
- **Auflösen und Ändern:** Ein Gebiet kann aufgelöst werden — Häuser ohne Ergebnis fallen zurück auf „Unerreicht", bereits verteilte/gesprochene behalten ihr Ergebnis. Einzelne Häuser können per Klick einem anderen Gebiet zugeschlagen werden.

Technisch ist das Punkt-in-Polygon-Standardhandwerk: in Stufe 1 clientseitig (turf.js) — bei einem Pilotgebiet mit wenigen tausend Gebäuden verzögerungsfrei —, ab Stufe 2 serverseitig in PostGIS (`ST_Within`).

---

## 5. Verteiler ohne Login: Sitzungs-Namen

Stufe 1 kommt bewusst ohne Konten aus, soll die Zuteilung aber vollständig vorführen können. Dafür gibt es **Verteiler-Namen, die nur für die Sitzung existieren**:

- In einer Seitenleiste legt man frei Namen an („Maria", „Familie Weber", „Team Nord"). Jeder Name bekommt automatisch eine Farbe; die Gebiete auf der Karte tragen die Farbe ihres Verteilers als Umrandung (die Gebäudefüllung zeigt weiterhin den Status — zwei getrennte visuelle Ebenen, die sich nicht in die Quere kommen).
- Die Namen sind **sitzungsgebunden**: Sie leben im Browser und verschwinden mit dem Ende der Sitzung. Es entsteht bewusst kein Nutzerbestand und nichts Personenbezogenes — für den Demonstrator sind es Spielfiguren.
- Damit eine vorbereitete Vorführung nicht bei jedem Öffnen neu aufgebaut werden muss, gibt es **Export/Import als Datei**: Der komplette Demo-Stand (Namen, Gebiete, Status) lässt sich als JSON sichern und wieder laden. Das ist ein bewusster Handgriff — nichts wird ungefragt dauerhaft gespeichert. Dazu ein „Demo zurücksetzen"-Knopf.

### Vorbereitet für den Bibel TV Login

Im Datenmodell ist der Verteiler von Anfang an ein eigenes Objekt (`distributor`) mit interner ID; Gebiete und Zuteilungen verweisen nur auf diese ID. In Stufe 3 kommt ein Feld `external_id` hinzu, das die ID aus dem Bibel TV Login (mBTV-JWT, siehe 9.1) trägt. **Die Zuordnungslogik ändert sich beim Login-Umstieg nicht** — nur die Identitätsquelle wechselt von „frei eingetippter Sitzungsname" zu „angemeldetes Konto". Genau dafür lohnt sich die saubere Trennung jetzt.

---

## 6. Bedienung im Überblick (Stufe 1)

```
┌─────────────────────────────────────────────────────────────────────┐
│  UNERREICHT 1.240 │ ZUGETEILT 380 │ VERTEILT 512 │ GESPROCHEN 87    │  ← KPI-Leiste, fixiert
├───────────────────────────────────────────────┬─────────────────────┤
│                                               │  Verteiler          │
│                                               │  ● Maria            │
│              KARTE                            │  ● Team Nord        │
│   Gebäude gefüllt nach Status                 │  [+ Name anlegen]   │
│   Gebiete umrandet nach Verteiler             │                     │
│                                               │  Gebiete            │
│   [Lasso] [Polygon] [Auswählen]               │  ▸ Gebiet 1 → Maria │
│                                               │    Musterstr. 1–41  │
│                                               │    38 Häuser · 61 % │
└───────────────────────────────────────────────┴─────────────────────┘
```

- **Haus anklicken** → kleines Panel mit den vier Zuständen als große Schaltflächen (Unerreicht ist der Ausgangszustand; wählbar sind „Zuteilen an…", „Verteilt", „Persönlich gesprochen", plus „Nicht zustellbar" als Ausnahme und „Zurücknehmen").
- **Gebiet in der Liste anklicken** → Karte springt hin, Straßenliste und Fortschritt des Gebiets erscheinen.
- **Undo** für jede Aktion — fällt aus dem Ereignisprotokoll ohne Zusatzaufwand ab.

Die Ein-Klick-Philosophie aus v1 gilt weiter: keine Formulare, keine Pflichtfelder.

---

## 7. Datenmodell

Vier Objekte; es gibt weiterhin bewusst keine Entität „Bewohner", „Haushalt" oder „Kontakt".

### `building` — unverändert aus v1
Ortsschicht aus OSM/ALKIS: `building_id`, `osm_id`/`alkis_id`, `geometry`, `street`, `housenumber`, `postcode`, `city`, `source`, `imported_at`. Keine Klingelschilder, keine Türebene.

### `distributor` — neu (ersetzt `volunteer` aus v1 für die ersten Stufen)
| Feld | Bedeutung |
|---|---|
| `distributor_id` | interne ID |
| `display_name` | frei gewählter Name |
| `color` | Kartenfarbe |
| `external_id` | *leer bis Stufe 3*, dann Bibel-TV-Login-ID |

In Stufe 1 sitzungsgebunden, ab Stufe 2 serverseitig, ab Stufe 3 kontogebunden.

### `area`
| Feld | Bedeutung |
|---|---|
| `area_id`, `name` | ID, Anzeigename |
| `boundary` | gezeichnetes Polygon (Anzeige/Bearbeitung) |
| `building_ids` | **eingefrorene** Gebäudeliste (die Wahrheit der Zuteilung) |
| `distributor_id` | zugeteilter Verteiler |

### `event` — das Herzstück, unverändert im Prinzip aus v1
Jede Handlung ist ein unveränderliches Ereignis: `event_id` (UUID), Typ (`zugeteilt`, `verteilt`, `gesprochen`, `nicht_zustellbar`, `zurückgenommen`, `gebiet_angelegt`, …), Bezug (`building_id`/`area_id`), `distributor_id`, grober Zeitstempel. Der aktuelle Zustand jedes Hauses ist immer die **Ableitung** aus der Ereignisfolge nach den Rangregeln aus 2.2.

Dass schon der Browser-Demonstrator so arbeitet, hat drei Gründe: Undo/Redo sind trivial; der Export/Import ist einfach die Ereignisliste; und Stufe 2/3 übernehmen dasselbe Format — hochladen statt umbauen.

---

## 8. Datenpipeline (Stufe 1: Pilotgebiet)

Für den Demonstrator wird kein Deutschland-Import gebraucht, sondern ein sauberes Pilotgebiet:

1. **Extrakt:** Geofabrik-Auszug des betreffenden Bundeslands (oder Overpass-Abfrage für das Pilotgebiet), gefiltert auf `building=*` **mit** `addr:housenumber`.
2. **Zählbasis bereinigen:** Nebengebäude raus (`building=garage|shed|carport|roof|…`), Duplikate zusammenführen. Definition festhalten: *Zählbasis = adressierte (Wohn-)Gebäude.* Diese Definition bestimmt die „Gesamt"-Zahl der KPI-Leiste und muss deshalb dokumentiert und stabil sein.
3. **Ausgabe:** eine GeoJSON-Datei des Pilotgebiets (bei einem Stadtteil wenige MB — problemlos direkt ladbar) plus selbst gehostete Basiskarten-Kacheln (PMTiles aus demselben Extrakt; eine statische Datei, kein Kartenserver nötig).

Ab Stufe 2 wächst dieselbe Pipeline auf PostGIS und ganz Deutschland (22–23 Mio. Gebäude — für PostGIS unkritisch, siehe v1 Abschnitt 10); die ALKIS-Lückenfüllung über die kostenlosen Länder-Gebäudereferenzen bleibt wie in v1 Abschnitt 9 beschrieben, ebenso der ODbL-Lizenzhinweis (Share-alike erst bei Weitergabe der kombinierten Datenbank relevant).

---

## 9. Architektur je Stufe — und die Bibel-TV-Standards

### Stufe 1: statische Website, null Backend
Frontend-Anwendung + GeoJSON + PMTiles auf einem statischen Webspace. Kein Server, keine Datenbank, keine Schnittstelle — damit auch fast keine Betriebs- und Sicherheitsfläche. Nach den Umgebungsbezeichnungen des Hauses (PS#0003) ist der Demonstrator eine **alpha**-Umgebung: „Entwicklungsstand, der gezeigt werden kann." Als für Nutzer bestimmter Inhalt läuft er gemäß PS#0004 unter einer bibeltv.de-Subdomain, ohne abweichende Ports.

### Stufe 2: schlankes Backend
- PostgreSQL/PostGIS + kleine HTTP-API, die Ereignisse entgegennimmt und abgeleitete Stände/KPIs liefert.
- Betrieb als Docker-Image nach **PS#0005**: Konfiguration ausschließlich über einzeln benannte Umgebungsvariablen (im README dokumentiert), zustandsloses Image, Persistenz nur in der zentralen Datenbank, Logs nach stdout/stderr, Versionierung nach SemVer mit `BTV_IMAGE_VERSION` und Versionsendpunkt `GET /.well-known/version`.
- Netz und Ports nach **PS#0002**: Webanwendung lauscht im Container auf 8080, gemappt ins web-net (`192.168.2.0`), Datenbank ins db-net (`192.168.3.0`); der Außenport wird bei Deployment in die zentrale Porttabelle eingetragen, nichts lauscht auf der öffentlichen IP.
- Umgebungen heißen `alpha` / `beta` / `live` (PS#0003), klein geschrieben in technischen Kontexten.

### Stufe 3: Login und Feld
- **Anmeldung nach PS#0001:** Identifikation über das Mein-Bibel-TV-JWT, übergeben im Header `X-MBTV-TOKEN`; das Backend validiert das Token und ordnet die mBTV-User-ID dem `distributor.external_id` zu. Keine User-IDs aus dem Client ohne backendseitige Authentizitätsprüfung, keine Token in Query-Parametern. Bei späterer Service-zu-Service-Kommunikation wird die validierte ID per `X-MBTV-ID` weitergereicht.
- **PWA offline:** Service Worker, lokale Ereignis-Warteschlange, Hintergrund-Sync — nach den Prioritätsregeln, die seit Stufe 1 gelten (v1 Abschnitt 6 bleibt die Referenz). Erst wenn die PWA im Feldtest an Grenzen stößt (z. B. Kartenspeicher auf iOS), wird eine native Verpackung (Capacitor) erwogen — der Web-Code bleibt dabei erhalten.

---

## 10. Datenschutz je Stufe

- **Stufe 1:** Es gibt schlicht keine personenbezogenen Daten — fiktive Sitzungsnamen, alles im Browser, kein Server. Der Demonstrator ist datenschutzrechtlich ein Nullrisiko und kann sofort intern gezeigt werden.
- **Stufe 2:** Weiterhin keine Personendaten, solange Verteiler-Namen frei gewählte Labels bleiben; der Serverbetrieb bringt die üblichen technischen Pflichten (Verschlüsselung, EU-Hosting).
- **Stufe 3:** Mit dem Login kommen echte Daten der eigenen Ehrenamtlichen — wie in v1 Abschnitt 7 beschrieben eine normale, gut beherrschbare Verarbeitung (Verzeichnis der Verarbeitungstätigkeiten, Löschkonzept nach Aktionsende, grobe Zeitstempel). Die rote Linie bleibt unverändert: keine adressscharfen Aussagen über Bewohner, kein „abgelehnt", keine Freitexte an Häusern.

---

## 11. Technologie-Vorschlag

| Baustein | Vorschlag | Begründung |
|---|---|---|
| Frontend | **TypeScript + React (Vite)** | größtes Ökosystem rund um die Kartenbibliotheken; schneller Start |
| Karte | **MapLibre GL JS** | offener Standard, Vektorkacheln, später identisch in der PWA/App nutzbar |
| Zeichnen | **Terra Draw** (Freihand + Polygon auf MapLibre) | genau das Lasso-Bedienmodell aus Abschnitt 4 |
| Geometrie | **turf.js** (Stufe 1) → **PostGIS** (ab Stufe 2) | Punkt-in-Polygon client- bzw. serverseitig |
| Basiskarte | **PMTiles** (selbst gehostete statische Datei) | keine Drittabflüsse, kein Kartenserver-Betrieb |
| Zustand | Ereignisliste + abgeleiteter Store | Undo gratis, Format identisch mit Stufe 2/3 |
| Backend (ab Stufe 2) | PostgreSQL + PostGIS, schlanke HTTP-API, Docker nach PS#0005 | siehe 9 |

---

## 12. Umsetzungsplan und offene Entscheidungen

### Stufe 1 — Demonstrator (überschaubarer Umfang)

Enthalten: Pilotgebiet laden · KPI-Leiste · Karte mit Statusfarben · Lasso-/Polygon-Zuteilung mit Straßenliste · Sitzungs-Verteiler mit Farben · Haus-Klick mit den vier Zuständen · Undo · Demo-Reset · Export/Import des Demo-Stands.
Bewusst **nicht** enthalten: Login, Backend, Offline, Mehrbenutzer-Gleichzeitigkeit, Postgebiets-Anbindung.

### Vor dem Start zu entscheiden

1. **Pilotgebiet: ✅ entschieden (03.07.2026) — Bonn-Bad Godesberg.** Der Stadtbezirk ist in OSM sauber als Verwaltungsgrenze abgegrenzt und bringt gemischte Bebauung (Villenviertel, Geschosswohnungen, Zentrum). Abdeckungsprüfung per Overpass (Datenstand 02.07.2026): 21.655 Gebäude, davon 14.603 mit Hausnummer direkt am Gebäude, plus 804 Adresspunkte. Ein Großteil der Gebäude ohne Hausnummer sind Nebengebäude, die ohnehin aus der Zählbasis fallen — die Abdeckung ist für den Demonstrator gut geeignet.
2. **Zählbasis:** Bestätigung der Definition „adressierte Wohngebäude, Nebengebäude raus" — daran hängt jede Zahl in der KPI-Leiste.
3. **Häuser vs. Haushalte in der Außenkommunikation:** KPI-Leiste zählt Häuser; wann und wie eine Haushalts-Schätzung ergänzt wird.
4. **Ablehnungs-Handling im Feld (Stufe 3):** Wie wird ein Haus „abgeschlossen ohne Übergabe", ohne eine adressscharfe Ablehnung zu speichern — Empfehlung aus v1 bleibt: nicht adressscharf, nur aggregiert, Entscheidung vor Stufe 3.
5. **Subdomain/Hosting** des Demonstrators (alpha-Umgebung unter bibeltv.de, PS#0003/PS#0004).
