<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib uri="jakarta.tags.core" prefix="c" %>
<c:set var="pageActive" value="catalogue"/>
<!DOCTYPE html>
<html lang="fr">
<head>
    <title><c:out value="${livre.titre}"/> — BiblioTech</title>
    <%@ include file="/WEB-INF/jspf/tete.jspf" %>
</head>
<body>
<%@ include file="/WEB-INF/jspf/entete.jspf" %>

<main class="conteneur">
    <a class="retour" href="<c:url value='/livres'/>">Retour au catalogue</a>

    <article class="fiche">
        <h1><c:out value="${livre.titre}"/></h1>

        <dl>
            <dt>Auteur</dt>
            <dd><c:out value="${livre.auteur}"/></dd>

            <dt>Année de publication</dt>
            <dd><c:out value="${livre.anneePublication}"/></dd>

            <dt>Exemplaires</dt>
            <dd><c:out value="${livre.exemplairesDisponibles}"/> disponible(s) sur <c:out value="${livre.exemplairesTotal}"/></dd>

            <dt>Statut</dt>
            <dd>
                <c:choose>
                    <c:when test="${livre.exemplairesDisponibles > 0}">
                        <span class="pastille ok">Disponible à l'emprunt</span>
                    </c:when>
                    <c:otherwise>
                        <span class="pastille vide">Tous les exemplaires sont empruntés</span>
                    </c:otherwise>
                </c:choose>
            </dd>
        </dl>

        <div class="barre-actions">
            <a class="btn" href="<c:url value='/livres/${livre.id}/edit'/>">Modifier</a>
            <form method="post" action="<c:url value='/livres/${livre.id}/delete'/>"
                  onsubmit="return confirm('Supprimer ce livre ?');">
                <button type="submit" class="lien-danger">Supprimer ce livre</button>
            </form>
        </div>
    </article>
</main>
</body>
</html>