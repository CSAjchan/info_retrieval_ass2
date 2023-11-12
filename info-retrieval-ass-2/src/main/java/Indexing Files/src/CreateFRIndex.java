package assignment2;

import java.io.IOException;
import java.nio.file.Paths;
import java.nio.file.Files;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.TextField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

public class CreateFRIndex {
    private static String INDEX_DIRECTORY = "fr_index";

    public static void main(String[] args) throws IOException {
        if (args.length <= 0) {
            System.out.println("Expected Federal Register documents directory as input");
            System.exit(1);
        }

        Analyzer analyzer = new StandardAnalyzer();
        Directory directory = FSDirectory.open(Paths.get(INDEX_DIRECTORY));
        IndexWriterConfig config = new IndexWriterConfig(analyzer);
        config.setOpenMode(IndexWriterConfig.OpenMode.CREATE);
        IndexWriter iwriter = new IndexWriter(directory, config);

        // args[0] is the path to the directory containing all FR files
        Files.walk(Paths.get(args[0]))
            .filter(Files::isRegularFile)
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


