import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.similarities.Similarity;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import java.io.*;
import java.nio.file.Paths;
import java.util.ArrayList;

public class Main {
    public static void main(String[] args) throws IOException, ParseException, java.text.ParseException {
        long start = System.currentTimeMillis();
        CreateFTIndex.main("src\\main\\resources\\AssignmentTwo\\ft");
	    System.out.println("ft done");
	    CreateLATimesIndex.main("src\\main\\resources\\AssignmentTwo\\latimes");
        System.out.println("latimes done");
        CreateFBISIndex.main("src\\main\\resources\\AssignmentTwo\\fbis");
        System.out.println("fbis done");
        CreateFRIndex.main("src\\main\\resources\\AssignmentTwo\\fr94");
        System.out.println("fr94 done");
        long finish = System.currentTimeMillis();
        long timeElapsed = (finish - start)/1000;
        System.out.println("time elapsed for all in seconds: " + timeElapsed);
        
        ArrayList<String> queries = Queries.ProcessQueryFile("src\\main\\resources\\topicFolder\\topics", "all");
        System.out.println(queries.size());

        String indexDir = "index2";  // path to index
        Analyzer analyzer = new EnglishAnalyzer();
		Directory directory = FSDirectory.open(Paths.get(indexDir));
		DirectoryReader ireader = DirectoryReader.open(directory);
		IndexSearcher isearcher = new IndexSearcher(ireader);

        // the best one
        Similarity bm25LMSimilarity = RetrieveModel.getModel("BM25_LMDirichletSimilarity");
        isearcher.setSimilarity(bm25LMSimilarity);    

	    
        QueryParser parser = new QueryParser("TEXT", analyzer);

        String filePath = "src\\main\\resources\\results.txt";
        FileWriter writer = new FileWriter(filePath);
        int index =401;
        for(String query : queries){
            Query text = parser.parse(query);

			ScoreDoc[] hits = isearcher.search(text, 1000).scoreDocs;
            for (int j = 0; j < hits.length; j++)
            {
              Document hitDoc = isearcher.doc(hits[j].doc);
              System.out.println(j + ") " + hitDoc.get("DOCNO") + " " + hits[j].score);
              writer.write(index + " 0 " +  hitDoc.get("DOCNO") + " " + (j+1) + " " + hits[j].score  + " STANDARD\n");
            }
          index++;
        }
        writer.close();
    }
}
