import edu.stanford.nlp.parser.lexparser.LexicalizedParser;
import edu.stanford.nlp.pipeline.CoreDocument;
import edu.stanford.nlp.pipeline.CoreSentence;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.semgraph.SemanticGraphFactory;
import edu.stanford.nlp.simple.Document;
import edu.stanford.nlp.simple.Sentence;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class MyParser {
    private static Properties props = new Properties();
    static{
        props.setProperty("annotators", "tokenize,ssplit,pos,lemma,ner,parse,depparse,coref,kbp,quote");
        props.setProperty("coref.algorithm", "neural");
        props.setProperty("maxLength", "5");
    }
    private interface SentenceParser{
        public String parse(Sentence s);
    }
    public static Map<String, SentenceParser> parsers;
    static {
        parsers = new HashMap<>();
        parsers.put("POS", s->s.posTags().toString());
        parsers.put("CONSTITUENCY", s->s.posTags().toString());
        parsers.put("DEPENDENCY", s->s.posTags().toString());
    }

    public static String parse(String taskType, String inPath) throws MyParserException {
        try{
            String outpath = inPath+"_out";
            Document doc = new Document(readFile(inPath, StandardCharsets.US_ASCII));
            File fout = new File(outpath);
            FileOutputStream fos = new FileOutputStream(fout);
            BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(fos));
            for(Sentence s: doc.sentences()){
                bw.write(parsers.get(taskType).parse(s));
                bw.newLine();
            }
            return outpath;
        } catch (IOException e) {
            throw new MyParserException(e);
        }
    }

    static String readFile(String path, Charset encoding)
            throws IOException
    {
        byte[] encoded = Files.readAllBytes(Paths.get(path));
        return new String(encoded, encoding);
    }
}
