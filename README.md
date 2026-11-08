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
### Q0.1
La portée provided signifie que la dépendance est nécessaire à la compilation, mais qu’elle sera fournie par l’environnement d’exécution : Maven ne l’inclut donc pas dans le WAR. C’est le cas de jakarta.jakartaee-web-api, car Tomcat 10.1 embarque déjà sa propre implémentation de l’API Servlet/JSP (tomcat/lib/servlet-api.jar, jsp-api.jar, etc.). Notre code a seulement besoin de ces interfaces pour compiler (HttpServlet, @WebServlet…).

Si on la passait en compile, Maven copierait le jar de l’API dans WEB-INF/lib. Les classes jakarta.servlet.* existeraient alors en double : celles du serveur et celles de l’application. Cela rend le WAR plus lourd inutilement et crée un risque de conflit de classloaders : une même classe chargée par deux classloaders différents est considérée comme deux classes distinctes, ce qui peut provoquer des ClassCastException ou des LinkageError, notamment si les versions diffèrent. Tomcat limite ce risque en refusant de charger lui-même les classes jakarta.servlet.* présentes dans l’application, mais embarquer une API déjà fournie par le serveur reste une mauvaise pratique.

### Q0.2

Un dépôt Git conserve tout l’historique : même si le mot de passe est supprimé dans un commit ultérieur, il reste lisible dans les anciens commits (git log -p) et dans tous les clones déjà faits. Sur un dépôt public, des robots parcourent GitHub en permanence pour récupérer automatiquement les secrets publiés. De plus, coder le mot de passe en dur lie le code à un environnement : il faudrait recompiler pour passer du poste de développement au serveur de production, et tous les développeurs partageraient le même secret. Enfin, changer un mot de passe compromis imposerait de modifier le code et de redéployer.

Deux moyens d’externaliser (hors variables d’environnement) :

1.	Une DataSource JNDI déclarée dans le serveur : les paramètres de connexion sont décrits dans le context.xml de Tomcat (balise <Resource>), en dehors du WAR. L’application la récupère par son nom (java:comp/env/jdbc/bibliotech). Ce mécanisme apporte aussi un pool de connexions.
	
2.	Un fichier de configuration externe non versionné (par exemple db.properties), placé hors du projet ou exclu par .gitignore, avec un fichier d’exemple db.properties.example sans secret dans le dépôt.
	
On peut aussi citer, en entreprise, un gestionnaire de secrets (HashiCorp Vault, AWS Secrets Manager, Azure Key Vault), qui fournit les identifiants à l’exécution, gère leur rotation et trace les accès.
