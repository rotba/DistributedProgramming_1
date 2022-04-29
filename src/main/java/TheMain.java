import edu.stanford.nlp.simple.Document;
import edu.stanford.nlp.simple.Sentence;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.sqs.SqsClient;

import java.util.Arrays;

public class TheMain {
    public static void main(String[] args) {
        String[] subArgs = Arrays.asList(args)
                .subList(1, args.length)
                .toArray(new String[0]);
        if(args[0].equals("LA")){
            LocalApp.mainLA(subArgs);
        }else if(args[0].equals("TEAR")){
            Utils.tearDown();
        }else if (args[0].equals("MLT")){
            String[] args1 = new String[]{"LA", "/home/rotemb271/Code/School/bgu/extern/DistributedProgramming_1/input-sample.txt", "/home/rotemb271/Code/School/bgu/extern/DistributedProgramming_1/out.html", "10"};
            String[] args2 = new String[]{"LA", "/home/rotemb271/Code/School/bgu/extern/DistributedProgramming_1/input-sample.txt", "/home/rotemb271/Code/School/bgu/extern/DistributedProgramming_1/out.html", "10" ,"terminate"};
            Thread t1 = new Thread(()->main(args1));
            Thread t2 = new Thread(()->main(args2));
            t1.start();
            try {
                t1.join();
                t2.start();
                t2.join();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }else if (args[0].equals("LST")){
            Utils.list();
        }else if(args[0].equals("CLR")){
            Utils.clearResources();
        }else if(args[0].equals("WKR")){
            Worker.wrkrMain(subArgs);
        }else if(args[0].equals("PSR")){
            Document doc = new Document("add your text here! It can contain multiple sentences.");
            for (Sentence sent : doc.sentences()) {  // Will iterate over two sentences
                // We're only asking for words -- no need to load any models yet
                System.out.println("The second word of the sentence '" + sent + "' is " + sent.word(1));
                // When we ask for the lemma, it will load and run the part of speech tagger
                System.out.println("The third lemma of the sentence '" + sent + "' is " + sent.lemma(2));
                // When we ask for the parse, it will load and run the parser
                System.out.println("The parse of the sentence '" + sent + "' is " + sent.parse());
                // ...
            }
        }else if(args[0].equals("HMSG")){
            Region region = Region.US_EAST_1;
            S3Client s3 = S3Client.builder()
                    .region(region)
                    .build();
            System.out.println(Worker.handleMsg(s3, "robarakbucket1651229518122","POS", "https://www.gutenberg.org/files/1659/1659-0.txt", 0, "loc"+System.currentTimeMillis()));
        }
    }
}
