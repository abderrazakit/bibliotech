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

### Q0.1 : Pourquoi `provided` ?
Tomcat contient déjà l'API Servlet. On en a besoin pour compiler, mais pas dans le WAR : `provided` dit justement à Maven de ne pas l'y mettre.
En `compile`, le jar serait copié dans le WAR. Les mêmes classes existeraient alors deux fois (dans Tomcat et dans l'application), ce qui alourdit le WAR et peut créer des conflits.

### Q0.2 : Pourquoi ne pas mettre les identifiants dans le code ?
Git garde tout l'historique : un mot de passe poussé une fois reste visible pour toujours, et n'importe qui peut le lire sur un dépôt public.
Deux autres solutions pour les sortir du code :
- une **DataSource JNDI** déclarée dans le `context.xml` de Tomcat ;
- un **fichier de configuration** (ex. `db.properties`) exclu de Git par le `.gitignore`.

### QA.1 : Pourquoi un Record est-il immuable ?
Ses champs sont `final` et il n'a pas de setters : une fois créé, on ne peut plus le modifier.
Dans Tomcat, une seule instance de Servlet sert plusieurs requêtes en même temps. Un objet qui ne peut pas changer peut être partagé entre ces threads sans risque, sans avoir besoin de synchronisation.

### QA.2 : Sealed interface ou interface classique ?
Une interface classique peut être implémentée par n'importe quelle classe. Une interface `sealed` donne la liste exacte des classes autorisées (`permits`), et le compilateur refuse les autres.
Avantage : dans un `switch`, le compilateur connaît tous les cas possibles. Il vérifie qu'on n'en oublie aucun, et on n'a plus besoin de `default`.

### QA.3 : Exhaustivité du switch et ajout de `Perdu`
Comme l'interface est `sealed`, le compilateur sait qu'un statut ne peut être que `EnCours`, `Rendu` ou `EnRetard`. Si les trois sont traités, le `switch` est complet : pas besoin de `default`.
Si on ajoutait `Perdu`, le `switch` ne serait plus complet et le code ne compilerait plus. Le compilateur nous obligerait à gérer ce nouveau cas, au lieu de laisser passer un bug silencieux.

## Choix techniques
- **`Rendu` a un champ `joursDeRetard` en plus** : le record du sujet ne stockait pas le nombre de jours, ce qui empêchait de calculer la pénalité de 0,50 €/jour.
- **Utilisateur PostgreSQL dédié `bibliotech_app`** : l'application n'utilise pas le super-utilisateur `postgres`.
- **Tomcat sur le port 8081** : le port 8080 était déjà pris par un autre service (`httpd.exe`).

### QB.1 : Pourquoi un formulaire HTML ne fait que GET et POST ?
La norme HTML n'autorise que `get` et `post` dans l'attribut `method` d'un formulaire. Les formulaires ont été conçus pour deux usages : **lire** une page (GET) et **envoyer** des données (POST).
PUT et DELETE ont été envisagés pour HTML5, puis abandonnés : on ne savait pas bien comment le navigateur devait gérer leurs réponses, et ils auraient permis à n'importe quel site d'envoyer ces requêtes vers un autre site.
On contourne la limite avec un POST vers une URL dédiée (ex. `POST /livres/3/delete`), ou avec `fetch()` en JavaScript.

### QB.2 : `${livre.titre}` ou `<c:out value="${livre.titre}"/>` ?
`${livre.titre}` écrit la valeur **telle quelle** dans le HTML. `<c:out>` **échappe** les caractères spéciaux (`<` devient `&lt;`, `>` devient `&gt;`, `"` devient `&#034;`…).
Exemple avec le titre `<script>alert('XSS')</script>` :
- avec `${livre.titre}`, le navigateur reçoit une vraie balise `<script>` et **exécute** le code (faille XSS) ;
- avec `<c:out>`, il reçoit `&lt;script&gt;…` et **affiche** simplement le texte, sans l'exécuter.

### QB.3 : Que se passe-t-il sans PRG ?
L'utilisateur ajoute un livre : le serveur fait l'INSERT puis un forward vers la liste, et l'URL reste celle du POST.
S'il appuie sur F5 (ou revient en arrière puis en avant), le navigateur propose de renvoyer le formulaire.
S'il accepte, le même POST repart, un second INSERT est fait, et le livre apparaît **en double** dans le catalogue.

### QC.1 : Pourquoi créer `GenericDAO<T, ID>` dès le premier DAO ?
1. **On peut changer l'implémentation sans toucher au reste.** La servlet ne connaît que l'interface : on est passé de `LivreDAOMemoire` (partie B) à `LivreDAO` (JDBC) en modifiant une seule ligne.
2. **Tous les DAO ont la même forme.** `LivreDAO` et `EmpruntDAO` offrent les mêmes méthodes (`findAll`, `findById`, `save`, `update`, `delete`) : le code est plus lisible, et on écrit une seule fois un code qui marche pour tous les DAO.
Bonus : on peut tester une servlet avec un faux DAO en mémoire, sans base de données.

### QC.2 : Pourquoi `PreparedStatement` plutôt que `Statement` ?
1. **Sécurité : il bloque l'injection SQL.** Les valeurs sont envoyées à part, avec des `?`. Elles ne sont jamais lues comme du code SQL.
   Avec `Statement` et une concaténation, un titre comme `' OR '1'='1` ou `'; DROP TABLE livre; --` changerait le sens de la requête.
2. **Performance : la requête est préparée une seule fois.** La base peut réutiliser son plan d'exécution quand on relance la même requête avec d'autres valeurs.
En plus, le driver gère lui-même les types et les caractères spéciaux (dates, apostrophes comme dans « L'Étranger »…).

### QC.3 : Que se passe-t-il sans `setAutoCommit(false)` ?
Sans cette ligne, JDBC valide chaque requête **dès qu'elle est exécutée**.
Si une erreur arrive entre l'UPDATE et l'INSERT : l'UPDATE est déjà enregistré (le livre a **un exemplaire disponible en moins**), mais l'INSERT n'a pas eu lieu (**aucun emprunt** n'est créé).
La base est alors **incohérente** : un exemplaire a « disparu » sans que personne ne l'ait emprunté, et le `rollback()` ne peut rien annuler, puisque tout est déjà validé.