import java.io.IOException;
import java.nio.file.Paths;
import java.nio.file.Files;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.TextField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

public class CreateFTIndex {
    private static String INDEX_DIRECTORY = "index2";

    public static void main(String path) throws IOException {

        Analyzer analyzer = new EnglishAnalyzer();
        Directory directory = FSDirectory.open(Paths.get(INDEX_DIRECTORY));
        IndexWriterConfig config = new IndexWriterConfig(analyzer);

        String model = "BM25";

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
        
        config.setOpenMode(IndexWriterConfig.OpenMode.CREATE);
        IndexWriter iwriter = new IndexWriter(directory, config);

        // Assuming args[0] is the path to the directory containing all FT files
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
            addField(doc, "HEADLINE", docContent, "<HEADLINE>(.*?)</HEADLINE>");
            addField(doc, "TEXT", docContent, "<TEXT>(.*?)</TEXT>");
            addField(doc, "PUB", docContent, "<PUB>(.*?)</PUB>");
            addField(doc, "PAGE", docContent, "<PAGE>(.*?)</PAGE>");
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

