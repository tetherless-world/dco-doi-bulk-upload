import org.apache.commons.httpclient.URIException;
import org.apache.commons.httpclient.util.URIUtil;
import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.UUID;

/**
 * Utility class for using SPARQL queries against the VIVO endpoint.
 *
 * Created by HanWang on 11/23/14.
 */
public class SparqlQueryUtils {


    public static String endpoint = "";
    public static String vivoSparqlUpdateAPI = "";

    public static void init(){
        vivoSparqlUpdateAPI = "http://udco.tw.rpi.edu/vivo/api/sparqlUpdate";
        endpoint = "https://info.deepcarbon.net/endpoint";

    }

    public static JSONArray vivoSparqlSelect(String sparqlQuery) {
        if (endpoint.length()==0){
            init();
        }

        String queryStr =
                "PREFIX dco: <http://info.deepcarbon.net/schema#> " +
                "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> " +
                "PREFIX rdf:   <http://www.w3.org/1999/02/22-rdf-syntax-ns#>" +
                "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#> " +
                "PREFIX vivo: <http://vivoweb.org/ontology/core#> " +
                "PREFIX vitro: <http://vitro.mannlib.cornell.edu/ns/vitro/0.7#>" +
                "PREFIX foaf: <http://xmlns.com/foaf/0.1/> " +
                "PREFIX vcard: <http://www.w3.org/2006/vcard/ns#> " +
                "PREFIX obo: <http://purl.obolibrary.org/obo/> " +
                "PREFIX skos: <http://www.w3.org/2004/02/skos/core#> " +
                sparqlQuery;


        String encodedQuery;
        JSONArray results = new JSONArray();
        HttpClient client = new DefaultHttpClient();
        try {
            encodedQuery = URLEncoder.encode(queryStr, "UTF-8");
            String outputFormat = "&output=json";
            String url = endpoint + "/sparql?query=" + encodedQuery + outputFormat;

            URL obj = new URL(url);
            HttpURLConnection con = (HttpURLConnection) obj.openConnection();

            // optional default is GET
            con.setRequestMethod("GET");
            //add request header
            con.setRequestProperty("User-Agent", "Mozilla/5.0");

            BufferedReader in = new BufferedReader(
                    new InputStreamReader(con.getInputStream()));
            String inputLine;
            StringBuffer response = new StringBuffer();

            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();

            JSONObject jsonObject = new JSONObject(response.toString());
            results = jsonObject.getJSONObject("results").getJSONArray("bindings");

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            client.getConnectionManager().shutdown();
        }
        return results;
    }

    public static boolean vivoSparqlAsk(String sparqlQuery) {
        if (endpoint.length()==0){
            init();
        }
        String queryStr =
                "PREFIX dco: <http://info.deepcarbon.net/schema#> " +
                "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> " +
                "PREFIX rdf:   <http://www.w3.org/1999/02/22-rdf-syntax-ns#>" +
                "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#> " +
                "PREFIX vivo: <http://vivoweb.org/ontology/core#> " +
                "PREFIX vitro: <http://vitro.mannlib.cornell.edu/ns/vitro/0.7#>" +
                "PREFIX foaf: <http://xmlns.com/foaf/0.1/> " +
                "PREFIX vcard: <http://www.w3.org/2006/vcard/ns#> " +
                "PREFIX obo: <http://purl.obolibrary.org/obo/> " +
                "PREFIX skos: <http://www.w3.org/2004/02/skos/core#> " +
                sparqlQuery;
        String encodedQuery;
        boolean result = Boolean.FALSE;
        HttpClient client = new DefaultHttpClient();
        try {
            encodedQuery = URIUtil.encodeQuery(queryStr);
            String outputFormat = "&output=json";
            String url = endpoint + "/sparql?query=" + encodedQuery + outputFormat;

            URL obj = new URL(url);
            HttpURLConnection con = (HttpURLConnection) obj.openConnection();

            // optional default is GET
            con.setRequestMethod("GET");
            //add request header
            con.setRequestProperty("User-Agent", "Mozilla/5.0");

            BufferedReader in = new BufferedReader(
                    new InputStreamReader(con.getInputStream()));
            String inputLine;
            StringBuffer response = new StringBuffer();

            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();

            JSONObject jsonObject = new JSONObject(response.toString());
            result = jsonObject.getBoolean("boolean");

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            client.getConnectionManager().shutdown();
        }
        return result;
    }

    public static int vivoSparqlInsert(String password, String email, String data) throws Exception {
        if (endpoint.length()==0){
            init();
        }
        URL obj = new URL(vivoSparqlUpdateAPI);
        HttpURLConnection con = (HttpURLConnection) obj.openConnection();

        //add reuqest header
        con.setRequestMethod("POST");
        con.setRequestProperty("User-Agent", "Mozilla/5.0");
        con.setRequestProperty("Accept-Language", "en-US,en;q=0.5");

        String n3 =
            "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> " +
            "PREFIX vitro: <http://vitro.mannlib.cornell.edu/ns/vitro/0.7#>" +
            "PREFIX vivo: <http://vivoweb.org/ontology/core#> " +
            "PREFIX dco: <http://info.deepcarbon.net/schema#> " +
            "PREFIX foaf: <http://xmlns.com/foaf/0.1/>" +
            "PREFIX rdf:   <http://www.w3.org/1999/02/22-rdf-syntax-ns#>" +
            "PREFIX obo: <http://purl.obolibrary.org/obo/> " +
            "PREFIX vcard: <http://www.w3.org/2006/vcard/ns#> " +
            "PREFIX skos: <http://www.w3.org/2004/02/skos/core#> " +
            "INSERT DATA { " +
            "GRAPH <http://vitro.mannlib.cornell.edu/default/vitro-kb-2> " +
            "{ " + data + " }}";
        String payload = "password=" + password + "&email=" + email + "&update=" + n3;
        try {
            payload = URIUtil.encodeQuery(payload);
        } catch (URIException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        String urlParameters = payload;

        // Send post request
        con.setDoOutput(true);
        DataOutputStream wr = new DataOutputStream(con.getOutputStream());
        wr.writeBytes(urlParameters);
        wr.flush();
        wr.close();

        int responseCode = con.getResponseCode();
        BufferedReader in = new BufferedReader(
                new InputStreamReader(con.getInputStream()));
        String inputLine;
        StringBuffer response = new StringBuffer();

        while ((inputLine = in.readLine()) != null) {
            response.append(inputLine);
        }
        in.close();

        return responseCode;
    }

    public static String generateIndividualURI() {
        if (endpoint.length()==0){
            init();
        }
        String uri = null;
        String queryStr = "";
        do {
            uri = "http://info.deepcarbon.net/individual/" + UUID.randomUUID().toString();
            queryStr = "ASK { <" + uri + "> ?p ?o }";

        } while(vivoSparqlAsk(queryStr));
        return uri;
    }

}

