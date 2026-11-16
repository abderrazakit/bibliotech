# BiblioTech : TP 02, passage à JPA / Hibernate

Dans ce TP, j'ai repris l'application **BiblioTech** du TP 01 (catalogue de livres, emprunts, retours, retards et pénalités) et j'ai remplacé tout l'accès à la base en **JDBC** par **JPA + Hibernate**.

Pour l'utilisateur, rien ne change : mêmes pages, mêmes adresses, même fonctionnement.
Dans le code, en revanche, il n'y a plus une seule ligne de SQL écrite à la main, plus de `ResultSet` et plus de gestion de connexion : Hibernate s'en occupe.

> La version JDBC du TP 01 n'a pas été modifiée : elle est sur la branche `main` et le tag [`tp1-jdbc`](../../tree/tp1-jdbc).
> Ce TP 02 se trouve sur la branche **`tp2-jpa`**.

---

## Sommaire

1. [Outils utilisés](#1-outils-utilisés)
2. [Lancer le projet](#2-lancer-le-projet)
3. [Comment le code est organisé](#3-comment-le-code-est-organisé)
4. [Mes choix et les écarts avec le sujet](#4-mes-choix-et-les-écarts-avec-le-sujet)
5. [Réponses aux questions](#5-réponses-aux-questions)
6. [Exercice 7 : les pièges de JPA](#6-exercice-7--les-pièges-de-jpa)
7. [Preuve par les logs : un emprunt = une transaction](#7-preuve-par-les-logs--un-emprunt--une-transaction)
8. [Les tests](#8-les-tests)
9. [Ce qui m'a posé problème](#9-ce-qui-ma-posé-problème)

---

## 1. Outils utilisés

Outils 
- Java => 21 pour la compilation (JDK 25 installé sur ma machine)
- Jakarta EE => 10 (Servlet 6.0, JSP, JSTL 3.0)
- JPA / Hibernate =>ernate Validator 8.0.1 (Bean Validation)
- Serveur => Tomcat 10.1.60, sur le port **8081**
- Base de données => PostgreSQL 18.6
- Build => Maven 3.9
- Tests => JUnit 5, Mockito, AssertJ, H2 (base en mémoire)

---

## 2. Lancer le projet

### Étape 1 : la base de données
Elle est **identique au TP 01**. Si elle existe déjà, il n'y a rien à faire.

```sql
-- Connecté en tant que postgres :
CREATE ROLE bibliotech_app WITH LOGIN PASSWORD '<mot_de_passe>';
CREATE DATABASE bibliotech OWNER bibliotech_app;
```

Ensuite, sur la base `bibliotech`, exécuter `SET ROLE bibliotech_app;` puis le script `sql/schema.sql`.

Hibernate ne crée et ne modifie **aucune** table. Au démarrage, il vérifie seulement que mes classes Java correspondent bien aux tables existantes (`hbm2ddl.auto=validate`).

### Étape 2 : les variables d'environnement
Le mot de passe n'est jamais écrit dans le code. Il faut définir ces trois variables dans la configuration de lancement de Tomcat :

```
DB_URL=jdbc:postgresql://localhost:5432/bibliotech
DB_USER=bibliotech_app
DB_PASSWORD=<mot_de_passe>
```

Les tests, eux, n'en ont pas besoin : ils utilisent une base H2 en mémoire.

### Étape 3 : compiler et tester
```
mvn clean package
```
Cette commande compile le projet, lance les **42 tests** et crée le fichier `target/bibliotech.war`.
Les tests fonctionnent même si PostgreSQL est arrêté.

### Étape 4 : déployer et ouvrir
Déployer `target/bibliotech.war` sur Tomcat 10.1, puis ouvrir :

Pages

- Catalogue => http://localhost:8081/bibliotech/livres 
- Emprunts en cours => http://localhost:8081/bibliotech/emprunts 
- État du service => http://localhost:8081/bibliotech/health 
- Pages de diagnostic (exercice 7) => http://localhost:8081/bibliotech/diagnostic/lazy?id=2 

Les autres pages de diagnostic : `/diagnostic/join-fetch`, `/osiv`, `/n-plus-1`, `/n-plus-1-corrige` et `/json`.

> **Avec Eclipse** : ne pas lancer `mvn clean` pendant que Tomcat tourne. Eclipse recopie alors un dossier `target` à moitié vide et l'application ne démarre plus. Il faut d'abord arrêter Tomcat, lancer le build, puis faire « Clean » sur le serveur et le redémarrer.

---

## 3. Comment le code est organisé

### Le principe : chaque couche a un seul rôle

```
  Navigateur
      │   requêtes HTTP (GET / POST)
      ▼
┌─────────────┐
│    web/     │  Les servlets : elles lisent la requête, appellent un service
│             │  et choisissent la page à afficher. Elles ne touchent jamais à la base.
└─────────────┘
      ▼
┌─────────────┐
│  service/   │  Les règles métier ET les transactions.
│             │  C'est la seule couche qui ouvre, valide ou annule une transaction.
└─────────────┘
      ▼
┌─────────────┐
│ repository/ │  Les requêtes JPQL, et rien d'autre (aucune règle métier).
└─────────────┘
      ▼
┌─────────────┐
│ persistence/│  La « plomberie » JPA : JpaUtil (la fabrique d'EntityManager)
│             │  et Tx (début, validation ou annulation d'une transaction).
└─────────────┘
      ▼
  PostgreSQL       (schéma du TP 01, inchangé)

  model/  →  les entités Livre, Etudiant, Emprunt + le modèle métier du TP 01.
             Toutes les couches s'en servent.
```

### Les fichiers du projet

```
bibliotech/
├── pom.xml
├── sql/schema.sql                       → tables et données de démo (inchangé depuis le TP 01)
├── docs/captures-tp2/                   → captures d'écran du TP 02
└── src/
    ├── main/java/com/bibliotech/
    │   ├── model/        → Livre, Etudiant, Emprunt (entités JPA)
    │   │                   StatutEmprunt, PenaliteCalculator, LivreAvecEmprunts
    │   │                   ValidationException, LivreIndisponibleException, Validations
    │   ├── persistence/  → JpaUtil, Tx, JpaLifecycleListener
    │   ├── repository/   → JpaRepository, LivreRepository, EtudiantRepository, EmpruntRepository
    │   ├── service/      → LivreService, EmpruntService, EtudiantService, SanteService, Validateur
    │   └── web/          → LivreServlet, EmpruntServlet, HealthServlet, DiagnosticServlet, LigneEmprunt
    ├── main/resources/META-INF/persistence.xml   → configuration JPA de l'application (PostgreSQL)
    ├── main/webapp/WEB-INF/                      → web.xml et les pages JSP
    ├── test/resources/META-INF/persistence.xml   → configuration JPA des tests (H2)
    └── test/java/com/bibliotech/                 → les 42 tests
```

---

## 4. Mes choix et les écarts avec le sujet

**Ce que j'ai gardé du TP 01**
- Le statut « riche » d'un emprunt (`EnCours`, `EnRetard`, `Rendu`) et le calcul des pénalités. En base, on ne stocke qu'un simple `enum`, et `Emprunt.statutMetier()` recalcule le statut détaillé à partir des dates.
- L'affichage des erreurs sous chaque champ du formulaire. Les règles sont maintenant écrites en annotations (`@NotBlank`, `@Pattern`...).

**Deux petites améliorations**
- `JpaUtil` crée la connexion à la base au premier besoin. Si PostgreSQL est arrêté, `/health` répond KO, puis OK dès que la base revient, sans redémarrer Tomcat.
- `JpaLifecycleListener` ferme proprement les connexions quand l'application s'arrête.

---

## 5. Réponses aux questions

### Q1.1 : Pourquoi une seule `EntityManagerFactory`, mais un `EntityManager` par requête ?
L'`EntityManagerFactory` coûte cher à créer : elle lit la configuration, analyse toutes les entités, vérifie les tables et prépare les connexions. On la crée **une seule fois** pour toute l'application, et tout le monde la partage sans risque.

L'`EntityManager`, lui, est léger, mais il ne doit **pas être partagé** : il garde en mémoire tous les objets qu'il a chargés. On en crée un pour chaque requête, puis on le ferme. Si plusieurs utilisateurs partageaient le même, leurs données se mélangeraient et sa mémoire grossirait sans fin.

### Q2.1 : Pourquoi un simple `enum` à la place de la sealed interface ?
JPA sait enregistrer des entités ou des valeurs simples (texte, nombre, date, enum), mais **pas une famille de records**. Il a besoin de pouvoir créer l'objet vide puis remplir ses champs, ce qu'un record ne permet pas.

En plus, `EnRetard` contient un nombre de jours de retard qui **change tous les jours** : ça n'aurait aucun sens de le stocker.

J'enregistre donc un simple `enum` sous forme de texte (`EN_COURS`, `RENDU`), et la méthode `statutMetier()` recalcule le statut détaillé à la lecture. J'ai ainsi gardé le pattern matching et le calcul des pénalités du TP 01.

### Q2.2 : Pourquoi tous les `@ManyToOne` en `LAZY` ?
Par défaut, un `@ManyToOne` est en `EAGER` : chaque fois qu'on charge un emprunt, Hibernate charge **aussi** son livre et son étudiant, même si on n'en a pas besoin. Pour 100 emprunts, ça peut faire des dizaines de requêtes en plus, sans qu'on puisse l'empêcher.

En `LAZY`, Hibernate ne charge le livre et l'étudiant **que si on s'en sert**. C'est nous qui décidons, requête par requête, ce qu'il faut charger (avec `JOIN FETCH`).

### Q3.1 : Pourquoi `findAll()` n'utilise pas de transaction, alors que `save()` et `deleteById()` si ?
`findAll()` ne fait que **lire** : il n'y a rien à valider ni à annuler, et JPA autorise les lectures sans transaction.
`save()` et `deleteById()` **modifient** la base, et JPA n'enregistre rien sans transaction.

C'est donc correct pour une lecture simple, avec deux limites :
- si on enchaîne plusieurs lectures, elles ne voient pas forcément la base dans le même état ;
- une fois l'`EntityManager` fermé, on ne peut plus charger les relations `LAZY`.

Quand plusieurs lectures doivent être cohérentes entre elles, on les met dans une même transaction.

### Q4.1 : Pourquoi ne pas faire `livreRepository.save()` puis `empruntRepository.save()` ?
Parce que chaque `save()` a **sa propre** transaction. Voici ce qui peut arriver :
1. le premier `save()` baisse le stock du livre, et c'est enregistré ;
2. le second `save()` échoue (étudiant inconnu, coupure de la base, arrêt du serveur...) ;
3. résultat : le stock a baissé, mais **aucun emprunt n'existe**. Un exemplaire a disparu pour toujours.

Les deux modifications doivent être dans **la même transaction** : soit tout est enregistré, soit rien. C'est le service qui connaît cette règle, c'est donc lui qui ouvre la transaction.

### Q4.2 : Que se passe-t-il si `LivreIndisponibleException` est levée après la baisse du stock ?
`Tx.inTransaction` attrape l'erreur, **annule toute la transaction** (rollback), puis relance l'erreur. Rien n'est enregistré : ni la baisse du stock, ni l'emprunt. Le test `TxTest` le prouve.

Avec JPA, la baisse du stock ne modifie d'abord que l'objet Java en mémoire. La base n'est mise à jour qu'à la fin, et l'annulation efface tout.

Au TP 01 en JDBC, c'était à moi d'écrire le `rollback()` dans chaque méthode. Un oubli, et le stock restait faux. Ici, ce code est écrit **une seule fois**, dans `Tx`.

### Q4.3 : Pourquoi ne pas mettre `@Transactional` sur une servlet ?
D'abord, sur Tomcat seul, cette annotation **ne ferait rien** : aucun composant n'est là pour la lire.

Ensuite, même sur un serveur plus complet, ce serait une mauvaise idée :
- la servlet aurait **deux rôles** (gérer le web et gérer la base), ce qui va contre le principe de responsabilité unique (le « S » de SOLID) ;
- la couche web **dépendrait** de la base de données, alors que ce devrait être l'inverse (le « D » de SOLID) ;
- pour ajouter une API ou un traitement automatique, il faudrait **recopier** toute la gestion des transactions (le « O » de SOLID) ;
- la transaction resterait ouverte pendant tout l'affichage de la page, et bloquerait la base trop longtemps.

### Q5.1 : Si on passe à WildFly avec `@Transactional`, combien de fichiers faut-il modifier ?
Très peu, et ils sont tous regroupés :
- les **services** : on remplace `Tx.inTransaction(...)` par `@Transactional` ;
- le package **persistence** : `JpaUtil` et `Tx` disparaissent, et `persistence.xml` est adapté au serveur ;
- éventuellement les **repositories**, si le serveur leur fournit directement l'`EntityManager`.

Les servlets, les pages JSP et les entités ne changent pas. C'est tout l'intérêt de bien séparer les couches.

### Q7.1 : Pourquoi « Open Session In View » est une mauvaise pratique sur une grosse application ?
Cette technique garde la connexion à la base ouverte pendant **tout** le traitement d'une requête, affichage compris. Ça pose quatre problèmes :
- **les connexions sont bloquées** : avec beaucoup d'utilisateurs, il n'y en a plus assez, même si la base ne travaille presque pas ;
- **des requêtes cachées** : une simple boucle dans une page JSP peut lancer une requête par ligne, sans que personne ne le voie ;
- **les couches se mélangent** : la page d'affichage finit par piloter la base de données ;
- **on ne sait plus** quelles lectures sont faites dans quelle transaction.

C'est pratique sur un petit projet. Sur un gros, on préfère charger dans le service **exactement** ce dont la page a besoin.

---

## 6. Exercice 7 : les pièges de JPA

Pour chaque piège, j'ai d'abord **provoqué l'erreur**, puis je l'ai **expliquée** et **corrigée**.

### 6.1 L'erreur `LazyInitializationException`

** Ce qui se passe** (page `/diagnostic/lazy?id=2`)
```
org.hibernate.LazyInitializationException: failed to lazily initialize a collection of role:
com.bibliotech.model.Livre.emprunts: could not initialize proxy - no Session
```

** Pourquoi**
Les emprunts d'un livre sont en `LAZY` : Hibernate ne les charge pas tout de suite. Il les chargera plus tard, si on en a besoin, mais il lui faut pour ça la connexion à la base. Or cette connexion a déjà été fermée. Quand on demande les emprunts, Hibernate ne peut plus aller les chercher.

** Correction 1 : `JOIN FETCH`** (page `/diagnostic/join-fetch?id=2`)
On demande le livre **et** ses emprunts dans la même requête :
`SELECT l FROM Livre l LEFT JOIN FETCH l.emprunts WHERE l.id = :id`
Tout arrive en **1 seule requête**. C'est la solution que j'ai retenue.

** Correction 2 : Open Session In View** (page `/diagnostic/osiv?id=2`)
On garde la connexion ouverte jusqu'à la fin. Ça marche, mais il faut **2 requêtes**, et c'est une mauvaise pratique à grande échelle (voir Q7.1).

### 6.2 Le problème « n+1 »

** Ce qui se passe** (page `/diagnostic/n-plus-1`)
Pour afficher le nombre d'emprunts en cours de chaque livre, Hibernate fait **1 requête** pour la liste des livres, puis **1 requête par livre**. Avec 5 livres, ça fait 6 requêtes. Avec 1000 livres, ça en ferait 1001.

```
(coller ici le log de /diagnostic/n-plus-1 : 1 select sur livre, puis 5 select sur emprunt)
```

** La correction** (page `/diagnostic/n-plus-1-corrige`, et la page `/livres`)
Une seule requête, où c'est la base qui compte les emprunts :
```sql
SELECT new com.bibliotech.model.LivreAvecEmprunts(l, COUNT(e))
  FROM Livre l
  LEFT JOIN l.emprunts e ON e.dateRetourEffective IS NULL
 GROUP BY l.id, l.titre, l.auteur, l.anneePublication, l.exemplairesTotal, l.exemplairesDisponibles
 ORDER BY l.titre
```
```
(coller ici l'unique requête SQL de /diagnostic/n-plus-1-corrige)
```
On obtient **le même résultat, avec 1 requête au lieu de 6**.

** Pourquoi `GROUP BY` et pas `JOIN FETCH` ?**

| | `JOIN FETCH` | `GROUP BY` + `COUNT` (mon choix) |
|---|---|---|
| Ce qui revient de la base | tous les emprunts de tous les livres | une ligne par livre, avec le nombre |
| Qui compte | Java, en mémoire | la base de données |
| Quand l'historique grandit | de plus en plus lourd | toujours aussi léger |
| Limite | aucune | on obtient un nombre, pas la liste des emprunts |

Ici, la page affiche seulement **un nombre** : inutile de charger tous les emprunts pour les compter.

![Catalogue avec les emprunts en cours](docs/captures-tp2/catalogue-emprunts-en-cours.png)

### 6.3 La boucle infinie en JSON

** Ce qui se passe** (page `/diagnostic/json?id=2`, avant correction)
```
JsonMappingException: Document nesting depth (1001) exceeds the maximum allowed (1000)
(through reference chain: Livre["emprunts"] -> Emprunt["livre"] -> Livre["emprunts"] -> ...)
```

** Pourquoi**
Un livre connaît ses emprunts, et chaque emprunt connaît son livre. Pour écrire le JSON, Jackson écrit le livre, puis ses emprunts, puis le livre de chaque emprunt, puis à nouveau ses emprunts... **sans fin**. Jackson s'arrête à 1000 niveaux ; les anciennes versions plantaient carrément (`StackOverflowError`).

** Les deux solutions proposées par le sujet**

| | `@JsonIgnore` sur les emprunts (mon choix) | `@JsonManagedReference` / `@JsonBackReference` |
|---|---|---|
| Principe | on n'écrit jamais la liste des emprunts | on écrit les emprunts, mais pas le retour vers le livre |
| Avantages | une seule annotation ; casse tous les cycles ; évite aussi la `LazyInitializationException` | le JSON du livre contient ses emprunts |
| Inconvénients | les emprunts n'apparaissent pas dans le JSON | deux annotations par relation ; l'erreur LAZY reste possible |

Après correction, la page affiche un JSON propre, sans boucle.

 Dans un vrai projet, on n'envoie pas directement les entités en JSON : on utilise des petites classes faites pour ça (des **DTO**, comme `LivreAvecEmprunts`). Le problème ne peut alors plus se produire.

---

## 7. Preuve par les logs : un emprunt = une transaction

Voici ce qu'affiche Hibernate (`show_sql=true`) quand on clique sur **« Emprunter »** :

```
Hibernate:
    select l1_0.id, l1_0.annee_publication, l1_0.auteur, l1_0.exemplaires_disponibles,
           l1_0.exemplaires_total, l1_0.titre
    from livre l1_0
    where l1_0.id=? for no key update
Hibernate:
    select e1_0.id, e1_0.email, e1_0.nom, e1_0.numero_etudiant, e1_0.prenom
    from etudiant e1_0
    where e1_0.id=?
Hibernate:
    insert into emprunt
        (date_emprunt, date_retour_effective, date_retour_prevue, etudiant_id, livre_id, statut)
    values (?, ?, ?, ?, ?, ?)
    returning id
Hibernate:
    update livre
    set annee_publication=?, auteur=?, exemplaires_disponibles=?, exemplaires_total=?, titre=?
    where id=?
```

**Comment lire ce log**

| Requête | Ce qu'elle fait |
|---|---|
| `select ... from livre ... for no key update` | lit le livre **et le verrouille** : un autre emprunt du même livre doit attendre |
| `select ... from etudiant` | vérifie que l'étudiant existe |
| `insert into emprunt` | crée l'emprunt |
| `update livre` | enlève un exemplaire du stock |

À retenir :
-  **Le verrou** (`for no key update`) corrige le bug du dernier exemplaire du TP 01, sans écrire de SQL. Le test `RaceConditionH2Test` le confirme : sur deux emprunts simultanés, un seul réussit et le stock reste à 0.
-  **L'`INSERT` passe avant l'`UPDATE`** : Hibernate doit créer l'emprunt tout de suite pour connaître son numéro. L'ordre n'a pas d'importance, puisque tout est validé ensemble.
-  **Je n'ai jamais appelé `save()` sur le livre** : Hibernate a vu tout seul que le stock avait changé.
-  **Les deux modifications sont dans la même transaction** : soit tout est enregistré, soit rien.

Après l'emprunt, la page `/emprunts` ne fait que **3 requêtes**, quel que soit le nombre d'emprunts affichés.

---

## 8. Les tests

**42 tests**, tous au vert avec `mvn clean package`.

| Classe | Nombre | Type | Ce qu'elle vérifie |
|---|---|---|---|
| `PenaliteCalculatorTest` | 13 | unitaire | le calcul des pénalités (repris du TP 01) |
| `ValidationTest` | 12 | unitaire | les règles de validation des livres, étudiants et emprunts |
| `LivreServiceTest` | 5 | unitaire (Mockito) | les règles métier sur les livres, sans base de données |
| `EmpruntServiceH2Test` | 6 | intégration (H2) | emprunt, retour, double retour, refus, stock correct |
| `TxTest` | 1 | intégration (H2) | une erreur annule bien toutes les modifications |
| `RaceConditionH2Test` | 1 | intégration (H2) | deux emprunts en même temps du dernier exemplaire |
| `EmpruntRepositoryH2Test` | 3 | intégration (H2) | les requêtes sur les emprunts et la recherche |
| `LivreRepositoryH2Test` | 1 | intégration (H2) | la requête unique qui corrige le n+1 |

 Les tests d'intégration utilisent **H2**, une base qui vit en mémoire et repart vide à chaque test. Ils ne dépendent donc ni de PostgreSQL, ni des données laissées par un autre test. C'est ce qui manquait au TP 01, où les tests cassaient dès que j'avais utilisé l'application.

---

## 9. Ce qui m'a posé problème

| Problème | Ce que j'ai appris |
|---|---|
| Les tests du TP 01 cassaient après avoir utilisé l'application | Des tests qui lisent la vraie base dépendent de son contenu. Solution : une base de test H2, vide à chaque test. |
| L'API JPA en `provided` dans le sujet | Tomcat ne fournit pas JPA : il faut l'embarquer dans le WAR. |
| `${env.DB_URL}` ignoré par Hibernate | Les identifiants sont passés en Java par `JpaUtil`. |
| Erreur « No Persistence provider » | La configuration H2 des tests avait été collée à la place de celle de l'application. Chaque `persistence.xml` doit rester dans son dossier (`main` ou `test`). |
| `ClassNotFoundException` sur Byte Buddy | En déclarant cette bibliothèque « pour les tests seulement », je l'ai retirée du WAR alors qu'Hibernate en a besoin. Il faut vérifier le contenu de `WEB-INF/lib`, pas seulement que le build passe. |
| Mockito ne marchait pas avec le JDK 25 | Il a fallu imposer une version plus récente de Byte Buddy. |
| Hibernate cherchait une bibliothèque JSON pendant les tests | Ajout de Yasson, uniquement pour les tests. |
| L'application ne démarrait plus après un `mvn clean` | Eclipse avait recopié un dossier incomplet. Toujours arrêter Tomcat avant de lancer Maven. |
| Avertissement « built-in connection pool » | Le petit gestionnaire de connexions d'Hibernate suffit pour un TP. En production, on utiliserait HikariCP. |