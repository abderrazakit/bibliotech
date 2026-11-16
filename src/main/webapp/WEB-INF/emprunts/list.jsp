<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib uri="jakarta.tags.core" prefix="c" %>
<%@ taglib uri="jakarta.tags.functions" prefix="fn" %>
<c:set var="pageActive" value="emprunts"/>
<!DOCTYPE html>
<html lang="fr">
<head>
    <title>Emprunts en cours — BiblioTech</title>
    <%@ include file="/WEB-INF/jspf/tete.jspf" %>
</head>
<body>
<%@ include file="/WEB-INF/jspf/entete.jspf" %>

<main class="conteneur">
    <h1>Emprunts en cours</h1>
    <p class="intro">Enregistrez un nouvel emprunt ou le retour d'un livre. Les emprunts en retard apparaissent en rose, avec leur pénalité.</p>

    <c:if test="${not empty flashMessage}">
        <p class="message ${flashType}" role="status"><c:out value="${flashMessage}"/></p>
    </c:if>

    <form class="emprunt-nouveau" method="post" action="<c:url value='/emprunts'/>">
        <div class="champ">
            <label for="livreId">Livre</label>
            <select id="livreId" name="livreId">
                <option value="">Choisir un livre</option>
                <%-- livre = entité JPA : getters, donc pas de parenthèses --%>
                <c:forEach var="livre" items="${livresDisponibles}">
                    <option value="<c:out value='${livre.id}'/>">
                        <c:out value="${livre.titre}"/> (<c:out value="${livre.exemplairesDisponibles}"/> disponible(s))
                    </option>
                </c:forEach>
            </select>
        </div>
        <div class="champ">
            <label for="etudiantId">Étudiant</label>
            <select id="etudiantId" name="etudiantId">
                <option value="">Choisir un étudiant</option>
                <c:forEach var="etudiant" items="${etudiants}">
                    <option value="<c:out value='${etudiant.id}'/>">
                        <c:out value="${etudiant.nomComplet}"/>
                    </option>
                </c:forEach>
            </select>
        </div>
        <button type="submit" class="btn">Emprunter</button>
        <p class="aide">Durée de l'emprunt : <c:out value="${dureeEmprunt}"/> jours. Seuls les livres avec un exemplaire disponible sont proposés.</p>
    </form>

    <c:choose>
        <c:when test="${empty emprunts}">
            <div class="table-cadre vide-etat">
                <p>Aucun emprunt en cours. Utilisez le formulaire ci-dessus pour en enregistrer un.</p>
            </div>
        </c:when>
        <c:otherwise>
            <p class="compteur"><c:out value="${fn:length(emprunts)}"/> emprunt(s) en cours</p>
            <div class="table-cadre">
                <table>
                    <thead>
                    <tr>
                        <th scope="col">Livre</th>
                        <th scope="col">Étudiant</th>
                        <th scope="col">Emprunté le</th>
                        <th scope="col">À rendre le</th>
                        <th scope="col">Statut</th>
                        <th scope="col">Pénalité</th>
                        <th scope="col">Action</th>
                    </tr>
                    </thead>
                    <tbody>
                    <%-- e = LigneEmprunt, toujours un record : on garde les parenthèses --%>
                    <c:forEach var="e" items="${emprunts}">
                        <tr class="${e.enRetard() ? 'retard' : ''}">
                            <td class="titre"><c:out value="${e.titreLivre()}"/></td>
                            <td><c:out value="${e.etudiant()}"/></td>
                            <td><c:out value="${e.dateEmprunt()}"/></td>
                            <td><c:out value="${e.dateRetourPrevue()}"/></td>
                            <td><span class="pastille ${e.enRetard() ? 'vide' : 'ok'}"><c:out value="${e.statut()}"/></span></td>
                            <td><c:out value="${e.penalite()}"/></td>
                            <td>
                                <form method="post" action="<c:url value='/emprunts/${e.id()}/retour'/>">
                                    <button type="submit" class="btn btn-discret btn-petit">Enregistrer le retour</button>
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