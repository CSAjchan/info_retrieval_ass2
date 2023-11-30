import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.synonym.SynonymFilter;
import org.apache.lucene.analysis.synonym.SynonymMap;
import org.apache.lucene.analysis.synonym.WordnetSynonymParser;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.queryparser.classic.ParseException;

public class Queries {
    public static WordnetSynonymParser parser;
    public static SynonymMap synonymMap;
    public static ArrayList<String> ProcessQueryFile(String path, String mode) throws IOException, ParseException, java.text.ParseException{
        buildQueryExpansionMap();
        ArrayList<String> queries = new ArrayList<String>();
        try {
            if(mode.contentEquals("titles")) {
                queries = extractTitle(path);
            }
            else if (mode.contentEquals("descriptions")) {
                queries = extractDescriptions(path);
            }
            else if (mode.contentEquals("narratives")) {
                queries = extractNarrative(path);
            }
            else {
                queries = extractAll(path);
            }
            return queries;
        } catch (IOException e) {
            System.err.println("Error: " + e.getMessage());
        }
        return null;
    }

    //does not include narrative
    private static ArrayList<String> extractAll(String filePath) throws IOException {
        String temp;
        ArrayList<String> textArray = new ArrayList<String>();
        
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            StringBuilder text = new StringBuilder();
            boolean inNarr = false;
            boolean endOfDoc = false;
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("<top>")) {
                    inNarr = false;
                    endOfDoc = false;
                    text.setLength(0); //new doc
                    line = line.substring("<top>".length()).trim();
                }
                if (line.startsWith("<title>")) {
                    line = line.substring("<title>".length()).trim(); //get rid of first line with the tag
                }
                if (line.startsWith("<narr>")) {
                    inNarr = true;
                    line = line.substring("<narr> Narrative:".length()).trim(); //get rid of first line with the tag
                }
                if (line.startsWith("<num>")) {
                    line = "";
                }
                if (line.startsWith("<desc>")) {
                    line = line.substring("<desc> Description:".length()).trim(); //get rid of first line with the tag
                }
                if(inNarr == false){
                    text.append(line + " ");
                }
                if(inNarr == true){
                if (line.startsWith("</top>")) {
                    temp = text.toString();
                    temp.replace("</top>", "");
                    temp = temp.replaceAll("[^\\p{L}\\s]", "");
                    System.out.println("Document Text:\n" + temp);
                    textArray.add(temp);
                 }
                }
            }
            }
        
        return textArray;
    }


    private static ArrayList<String> extractDescriptions(String filePath) throws IOException {
        ArrayList<String> textArray = new ArrayList<String>();
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            StringBuilder text = new StringBuilder();
            boolean inDesc = false;

            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println("READ LINE: " + line);
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
                        textArray.add(expandQuery(text.toString().trim().replaceAll("[^\\p{L}\\s]", "")));
                    }
                }
            }
        }
        return textArray;
}

private static ArrayList<String> extractTitle(String filePath) throws IOException {
        ArrayList<String> textArray = new ArrayList<String>();
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {

            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("<title>")) {
                    line = line.substring("<title>".length()).trim();
                    line = line.replaceAll("[^\\p{L}\\s]", "");
                    textArray.add(line);
                }
            }
        }
        return textArray;
}

    private static ArrayList<String> extractNarrative(String filePath) throws IOException {
            ArrayList<String> textArray = new ArrayList<String>();
            try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
                StringBuilder text = new StringBuilder();
                boolean inNarr = false;

                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.startsWith("<top>")) {
                        //new doc
                        text.setLength(0);
                    }

                    if (line.startsWith("<narr>")) {
                        inNarr = true;
                        line = line.substring("<narr> Narrative:".length()).trim(); //get rid of first line with the tag
                    }
                    if (inNarr) {
                        
                        text.append(line);
                        if (line.startsWith("</top>")) {
                            inNarr = false;
                            text.delete(text.length() - "</top>:".length(), text.length());
                            textArray.add(text.toString().trim().replaceAll("[^\\p{L}\\s]", ""));
                        }
                    }
                }
            }
            return textArray;
    }

    //builds synonym map from synonym file from WordNet
    public static void buildQueryExpansionMap () throws IOException, java.text.ParseException {
        try {
            parser = new WordnetSynonymParser(true, true, new StandardAnalyzer());
            parser.parse(new FileReader("src\\main\\resources\\wn_s.pl"));
            synonymMap = parser.build();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // expands the query using the synonym map built earlier
    public static String expandQuery(String query) throws IOException{
        String tempText = "";
        String result = "";
        Analyzer analyzer = new EnglishAnalyzer();

        TokenStream tokenStream = analyzer.tokenStream("TEXT", new StringReader(query));
        TokenStream synonymTokenStream = new SynonymFilter(tokenStream, synonymMap, true); //expanded query
        synonymTokenStream.reset();
        CharTermAttribute termAttribute = synonymTokenStream.addAttribute(CharTermAttribute.class);

        while (synonymTokenStream.incrementToken()) {
            tempText += " " + termAttribute.toString();
        }
        System.out.println("Original text: " + query);
        System.out.println("Expanded text: " + tempText);
        result = tempText;
        tempText = "";
        synonymTokenStream.close();
        analyzer.close();
        return result.replaceAll("[^\\p{L}\\s]", "");
    }
}
