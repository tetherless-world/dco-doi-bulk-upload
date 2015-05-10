import javax.swing.text.html.HTMLDocument;
import java.io.*;
import java.util.*;

/**
 * Created by cheny on 3/12/15.
 */
public class TestWorkflow {

    public static void main(String[] args){

        String doi = "";
        File f = new File("pure_dois.txt");
        File of = new File("output_file.txt");
        int counter = 0;
        try{
            BufferedReader br = new BufferedReader(new FileReader(f));
            BufferedWriter bw = new BufferedWriter(new FileWriter(of));

            String line;
            while ((line = br.readLine()) != null) {
                if(line.length()>0){
                    //System.out.println(line);
                    doi = line;
                    AddPublicationUsingDOIStepOneController oneController = new AddPublicationUsingDOIStepOneController();
                    Map<String,String> metadata = oneController.processDoi(doi);
                    //System.out.println(metadata.toString());
                    bw.newLine();
                    bw.write(doi);
                    bw.newLine();
                    bw.write(metadata.toString());
                    bw.newLine();
                    counter++;
                    System.out.println(counter);
                    //AddPublicationUsingDOIStepTwoController twoController = new AddPublicationUsingDOIStepTwoController();
                    //twoController.processDOI(doi,metadata);
                    // Link metadata with entities in VIVO using sparql
                }
            }
            bw.close();

        } catch (IOException e) {
            e.printStackTrace();
        }

        // Retrieve metadata



    }
}
