<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib uri="jakarta.tags.core" prefix="c" %>
<!DOCTYPE html>
<html lang="fr">
<head>
    <meta charset="UTF-8">
    <title>BiblioTech — <c:out value="${titrePage}"/></title>
    <style>
        body   { font-family: Arial, sans-serif; margin: 2rem; color: #222; }
        form   { max-width: 32rem; }
        label  { display: block; font-weight: bold; margin-top: 1rem; }
        input  { width: 100%; padding: .4rem; box-sizing: border-box; }
        .champ-invalide { border: 2px solid #b91c1c; }
        .erreur { color: #b91c1c; font-size: .9rem; display: block; margin-top: .2rem; }
        .alerte { background: #fee2e2; color: #b91c1c; padding: .6rem; border-radius: 4px; }
        .btn    { margin-top: 1.5rem; padding: .5rem 1rem; background: #2563eb; color: #fff;
                  border: none; border-radius: 4px; cursor: pointer; }
    </style>
</head>
<body>

<p><a href="<c:url value='/livres'/>">← Retour au catalogue</a></p>

<h1><c:out value="${titrePage}"/></h1>

<%-- Message général (si une erreur ne concerne pas un champ précis) --%>
<c:if test="${not empty errors.global}">
    <p class="alerte"><c:out value="${errors.global}"/></p>
</c:if>

<form method="post" action="<c:url value='${actionUrl}'/>">

    <label for="titre">Titre</label>
    <input type="text" id="titre" name="titre"
           value="<c:out value='${form.titre}'/>"
           class="${not empty errors.titre ? 'champ-invalide' : ''}">
    <c:if test="${not empty errors.titre}">
        <span class="erreur"><c:out value="${errors.titre}"/></span>
    </c:if>

    <label for="auteur">Auteur</label>
    <input type="text" id="auteur" name="auteur"
           value="<c:out value='${form.auteur}'/>"
           class="${not empty errors.auteur ? 'champ-invalide' : ''}">
    <c:if test="${not empty errors.auteur}">
        <span class="erreur"><c:out value="${errors.auteur}"/></span>
    </c:if>

    <label for="anneePublication">Année de publication</label>
    <input type="text" id="anneePublication" name="anneePublication" inputmode="numeric"
           value="<c:out value='${form.anneePublication}'/>"
           class="${not empty errors.anneePublication ? 'champ-invalide' : ''}">
    <c:if test="${not empty errors.anneePublication}">
        <span class="erreur"><c:out value="${errors.anneePublication}"/></span>
    </c:if>

    <label for="exemplairesTotal">Nombre total d'exemplaires</label>
    <input type="text" id="exemplairesTotal" name="exemplairesTotal" inputmode="numeric"
           value="<c:out value='${form.exemplairesTotal}'/>"
           class="${not empty errors.exemplairesTotal ? 'champ-invalide' : ''}">
    <c:if test="${not empty errors.exemplairesTotal}">
        <span class="erreur"><c:out value="${errors.exemplairesTotal}"/></span>
    </c:if>

    <label for="exemplairesDisponibles">Exemplaires disponibles</label>
    <input type="text" id="exemplairesDisponibles" name="exemplairesDisponibles" inputmode="numeric"
           value="<c:out value='${form.exemplairesDisponibles}'/>"
           class="${not empty errors.exemplairesDisponibles ? 'champ-invalide' : ''}">
    <c:if test="${not empty errors.exemplairesDisponibles}">
        <span class="erreur"><c:out value="${errors.exemplairesDisponibles}"/></span>
    </c:if>

    <button type="submit" class="btn">Enregistrer</button>
</form>

</body>
</html>