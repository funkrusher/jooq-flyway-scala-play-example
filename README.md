# jOOQ with Flyway, Scala and Play-Framework Example

This Example-Project is based on (and written with help of) the following article:
- https://blog.jooq.org/2016/01/14/reactive-database-access-part-3-using-jooq-with-scala-futures-and-actors/

----

Dieses Beispielprojekt demonstriert die Verwendung von jOOQ in einer Scala / Play-Framework Umgebung. 

Aufgrund folgender Punkte könnte es Sinn machen jOOQ anstellte von Slick zu verwenden und dieses Projekt kann dabei helfen solche Anwendungsfälle zu testen:
- Performance
- Nachvollziehbarkeit des ausgeführten SQL

für Details, siehe auch: https://scalac.io/scala-slick-experience/

# Play-Framework Evolutions

Leider ist die "Evolutions" Funktionalität des Play-Frameworks so ausgelegt, dass das Projekt erst gestartet werden muss (hierzu muss es kompiliert werden). Erst danach ist es möglich die Evolutions durch Öffnen der Webbrowser-Addresse anzustossen.

es kann jedoch durch Verwendung des jOOQ-Codes (Autogenerierte Klassen) zu Situationen kommen, in denen der jOOQ-Code im "target"-Verzeichnis gelöscht wurde und somit die Applikation nicht gestartet werden kann, da sie nicht kompiliert. Der jOOQ-Code kann aber nur neu erzeugt werden, wenn die Datenbank im passenden Zustand ist (der oft erst nach Ausführung der Evolutions erreicht ist). Dies ist ein Henne-Ei Problem.

Soweit ich weiß, gibt es keine einfache Möglichkeit die Evolutions als Task in der "build.sbt" einzurichten, bzw. so einzurichten dass sie nicht den kompletten Classpath des Projekts benötigen, was ein kompilieren des Projekts erzwingt was wir wegen dem jOOQ-Codegenerator zu diesem Zeitpunkt nicht tun dürfen.

Daher nutze ich hier Flyway, welches auch nicht ideal aber zumindest ermöglicht, dass man ein SBT-Task einrichten kann, was ohne den Classpath auskommt und daher in einem Zustand ausgeführt werden kann, in welchem der Code nicht kompiliert werden muss.
siehe: https://davidmweber.github.io/flyway-sbt-docs/

# Vorbereitung

Eine neue MariaDB-Datenbank "testdb" in seinem lokalen XAMPP (LAMP) anlegen.

Die Login-Daten für diese Datenbank in der `conf/application.conf` eintragen.  

Das Projekt in Intellij importieren und dann die SBT-Shell starten. Hier dann nacheinander folgende Kommandos ausführen:

```
clean
flywayMigrate
generateJOOQ
run
```
Mit `clean` löscht man alles in seinem target-Folder. Mit `flywayMigrate` befüllt man seine testdb-Datenbank mit den erforderlichen Tabellen und Testdaten. Mit `generateJOOQ` erzeugt man in seinem target-Folder für diese testdb-Datenbank alle erforderlichen autogenerierten Klassen, die im Programmcode bereits verwendet und referenziert werden. Mit `run` startet man die Play-Applikation.

Dies sollte alles ohne Fehler ausgeführt werden. Nun kann man im Webbrowser die folgenden URLs ansteuern um jOOQ zu testen:

```
http://localhost:9000/
http://localhost:9000/book          (Bücher anzeigen, anlegen, löschen)
http://localhost:9000/author        (Autoren anzeigen, anlegen, löschen)
http://localhost:9000/test1         (Komplexer Select mit Join)
http://localhost:9000/test2         (5000 Autoren mit Batch-Insert einfügen)
http://localhost:9000/test3         (Alle Autoren anhand eines LIKE löschen)
http://localhost:9000/test4         (Transaktions-Beispiel)
http://localhost:9000/test5         (Transaktions-Beispiel für Fehlerfall / Rollback)
```

