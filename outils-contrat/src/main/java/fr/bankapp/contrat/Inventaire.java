package fr.bankapp.contrat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Inventaire de l'existant (approche ascendante) : liste les opérations publiques de chaque module
 * de BankApp, avec des indices tirés de leur code. L'inventaire ne décide rien : c'est à vous de
 * classer chaque opération.
 *
 * Lancement : mvn -q -pl outils-contrat exec:java "-Dexec.mainClass=fr.bankapp.contrat.Inventaire"
 */
public class Inventaire {

    /** Une opération publique trouvée dans le code, avec ses indices. */
    record Entree(String module, String classe, String operation, List<String> indices) {
    }

    private static final Pattern METHODE = Pattern.compile(
            "^\\s{4}public\\s+(?:static\\s+)?(?!class|record|enum|interface)[\\w<>\\[\\], ]+?\\s+(\\w+)\\s*\\(", Pattern.MULTILINE);

    private static final Map<String, Pattern> INDICES = new LinkedHashMap<>();

    /** Schéma de base de chaque module : lire le schéma d'un autre module est un indice fort. */
    private static final Map<String, String> SCHEMAS = Map.of(
            "CPT_DB", "comptes", "PRET_DB", "prets", "RISK_DB", "risque", "CRM_DB", "crm");
    private static final Pattern SCHEMA = Pattern.compile("([A-Z]+_DB)\\.");

    static {
        INDICES.put("lit la table RISK_DB.T_SCORE", Pattern.compile("RISK_DB\\.T_SCORE"));
        INDICES.put("calcule un risque", Pattern.compile("PENALITE|8 \\* incidents|Seuils\\.|calculateur\\.calculer"));
        INDICES.put("envoie un SMS", Pattern.compile("new AcmeSms"));
        INDICES.put("consulte un solde", Pattern.compile("getSolde|SUM\\(SOLDE\\)"));
    }

    public static void main(String[] args) throws IOException {
        List<Entree> entrees = new ArrayList<>();
        entrees.addAll(parcourir(Path.of("monolithe/src/main/java/fr/bankapp"), true));
        entrees.addAll(parcourir(Path.of("risque-service/src/main/java/fr/bankapp/risqueservice"), false));

        System.out.printf("%-16s %-28s %-24s %s%n", "MODULE", "CLASSE", "OPÉRATION", "INDICES");
        for (Entree e : entrees) {
            System.out.printf("%-16s %-28s %-24s %s%n", e.module(), e.classe(), e.operation(), String.join(", ", e.indices()));
        }

        System.out.println();
        System.out.println("Indices présents dans plusieurs modules (doublons possibles) :");
        for (String indice : INDICES.keySet()) {
            Map<String, List<String>> parModule = new TreeMap<>();
            for (Entree e : entrees) {
                if (e.indices().contains(indice)) {
                    parModule.computeIfAbsent(e.module(), m -> new ArrayList<>()).add(e.classe() + "." + e.operation());
                }
            }
            if (parModule.size() > 1) {
                System.out.println("  " + indice + " : " + parModule);
            }
        }
    }

    static List<Entree> parcourir(Path racine, boolean monolithe) throws IOException {
        List<Entree> entrees = new ArrayList<>();
        List<Path> fichiers;
        try (Stream<Path> flux = Files.walk(racine)) {
            fichiers = flux.filter(f -> f.toString().endsWith(".java")).sorted().toList();
        }
        for (Path fichier : fichiers) {
            String source = Files.readString(fichier);
            if (!source.contains("public class") || source.contains("extends RuntimeException")) {
                continue;
            }
            String relatif = racine.relativize(fichier).toString().replace('\\', '/');
            String module = monolithe ? (relatif.contains("/") ? relatif.substring(0, relatif.indexOf('/')) : "(racine)")
                    : "risque-service";
            if (monolithe && relatif.contains("/interne/")) {
                module = module + "/interne";
            }
            String classe = fichier.getFileName().toString().replace(".java", "");
            Matcher methode = METHODE.matcher(source);
            while (methode.find()) {
                String nom = methode.group(1);
                if (nom.equals("main") || nom.equals(classe)) {
                    continue;
                }
                String corps = corps(source, methode.end());
                List<String> indices = new ArrayList<>();
                INDICES.forEach((libelle, motif) -> {
                    if (motif.matcher(corps).find()) {
                        indices.add(libelle);
                    }
                });
                String moduleSeul = module.replace("/interne", "");
                Matcher schema = SCHEMA.matcher(corps);
                while (schema.find()) {
                    String proprietaire = SCHEMAS.get(schema.group(1));
                    if (proprietaire != null && !proprietaire.equals(moduleSeul)
                            && !indices.contains("lit le schéma d'un autre module")) {
                        indices.add("lit le schéma d'un autre module");
                    }
                }
                entrees.add(new Entree(module, classe, nom, indices));
            }
        }
        return entrees;
    }

    /** Corps de la méthode : du premier « { » après la signature jusqu'à l'accolade fermante associée. */
    private static String corps(String source, int depuis) {
        int debut = source.indexOf('{', depuis);
        int profondeur = 0;
        for (int i = debut; i < source.length(); i++) {
            char c = source.charAt(i);
            if (c == '{') {
                profondeur++;
            } else if (c == '}' && --profondeur == 0) {
                return source.substring(debut, i + 1);
            }
        }
        return "";
    }
}
