<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib uri="jakarta.tags.core" prefix="c" %>
<%@ taglib uri="jakarta.tags.functions" prefix="fn" %>
<!DOCTYPE html>
<html lang="fr">
<head>
    <meta charset="UTF-8">
    <title>BiblioTech - Catalogue</title>
    <style>
        body  { font-family: Arial, sans-serif; margin: 2rem; color: #222; }
        table { border-collapse: collapse; width: 100%; margin-top: 1rem; }
        th, td { border: 1px solid #ccc; padding: .5rem .75rem; text-align: left; }
        th    { background: #f0f0f0; }
        .btn  { display: inline-block; padding: .4rem .8rem; background: #2563eb; color: #fff;
                text-decoration: none; border-radius: 4px; }
        form.inline { display: inline; }
    </style>
</head>
<body>

<h1>BiblioTech - Catalogue</h1>

<p><a class="btn" href="<c:url value='/livres/nouveau'/>">+ Ajouter un livre</a></p>

<c:choose>
    <c:when test="${empty livres}">
        <p>Aucun livre dans le catalogue.</p>
    </c:when>
    <c:otherwise>
        <p><c:out value="${fn:length(livres)}"/> livre(s) dans le catalogue.</p>

        <table>
            <thead>
            <tr>
                <th>Titre</th>
                <th>Auteur</th>
                <th>Année</th>
                <th>Disponibles</th>
                <th>Actions</th>
            </tr>
            </thead>
            <tbody>
            <c:forEach var="livre" items="${livres}">
                <tr>
                    <td><c:out value="${livre.titre()}"/></td>
                    <td><c:out value="${livre.auteur()}"/></td>
                    <td><c:out value="${livre.anneePublication()}"/></td>
                    <td><c:out value="${livre.exemplairesDisponibles()}"/> / <c:out value="${livre.exemplairesTotal()}"/></td>
                    <td>
                        <a href="<c:url value='/livres/${livre.id()}'/>">Voir</a> |
                        <a href="<c:url value='/livres/${livre.id()}/edit'/>">Modifier</a> |
                        <form class="inline" method="post"
                              action="<c:url value='/livres/${livre.id()}/delete'/>"
                              onsubmit="return confirm('Supprimer ce livre ?');">
                            <button type="submit">Supprimer</button>
                        </form>
                    </td>
                </tr>
            </c:forEach>
            </tbody>
        </table>
    </c:otherwise>
</c:choose>

</body>
</html>