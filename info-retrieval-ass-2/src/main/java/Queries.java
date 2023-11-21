import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Paths;
import java.util.ArrayList;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.CharArraySet;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.SynonymQuery;
import org.apache.lucene.search.SynonymQuery.Builder;
import org.apache.lucene.analysis.synonym.SynonymFilter;
import org.apache.lucene.analysis.synonym.SynonymMap;
import org.apache.lucene.analysis.synonym.WordnetSynonymParser;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.similarities.Similarity;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.document.Document;

public class Queries {
    public static WordnetSynonymParser parser;
    public static SynonymMap synonymMap;
    public static ArrayList<String> ProcessQueryFile(String path, String mode) throws IOException, ParseException, java.text.ParseException{
        //System.out.println("here");
        buildQueryExpansinParser();
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
                for(int i=0; i< 3; i++){
                expandQuery(queries.get(i));
                }
            }
            return queries;
        } catch (IOException e) {
            System.err.println("Error: " + e.getMessage());
        }
        return null;
    }

    private static ArrayList<String> extractAll(String filePath) throws IOException {
        ArrayList<String> textArray = new ArrayList<String>();
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            StringBuilder text = new StringBuilder();

            String line;
            while ((line = reader.readLine()) != null) {
               // line = line.trim();
                //System.out.println(line);
                if (line.startsWith("<top>")) {
                    text.setLength(0); //new doc
                    System.out.println("new document");
                    line = line.substring("<top>".length()).trim();
                }
                if (line.startsWith("<narr>")) {
                    line = line.substring("<narr> Narrative:".length()).trim(); //get rid of first line with the tag
                }
                if (line.startsWith("<desc>")) {
                    line = line.substring("<desc> Description:".length()).trim(); //get rid of first line with the tag
                }
                String lineNew = line.replaceAll("[^\\p{L}\\s]", "");
                text.append(lineNew);
                if (line.startsWith("</top>")) {
                    text.delete(text.length() - "</top>".length(), text.length());
                    System.out.println("Document Text:\n" + text.toString().trim());
                    textArray.add(text.toString().trim());
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
                    line = line.replaceAll("[^a-zA-Z]", "");
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

private static ArrayList<String> extractTitle(String filePath) throws IOException {
        ArrayList<String> textArray = new ArrayList<String>();
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {

            String line;
            while ((line = reader.readLine()) != null) {
               // line = line.trim();
                //System.out.println(line);
                if (line.startsWith("<top>")) {
                    System.out.println("new document");
                }

                if (line.startsWith("<title>")) {
                    line = line.substring("<title>".length()).trim();
                    line = line.replaceAll("[^a-zA-Z]", "");
                    textArray.add(line);
                    System.out.println(line);
                }
                // if (line.startsWith("</top>")) {
                // }
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
                // line = line.trim();
                    //System.out.println(line);
                    if (line.startsWith("<top>")) {
                        text.setLength(0); //new doc
                        System.out.println("new document");
                    }

                    if (line.startsWith("<narr>")) {
                        inNarr = true;
                        line = line.substring("<narr> Narrative:".length()).trim(); //get rid of first line with the tag
                    }
                    if (inNarr) {
                        line = line.replaceAll("[^a-zA-Z]", "");
                        text.append(line);
                        System.out.println("line added " + line);
                        if (line.startsWith("</top>")) {
                            inNarr = false;
                            text.delete(text.length() - "</top>:".length(), text.length()); 
                            System.out.println("Narrative Text:\n" + text.toString().trim());
                            textArray.add(text.toString().trim());
                        }
                    }
                    // if (line.startsWith("</top>")) {
                    // }
                }
            }
            return textArray;
    }

    public static void buildQueryExpansinParser () throws IOException, java.text.ParseException {
        try {
            // Creating a WordnetSynonymParser instance
            parser = new WordnetSynonymParser(true, true, new StandardAnalyzer());

            // Parsing the synonym file
            parser.parse(new FileReader("src\\main\\resources\\wn_s.pl"));

            // Building the synonym map
            synonymMap = parser.build();


        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void expandQuery(String query) throws IOException{
        String tempText = "";
        StandardAnalyzer analyzer = new StandardAnalyzer();

        // TokenStream to process the query
        TokenStream tokenStream = analyzer.tokenStream("TEXT", new StringReader(query));

        // Creating a SynonymFilter using the obtained SynonymMap
        TokenStream synonymTokenStream = new SynonymFilter(tokenStream, synonymMap, true);

        // Accessing the tokens after synonym expansion
        synonymTokenStream.reset();
        CharTermAttribute termAttribute = synonymTokenStream.addAttribute(CharTermAttribute.class);

        while (synonymTokenStream.incrementToken()) {
            String expandedTerm = termAttribute.toString();
            tempText += " " + expandedTerm;
        }
        System.out.println(tempText);
        tempText = "";
        // Closing token streams
        synonymTokenStream.close();
        analyzer.close();
    }
    

    
    // add index searcher to generate score documents
    public static ArrayList<String> QueryIndex(ArrayList<customQuery> queries, Similarity similarity, Analyzer analyzer) throws IOException, ParseException {

        Directory directory = FSDirectory.open(Paths.get("index2"));

        DirectoryReader directoryReader = DirectoryReader.open(directory);

        IndexSearcher searcher = new IndexSearcher(directoryReader);
        searcher.setSimilarity(similarity);
        ArrayList<String> results = new ArrayList<>();

        // todo:  need to be changed based on the indexed document field
        MultiFieldQueryParser queryParser = new MultiFieldQueryParser(new String[]{"head", "text"}, analyzer);
        queryParser.setAllowLeadingWildcard(true);
        
        int queryCount = 1;
        
        for(customQuery query : queries){
            Query qry = queryParser.parse(QueryParser.escape(query.Text));

            TopDocs topDocs = searcher.search(qry, 1000);
            ScoreDoc[] bestHits = topDocs.scoreDocs;

            for(ScoreDoc hit: bestHits) {
                Document doc = searcher.doc(hit.doc);
                String result = String.format("%03d 0 %s 0 %f STANDARD%n", queryCount, doc.get("id"), hit.score);
                results.add(result);
                System.out.println(result);
            }
            queryCount++;
        }
        directoryReader.close();
        directory.close();
        return results;
    }

}
