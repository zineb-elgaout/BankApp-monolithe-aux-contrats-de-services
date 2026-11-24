package fr.bankapp.risqueservice.validation;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import javax.xml.XMLConstants;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Validation en entrée du service Risque : une demande de prêt, en JSON ou en XML,
 * est confrontée au contrat avant tout traitement. Un service ne fait jamais confiance
 * à ce qu'il reçoit (chapitre 3, slide 24).
 */
public class ValidateurDemande {

    /** Nom de l'élément en cause, dans les messages du validateur XML (anglais ou français). */
    private static final Pattern ELEMENT = Pattern.compile("(?:element|élément) '(?:\\w+:)?(\\w+)'");

    private final ObjectMapper json = new ObjectMapper();
    private final JsonSchema schemaJson;
    private final Schema schemaXml;

    /**
     * @param contrat le contrat OpenAPI qui contient le schéma DemandePretDto (contrats/risque.yaml)
     * @param xsd     le schéma XSD de la demande en XML (contrats/xml/demande-pret.xsd)
     */
    public ValidateurDemande(Path contrat, Path xsd) {
        try {
            JsonNode racine = new ObjectMapper(new YAMLFactory()).readTree(contrat.toFile());
            JsonNode demande = racine.at("/components/schemas/DemandePretDto");
            schemaJson = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V7).getSchema(demande);
            schemaXml = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI).newSchema(xsd.toFile());
        } catch (IOException | SAXException e) {
            throw new IllegalStateException("Schéma de la demande illisible : " + e.getMessage(), e);
        }
    }

    /** Retourne la première erreur trouvée, ou rien si la demande JSON respecte le contrat. */
    public Optional<ErreurValidation> validerJson(String demande) {
        JsonNode message;
        try {
            message = json.readTree(demande);
        } catch (JsonProcessingException e) {
            return Optional.of(ErreurValidation.messageInvalide("(message)", "JSON bien formé", null));
        }
        return schemaJson.validate(message).stream()
                .min(Comparator.comparing(v -> v.getInstanceLocation().toString()))
                .map(ValidateurDemande::traduire);
    }

    /** Une violation du schéma JSON, traduite dans le format d'erreur du cours. */
    private static ErreurValidation traduire(ValidationMessage violation) {
        if (violation.getType().equals("required")) {
            return ErreurValidation.messageInvalide(violation.getProperty(), "obligatoire", null);
        }
        String champ = violation.getInstanceLocation().toString().replaceFirst("^\\$\\.", "");
        String regle = violation.getType() + " " + violation.getSchemaNode().asText();
        JsonNode recu = violation.getInstanceNode();
        return ErreurValidation.messageInvalide(champ, regle, recu.isTextual() ? recu.asText() : recu);
    }

    /** Retourne la première erreur trouvée, ou rien si la demande XML respecte le schéma XSD. */
    public Optional<ErreurValidation> validerXml(String demande) {
        List<SAXParseException> erreurs = new ArrayList<>();
        Validator validateur = schemaXml.newValidator();
        validateur.setErrorHandler(new ErrorHandler() {
            @Override
            public void warning(SAXParseException e) {
            }

            @Override
            public void error(SAXParseException e) {
                erreurs.add(e);
            }

            @Override
            public void fatalError(SAXParseException e) {
                erreurs.add(e);
            }
        });
        try {
            validateur.validate(new StreamSource(new StringReader(demande)));
        } catch (SAXException | IOException e) {
            return Optional.of(ErreurValidation.messageInvalide("(message)", "XML bien formé", null));
        }
        if (erreurs.isEmpty()) {
            return Optional.empty();
        }
        String regle = erreurs.get(0).getMessage().replaceFirst("^cvc-[\\w.-]+\\s?:\\s*", "");
        String champ = erreurs.stream()
                .map(e -> ELEMENT.matcher(e.getMessage()))
                .filter(Matcher::find)
                .map(m -> m.group(1))
                .findFirst().orElse("(message)");
        return Optional.of(ErreurValidation.messageInvalide(champ, regle, null));
    }
}
