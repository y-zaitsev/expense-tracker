# Expense Tracker

Eine Desktop-Anwendung zur Verwaltung persönlicher Ausgaben und Budgets. Das Projekt ist mit Kotlin entwickelt und nutzt SQLite zur lokalen Speicherung der Daten. Die Anwendung bietet eine grafische Oberfläche und kann alternativ über die Kommandozeile gestartet werden.

## Funktionen

- **Ausgaben verwalten** – Ausgaben erfassen und verwalten.
- **Budgets verwalten** – Budgets über eine eigene Datenzugriffsschicht bearbeiten.
- **Lokale Datenspeicherung** – Daten in einer lokalen SQLite-Datenbank speichern.
- **CSV-Export** – Ausgabedaten als CSV-Datei exportieren.
- **Grafische Oberfläche** – Die Anwendung startet standardmäßig mit einer Desktop-Oberfläche.
- **Kommandozeile** – Die Anwendung mit dem Argument `--cli` im Terminal starten.
- **Eigener Datenbankpfad** – Bei Bedarf eine andere Datenbankdatei verwenden.

## Verwendete Technologien

| Technologie | Einsatz |
|---|---|
| Kotlin | Programmiersprache |
| JVM 17 | Laufzeitumgebung |
| Gradle Kotlin DSL | Build-Konfiguration |
| SQLite | Lokale Datenbank |
| SQLite JDBC | Verbindung zur Datenbank |
| Java Swing | Grafische Oberfläche |
| JUnit Platform | Ausführung der Tests |

## Voraussetzungen

- JDK 17 oder neuer
- Git

Gradle muss nicht separat installiert werden, da das Projekt den Gradle Wrapper enthält.

## Installation

Repository klonen und in das Projektverzeichnis wechseln:

```bash
git clone https://github.com/y-zaitsev/expense-tracker.git
cd expense-tracker
```

## Projekt bauen

Unter Linux oder macOS:

```bash
./gradlew build
```

Unter Windows:

```powershell
.\gradlew.bat build
```

## Anwendung starten

### Grafische Oberfläche

Die Anwendung mit folgendem Befehl starten:

```bash
./gradlew run
```

Unter Windows:

```powershell
.\gradlew.bat run
```

Standardmäßig wird die grafische Oberfläche gestartet.

### Kommandozeile

Um die Anwendung im Terminal zu starten, folgenden Befehl verwenden:

```bash
./gradlew run --args="--cli"
```

Unter Windows:

```powershell
.\gradlew.bat run --args="--cli"
```

### Eigenen Datenbankpfad festlegen

Standardmäßig wird die Datenbank unter folgendem Pfad gespeichert:

```text
~/.expense-tracker/expenses.db
```

Es ist möglich, einen anderen Pfad für die Datenbank anzugeben. Beispiel:

```bash
./gradlew run --args="--cli ./data/expenses.db"
```

Falls das übergeordnete Verzeichnis noch nicht existiert, wird es automatisch erstellt.

Die Argumente können kombiniert werden: `--cli` startet die Kommandozeile, während der Pfad die gewünschte Datenbankdatei festlegt.

## Datenspeicherung

Die Anwendung speichert Ausgaben und Budgets in einer lokalen SQLite-Datenbank. Die Daten werden über eigene Repositories verwaltet.

Die Datenbank liegt standardmäßig im Benutzerverzeichnis und nicht im Projektordner. Dadurch bleiben die gespeicherten Daten vom Quellcode getrennt.

Um Datenverlust zu vermeiden, sollte die Datenbank regelmäßig gesichert werden, bevor Dateien gelöscht oder ersetzt werden.

## Projektstruktur

Der Code ist in mehrere Bereiche aufgeteilt. Dadurch bleiben Datenbankzugriffe, Anwendungslogik, Datenexport und Benutzeroberfläche voneinander getrennt.

```text
expense-tracker/
├── src/
│   └── main/
│       └── kotlin/
│           └── com/
│               └── example/
│                   └── expensetracker/
│                       ├── data/
│                       ├── domain/
│                       ├── export/
│                       ├── service/
│                       ├── ui/
│                       │   └── gui/
│                       └── Main.kt
├── gradle/
│   └── wrapper/
├── build.gradle.kts
├── settings.gradle.kts
├── gradlew
└── gradlew.bat
```

Die wichtigsten Bereiche:

- `Main.kt` – Startpunkt der Anwendung. Wählt die Benutzeroberfläche aus und verbindet die einzelnen Komponenten.
- `data` – Zugriff auf die SQLite-Datenbank sowie Verwaltung von Ausgaben und Budgets.
- `domain` – Zentrale Datenklassen und Fehler rund um die Datenspeicherung.
- `service` – Enthält die Logik für die Verwaltung von Ausgaben und Budgets.
- `export` – Funktionen zum Export von Daten als CSV-Datei.
- `ui` – Benutzeroberflächen für das Terminal und den Desktop.
- `build.gradle.kts` – Enthält die Build-Einstellungen, Abhängigkeiten und den Einstiegspunkt der Anwendung.

## Tests

Alle Tests können mit folgendem Befehl ausgeführt werden:

```bash
./gradlew test
```

Unter Windows:

```powershell
.\gradlew.bat test
```

## Aufbau der Anwendung

Die Anwendung ist in mehrere Bereiche aufgeteilt. Die Repositories kümmern sich um den Zugriff auf die Datenbank. Der `ExpenseService` übernimmt die Verarbeitung der Ausgaben. Die Startklasse verbindet diese Komponenten mit der ausgewählten Benutzeroberfläche.

Durch diese Aufteilung bleibt der Code übersichtlich und kann leichter erweitert und getestet werden.

