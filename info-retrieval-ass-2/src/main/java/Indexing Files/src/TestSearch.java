package assignment2;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.FSDirectory;
import java.nio.file.Paths;
import java.util.Scanner;

public class TestSearch {
    public static void main(String[] args) throws Exception {
        String indexDir = "latimes_index";  // path to index 
        DirectoryReader directoryReader = DirectoryReader.open(FSDirectory.open(Paths.get(indexDir)));
        IndexSearcher searcher = new IndexSearcher(directoryReader);

        try (Scanner scanner = new Scanner(System.in)) {
            System.out.println("Enter query:");

            String queryString = scanner.nextLine();
            QueryParser parser = new QueryParser("TEXT", new StandardAnalyzer());
            Query query = parser.parse(queryString);

            TopDocs results = searcher.search(query, 10);
            System.out.println("Total Hits: " + results.totalHits);

            for (ScoreDoc scoreDoc : results.scoreDocs) {
                Document doc = searcher.doc(scoreDoc.doc);
                System.out.println("DOCNO: " + doc.get("DOCNO"));
                // Add more fields if needed
            }
        }

        directoryReader.close();
    }
}

