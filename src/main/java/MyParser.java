import edu.stanford.nlp.parser.lexparser.LexicalizedParser;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

public class MyParser {

    public static Map<String, String> parsersLexicalParser;
    static {
        parsersLexicalParser = new HashMap<>();
        parsersLexicalParser.put("POS", "wordsAndTags");
        parsersLexicalParser.put("CONSTITUENCY", "typedDependencies");
        parsersLexicalParser.put("DEPENDENCY", "penn");
    }

    public static String parse(String taskType, String inPath) throws MyParserException {
        try{
            LexicalizedParser.main(new String[]{"-retainTMPSubcategories","-writeOutputFiles", "-maxLength","60","-outputFormat", parsersLexicalParser.get(taskType), "edu/stanford/nlp/models/lexparser/englishPCFG.ser.gz", inPath});
            return inPath+".stp";
        } catch (Exception e){
            System.err.println("MyParser: GenExc");
            e.printStackTrace();
            throw new MyParserException(e);
        } catch (OutOfMemoryError e){
            System.err.println("MyParser: OOM");
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
