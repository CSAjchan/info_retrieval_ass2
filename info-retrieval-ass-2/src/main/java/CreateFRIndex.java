import java.io.IOException;
import java.nio.file.Paths;
import java.nio.file.Files;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.TextField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.search.similarities.BM25Similarity;
import org.apache.lucene.search.similarities.BooleanSimilarity;
import org.apache.lucene.search.similarities.ClassicSimilarity;
import org.apache.lucene.search.similarities.LMDirichletSimilarity;
import org.apache.lucene.search.similarities.MultiSimilarity;
import org.apache.lucene.search.similarities.Similarity;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

public class CreateFRIndex {
    private static String INDEX_DIRECTORY = "index2";

    public static void main(String path) throws IOException {

        Analyzer analyzer = new EnglishAnalyzer();
        Directory directory = FSDirectory.open(Paths.get(INDEX_DIRECTORY));
        IndexWriterConfig config = new IndexWriterConfig(analyzer);

        String model = "BM25_LMDirichlet";

        switch (model) {
            case "BM25":
                config.setSimilarity(new BM25Similarity(1.5f,0.75f));
                break;
            case "Classic":
                config.setSimilarity(new ClassicSimilarity());
                break;
            case "LMDirichlet":
                config.setSimilarity(new LMDirichletSimilarity());
                break;
            case "Boolean":
                config.setSimilarity(new BooleanSimilarity());
                break;
            case "BM25_Classic":
                config.setSimilarity(new MultiSimilarity(new Similarity[]{new BM25Similarity(), new ClassicSimilarity()}));
                break;
            case "Classic_LMDirichlet":
                config.setSimilarity(new MultiSimilarity(new Similarity[]{new ClassicSimilarity(), new LMDirichletSimilarity()}));
                break;
            case "BM25_LMDirichlet":
                config.setSimilarity(new MultiSimilarity(new Similarity[]{new BM25Similarity(), new LMDirichletSimilarity()}));
                break;
        }
        
        IndexWriter iwriter = new IndexWriter(directory, config);

        // args[0] is the path to the directory containing all FR files
        Files.walk(Paths.get(path))
            .filter(Files::isRegularFile).parallel()
            .forEach(filePath -> {
                try {
                    List<String> lines = Files.readAllLines(filePath);
                    String content = String.join("\n", lines);
                    processContent(content, iwriter);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });

        iwriter.close();
        directory.close();
    }

    private static void processContent(String content, IndexWriter iwriter) throws IOException {
        Pattern pattern = Pattern.compile("<DOC>(.*?)</DOC>", Pattern.DOTALL);
        Matcher matcher = pattern.matcher(content);

        while (matcher.find()) {
            String docContent = matcher.group(1);
            Document doc = new Document();
            addField(doc, "DOCNO", docContent, "<DOCNO>(.*?)</DOCNO>");
            addField(doc, "PARENT", docContent, "<PARENT>(.*?)</PARENT>");
            addField(doc, "TEXT", docContent, "<TEXT>(.*?)</TEXT>");

            // Additional fields based on the Federal Register dataset
            addField(doc, "USDEPT", docContent, "<USDEPT>(.*?)</USDEPT>");
            addField(doc, "AGENCY", docContent, "<AGENCY>(.*?)</AGENCY>");
            addField(doc, "USBUREAU", docContent, "<USBUREAU>(.*?)</USBUREAU>");
            addField(doc, "DOCTITLE", docContent, "<DOCTITLE>(.*?)</DOCTITLE>");
            addField(doc, "ADDRESS", docContent, "<ADDRESS>(.*?)</ADDRESS>");
            addField(doc, "FURTHER", docContent, "<FURTHER>(.*?)</FURTHER>");
            addField(doc, "SUMMARY", docContent, "<SUMMARY>(.*?)</SUMMARY>");
            addField(doc, "ACTION", docContent, "<ACTION>(.*?)</ACTION>");
            addField(doc, "SIGNER", docContent, "<SIGNER>(.*?)</SIGNER>");
            addField(doc, "SIGNJOB", docContent, "<SIGNJOB>(.*?)</SIGNJOB>");
            addField(doc, "SUPPLEM", docContent, "<SUPPLEM>(.*?)</SUPPLEM>");
            addField(doc, "BILLING", docContent, "<BILLING>(.*?)</BILLING>");
            addField(doc, "FRFILING", docContent, "<FRFILING>(.*?)</FRFILING>");
            addField(doc, "DATE", docContent, "<DATE>(.*?)</DATE>");
            addField(doc, "CFRNO", docContent, "<CFRNO>(.*?)</CFRNO>");
            addField(doc, "RINDOCK", docContent, "<RINDOCK>(.*?)</RINDOCK>");

            // Optional fields (may contain multiple occurrences within a document), something to look into
            addField(doc, "TABLE", docContent, "<TABLE>(.*?)</TABLE>");
            addField(doc, "FOOTNOTE", docContent, "<FOOTNOTE>(.*?)</FOOTNOTE>");
            addField(doc, "FOOTCITE", docContent, "<FOOTCITE>(.*?)</FOOTCITE>");
            addField(doc, "FOOTNAME", docContent, "<FOOTNAME>(.*?)</FOOTNAME>");

            // Exclude IMPORT tag as it's probably not useful for retrieval i think

            iwriter.addDocument(doc);
        }
    }

    private static void addField(Document doc, String fieldName, String content, String regex) {
        Pattern pattern = Pattern.compile(regex, Pattern.DOTALL);
        Matcher matcher = pattern.matcher(content);
        if (matcher.find()) {
            String fieldContent = matcher.group(1).trim();
            if (!fieldContent.isEmpty()) {
                Field field = fieldName.equals("DOCNO") ? new StringField(fieldName, fieldContent, Field.Store.YES) :
                                                         new TextField(fieldName, fieldContent, Field.Store.YES);
                doc.add(field);
            }
        }
    }
}


