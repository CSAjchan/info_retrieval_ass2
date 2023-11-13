import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Scanner;

public class Queries {
    public static ArrayList<String> ProcessQueryFile(String path){
        //System.out.println("here");

        try {
            ArrayList<String> queries = extractDescriptions(path);
            return queries;
        } catch (IOException e) {
            System.err.println("Error: " + e.getMessage());
        }
        return null;
    }

    private static ArrayList<String> extractDescriptions(String filePath) throws IOException {
        ArrayList<String> textArray = new ArrayList<String>();
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            StringBuilder text = new StringBuilder();
            boolean inDesc = false;

            String line;
            while ((line = reader.readLine()) != null) {
               // line = line.trim();
                //System.out.println(line);
                if (line.startsWith("<top>")) {
                    text.setLength(0); //new doc
                    System.out.println("new document");
                }

                if (line.startsWith("<desc>")) {
                    inDesc = true;
                    line = line.substring("<desc> Description:".length()).trim(); //get rid of first line with the tag
                }
                if (inDesc) {
                    text.append(line);
                    System.out.println("line added " + line);
                    if (line.startsWith("<narr>")) {
                        inDesc = false;
                        text.delete(text.length() - "<narr> Narrative:".length(), text.length()); //get rid of <narr> tag that was added
                        System.out.println("Description Text:\n" + text.toString().trim());
                        textArray.add(text.toString().trim());
                    }
                }
                // if (line.startsWith("</top>")) {
                // }
            }
        }
        return textArray;
}


}
