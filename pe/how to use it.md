# How to use the student forum

This guide explains how to start the application and use its student and moderator views on Windows.

## Login details

Enter the matching account details on the login page. The passwords are chosen when the backend starts; the application does not create accounts or store them in the database.

| Account | Username | Password |
| --- | --- | --- |
| Student | `student` | _______b3gIWgwAm1wA7VKXiOY1jeLROWTeOjme6E3CkHN-XqQ_______________________ |
| Moderator | `moderator` | ____h5qPs3cR-fxZJHfspsgd9wW-uQoMx4n0__________________________ |

Write passwords here only in a private copy kept on your own computer. Do not commit or share a document containing passwords. If you do not want passwords written down, leave these fields blank and enter them when prompted in the backend terminal.

## Start the application

You need Java 17, Maven, Node.js with npm, and a running MySQL 8 server.

### 1. Start MySQL

Start your local MySQL service if it is not already running. If you use the portable MySQL installation supplied with this project, configure `MYSQL_HOME` and `MYSQL_DATA` for its installation and data folders, then run this in PowerShell:

```powershell
$server = Join-Path $env:MYSQL_HOME 'bin\mysqld.exe'
$mysqlArgs = @(
    "--basedir=$env:MYSQL_HOME",
    "--datadir=$env:MYSQL_DATA",
    '--bind-address=127.0.0.1',
    '--port=3306',
    '--mysqlx=OFF',
    "--log-error=$env:MYSQL_DATA\server-error.log"
)
Start-Process -FilePath $server -ArgumentList $mysqlArgs -WindowStyle Hidden
```

The backend connects to `student_forum` using the `DB_USERNAME` and `DB_PASSWORD` environment variables. Use the MySQL application account configured on your computer (the project setup uses `forum_app`).

### 2. Start the backend

Open a PowerShell terminal at the project folder. Set the usernames and MySQL account, then enter the passwords when prompted. Password input is hidden:

```powershell
$env:STUDENT_USERNAME = 'student'
$env:MODERATOR_USERNAME = 'moderator'
$env:DB_USERNAME = 'forum_app'

$studentPasswordPointer = $null
$moderatorPasswordPointer = $null
$databasePasswordPointer = $null
try {
    $secureStudentPassword = Read-Host 'Choose student password' -AsSecureString
    $studentPasswordPointer = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($secureStudentPassword)
    $env:STUDENT_PASSWORD = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($studentPasswordPointer)

    $secureModeratorPassword = Read-Host 'Choose moderator password' -AsSecureString
    $moderatorPasswordPointer = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($secureModeratorPassword)
    $env:MODERATOR_PASSWORD = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($moderatorPasswordPointer)

    $secureDatabasePassword = Read-Host 'MySQL application account password' -AsSecureString
    $databasePasswordPointer = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($secureDatabasePassword)
    $env:DB_PASSWORD = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($databasePasswordPointer)

    mvn -f backend\pom.xml spring-boot:run
}
finally {
    if ($studentPasswordPointer) { [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($studentPasswordPointer) }
    if ($moderatorPasswordPointer) { [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($moderatorPasswordPointer) }
    if ($databasePasswordPointer) { [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($databasePasswordPointer) }
    Remove-Item Env:STUDENT_PASSWORD,Env:MODERATOR_PASSWORD,Env:DB_PASSWORD -ErrorAction SilentlyContinue
}
```

Keep this terminal open while using the application. If your MySQL account does not use the default database URL, set `DB_URL` before starting the backend, for example:

```powershell
$env:DB_URL = 'jdbc:mysql://localhost:3306/student_forum?createDatabaseIfNotExist=true&useSSL=false&serverTimezone=UTC'
```

### 3. Start the Angular frontend

Open a second terminal:

```powershell
Set-Location frontend
npm install
npm start
```

Open <http://localhost:4200/login>.

## Student view

1. Sign in with the student username and the student password chosen when starting the backend.
2. The student view opens at `/home`.
3. Read published comments in the campus feed.
4. Use the form to submit a title and comment. A submitted comment remains pending and does not appear in the public feed until a moderator approves it.
5. Use **Actualiser** to reload the feed when needed.

## Moderator view

1. Sign in with the moderator username and the moderator password chosen when starting the backend.
2. The moderator view opens at `/moderation`.
3. Review pending comments, their AI scores, detected topic, and AI recommendation.
4. Enter a reason before choosing **Approuver et publier** or **Rejeter**. The AI recommendation is advisory; the moderator makes the final decision.
5. The moderation statistics show pending, published, and rejected comments. Use **Actualiser** to reload the queue and statistics.
6. The moderator can also analyze one comment or a batch of up to 100 comments. Analysis alone never publishes or rejects a comment.
7. The approved feed allows moderators to edit or delete published comments.

## Hugging Face moderation

To use AI analysis, set a valid Hugging Face Inference Providers token in the backend terminal before starting Spring Boot:

```powershell
$env:HF_API_TOKEN = 'your-token'
```

Do not write the token into source code, `application.properties`, or this guide. If you set the token in the terminal, remove it when you stop the backend:

```powershell
Remove-Item Env:HF_API_TOKEN -ErrorAction SilentlyContinue
```

Without a valid token, requests that need AI analysis will fail; the application does not silently publish a comment instead. The AI scores and topic classification may be inaccurate, particularly across languages. A moderator always makes the publication decision.

## Troubleshooting

- **The login page does not open:** check that the Angular terminal is running and visit <http://localhost:4200/login>.
- **The page reports that it cannot reach the server:** check that the backend terminal is running on port `8080`.
- **A login fails:** use the matching student or moderator password entered when the backend started. If the backend was restarted, enter the new passwords chosen for that run.
- **A database connection fails:** verify that MySQL is running and that `DB_USERNAME`, `DB_PASSWORD`, and, if needed, `DB_URL` match your local configuration.
- **AI analysis fails:** verify that `HF_API_TOKEN` is valid and permitted to use Inference Providers.

Sessions are stored in an HTTP-only browser cookie and expire after 30 minutes of inactivity. Signing out ends the current session. These settings are intended for a local academic demonstration; a public deployment needs HTTPS, CSRF protection, and a proper account-management system.
