import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.reflect.Array;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

public class AddPublicationUsingDOIStepOneController {

    private static final long serialVersionUID = 1L;
    private static final String crossRefAPI = "http://api.crossref.org/works/";
    private static final String[] venueTitleExcludedWordList =
            {"a", "an", "the", "about", "above", "across", "after", "against", "after", "around", "at", "before",
                    "behind", "below", "beneath", "beside", "besides", "between", "beyond", "by", "down", "during", "except",
                    "for", "from", "in", "inside", "into", "like", "near", "of", "off", "on", "out", "outside", "over",
                    "since", "through", "throughout", "till", "to", "toward", "under", "until", "up", "upon", "with", "without"};
    private static final Map<String, String> venueTypes;

    static {
        venueTypes = new HashMap<String, String>();
        venueTypes.put("http://purl.org/ontology/bibo/Book", "Book");
        venueTypes.put("http://info.deepcarbon.net/schema#BookSeries", "Book Series");
        venueTypes.put("http://purl.org/ontology/bibo/Conference", "Conference");
        venueTypes.put("http://purl.org/ontology/bibo/Journal", "Journal");
        venueTypes.put("http://purl.org/ontology/bibo/Magazine", "Magazine");
        venueTypes.put("http://vivoweb.org/ontology/core#Newsletter", "Newsletter");
        venueTypes.put("http://purl.org/ontology/bibo/Newspaper", "Newspaper");
        venueTypes.put("http://info.deepcarbon.net/schema#Proceedings", "Proceedings");
        venueTypes.put("http://purl.org/ontology/bibo/Website", "Website");
    }

    Map<String, String> processDoi(String doi) {
        // Check if the publication with the given DOI is already in the system.
        String checkPublicationExistenQuery =
                "SELECT * WHERE {" +
                        "{ ?pub <http://purl.org/ontology/bibo/doi> \"" + doi.toLowerCase() + "\" . } UNION { ?pub <http://purl.org/ontology/bibo/doi> \"" + doi.toUpperCase() + "\" . } " +
                        "?pub <http://www.w3.org/2000/01/rdf-schema#label> ?label . }";
        JSONArray checkPublicationExistenQueryResults = SparqlQueryUtils.vivoSparqlSelect(checkPublicationExistenQuery);
        if (checkPublicationExistenQueryResults.length() > 0) {
            HashMap<String, String> map = new HashMap<String, String>();
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
            return map;
        } else {
            try {
                if (doi.isEmpty()) throw new NullPointerException("No DOI was entered.");

                JSONObject metadata = getPublicationMetadataViaCrossRef(doi);
                if (metadata == null)
                    throw new PublicationMetadataImportException("This DOI has no record in CrossRef.");

                Map<String, Object> metadataMap = parsePublicationMetadataJSONObject(metadata);

                ArrayList<HashMap<String, Object>> authors = (ArrayList<HashMap<String, Object>>) metadataMap.get("authors");
                if (authors != null) {
                    for (HashMap<String, Object> author : authors) {
                        matchAuthor(author);

                    }
                }

                HashMap<String, Object> venue = (HashMap<String, Object>) metadataMap.get("venue");
                matchVenue(venue);

                Map<String, String> metadataStr = new HashMap<String, String>();
                Map<String, Object> templateData = new HashMap<String, Object>();
                templateData.put("doi", doi);
                templateData.put("metadata", metadataMap);

                Iterator itMeta = metadataMap.entrySet().iterator();
                while (itMeta.hasNext()) {

                    Map.Entry pair = (Map.Entry) itMeta.next();

                    if (pair.getKey().equals("authors")) {
                        ArrayList<HashMap<String, Object>> values = (ArrayList<HashMap<String, Object>>) pair.getValue();

                        for (int i = 0; i < values.size(); i++) {
                            if (values.get(i).get("matching") != null) {
                                // Found match in triplestore
                                metadataStr.put("author-" + i, ((HashMap<String, Object>) values.get(i).get("matching")).get("uri").toString());
                            } else {
                                // Not found match in triplestore
                                metadataStr.put("author-" + i, values.get(i).get("family") + "," + values.get(i).get("given"));
                            }
                        }
                    } else if (pair.getKey().equals("venue")) {
                        HashMap<String, Object> values = (HashMap<String, Object>) pair.getValue();
                        if (values.get("matching") != null) {
                            metadataStr.put("venue", ((HashMap<String, String>) values.get("matching")).get("uri"));
                        } else {
                            metadataStr.put("venue", values.get("label").toString());
                        }
                    } else if (pair.getKey().equals("pubType")){
                        metadataStr.put(pair.getKey().toString(),pair.getValue().toString());
                    } else {
                        metadataStr.put(pair.getKey().toString(), ((List<String>) pair.getValue()).get(0));
                    }
                }
                metadataStr.put("doi", doi);
                metadataStr.put("pubType", matchPubType(metadataMap.get("pubType").toString()));

                return metadataStr;

            } catch (Throwable th) {
                HashMap<String, String> map = new HashMap<String, String>();
                map.put("errorMessage", th.toString());
                return map;
            }
        }
    }

    private JSONObject getPublicationMetadataViaCrossRef(String doi) {
        JSONObject metadata = new JSONObject();
        String url = crossRefAPI + doi;
		/*
		HttpClient client = new DefaultHttpClient();
		
		HttpGet request = new HttpGet(url);
		HttpResponse response;
		*/

        try {
			/*
			response = client.execute(request);
			BufferedReader reader = new BufferedReader(new InputStreamReader(response.getEntity().getContent(), "UTF-8"));
			StringBuilder builder = new StringBuilder();
			for (String line = null; (line = reader.readLine()) != null;) {
			    builder.append(line).append("\n");
			}
			*/
            URL obj = new URL(url);
            HttpURLConnection con = (HttpURLConnection) obj.openConnection();

            // optional default is GET
            con.setRequestMethod("GET");
            //add request header
            con.setRequestProperty("User-Agent", "Mozilla/5.0");

            BufferedReader in = new BufferedReader(
                    new InputStreamReader(con.getInputStream()));
            String inputLine;
            StringBuffer builder = new StringBuffer();

            while ((inputLine = in.readLine()) != null) {
                builder.append(inputLine).append("\n");
            }
            in.close();

            if (builder.toString().startsWith("{")) {
                JSONTokener tokener = new JSONTokener(builder.toString());
                JSONObject json = new JSONObject(tokener);
                if (json.has("message")) {
                    if (!json.isNull("message"))
                        metadata = json.getJSONObject("message");
                    else
                        metadata = null;
                } else metadata = null;
            } else
                metadata = null;
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            metadata = null;
        }
        return metadata;
    }

    private Map<String, Object> parsePublicationMetadataJSONObject(JSONObject json) {
        Map<String, Object> metadata = new HashMap<String, Object>();
        // Title
        if (json.has("title")) metadata.put("titles", getTitlesFromJSON(json));
        else metadata.put("titles", null);
        // Authors
        if (json.has("author")) metadata.put("authors", getAuthorsFromJSON(json));
        else metadata.put("authors", null);
        // Sometime there is no field of authors but editors
        if (json.has("editors")) metadata.put("authors", getEditorsFromJSON(json));

        // Venue
        if (json.has("container-title")) metadata.put("venue", getVenuesFromJSON(json));
        else metadata.put("venue", null);
        // Publication year
        if (json.has("issued")) metadata.put("publicationYears", getPublicationYearsFromJSON(json));
        else metadata.put("publicationYear", null);
        // Publication type
        if (json.has("type")) metadata.put("pubType", getPublicationTypeFromJSON(json));
        else metadata.put("pubType", null);
        return metadata;
    }

    private List<String> getTitlesFromJSON(JSONObject json) {
        List<String> titles = new ArrayList<String>();
        if (!json.isNull("title")) {
            try {
                JSONArray arr = json.getJSONArray("title");
                for (int i = 0; i < arr.length(); i++) {
                    titles.add(arr.getString(i));
                }
            } catch (JSONException e) {
                titles = null;
                e.printStackTrace();
            }
        } else titles = null;
        return titles;
    }

    private ArrayList<HashMap<String, Object>> getEditorsFromJSON(JSONObject json) {
        ArrayList<HashMap<String, Object>> editors = new ArrayList<HashMap<String, Object>>();
        if (!json.isNull("editors")) {
            try {
                JSONArray arr = json.getJSONArray("editors");
                for (int i = 0; i < arr.length(); i++) {
                    JSONObject authorJson = arr.getJSONObject(i);
                    HashMap<String, Object> editor = new HashMap<String, Object>();
                    editor.put("family", authorJson.getString("family"));
                    editor.put("given", authorJson.getString("given"));
                    editors.add(editor);
                }
            } catch (JSONException e) {
                editors = null;
                e.printStackTrace();
            }
        } else editors = null;
        return editors;
    }

    private ArrayList<HashMap<String, Object>> getAuthorsFromJSON(JSONObject json) {
        ArrayList<HashMap<String, Object>> authors = new ArrayList<HashMap<String, Object>>();
        if (!json.isNull("author")) {
            try {
                JSONArray arr = json.getJSONArray("author");
                for (int i = 0; i < arr.length(); i++) {
                    JSONObject authorJson = arr.getJSONObject(i);
                    HashMap<String, Object> author = new HashMap<String, Object>();
                    author.put("family", authorJson.getString("family"));
                    author.put("given", authorJson.getString("given"));
                    authors.add(author);
                }
            } catch (JSONException e) {
                authors = null;
                e.printStackTrace();
            }
        } else authors = null;
        return authors;
    }

    private HashMap<String, Object> getVenuesFromJSON(JSONObject json) {
        HashMap<String, Object> venue = new HashMap<String, Object>();
        if (!json.isNull("container-title")) {
            try {
                JSONArray arr = json.getJSONArray("container-title");
                String venueStr = "";
                for (int i = 0; i < arr.length(); i++) {
                    String tmpStr = arr.getString(i);
                    if (tmpStr.length() > venueStr.length())
                        venueStr = tmpStr;
                }
                venue.put("label", venueStr);
            } catch (JSONException e) {
                venue = null;
                e.printStackTrace();
            }
        } else venue = null;
        return venue;
    }

    private List<Integer> getPublicationYearsFromJSON(JSONObject json) {
        List<Integer> years = new ArrayList<Integer>();
        if (!json.isNull("issued")) {
            try {
                JSONObject issued = json.getJSONObject("issued");
                JSONArray dates = issued.getJSONArray("date-parts");
                for (int i = 0; i < dates.length(); i++) {
                    JSONArray date = dates.getJSONArray(i);
                    years.add((Integer) date.get(0));
                }
            } catch (JSONException e) {
                years = null;
                e.printStackTrace();
            }
        } else years = null;
        return years;
    }

    private String getPublicationTypeFromJSON(JSONObject json) {

        String type = new String();
        if (!json.isNull("type")) {
            try {

                type = json.get("type").toString();
            } catch (JSONException e) {
                type = null;
                e.printStackTrace();
            }
        } else type = null;
        return type;
    }


    public int LevenshteinDistance(String s0, String s1) {
        int len0 = s0.length() + 1;
        int len1 = s1.length() + 1;

        // the array of distances
        int[] cost = new int[len0];
        int[] newcost = new int[len0];

        // initial cost of skipping prefix in String s0
        for (int i = 0; i < len0; i++) cost[i] = i;

        // dynamically computing the array of distances

        // transformation cost for each letter in s1
        for (int j = 1; j < len1; j++) {
            // initial cost of skipping prefix in String s1
            newcost[0] = j;

            // transformation cost for each letter in s0
            for (int i = 1; i < len0; i++) {
                // matching current letters in both strings
                int match = (s0.charAt(i - 1) == s1.charAt(j - 1)) ? 0 : 1;

                // computing cost for each transformation
                int cost_replace = cost[i - 1] + match;
                int cost_insert = cost[i] + 1;
                int cost_delete = newcost[i - 1] + 1;

                // keep minimum cost
                newcost[i] = Math.min(Math.min(cost_insert, cost_delete), cost_replace);
            }

            // swap cost/newcost arrays
            int[] swap = cost;
            cost = newcost;
            newcost = swap;
        }

        // the distance is the cost for transforming all letters in both strings
        return cost[len0 - 1];
    }

    private void matchAuthor(Map<String, Object> author) {
        String familyName = (String) author.get("family");
        String processedFamilyName = familyName.replaceAll("\\.", "").toLowerCase();
        String givenName = (String) author.get("given");
        String processedGivenName = givenName.replaceAll("\\.", "").toLowerCase();
        String label = familyName + ", " + givenName;
        String uri = new String();
        HashMap<String, String> matching = new HashMap<String, String>();

        String exactNameMatchingQueryStr =
                "SELECT DISTINCT ?uri WHERE " +
                        "{ ?uri a foaf:Person . { ?uri rdfs:label \"" + label + "\" . } UNION { ?uri rdfs:label \"" + label + "\"@en-US . } UNION { ?uri rdfs:label \"" + label + "\"^^xsd:string . } } ";
        //System.out.println("Exact author name matching SPARQL query: " + exactNameMatchingQueryStr);
        JSONArray exactNameMatchingQueryResults = SparqlQueryUtils.vivoSparqlSelect(exactNameMatchingQueryStr);
        if (exactNameMatchingQueryResults.length() > 0) {
            try {
                JSONObject result = (JSONObject) exactNameMatchingQueryResults.get(0);
                uri = ((JSONObject) result.getJSONObject("uri")).getString("value");
            } catch (JSONException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        // If there is exact match found, then the rest of the code doesn't have to do anything
        if (!uri.isEmpty()) author.put("uri", uri);
        else author.put("uri", null);

        String fuzzyNameMatchingQueryStr =
                "SELECT DISTINCT ?uri ?label WHERE " +
                        "{ ?uri a foaf:Person ; rdfs:label ?label ; obo:ARG_2000028 [vcard:hasName ?name] . " +
                        "?name vcard:familyName ?fn ; vcard:givenName ?gn . " +
                        "FILTER(contains(lcase(?fn), \"" + processedFamilyName + "\") || contains(\"" + processedFamilyName + "\", lcase(?fn))) . } ";
        //"FILTER(contains(lcase(?gn), \"" + processedGivenName + "\") || contains(\"" + processedGivenName + "\", lcase(?gn))) . } ";

        JSONArray fuzzyNameMatchingQueryResults = SparqlQueryUtils.vivoSparqlSelect(fuzzyNameMatchingQueryStr);
        int levensteinDistance = Integer.MAX_VALUE;

        if (fuzzyNameMatchingQueryResults.length() > 0) {
            try {
                for (int i = 0; i < fuzzyNameMatchingQueryResults.length(); i++) {
                    JSONObject result = (JSONObject) fuzzyNameMatchingQueryResults.get(i);
                    String authorUri = ((JSONObject) result.getJSONObject("uri")).getString("value");
                    String authorLabel = ((JSONObject) result.getJSONObject("label")).getString("value");
                    if (!uri.equals(authorUri)) {
                        HashMap<String, String> matchingResult = new HashMap<String, String>();
                        matchingResult.put("uri", authorUri);
                        matchingResult.put("label", authorLabel);
                        if (LevenshteinDistance(authorLabel, familyName + "," + givenName) < levensteinDistance) {
                            levensteinDistance = LevenshteinDistance(authorLabel, familyName + "," + givenName);
                            matching = matchingResult;
                        }
                    }
                }
            } catch (JSONException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

        if (matching.size() > 0) author.put("matching", matching);
        else author.put("matching", null);
    }

    private void matchVenue(Map<String, Object> venue) {
        String label = (String) venue.get("label");
        String processedLabel = label.replaceAll("[^A-Za-z0-9\\s]", "").toLowerCase();
        String[] processedLabelArray = processedLabel.split("\\s+");
        String uri = new String();
        HashMap<String, String> matching = new HashMap<String, String>();
        String exactLabelMatchingQueryStr =
                "SELECT DISTINCT ?uri WHERE " +
                        "{ ?uri a vivo:InformationResource . { ?uri rdfs:label \"" + label + "\" . } UNION { ?uri rdfs:label \"" + label + "\"@en-US . } UNION { ?uri rdfs:label \"" + label + "\"^^xsd:string . } } ";
        //System.out.println("Exact venue label matching SPARQL query: " + exactLabelMatchingQueryStr);
        JSONArray exactLabelMatchingQueryResults = SparqlQueryUtils.vivoSparqlSelect(exactLabelMatchingQueryStr);
        if (exactLabelMatchingQueryResults.length() > 0) {
            try {
                JSONObject result = (JSONObject) exactLabelMatchingQueryResults.get(0);
                uri = ((JSONObject) result.getJSONObject("uri")).getString("value");
            } catch (JSONException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        if (!uri.isEmpty()) venue.put("uri", uri);
        else venue.put("uri", null);
        String fuzzyLabelMatchingQueryStr =
                "SELECT DISTINCT ?uri ?label WHERE " +
                        "{ ?uri a vivo:InformationResource ; rdfs:label ?label . ";
        for (String s : processedLabelArray) {
            if (!Arrays.asList(venueTitleExcludedWordList).contains(s))
                fuzzyLabelMatchingQueryStr += "FILTER(contains(lcase(?label), \"" + s + "\") || contains(\"" + s + "\", lcase(?label))) . ";
        }
        fuzzyLabelMatchingQueryStr += "}";
        //System.out.println("Fuzzy venue label matching SPARQL query: " + fuzzyLabelMatchingQueryStr);
        JSONArray fuzzyNameMatchingQueryResults = SparqlQueryUtils.vivoSparqlSelect(fuzzyLabelMatchingQueryStr);
        int levensteinDistance = Integer.MAX_VALUE;
        if (fuzzyNameMatchingQueryResults.length() > 0) {
            try {
                for (int i = 0; i < fuzzyNameMatchingQueryResults.length(); i++) {
                    JSONObject result = (JSONObject) fuzzyNameMatchingQueryResults.get(i);
                    String venueUri = ((JSONObject) result.getJSONObject("uri")).getString("value");
                    String venueLabel = ((JSONObject) result.getJSONObject("label")).getString("value");
                    if (!uri.equals(venueUri)) {
                        HashMap<String, String> matchingResult = new HashMap<String, String>();
                        matchingResult.put("uri", venueUri);
                        matchingResult.put("label", venueLabel);
                        if (LevenshteinDistance(processedLabel, venueLabel) < levensteinDistance) {
                            levensteinDistance = LevenshteinDistance(processedLabel, venueLabel);
                            matching = matchingResult;
                        }
                    }
                }
            } catch (JSONException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

        if (matching.size() > 0) venue.put("matching", matching);
        else venue.put("matching", null);
    }

    private String matchPubType(String pubTypeStr) {
        Iterator it = venueTypes.entrySet().iterator();
        int levensteinDistance = Integer.MAX_VALUE;
        String matchedUri = "";

        while(it.hasNext()){
            Map.Entry ent = (Map.Entry) it.next();
            if(levensteinDistance>this.LevenshteinDistance(ent.getValue().toString(),pubTypeStr)){
                levensteinDistance = this.LevenshteinDistance(ent.getValue().toString(),pubTypeStr);
                matchedUri = ent.getKey().toString();
            }
        }

        return matchedUri;

    }



}
