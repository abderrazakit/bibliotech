<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib uri="jakarta.tags.core" prefix="c" %>
<%@ taglib uri="jakarta.tags.functions" prefix="fn" %>
<c:set var="pageActive" value="catalogue"/>
<!DOCTYPE html>
<html lang="fr">
<head>
    <title>Catalogue — BiblioTech</title>
    <%@ include file="/WEB-INF/jspf/tete.jspf" %>
</head>
<body>
<%@ include file="/WEB-INF/jspf/entete.jspf" %>

<main class="conteneur">

    <section class="accueil">
        <div>
            <h1>Le catalogue de la bibliothèque</h1>
            <p>Consultez les livres et leurs exemplaires disponibles, ajoutez-en de nouveaux ou mettez à jour le fonds.</p>

            <form class="recherche" method="get" action="<c:url value='/livres'/>" role="search">
                <label for="q" class="sr">Rechercher un titre</label>
                <input type="text" id="q" name="q" placeholder="Rechercher un titre"
                       value="<c:out value='${param.q}'/>">
                <button type="submit" class="btn btn-discret">Rechercher</button>
            </form>

            <p class="appel"><a class="btn" href="<c:url value='/livres/nouveau'/>">Ajouter un livre</a></p>
        </div>
        <div class="etagere" role="img" aria-label="Étagère de livres colorés"></div>
    </section>

    <c:if test="${not empty param.q}">
        <%-- Bonus 2 : valeur échappée par <c:out> --%>
        <p class="resultats">
            Résultats pour « <c:out value="${param.q}"/> ».
            <a href="<c:url value='/livres'/>">Effacer la recherche</a>
        </p>
    </c:if>

    <c:choose>
        <c:when test="${empty livres}">
            <div class="table-cadre vide-etat">
                <p><c:out value="${empty param.q ? 'Le catalogue est vide. Ajoutez un premier livre pour commencer.' : 'Aucun titre ne correspond à cette recherche.'}"/></p>
            </div>
        </c:when>
        <c:otherwise>
            <p class="compteur"><c:out value="${fn:length(livres)}"/> livre(s)</p>
            <div class="table-cadre">
                <table>
                    <thead>
                    <tr>
                        <th scope="col">Titre</th>
                        <th scope="col">Auteur</th>
                        <th scope="col">Année</th>
                        <th scope="col">Disponibles</th>
                        <th scope="col">Actions</th>
                    </tr>
                    </thead>
                    <tbody>
                    <c:forEach var="livre" items="${livres}">
                        <tr>
                            <td class="titre"><c:out value="${livre.titre()}"/></td>
                            <td><c:out value="${livre.auteur()}"/></td>
                            <td><c:out value="${livre.anneePublication()}"/></td>
                            <td>
                                <span class="pastille ${livre.exemplairesDisponibles() > 0 ? 'ok' : 'vide'}">
                                    <c:out value="${livre.exemplairesDisponibles()}"/> sur <c:out value="${livre.exemplairesTotal()}"/>
                                </span>
                            </td>
                            <td class="actions">
                                <a href="<c:url value='/livres/${livre.id()}'/>">Voir</a>
                                <a href="<c:url value='/livres/${livre.id()}/edit'/>">Modifier</a>
                                <form class="inline" method="post"
                                      action="<c:url value='/livres/${livre.id()}/delete'/>"
                                      onsubmit="return confirm('Supprimer ce livre ?');">
                                    <button type="submit" class="lien-danger">Supprimer</button>
                                </form>
                            </td>
                        </tr>
                    </c:forEach>
                    </tbody>
                </table>
            </div>
        </c:otherwise>
    </c:choose>

</main>
</body>
</html>