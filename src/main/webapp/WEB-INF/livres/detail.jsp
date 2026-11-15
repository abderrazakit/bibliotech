<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib uri="jakarta.tags.core" prefix="c" %>
<!DOCTYPE html>
<html lang="fr">
<head>
    <meta charset="UTF-8">
    <title>BiblioTech — <c:out value="${livre.titre()}"/></title>
    <style>
        body { font-family: Arial, sans-serif; margin: 2rem; color: #222; }
        dl   { display: grid; grid-template-columns: max-content auto; gap: .5rem 1.5rem; }
        dt   { font-weight: bold; }
        .ok  { color: #15803d; font-weight: bold; }
        .ko  { color: #b91c1c; font-weight: bold; }
        .btn { display: inline-block; padding: .4rem .8rem; background: #2563eb; color: #fff;
               text-decoration: none; border-radius: 4px; }
        form.inline { display: inline; }
    </style>
</head>
<body>

<p><a href="<c:url value='/livres'/>">← Retour au catalogue</a></p>

<h1><c:out value="${livre.titre()}"/></h1>

<dl>
    <dt>Auteur</dt>
    <dd><c:out value="${livre.auteur()}"/></dd>

    <dt>Année de publication</dt>
    <dd><c:out value="${livre.anneePublication()}"/></dd>

    <dt>Exemplaires</dt>
    <dd><c:out value="${livre.exemplairesDisponibles()}"/> disponible(s) sur <c:out value="${livre.exemplairesTotal()}"/></dd>

    <dt>Statut</dt>
    <dd>
        <c:choose>
            <c:when test="${livre.exemplairesDisponibles() > 0}">
                <span class="ok">Disponible à l'emprunt</span>
            </c:when>
            <c:otherwise>
                <span class="ko">Tous les exemplaires sont empruntés</span>
            </c:otherwise>
        </c:choose>
    </dd>
</dl>

<div>
    <a class="btn" href="<c:url value='/livres/${livre.id()}/edit'/>">Modifier</a>
    <form class="inline" method="post"
          action="<c:url value='/livres/${livre.id()}/delete'/>"
          onsubmit="return confirm('Supprimer ce livre ?');">
        <button type="submit">Supprimer</button>
    </form>
</div>

</body>
</html>