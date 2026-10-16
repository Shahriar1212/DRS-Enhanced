# DRS-Enhanced — COIT20258 Assessment 3

Three-tier, MVC Disaster Response System built from the selected DRS-Initial (Assessment 2):

| Tier | Technology | Package |
|------|------------|---------|
| Presentation (client) | JavaFX 21 + FXML (Scene Builder) | `com.cqu.drs.client`, `src/main/resources/com/cqu/drs/client/view` |
| Business logic (middle) | Multi-threaded TCP socket server (thread pool), services | `com.cqu.drs.server`, `com.cqu.drs.server.service`, `com.cqu.drs.util`, `com.cqu.drs.security` |
| Data | MySQL 8 via JDBC (schema created programmatically) | `com.cqu.drs.server.db` |

Shared domain model: `com.cqu.drs.model`; client/server messages: `com.cqu.drs.protocol`.

## New domain features (DRS-Enhanced)
1. **Evacuation Warning & Shelter Management** – tiered warnings (Advice / Watch and Act / Emergency
   Warning) recommended from the assessed severity, linked to shelters; shelter capacity, check-in/out
   (row-locked so concurrent clients cannot overfill), best-shelter recommendation.
2. **Damage Assessment & Restoration Planning** – field officers record infrastructure damage; the system
   raises a prioritised restoration job for the responsible department (power → Ergon, debris → Waste
   Management, buildings → demolition by Fire & Emergency, …), tracks progress and summarises recovery.

## Security
Login with PBKDF2-hashed passwords and lock-out after 5 failures · role-based access rights enforced by
the server (Public Reporter < Field Officer < Coordinator < Administrator) · AES-256-GCM encryption of
phone numbers and private keys · time stamps on every record · RSA-2048 digital signatures on reports and
on a SHA-256 hash-chained audit log (non-repudiation, tamper detection).

## Requirements
* JDK 17+ (NetBeans 20 bundled JDK is fine) · Maven (bundled with NetBeans) · MySQL Server 8.x running on localhost:3306
* Internet access the first time (Maven downloads JavaFX, MySQL Connector/J, JUnit)

## Run (NetBeans)
1. **Database account (once):** open `database/00_create_app_user.sql` in MySQL Workbench as `root` → Execute.
   (Or edit `db.user` / `db.password` in `drs.properties` to use your own MySQL account.)
2. **Open project:** File → Open Project → select the `DRS-Enhanced` folder.
3. **Build:** Clean and Build (runs the 80 JUnit tests too).
4. **Start the server:** open `src/main/java/com/cqu/drs/server/ServerMain.java` → right-click → **Run File**
   (or right-click project → Run Maven → *Run DRS Server*). It creates database `drs_enhanced` and all tables.
5. **Start the client:** **Run Project** (green ▶ / F6). Start it several times for several concurrent users.
6. Log in as `admin / Admin@123` → **Administration → Import DRS-Initial data → Import bundled DRS-Initial data**.
7. Other accounts: `coord / Coord@123`, `officer / Officer@123`, `public / Public@123`.

## Run (command line)
```
mvn clean package                                    # build + tests, creates target/DRS-Enhanced-1.0.0-all.jar
java -cp target/DRS-Enhanced-1.0.0-all.jar com.cqu.drs.server.ServerMain     # terminal 1
java -jar target/DRS-Enhanced-1.0.0-all.jar                                   # terminal 2 (client)
```
or `run-server.bat` and `run-client.bat` on Windows after building.

## Tests
`mvn test` (NetBeans: Test Project, Alt+F6). Unit tests need nothing else; `ServerIntegrationTest` starts a
real server against the MySQL database `drs_test` and is skipped automatically if MySQL is not reachable.

## SQL script
`database/drs_enhanced.sql` creates and populates the database (optional — the server creates the schema
itself). Regenerate with `SqlScriptGenerator` (Run File).
