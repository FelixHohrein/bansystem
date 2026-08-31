# Baublase Ban-System

Paper-Plugin (Minecraft 1.21) für permanents und temporäres Bannen, Historie, Templates und Alt-Check. Bedienung läuft vor allem über das GUI (`/bans`); Chat-Befehle bleiben als Shortcuts.

**Voraussetzungen:** Java 21, Paper 1.21, PostgreSQL.

## Schnellstart

1. Bauen: `mvn -q package`
2. JAR nach `plugins/` kopieren: `target/BaublaseBanSystem-1.0.0-SNAPSHOT.jar`
3. Server einmal starten. Das Plugin legt Configs an und verbindet **nicht** zur Datenbank (`connection-allowed: false`).
4. In `plugins/BaublaseBanSystem/database.yml` Host, Datenbank, User und Passwort eintragen, dann `connection-allowed: true`.
5. Server oder Plugin neu laden. Schlägt die Verbindung fehl, startet das Plugin **nicht**.

Tabellen werden mit `CREATE TABLE IF NOT EXISTS` angelegt (kein Flyway).

## Befehle

| Befehl | Beschreibung |
|---|---|
| `/bans` | Hauptmenü |
| `/bans <spieler>` | Punish-Menü für einen bekannten Spieler |
| `/bans help` | Hilfe |
| `/bans templates` | Template-Übersicht |
| `/bans reload` | Config, Sprache und Templates neu laden (keine DB-Reconnect) |
| `/ban <spieler> <grund>` | Permanent-Ban |
| `/tempban <spieler> <dauer> <grund>` | Temp-Ban |
| `/unban <spieler>` | Entbannen |
| `/banhistory <spieler>` | Ban-Historie |
| `/altcheck <spieler>` | Alt-Score und mögliche Zweitaccounts |
| `/bantemplate list\|set\|delete` | Templates |

Dauer: `1d`, `12h`, `30m`, kombinierbar (`1d12h`). `permanent` / `perm` für Vorlagen.

Offline bannen geht nur für Spieler, die schon auf **diesem** Server waren. Ein Grund ist immer Pflicht. Ein neuer Ban ersetzt einen aktiven.

## GUI

`/bans` ist der Hub: Spielerliste (Online zuerst), Gebannte, Suche, Templates, Statistik.

Ablauf Ban: Aktion → Dauer (Presets oder Chat) → Grund (Template oder Chat) → Bestätigung.

Spieler mit `bansystem.bypass` bleiben sichtbar, Ban-Buttons sind gesperrt („nicht bannbar“), außer Staff hat `bansystem.bypass.override`. History, Alt-Check und Unban bleiben möglich.

Sprache folgt der Client-Locale (DE/EN, Fallback Deutsch).

## Rechte

| Permission | Bedeutung |
|---|---|
| `bansystem.gui` | `/bans`-Menü |
| `bansystem.ban` | Permanent-Ban |
| `bansystem.tempban` | Temp-Ban |
| `bansystem.unban` | Entbannen |
| `bansystem.history` | Historie |
| `bansystem.altcheck` | Alt-Check |
| `bansystem.template` | Templates ändern |
| `bansystem.reload` | Reload |
| `bansystem.bypass` | Immun gegen Bans (nur online erkennbar) |
| `bansystem.bypass.override` | Darf Immune trotzdem bannen |
| `bansystem.admin` | Alle Rechte außer Bypass selbst |

Standard: OP, außer `bansystem.bypass` (`false`). Console darf Immune bannen.

## Konfiguration

Nach dem ersten Start unter `plugins/BaublaseBanSystem/`:

- `database.yml` — PostgreSQL + Connection-Pool
- `config.yml` — Debug, Spawn-/Public-Chunks, Alt-Score-Gewichte
- `banTemplate.yml` — Ban-Vorlagen
- `lang/de.yml`, `lang/en.yml` — Texte (MiniMessage)

Öffentliche Chunks (Spawn-Radius und `extra-chunks`) zählen nicht für den Standort-Teil des Alt-Scores.

## Alt-Score (0–100)

Wird bei jedem Check **live** aus Sessions und Chunk-Sichtungen berechnet. Wiederholte Signale über mehrere Tage füllen die Gewichte erst voll:

| Signal | Default |
|---|---|
| Gleiche aktuelle IP | 40 |
| Gemeinsame IPs über Tage | 25 |
| Nie gleichzeitig online | 15 |
| Ähnliche Login-Stunden über Tage | 10 |
| Gleiche private Chunks über Tage | 10 |
| Gleiche Locale + Client-Brand | 5 |

Chunks werden bei Join/Quit und alle 2 Minuten gespeichert. Der wahrscheinliche Main ist der älteste Account im Cluster (Score ≥ 20).

Kein Device-Fingerprinting, nur Vanilla-Signale (IP, Sessions, Locale, Brand, Login-Zeiten, private Chunks).

## Aufbau

```
net.baublase.bansystem
  bootstrap / config / i18n
  domain          Ban, Spieler, Session, Template, Alt
  application     Ban-, Alt-, Template-, Session-Logik
  storage         PostgreSQL + HikariCP
  command / gui / listener
```

Gebaut mit Maven, Paper-API provided, HikariCP und Postgres-Treiber in der JAR (Hikari relocated).
