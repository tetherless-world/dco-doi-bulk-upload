
import java.util.*;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;



public class AddPublicationUsingDOIStepTwoController{

    private static final long serialVersionUID = 1L;
    private String absoluteMachineURL = ServerInfo.getInstance().getAbsoluteMachineURL();
    private String rootUserPassword = ServerInfo.getInstance().getRootPassword();
    private String rootUserEmail = ServerInfo.getInstance().getRootName();
    private String returnURL = absoluteMachineURL;

    public String processDOI(String doi,Map<String,String> params) {
        // Check if the publication with the given DOI is already in the system.
        String checkPublicationExistenQuery =
                "SELECT * WHERE {" +
                        "{ ?pub <http://purl.org/ontology/bibo/doi> \"" + doi.toLowerCase() + "\" . } UNION { ?pub <http://purl.org/ontology/bibo/doi> \"" + doi.toUpperCase() + "\" . } " +
                        "?pub <http://www.w3.org/2000/01/rdf-schema#label> ?label . }";
        JSONArray checkPublicationExistenQueryResults = SparqlQueryUtils.vivoSparqlSelect(checkPublicationExistenQuery);
        if (checkPublicationExistenQueryResults.length() >0 ) {
            // Publication already exists
            System.out.println("Publication existed");
            HashMap<String,Object> map = new HashMap<String,Object>();
            map.put("errorMessage", "There is already a publication entry with this DOI in the system.");
            try {
                JSONObject existingPub = (JSONObject) checkPublicationExistenQueryResults.get(0);
                String existingPubURI = existingPub.getJSONObject("pub").getString("value");
                String existingPubLabel = existingPub.getJSONObject("label").getString("value");
                map.put("link", existingPubURI);
                map.put("linkLabel", existingPubLabel);
            } catch (JSONException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            return "";
        } else {
            // Publication not exists
            System.out.println("Not existed");
            try {

                // Get form input.
                Map<String, String> parameters = params;
                // Generate the n3 to be inserted.
                String newPublicationN3 = generateN3(parameters);
                System.out.println(newPublicationN3);
                // Insert the N3 using VIVO API
                int vivoInsertRequestStatusCode = 0;


                // Commit the changes to triplestore
                // vivoInsertRequestStatusCode = SparqlQueryUtils.vivoSparqlInsert(rootUserPassword, rootUserEmail, newPublicationN3);

                return "";
            } catch (Throwable th) {
                HashMap<String,Object> map = new HashMap<String,Object>();
                map.put("errorMessage", th.toString());
                return "";
            }
        }
    }

    private String generateN3(Map<String, String> parameters) {
        String triples = "";
        int counter = 0;
        Iterator it = parameters.entrySet().iterator();
        while (it.hasNext()) {
            counter++;
            Map.Entry ent = (Map.Entry)it.next();
            String key = ent.getKey().toString();
            //System.out.println("Key: " + key);
            String value = ent.getValue().toString();
            //System.out.println("Value: " + value);
            if (key.equals("titles") && !value.isEmpty()) {
                triples += "?newPub rdfs:label \"" + value + "\" . \n";
            }
            if (key.equals("pubType") && !value.isEmpty()) {
                triples += "?newPub a <" + value + "> . \n";
            }
            if (key.equals("doi") && !value.isEmpty()) {
                triples += "?newPub bibo:doi \"" + value + "\" . \n";
            }
            if (key.equals("publicationYears") && !value.isEmpty()) {
                triples += "?newPub dco:yearOfPublication \"" + value + "\"^^xsd:gYear . \n";
            }
            if (key.equals("venue") && !value.isEmpty()) {
                if (value.startsWith("http:")) {
                    triples += "?newPub vivo:hasPublicationVenue <" + value + "> . \n" +
                            "<" + value + "> vivo:publicationVenueFor ?newPub . \n";
                }
                else {
                    if (!parameters.get("venueType").isEmpty()) {
                        triples += "?newVenue a <" + parameters.get("venueType") + "> . \n" +
                                "?newVenue rdfs:label \"" + value + "\" . \n" +
                                "?newPub vivo:hasPublicationVenue ?newVenue . \n" +
                                "?newVenue vivo:publicationVenueFor ?newPub . \n";
                        String venueURI = generateIndividualURI();
                        DCOId dcoId = new DCOId();
                        dcoId.operate(venueURI, "URI", "create");
                        String pubDCOId = dcoId.getDCOId();
                        String pubDCOIdLabel = pubDCOId.substring(25);
                        triples += "?newVenue dco:hasDcoId <" + pubDCOId + "> . \n" +
                                "<" + pubDCOId + "> a dco:DCOID . \n" +
                                "<" + pubDCOId + "> rdfs:label \"" + pubDCOIdLabel + "\" . \n" +
                                "<" + pubDCOId + "> dco:dcoIdFor ?newVenue . \n";
                        triples = triples.replaceAll("\\?\\bnewVenue\\b", "<" + venueURI + ">");
                    }
                }
            }
            if (key.startsWith("author") && !value.isEmpty()) {
                String order = key.split("-")[1];
                if (value.startsWith("http:")) {
                    // Have existing author in system
                    triples += "?newPub vivo:relatedBy ?newAuthorship . \n" +
                            "?newAuthorship a vivo:Authorship . \n" +
                            "?newAuthorship vivo:relates <" + value + "> . \n" +
                            "?newAuthorship vivo:rank \"" + order + "\"^^xsd:int . \n";
                }
                else {
                    // Need to create new authors
                    String[] names = value.split(",");
                    String familyName = names[0].trim();
                    String givenName = names[1].trim();
                    triples += "?newPub vivo:relatedBy ?newAuthorship . \n" +
                            "?newAuthorship a vivo:Authorship . \n" +
                            "?newAuthorship vivo:relates ?newAuthor . \n" +
                            "?newAuthorship vivo:rank \"" + order + "\"^^xsd:int . \n" +
                            "?newAuthor a foaf:Person . \n" +
                            "?newAuthor rdfs:label \"" + value + "\" . \n" +
                            "?newAuthor obo:ARG_2000028 ?newVCard . \n" +
                            "?newVCard a vcard:Individual . \n" +
                            "?newVCard obo:ARG_2000029 ?newAuthor . \n" +
                            "?newVCard vcard:hasName ?newName . \n" +
                            "?newName a vcard:Name . \n" +
                            "?newName vcard:familyName \"" + familyName + "\" . \n" +
                            "?newName vcard:givenName \"" + givenName + "\" . \n";
                    String nameURI = generateIndividualURI();
                    String vCardURI = generateIndividualURI();
                    String authorURI = generateIndividualURI();
                    DCOId dcoId = new DCOId();
                    dcoId.operate(authorURI, "URI", "create");
                    String pubDCOId = dcoId.getDCOId();
                    String pubDCOIdLabel = pubDCOId.substring(25);
                    triples += "?newAuthor dco:hasDcoId <" + pubDCOId + "> . \n" +
                            "<" + pubDCOId + "> a dco:DCOID . \n" +
                            "<" + pubDCOId + "> rdfs:label \"" + pubDCOIdLabel + "\" . \n" +
                            "<" + pubDCOId + "> dco:dcoIdFor ?newAuthor . \n";
                    triples = triples.replaceAll("\\?\\bnewName\\b", "<" + nameURI + ">");
                    triples = triples.replaceAll("\\?\\bnewVCard\\b", "<" + vCardURI + ">");
                    triples = triples.replaceAll("\\?\\bnewAuthor\\b", "<" + authorURI + ">");
                }
                String authorshipURI = generateIndividualURI();
                triples = triples.replaceAll("\\?\\bnewAuthorship\\b", "<" + authorshipURI + ">");
            }

        }

        String publicationURI = generateIndividualURI();
        DCOId dcoId = new DCOId();
        dcoId.operate(publicationURI, "URI", "create");
        String pubDCOId = dcoId.getDCOId();
        String pubDCOIdLabel = pubDCOId.substring(25);
        triples += "?newPub dco:hasDcoId <" + pubDCOId + "> . \n" +
                "<" + pubDCOId + "> a dco:DCOID . \n" +
                "<" + pubDCOId + "> rdfs:label \"" + pubDCOIdLabel + "\" . \n" +
                "<" + pubDCOId + "> dco:dcoIdFor ?newPub . \n";
        triples = triples.replaceAll("\\?\\bnewPub\\b", "<" + publicationURI + ">");

        String n3 =
                "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n" +
                        "PREFIX vivo: <http://vivoweb.org/ontology/core#>\n" +
                        "PREFIX bibo: <http://purl.org/ontology/bibo/>\n" +
                        "PREFIX xsd:   <http://www.w3.org/2001/XMLSchema#>\n" +
                        "PREFIX dco: <http://info.deepcarbon.net/schema#>\n" +
                        "PREFIX foaf: <http://xmlns.com/foaf/0.1/>\n" +
                        "PREFIX obo: <http://purl.obolibrary.org/obo/>\n" +
                        "PREFIX vcard: <http://www.w3.org/2006/vcard/ns#>\n" +
                        "INSERT DATA {\n" +
                        "GRAPH <http://vitro.mannlib.cornell.edu/default/vitro-kb-2>\n" +
                        "{\n" + triples +
                        "\n}}";
        returnURL = publicationURI;
        return n3;
    }

    private String generateIndividualURI() {
        String uri = new String();
        String queryStr = "";
        String prefix = ServerInfo.getInstance().getDcoNamespace();
        do {
            uri = prefix + "/individual/" + UUID.randomUUID().toString();
            queryStr = "ASK { <" + uri + "> ?p ?o }";
        } while(SparqlQueryUtils.vivoSparqlAsk(queryStr));
        return uri;
    }

}
