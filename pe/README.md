# Forum étudiant avec modération IA

Mini-projet universitaire : un forum étudiant avec espaces de connexion distincts selon le rôle, développé avec Angular, Spring Boot 3, MySQL et l’API d’inférence Hugging Face.

## Prérequis

- JDK 17 et Maven
- Node.js et npm
- MySQL Server 8
- Un jeton Hugging Face autorisé à utiliser les Inference Providers pour activer la modération

## Démarrage local

Ouvrir un terminal PowerShell à la racine du projet. Si MySQL a été installé dans le profil Windows avec l’installation portable du projet, démarrer le serveur local ainsi :

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

La base `student_forum` et l’utilisateur `forum_app` sont configurés localement. Le mot de passe de l’application est conservé dans la variable d’environnement Windows `DB_PASSWORD`, pas dans les fichiers du projet.

Dans un premier terminal, depuis la racine, configurer les comptes de démonstration et démarrer le backend. Choisir deux mots de passe forts et différents :

```powershell
$env:MODERATOR_USERNAME = 'moderator'
$env:STUDENT_USERNAME = 'student'
$moderatorPasswordPointer = $null
$studentPasswordPointer = $null
try {
    $secureModeratorPassword = Read-Host 'Mot de passe modérateur' -AsSecureString
    $moderatorPasswordPointer = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($secureModeratorPassword)
    $env:MODERATOR_PASSWORD = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($moderatorPasswordPointer)
    $secureStudentPassword = Read-Host 'Mot de passe étudiant' -AsSecureString
    $studentPasswordPointer = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($secureStudentPassword)
    $env:STUDENT_PASSWORD = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($studentPasswordPointer)
    mvn -f backend\pom.xml spring-boot:run
}
finally {
    if ($moderatorPasswordPointer) { [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($moderatorPasswordPointer) }
    if ($studentPasswordPointer) { [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($studentPasswordPointer) }
    Remove-Item Env:MODERATOR_PASSWORD,Env:STUDENT_PASSWORD -ErrorAction SilentlyContinue
}
```

Le compte étudiant de démonstration et le compte modérateur sont conservés en mémoire par le backend. Ils sont deux comptes de rôle configurables, pas un système d’inscription ni des comptes étudiants stockés en base. Le mot de passe est demandé masqué ; il n’est ni écrit dans le projet ni conservé par l’interface après déconnexion.

La page de connexion est <http://localhost:4200/login>. Après connexion, un étudiant est dirigé vers `/home` et un modérateur vers `/moderation`. Les routes vérifient le rôle dans Angular et le backend contrôle également chaque appel API : seul un étudiant connecté peut soumettre un commentaire, et seul un modérateur peut accéder aux outils et à la file de modération.

Dans un second terminal, démarrer Angular :

```powershell
Set-Location frontend
npm start
```

Ouvrir ensuite <http://localhost:4200>. L’API est disponible à <http://localhost:8080/api/posts>.

## Activer Hugging Face

Créer un jeton Hugging Face avec la permission **Inference Providers**, puis le saisir de façon masquée dans le terminal qui servira à démarrer Spring Boot. Ajouter la configuration du jeton à la commande de lancement précédente :

```powershell
$tokenPointer = $null
$moderatorPasswordPointer = $null
$studentPasswordPointer = $null
try {
    $secureToken = Read-Host 'Jeton Hugging Face' -AsSecureString
    $tokenPointer = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($secureToken)
    $env:HF_API_TOKEN = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($tokenPointer)
    $env:MODERATOR_USERNAME = 'moderator'
    $env:STUDENT_USERNAME = 'student'
    $secureModeratorPassword = Read-Host 'Mot de passe modérateur' -AsSecureString
    $moderatorPasswordPointer = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($secureModeratorPassword)
    $env:MODERATOR_PASSWORD = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($moderatorPasswordPointer)
    $secureStudentPassword = Read-Host 'Mot de passe étudiant' -AsSecureString
    $studentPasswordPointer = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($secureStudentPassword)
    $env:STUDENT_PASSWORD = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($studentPasswordPointer)
    mvn -f backend\pom.xml spring-boot:run
}
finally {
    if ($tokenPointer) { [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($tokenPointer) }
    if ($moderatorPasswordPointer) { [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($moderatorPasswordPointer) }
    if ($studentPasswordPointer) { [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($studentPasswordPointer) }
    Remove-Item Env:HF_API_TOKEN -ErrorAction SilentlyContinue
    Remove-Item Env:MODERATOR_PASSWORD,Env:STUDENT_PASSWORD -ErrorAction SilentlyContinue
}
```

Ne pas écrire le jeton dans le code, dans `application.properties`, ni dans un fichier suivi par Git. Si `HF_API_TOKEN` est absent, vide ou égal au jeton fictif d’exemple, aucun appel Hugging Face n’est effectué.

Le modèle `unitary/toxic-bert` fournit le score de l’étiquette `toxic` ; le score non-toxique est calculé comme `1 - scoreToxic`. Le modèle `facebook/bart-large-mnli` classe le commentaire dans `cours`, `examen`, `stage` ou `hors sujet`. Les URLs des modèles se configurent avec `HF_TOXICITY_URL` et `HF_SUBJECT_URL`.

Les seuils fournissent un avis à l’IA : `scoreToxic < 0.30` → avis `PUBLIER`, `0.30 <= scoreToxic < 0.70` → `A_VERIFIER`, et `scoreToxic >= 0.70` → `BLOQUER`. Ils ne publient ni ne rejettent automatiquement une soumission.

Toute création est analysée puis enregistrée avec le statut `PENDING` et n’apparaît pas dans le fil public. Les anciennes lignes qui n’ont pas encore de statut sont aussi traitées comme en attente jusqu’à leur validation. Un modérateur doit se connecter à la file de modération et choisir « Approuver et publier » ou « Rejeter ». Le résultat IA (score, sujet, suggestion) reste un avis ; la décision appartient au modérateur. La modification et suppression des publications existantes sont également réservées au modérateur.

La file protégée est disponible dans l’interface Angular. API modérateur : `GET /api/moderation/queue`, `GET /api/moderation/queue/statistics`, `POST /api/moderation/queue/{id}/publish` et `POST /api/moderation/queue/{id}/reject`. Les actions de publication et de rejet doivent envoyer un motif dans le corps, par exemple `{"reason":"Le commentaire respecte les règles du forum."}`. Le motif, l’identifiant du modérateur et la date de décision sont conservés et le motif d’approbation est visible sur le fil public.

La connexion crée une session serveur avec un cookie HTTP-only, restaurée via `GET /api/auth/me` après rechargement et détruite via `POST /api/auth/logout`. Le mot de passe n’est pas conservé par Angular. La session expire après 30 minutes d’inactivité. Les comptes sont des comptes de démonstration en mémoire, pas des comptes persistés. Cette configuration vise une démonstration locale ; avant tout déploiement public, utiliser HTTPS, une gestion de comptes appropriée et activer une protection CSRF.

## API de modération

| Méthode | URL | Corps |
| --- | --- | --- |
| `POST` | `/api/moderation/analyze` | `{"text":"Commentaire à analyser"}` |
| `POST` | `/api/moderation/batch` | `{"commentaires":["Premier commentaire","Deuxième commentaire"]}` |

Les réponses contiennent `commentaire`, `scoreToxic`, `scoreNonToxic`, `sujet`, `scoresSujets`, `decision`, `explication` et `erreur`. L’explication précise le sens du seuil pour l’avis rendu et rappelle que le modérateur conserve la décision finale. Un lot accepte jusqu’à 100 commentaires non vides.

Exemple de réponse :

```json
{
  "commentaire": "Je révise pour l’examen de mathématiques.",
  "scoreToxic": 0.02,
  "scoreNonToxic": 0.98,
  "sujet": "examen",
  "scoresSujets": {
    "cours": 0.08,
    "examen": 0.87,
    "stage": 0.03,
    "hors sujet": 0.02
  },
  "decision": "PUBLIER",
  "explication": "Le score est sous 30 % : l’avis de l’IA est favorable, sous réserve de la décision du modérateur.",
  "erreur": null
}
```

## API REST

| Méthode | URL | Action |
| --- | --- | --- |
| `POST` | `/api/posts` | Analyser et placer une soumission en attente |
| `GET` | `/api/posts` | Lister les publications approuvées |
| `GET` | `/api/posts/{id}` | Consulter une publication approuvée |
| `PUT` | `/api/posts/{id}` | Modifier et soumettre de nouveau (modérateur) |
| `DELETE` | `/api/posts/{id}` | Supprimer (modérateur) |

Le corps d’une création ou modification contient `title`, `content` et `author`. Pour les créations, le serveur remplace toujours `author` par l’identifiant authentifié de l’étudiant ; il renseigne également `id`, `createdAt` et le statut. Les appels de modération, de modification et de suppression nécessitent les identifiants du modérateur. `POST /api/auth/login` vérifie les identifiants et renvoie le rôle utilisé par Angular pour rediriger vers le bon espace.

## Tests

À la racine du projet :

```powershell
mvn -f backend\pom.xml test
```

Dans `frontend/` :

```powershell
npm test -- --watch=false
npm run build
```

Les tests unitaires et les appels HTTP simulés de modération n’ont pas besoin d’un jeton Hugging Face. Un test d’inférence réel nécessite un jeton valide.

## Remarque sur le modèle

Le tableau de bord présente les nombres de publications en attente, publiées et rejetées. Les décisions IA sont des avis explicables, pas des décisions automatiques. Le modèle sert à assister la modération, pas à remplacer le jugement humain. Son seuil peut produire des faux positifs ou manquer certains contenus ; le résultat dépend notamment du contexte et de la langue. La fiche de [`unitary/toxic-bert`](https://huggingface.co/unitary/toxic-bert) distingue le modèle multilingue de cette variante et indique que ce dernier ne couvre pas l’arabe. [`facebook/bart-large-mnli`](https://huggingface.co/facebook/bart-large-mnli) est entraîné sur MultiNLI et ne garantit pas la classification de textes français ou arabes. L’application n’a donc pas de support fiable de ces trois langues vérifié ; un jeton d’inférence n’étant pas configuré pour cette session, aucun essai réel de ces langues n’a été effectué. Avant de présenter la modération comme multilingue, tester un corpus représentatif annoté par langue et choisir, avec validation, des modèles réellement adaptés aux langues visées.
