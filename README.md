# Classroom Equipment Borrowing and Return Management System

A Java Swing desktop application for recording classroom equipment, student borrowers, requisitions, returns, and historical records. Intended for **Software Development Fundamentals** and to demonstrate **Object-Oriented Programming**.

![Platform](https://img.shields.io/badge/Java-17%2B-blue) ![UI](https://img.shields.io/badge/UI-Java%20Swing-blue) ![Database](https://img.shields.io/badge/Database-MySQL%208%2B-blue)

## Features

- **Secure login:** On first launch, create an administrator account. Later launches require username/password. Passwords use a unique salt and PBKDF2-SHA256 hashing; there is **no default password**.
- **Dashboard:** Equipment counts, currently borrowed items, overdue items, students, and recent activity.
- **Equipment masterfile:** Add, edit, delete, search, and categorize individual items as electronic/non-electronic. Items with historical transactions cannot be deleted.
- **Students masterfile:** Add, edit, delete, and search student ID, name, course, and section. Students with historical transactions cannot be deleted.
- **Borrow Item:** Record requisition number, student, date/time, due time, and multiple uniquely identified items per transaction.
- **Return Item:** Mark active loans returned and automatically free those items for the next borrower.
- **History:** Search all borrowing and return transactions, including overdue status.

The application runs **locally on one Windows computer** and persists all records in a **MySQL** database (not CSV). A running MySQL server is required; nothing is hosted online.

## Requirements

- Java Development Kit **17 or newer** (Java 23 has been detected on the development PC)
- MySQL Server **8.0 or newer** installed and running
- **Visual Studio Code** with *Extension Pack for Java* (recommended)
- Optional: Maven 3.9+ to resolve dependencies automatically
- Internet access on first `run.ps1` use to download the official MySQL Connector/J driver

## Database setup (one time)

1. Install and start a MySQL 8.x server. In MySQL Workbench or the MySQL shell, open `database/schema.sql`.
2. **Change** `CHANGE_ME_TO_A_STRONG_PASSWORD` to a strong password before executing that SQL script. It creates the `classroom_equipment_db` database and a least-privilege `equipment_app` user.
3. Set the following environment variables **in the same terminal** where the program is launched:

   ```powershell
   $env:DB_URL = 'jdbc:mysql://localhost:3306/classroom_equipment_db?connectionTimeZone=LOCAL'
   $env:DB_USER = 'equipment_app'
   $env:DB_PASSWORD = 'YOUR_PASSWORD_FROM_SCHEMA_SQL'
   ```

   Replace the placeholder password with your actual local database password. **Never commit real credentials.**

4. When the app opens, it automatically creates any missing tables (`users`, `students`, `equipment`, `borrowings`). On its first launch, it prompts you to create an administrator. You must remember the username/password you choose.

**MySQL connection troubleshooting:** Check that the MySQL service is running, `localhost:3306` is accessible, the schema was applied successfully, and the DB credentials match the MySQL user. If you previously ran the SQL script with the wrong placeholder password, update the MySQL user password in MySQL manually.

## Run in Visual Studio Code

Open the **entire project folder** in Visual Studio Code:

```text
CS26 Project 2026/
└── Classroom Equipment Borrowing and Return System/
    ├── pom.xml
    ├── database/schema.sql
    ├── src/main/java/edu/cs26/equipment/
    └── run.ps1
```

**Option A — Maven:** Run `mvn compile exec:java` in VS Code's terminal (requires Maven). MySQL Connector/J is resolved from Maven Central.

**Option B — Built-in script (no Maven needed):** In VS Code's **PowerShell terminal** run:

```powershell
.\run.ps1
```

The script downloads the JDBC driver into ignored `lib/` on its first run, compiles the Java sources with `javac`, then launches the Swing application. If PowerShell restricts scripts, run `powershell -ExecutionPolicy Bypass -File .\run.ps1` in a terminal **only if you trust the checked-in script**.

The included `.vscode/launch.json` can also run the Java main class when the Java extensions, project dependencies, and DB environment variables are configured.

## How to use

1. Start MySQL, configure the environment variables, and launch the app.
2. On first launch **create the administrator**; then sign in.
3. Add students under **Students**. Add equipment under **Equipment** (example IDs `EQ-001`, `EQ-002`).
4. Open **Borrow Item**, select a student and one or more available equipment IDs, set the return deadline, and press **Save Borrowing**.
5. Open **Return Item**, select a borrowing row, and press **Mark Selected as Returned**.
6. Review records in **Borrowing History** or statistics in **Dashboard**.

### Business rules

- Each physical equipment item has a **unique equipment ID** and quantity **1** per borrowing line.
- Only **available** equipment may be borrowed; borrowing and return updates are database transactions.
- A due time **must be later** than the borrowing time.
- Returns preserve borrowing history instead of deleting transactions.
- Users can't delete equipment or students that are referenced by borrowing history.
- Intended for offline/single-user use, not a multi-user distributed deployment.

## OOP principles demonstrated

| Principle | Code |
| --- | --- |
| **Encapsulation** | `Equipment` stores private immutable attributes behind getters; `Repository` centralizes data access. |
| **Abstraction** | `Equipment` is an abstract class defining category and handling behavior. |
| **Inheritance** | `ElectronicEquipment` and `NonElectronicEquipment` inherit from `Equipment`. |
| **Polymorphism** | Each subclass overrides `handlingInstructions()`; UI invokes the correct implementation. |

## Compile and smoke-test without MySQL

Domain checks can be run without a database. From the folder root, compile all Java classes into `build/classes` and run the standalone `ModelSmokeTest` in `src/test/java`. Full database/integration testing requires the MySQL service and schema configured above.

## Project scope

Subject: **Software Development Fundamentals** · Language: **Java** · IDE: **Visual Studio Code** · UI: **Swing** · Storage: **MySQL** · Architecture: **OOP + JDBC**.

Security note: This is an academic desktop application, not a production identity platform. The initial administrator manages the application; staff-account creation and password resets are not yet exposed in the UI.
