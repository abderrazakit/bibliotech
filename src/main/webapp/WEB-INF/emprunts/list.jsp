<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib uri="jakarta.tags.core" prefix="c" %>
<%@ taglib uri="jakarta.tags.functions" prefix="fn" %>
<!DOCTYPE html>
<html lang="fr">
<head>
    <meta charset="UTF-8">
    <title>BiblioTech — Emprunts en cours</title>
    <style>
        body   { font-family: Arial, sans-serif; margin: 2rem; color: #222; }
        nav a  { margin-right: 1rem; }
        table  { border-collapse: collapse; width: 100%; margin-top: 1rem; }
        th, td { border: 1px solid #ccc; padding: .5rem .75rem; text-align: left; }
        th     { background: #f0f0f0; }
        tr.retard td { background: #fee2e2; }
        .succes { background: #dcfce7; color: #166534; padding: .6rem; border-radius: 4px; }
        .erreur { background: #fee2e2; color: #b91c1c; padding: .6rem; border-radius: 4px; }
        fieldset { max-width: 40rem; margin-top: 1rem; }
        select, button { padding: .35rem; margin: .25rem 0; }
        .btn   { background: #2563eb; color: #fff; border: none; border-radius: 4px; padding: .45rem .9rem; cursor: pointer; }
        form.inline { display: inline; }
    </style>
</head>
<body>

<nav>
    <a href="<c:url value='/livres'/>">Catalogue</a>
    <a href="<c:url value='/emprunts'/>"><strong>Emprunts en cours</strong></a>
</nav>

<h1>Emprunts en cours</h1>

<%-- Message flash (après une redirection PRG) --%>
<c:if test="${not empty flashMessage}">
    <p class="${flashType}"><c:out value="${flashMessage}"/></p>
</c:if>

<%-- ===== Formulaire : emprunter un livre ===== --%>
<form method="post" action="<c:url value='/emprunts'/>">
    <fieldset>
        <legend>Nouvel emprunt (durée : <c:out value="${dureeEmprunt}"/> jours)</legend>

        <label for="livreId">Livre</label><br>
        <select id="livreId" name="livreId">
            <option value="">— Choisir un livre —</option>
            <c:forEach var="livre" items="${livresDisponibles}">
                <option value="<c:out value='${livre.id()}'/>">
                    <c:out value="${livre.titre()}"/>
                    (<c:out value="${livre.exemplairesDisponibles()}"/> disponible(s))
                </option>
            </c:forEach>
        </select><br>

        <label for="etudiantId">Étudiant</label><br>
        <select id="etudiantId" name="etudiantId">
            <option value="">— Choisir un étudiant —</option>
            <c:forEach var="etudiant" items="${etudiants}">
                <option value="<c:out value='${etudiant.id()}'/>">
                    <c:out value="${etudiant.prenom()}"/> <c:out value="${etudiant.nom()}"/>
                    (<c:out value="${etudiant.numeroEtudiant()}"/>)
                </option>
            </c:forEach>
        </select><br>

        <button type="submit" class="btn">Emprunter</button>
    </fieldset>
</form>

<%-- ===== Tableau des emprunts en cours ===== --%>
<c:choose>
    <c:when test="${empty emprunts}">
        <p>Aucun emprunt en cours.</p>
    </c:when>
    <c:otherwise>
        <p><c:out value="${fn:length(emprunts)}"/> emprunt(s) en cours.</p>
        <table>
            <thead>
            <tr>
                <th>Livre</th>
                <th>Étudiant</th>
                <th>Emprunté le</th>
                <th>À rendre le</th>
                <th>Statut</th>
                <th>Pénalité</th>
                <th>Action</th>
            </tr>
            </thead>
            <tbody>
            <c:forEach var="e" items="${emprunts}">
                <tr class="${e.enRetard() ? 'retard' : ''}">
                    <td><c:out value="${e.titreLivre()}"/></td>
                    <td><c:out value="${e.etudiant()}"/></td>
                    <td><c:out value="${e.dateEmprunt()}"/></td>
                    <td><c:out value="${e.dateRetourPrevue()}"/></td>
                    <td><c:out value="${e.statut()}"/></td>
                    <td><c:out value="${e.penalite()}"/></td>
                    <td>
                        <form class="inline" method="post"
                              action="<c:url value='/emprunts/${e.id()}/retour'/>">
                            <button type="submit">Enregistrer le retour</button>
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