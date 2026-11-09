# BiblioTech - TP01 Jakarta EE

## Stack
Java 21 · Jakarta EE 10 · Tomcat 10.1 · PostgreSQL 18 · Maven 3.9 · JSTL 3.0

## Lancer le projet
1. Créer la base `bibliotech` et le rôle `bibliotech_app` dans PostgreSQL.
2. Définir les variables d'environnement `DB_URL`, `DB_USER`, `DB_PASSWORD`
   (ex. `DB_URL=jdbc:postgresql://localhost:5432/bibliotech`).
3. `mvn clean package` puis déployer `target/bibliotech.war` sur Tomcat 10.1.
4. Tester : http://localhost:8081/bibliotech/health

## Réponses aux questions

### Q0.1 — Pourquoi `provided` ?
Tomcat contient déjà l'API Servlet. On en a besoin pour compiler, mais pas dans le WAR : `provided` dit justement à Maven de ne pas l'y mettre.
En `compile`, le jar serait copié dans le WAR. Les mêmes classes existeraient alors deux fois (dans Tomcat et dans l'application), ce qui alourdit le WAR et peut créer des conflits.

### Q0.2 — Pourquoi ne pas mettre les identifiants dans le code ?
Git garde tout l'historique : un mot de passe poussé une fois reste visible pour toujours, et n'importe qui peut le lire sur un dépôt public.
Deux autres solutions pour les sortir du code :
- une **DataSource JNDI** déclarée dans le `context.xml` de Tomcat ;
- un **fichier de configuration** (ex. `db.properties`) exclu de Git par le `.gitignore`.

### QA.1 — Pourquoi un Record est-il immuable ?
Ses champs sont `final` et il n'a pas de setters : une fois créé, on ne peut plus le modifier.
Dans Tomcat, une seule instance de Servlet sert plusieurs requêtes en même temps. Un objet qui ne peut pas changer peut être partagé entre ces threads sans risque, sans avoir besoin de synchronisation.

### QA.2 — Sealed interface ou interface classique ?
Une interface classique peut être implémentée par n'importe quelle classe. Une interface `sealed` donne la liste exacte des classes autorisées (`permits`), et le compilateur refuse les autres.
Avantage : dans un `switch`, le compilateur connaît tous les cas possibles. Il vérifie qu'on n'en oublie aucun, et on n'a plus besoin de `default`.

### QA.3 — Exhaustivité du switch et ajout de `Perdu`
Comme l'interface est `sealed`, le compilateur sait qu'un statut ne peut être que `EnCours`, `Rendu` ou `EnRetard`. Si les trois sont traités, le `switch` est complet : pas besoin de `default`.
Si on ajoutait `Perdu`, le `switch` ne serait plus complet et le code ne compilerait plus. Le compilateur nous obligerait à gérer ce nouveau cas, au lieu de laisser passer un bug silencieux.

## Choix techniques
- **`Rendu` a un champ `joursDeRetard` en plus** : le record du sujet ne stockait pas le nombre de jours, ce qui empêchait de calculer la pénalité de 0,50 €/jour.
- **Utilisateur PostgreSQL dédié `bibliotech_app`** : l'application n'utilise pas le super-utilisateur `postgres`.
- **Tomcat sur le port 8081** : le port 8080 était déjà pris par un autre service (`httpd.exe`).