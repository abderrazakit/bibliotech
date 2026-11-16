package com.bibliotech.service;

import com.bibliotech.model.ValidationException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.hibernate.validator.messageinterpolation.ParameterMessageInterpolator;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/** Valide une entité avec Bean Validation et lève une ValidationException champ par champ. */
final class Validateur {

    /**
     * ParameterMessageInterpolator : nos messages sont des textes fixes,
     * pas besoin du moteur d'expressions EL (fonctionne aussi hors de Tomcat, dans les tests).
     */
    private static final Validator VALIDATOR = Validation.byDefaultProvider()
            .configure()
            .messageInterpolator(new ParameterMessageInterpolator())
            .buildValidatorFactory()
            .getValidator();

    /** Règles @AssertTrue → champ du formulaire sous lequel afficher le message. */
    private static final Map<String, String> CHAMP_DU_FORMULAIRE = Map.of(
            "anneeNonFuture",       "anneePublication",
            "disponiblesCoherents", "exemplairesDisponibles",
            "datesCoherentes",      "dateRetourPrevue",
            "retourCoherent",       "dateRetourEffective");

    private Validateur() {}

    static void valider(Object entite) {
        Set<ConstraintViolation<Object>> violations = VALIDATOR.validate(entite);
        if (violations.isEmpty()) {
            return;
        }
        Map<String, String> erreurs = new LinkedHashMap<>();
        violations.stream()
                .sorted(Comparator.comparing(v -> v.getPropertyPath().toString()))
                .forEach(v -> {
                    String propriete = v.getPropertyPath().toString();
                    String champ = CHAMP_DU_FORMULAIRE.getOrDefault(propriete, propriete);
                    erreurs.putIfAbsent(champ, v.getMessage());     // 1 message par champ
                });
        throw new ValidationException(erreurs);
    }
}