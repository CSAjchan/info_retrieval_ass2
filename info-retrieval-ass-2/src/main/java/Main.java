import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.SimpleAnalyzer;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.similarities.BM25Similarity;
import org.apache.lucene.search.similarities.ClassicSimilarity;
import org.apache.lucene.search.similarities.LMDirichletSimilarity;
import org.apache.lucene.search.similarities.Similarity;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;

public class Main {
    public static final int MAX_HITS_PER_PAGE = 50;
    public static final String ID = ".I";
    public static final String TITLE = ".T";
    public static final String AUTHOR = ".A";
    public static final String BIBLIOGRAPHY = ".B";
    public static final String TEXT = ".W";
    public static final String INDEX_PATH = "indexFolder";

    public static ArrayList<customDocument> ReadDocuments(String path){
        ArrayList<customDocument> documents = new ArrayList<>();
        InputStream inputStream = Main.class.getResourceAsStream(path);
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));

        try {
            String line = "";
            int idIndex = 0;
            String documentLines = "";

            //go through all the lines find Document lines from .I to .I send each document lines into a Document parser
            while((line = bufferedReader.readLine()) != null){
                if(line.startsWith(ID) && idIndex != 0){
                    documents.add(ParseDocument(documentLines));
                    documentLines = "";
                    documentLines = documentLines + " " + line;
                }
                documentLines = documentLines + " " + line;
                idIndex += 1;
            }
            documents.add(ParseDocument(documentLines));
        }
        catch (IOException e){
            System.out.println(e);
        }

        return documents;
    }

    public static ArrayList<customQuery> ReadQueries(String path){
        ArrayList<customQuery> queries = new ArrayList<>();
        InputStream inputStream = Main.class.getResourceAsStream(path);
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));

        try {
            String line = "";
            int idIndex = 0;
            String queryLines = "";

            while((line = bufferedReader.readLine()) != null){
                if(line.startsWith(ID) && idIndex != 0){
                    queries.add(ParseQuery(queryLines));
                    queryLines = "";
                    queryLines = queryLines + " " + line;
                }
                queryLines = queryLines + " " + line;
                idIndex += 1;
            }
            queries.add(ParseQuery(queryLines));
        }
        catch (IOException e){
            System.out.println(e);
        }

        return queries;
    }

    public static customDocument ParseDocument(String documentLines){
        int id = Integer.parseInt(getSubstring(documentLines, ID, TITLE).split(" ")[1]);
        String title = getSubstring(documentLines, TITLE, AUTHOR);
        String author = getSubstring(documentLines, AUTHOR, BIBLIOGRAPHY);
        String bibliography = getSubstring(documentLines, BIBLIOGRAPHY, TEXT);
        String text = getSubstring(documentLines, TEXT);

        return new customDocument(id, title, author, bibliography, text);
    }

    public static customQuery ParseQuery(String queryLines){
        int id = Integer.parseInt(getSubstring(queryLines, ID, TEXT).split(" ")[1]);
        String text = getSubstring(queryLines, TEXT);

        return new customQuery(id, text);
    }

    public static String getSubstring(String lines, String substringStart, String substringEnd){
        int startingIndexOfString = lines.indexOf(substringStart);
        int endingIndexOfString = lines.indexOf(substringEnd);

        return lines.substring(startingIndexOfString, endingIndexOfString);
    }

    public static String getSubstring(String lines, String substringStart){
        int startingIndexOfString = lines.indexOf(substringStart);

        return lines.substring(startingIndexOfString);
    }

    public static void DocumentIndex(ArrayList<customDocument> documents, Similarity similarity, Analyzer analyzer) throws IOException {
        IndexWriterConfig config = new IndexWriterConfig(analyzer);
        config.setSimilarity(similarity);
        config.setOpenMode(IndexWriterConfig.OpenMode.CREATE);
        Directory directory = FSDirectory.open(Paths.get(INDEX_PATH));
        IndexWriter indexWriter = new IndexWriter(directory, config);

        for(customDocument document : documents){
            Document luceneDocument = new Document();

            luceneDocument.add(new StringField("id", String.valueOf(document.Id), Field.Store.YES));
            luceneDocument.add(new TextField("title", document.Title, Field.Store.YES));
            luceneDocument.add(new TextField("author", document.Author, Field.Store.YES));
            luceneDocument.add(new TextField("bibliography", document.Bibliography, Field.Store.YES));
            luceneDocument.add(new TextField("content", document.Text, Field.Store.YES));

            indexWriter.addDocument(luceneDocument);
        }

        indexWriter.close();
        directory.close();
    }

    public static ArrayList<String> QueryIndex(ArrayList<customQuery> queries, Similarity similarity, Analyzer analyzer) throws IOException, ParseException {
        Directory directory = FSDirectory.open(Paths.get(INDEX_PATH));
        DirectoryReader directoryReader = DirectoryReader.open(directory);
        IndexSearcher searcher = new IndexSearcher(directoryReader);
        searcher.setSimilarity(similarity);

        ArrayList<String> results = new ArrayList<>();
        MultiFieldQueryParser queryParser = new MultiFieldQueryParser(new String[]{"title", "author", "bibliography", "content"}, analyzer);
        queryParser.setAllowLeadingWildcard(true);

        int queryCount = 1;
        for(customQuery query : queries){
            Query qry = queryParser.parse(query.Text);

            TopDocs topDocs = searcher.search(qry, MAX_HITS_PER_PAGE);
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

    public static void main(String[] args) throws IOException, ParseException {
        System.out.println("123");
//        ArrayList<customDocument> documents = ReadDocuments("/cran.all.1400");
//        ArrayList<customQuery> queries = ReadQueries("/cran.qry");
        //documents.forEach(document -> System.out.println(document.Id + " " + document.Author));
        //System.out.println(documents.size());
        //queries.forEach(query -> System.out.println(query.Id + " " + query.Text));
        //System.out.println(queries.size());

//        String analyzerName = "EnglishAnalyzer";
//        Analyzer analyzer = new EnglishAnalyzer();
//        String analyzer2Name = "SimpleAnalyzer";
//        Analyzer analyzer2 = new SimpleAnalyzer();
//
//        HashMap<String, Similarity> similarities = new HashMap<>();
//        similarities.put("ClassicSimilarity", new ClassicSimilarity());
//        similarities.put("BM25Similarity", new BM25Similarity());
//        similarities.put("LMDirichletSimilarity", new LMDirichletSimilarity());
//
//        for(HashMap.Entry<String, Similarity> similarity : similarities.entrySet()){
//            DocumentIndex(documents, similarity.getValue(), analyzer);
//
//            ArrayList<String> results = QueryIndex(queries, similarity.getValue(), analyzer);
//            BufferedWriter writer = new BufferedWriter(
//                    new FileWriter(String.format("results/%s", similarity.getKey() + "_" + analyzerName)));
//            for(String line : results) {
//                writer.write(line);
//            }
//
//            writer.close();
//        }
//
//        for(HashMap.Entry<String, Similarity> similarity : similarities.entrySet()){
//            DocumentIndex(documents, similarity.getValue(), analyzer2);
//
//            ArrayList<String> results = QueryIndex(queries, similarity.getValue(), analyzer2);
//            BufferedWriter writer = new BufferedWriter(
//                    new FileWriter(String.format("results/%s", similarity.getKey() + "_" + analyzer2Name)));
//            for(String line : results) {
//                writer.write(line);
//            }
//
//            writer.close();
//        }
    }
}
