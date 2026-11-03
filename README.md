# Classroom Equipment Borrowing and Return Management System

An offline, single-computer **Java Swing + MySQL** application built for Software Development Fundamentals. It uses object-oriented design to register classroom equipment, manage student borrowers, record equipment loans/returns, and review borrowing history.

## For teammates: install and run on your own PC

Each teammate needs their **own local MySQL server**. GitHub stores **source code**, not your MySQL records. Cloning the repo will not automatically copy another person's database or login account.

### 1. Install the prerequisites (Windows)

- [Java JDK 17 or newer](https://adoptium.net/) (not just the Java runtime)
- [Visual Studio Code](https://code.visualstudio.com/) plus the **Extension Pack for Java**
- [MySQL Community Server 8](https://dev.mysql.com/downloads/mysql/) and [MySQL Workbench](https://dev.mysql.com/downloads/workbench/)
- Git (or download the GitHub project ZIP)
- Internet on first app launch to download **MySQL Connector/J**

Confirm Java is installed by running `javac -version` and `java -version` in the VS Code terminal. The JDK must appear in the system PATH. **Maven is optional**; the project includes `run.ps1`.

### 2. Download and open the project

In a terminal:

```powershell
git clone https://github.com/renneyxremedios/Classroom-Equipment-Borrowing-and-Return-System.git
cd Classroom-Equipment-Borrowing-and-Return-System
code .
```

Alternatively, GitHub **Code → Download ZIP**, extract the entire ZIP, then use **File → Open Folder** in VS Code.

**Open the folder that contains `pom.xml`, `run.ps1`, and `database/`**, not a nested `src` folder.

### 3. Start MySQL and initialize the database in Workbench (once per PC)

1. Ensure the MySQL Server service is **Running**. If it is stopped, open PowerShell **as Administrator** and run `Start-Service MySQL80` (the name may differ by installation).
2. Open **MySQL Workbench**, connect to your **Local instance MySQL80** (or your own local MySQL connection) with the **root administrator password you set during MySQL installation**.
3. In Workbench, choose **File → Open SQL Script**, then open **`database/schema.sql`** from the downloaded project.
4. In the Workbench SQL editor, replace **`CHANGE_ME_TO_A_STRONG_PASSWORD`** with a strong, unique password **you choose for `equipment_app`**. Execute **all** of the SQL script using the lightning-bolt **Execute All** action. This creates:
   - Database `classroom_equipment_db`
   - Login `equipment_app`@`localhost`, with the required `REFERENCES` permission
   - Tables `users`, `students`, `equipment`, and `borrowings`
5. Check Workbench's **Action Output** for errors. For verification, run:
   ```sql
   SHOW DATABASES LIKE 'classroom_equipment_db';
   USE classroom_equipment_db;
   SHOW TABLES;
   ```
   Expected tables: `users`, `students`, `equipment`, `borrowings`.

**Important:** The password replacement is for the SQL editor on **your PC only**. Do **not** commit or push it to GitHub or send screenshots containing it. You do not need to share the root password or `equipment_app` password with teammates. Each PC uses its own credentials.

**If the `equipment_app` account already exists:** `CREATE USER IF NOT EXISTS` will not change its password. In Workbench, while connected as the administrator, you can run `ALTER USER 'equipment_app'@'localhost' IDENTIFIED BY 'YOUR_OWN_NEW_PASSWORD';` using your intended local password. If you previously got `REFERENCES command denied`, run `GRANT REFERENCES ON classroom_equipment_db.* TO 'equipment_app'@'localhost';`.

### 4. Configure your local app password (once per PC)

In **VS Code → Terminal → New Terminal** (PowerShell), run:

```powershell
Copy-Item .env.example .env
```

Open the newly created **`.env`** file in VS Code. Change **only** its `DB_PASSWORD` value to the `equipment_app` password you chose in Step 3.

Example of the format (the password shown is only a placeholder):

```dotenv
DB_URL=jdbc:mysql://localhost:3306/classroom_equipment_db?connectionTimeZone=LOCAL
DB_USER=equipment_app
DB_PASSWORD=REPLACE_WITH_YOUR_MYSQL_PASSWORD
```

Do not enter `$env:` in this file. It must contain plain `KEY=value` lines. **Never commit `.env`**; it is ignored by Git.

If you already have `DB_PASSWORD` set in your current PowerShell session, that **takes precedence** over the `.env` file. To use the value from `.env`, run `Remove-Item Env:DB_PASSWORD -ErrorAction SilentlyContinue` in the VS Code terminal or open a new terminal.

### 5. Run the desktop app

In the **project-root** PowerShell terminal:

```powershell
.\run.ps1
```

The script automatically reads your `.env`, downloads the MySQL JDBC driver the first time, compiles Java source code, and launches the Swing window. **Maven is not needed** for this method.

Alternatively, with the VS Code Java extensions installed, **Run and Debug → Classroom Equipment System** reads the same `.env`.

On the **first launch on each fresh database**, create your app's **administrator username and password**. This app login is **different** from MySQL's root and `equipment_app` passwords. Then sign in.

### 6. Check that the program works

1. Log in to the app.
2. Go to **Students** and add a test student with an ID.
3. Go to **Equipment** and add a unique equipment ID (for example `EQ-001`).
4. Go to **Borrow Item**, select the student and equipment, and save the borrowing.
5. Go to **Return Item**, select the active borrowing, and mark it returned.
6. Review **Borrowing History** and **Dashboard**.

**You are done when all six steps work.** If they don't, see Troubleshooting.

## Common errors

| Message / symptom | Meaning and fix |
| --- | --- |
| `Access denied for user 'equipment_app'@'localhost'` | Wrong `DB_PASSWORD`, or the account was not set up. Re-check `.env`; use Workbench as root to `ALTER USER` if needed. |
| `REFERENCES command denied` | Missing permission needed to create foreign keys. In Workbench as root: `GRANT REFERENCES ON classroom_equipment_db.* TO 'equipment_app'@'localhost';` |
| `Communications link failure` / `Connection refused` | MySQL service is stopped or listening on a different port. Start MySQL and confirm `localhost:3306`. |
| `Unknown database 'classroom_equipment_db'` | Execute **all of `database/schema.sql`** in MySQL Workbench as an administrator. |
| `DB_PASSWORD` missing | Create `.env` from `.env.example` and enter the MySQL **equipment_app** password. |
| `javac` not recognized | Install **JDK 17+**; restart VS Code and confirm `javac -version`. |
| PowerShell will not run `run.ps1` | After reviewing the script, launch it with `powershell -ExecutionPolicy Bypass -File .\run.ps1` in the project terminal. |
| Driver could not be downloaded | Check internet connection for Maven Central or use Maven to manage JDBC dependencies. |
| Program asks to create an administrator again | You may be connecting to a different or fresh database; verify `DB_URL` and `DB_USER`. |

## Tests and builds

Test the OOP model **without MySQL**:

```powershell
.\run.ps1 -SmokeTest
```

Compile without launching the application:

```powershell
.\run.ps1 -CompileOnly
```

With Maven installed, compile with `mvn compile`, run with `mvn exec:java`, and build with `mvn package`. GitHub Actions automatically compiles the project and runs standalone smoke tests, **not** a real MySQL integration test.

## Features

- **Login and first-run administrator setup** (PBKDF2-SHA256 salted password hashes; no default credentials).
- **Dashboard** with available/borrowed/overdue counts.
- **Equipment**: add/edit/delete/search items by ID and name, categorize electronic/non-electronic.
- **Students**: add/edit/delete/search student IDs, names, courses, and sections.
- **Borrow Item**: record requisition, student, date/time, return deadline and selected physical equipment.
- **Return Item**: record returns, release the item back to inventory.
- **History**: view all transactions including overdue and returned items.

## Object-Oriented Programming principles

| Principle | Demonstration |
| --- | --- |
| Encapsulation | `Equipment` has private fields and public getters; data access is managed through `Repository`. |
| Abstraction | Abstract `Equipment` defines properties and handling rules. |
| Inheritance | `ElectronicEquipment` and `NonElectronicEquipment` extend `Equipment`. |
| Polymorphism | Each subclass overrides `handlingInstructions()` and the UI calls the appropriate implementation. |

## Scope and limits

- Java 17+, Java Swing, JDBC, MySQL 8, Visual Studio Code.
- Offline **single-computer** desktop application, using a local MySQL server.
- One physical item per unique equipment ID; a borrowing requisition can contain multiple items.
- Borrowing/returns update data inside MySQL transactions and keep history.
- Deleting students/equipment with historical transactions is not allowed.
- No online access, cross-computer synchronized records, staff-account management UI, or password reset interface.
- Never store real database credentials in code or Git. Rotate any password accidentally exposed in a screenshot or commit.
