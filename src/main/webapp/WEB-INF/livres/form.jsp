<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib uri="jakarta.tags.core" prefix="c" %>
<c:set var="pageActive" value="catalogue"/>
<!DOCTYPE html>
<html lang="fr">
<head>
    <title><c:out value="${titrePage}"/> — BiblioTech</title>
    <%@ include file="/WEB-INF/jspf/tete.jspf" %>
</head>
<body>
<%@ include file="/WEB-INF/jspf/entete.jspf" %>

<main class="conteneur">
    <a class="retour" href="<c:url value='/livres'/>">Retour au catalogue</a>

    <div class="formulaire">
        <h1><c:out value="${titrePage}"/></h1>

        <c:if test="${not empty errors.global}">
            <p class="message erreur"><c:out value="${errors.global}"/></p>
        </c:if>

        <form method="post" action="<c:url value='${actionUrl}'/>" novalidate>

            <div class="champ">
                <label for="titre">Titre</label>
                <input type="text" id="titre" name="titre"
                       value="<c:out value='${form.titre}'/>"
                       class="${not empty errors.titre ? 'champ-invalide' : ''}">
                <c:if test="${not empty errors.titre}">
                    <span class="erreur-champ"><c:out value="${errors.titre}"/></span>
                </c:if>
            </div>

            <div class="champ">
                <label for="auteur">Auteur</label>
                <input type="text" id="auteur" name="auteur"
                       value="<c:out value='${form.auteur}'/>"
                       class="${not empty errors.auteur ? 'champ-invalide' : ''}">
                <c:if test="${not empty errors.auteur}">
                    <span class="erreur-champ"><c:out value="${errors.auteur}"/></span>
                </c:if>
            </div>

            <div class="champ">
                <label for="anneePublication">Année de publication</label>
                <input type="text" id="anneePublication" name="anneePublication" inputmode="numeric"
                       value="<c:out value='${form.anneePublication}'/>"
                       class="${not empty errors.anneePublication ? 'champ-invalide' : ''}">
                <c:if test="${not empty errors.anneePublication}">
                    <span class="erreur-champ"><c:out value="${errors.anneePublication}"/></span>
                </c:if>
            </div>

            <div class="grille-2">
                <div class="champ">
                    <label for="exemplairesTotal">Exemplaires au total</label>
                    <input type="text" id="exemplairesTotal" name="exemplairesTotal" inputmode="numeric"
                           value="<c:out value='${form.exemplairesTotal}'/>"
                           class="${not empty errors.exemplairesTotal ? 'champ-invalide' : ''}">
                    <c:if test="${not empty errors.exemplairesTotal}">
                        <span class="erreur-champ"><c:out value="${errors.exemplairesTotal}"/></span>
                    </c:if>
                </div>

                <%-- Seulement en modification : à la création, disponibles = total (règle du LivreService) --%>
                <c:if test="${modeEdition}">
                <div class="champ">
                    <label for="exemplairesDisponibles">Exemplaires disponibles</label>
                    <input type="text" id="exemplairesDisponibles" name="exemplairesDisponibles" inputmode="numeric"
                           value="<c:out value='${form.exemplairesDisponibles}'/>"
                           class="${not empty errors.exemplairesDisponibles ? 'champ-invalide' : ''}">
                    <c:if test="${not empty errors.exemplairesDisponibles}">
                        <span class="erreur-champ"><c:out value="${errors.exemplairesDisponibles}"/></span>
                    </c:if>
                </div>
                </c:if>
            </div>

            <div class="barre-actions">
                <button type="submit" class="btn">Enregistrer le livre</button>
                <a href="<c:url value='/livres'/>">Annuler</a>
            </div>
        </form>
    </div>
</main>
</body>
</html>