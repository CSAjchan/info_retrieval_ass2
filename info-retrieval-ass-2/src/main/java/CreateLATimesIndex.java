import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

public class CreateLATimesIndex {
    private static String INDEX_DIRECTORY = "index2";

    public static void main(String path) throws IOException {

        Analyzer analyzer = new StandardAnalyzer();
        Directory directory = FSDirectory.open(Paths.get(INDEX_DIRECTORY));
        IndexWriterConfig config = new IndexWriterConfig(analyzer);
        IndexWriter iwriter = new IndexWriter(directory, config);

        Files.list(Paths.get(path))  
            .filter(Files::isRegularFile)
            .forEach(filePath -> {
                try {
                    String content = readFileContent(filePath);
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
            addField(doc, "DOCID", docContent, "<DOCID>(.*?)</DOCID>");
            addField(doc, "DATE", docContent, "<DATE>(.*?)</DATE>");
            addField(doc, "SECTION", docContent, "<SECTION>(.*?)</SECTION>");
            addField(doc, "LENGTH", docContent, "<LENGTH>(.*?)</LENGTH>");
            addField(doc, "TEXT", docContent, "<TEXT>(.*?)</TEXT>");
            addField(doc, "TYPE", docContent, "<TYPE>(.*?)</TYPE>");
            // Add more fields

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

    private static String readFileContent(Path filePath) throws IOException {
        byte[] fileBytes = Files.readAllBytes(filePath);
        return new String(fileBytes, StandardCharsets.UTF_8);
    }
}

