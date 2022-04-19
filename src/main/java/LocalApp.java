import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.Message;

import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.List;

public class LocalApp {
    public static void mainLA(String[] args){
        String input = args[0];
        String output = args[1];
        String n = args[2];
        boolean terminate = args.length>3;

        System.out.println(Arrays.toString(new String[]{"LA args: ", input, output, n , Boolean.toString(terminate)}));
        String LA_MGR_SQS_url = null;
        String MGR_LA_SQS_url = null;
        String bucket = null;
        Region region = Region.US_EAST_1;
        SqsClient sqsClient = SqsClient.builder()
                .region(region)
                .build();
        S3Client s3 = S3Client.builder()
                .region(region)
                .build();
        try{
            LA_MGR_SQS_url = Utils.createQueue(sqsClient, "LA_MGR"+System.currentTimeMillis());
            MGR_LA_SQS_url = Utils.createQueue(sqsClient, "MGR_LA"+System.currentTimeMillis());
            bucket = "bucket" + System.currentTimeMillis();
            Utils.createBucket(s3, bucket);
            Thread mgr = createMGR(LA_MGR_SQS_url, MGR_LA_SQS_url, bucket, n, terminate);
            String inputLoc = "inputLoc" + System.currentTimeMillis();
//            Utils.putFileInBucket(s3, bucket, inputLoc, input);
            Utils.sendMsg(sqsClient, LA_MGR_SQS_url, inputLoc);
            List<Message> msgs = Utils.waitForMessagesFrom(sqsClient, MGR_LA_SQS_url, "MGR");
            String resultS3Loc = msgs.get(0).body();
            String summaryFile = Utils.getFileString(s3, bucket, resultS3Loc);
            generateHTML(summaryFile, output);
            mgr.join();
            System.out.println(String.format("LA complete"));
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            Utils.deleteAllQs(sqsClient);
            Utils.deleteAllBuckets(s3);
        }

    }

    public static void generateHTML(String summaryFile, String output) {
        List<String> lines = Arrays.asList(summaryFile.split("\\s*\n\\s*"));
        Document doc = Jsoup.parse("<html></html>");
        doc.body().addClass("body-styles-cls");
        for (String s:
             lines) {
            doc.body().appendElement("div").text(s);
        }
        try {
            FileWriter myWriter = new FileWriter(output);
            myWriter.write(doc.toString());
            myWriter.close();
        } catch (IOException e) {
            System.out.println("An error occurred.");
            e.printStackTrace();
        }
    }

    private static Thread createMGR(String la_mgr_sqs_url, String mgr_la_sqs_url, String bucket, String n, boolean terminate) {
        String[] args = new String[]{la_mgr_sqs_url, mgr_la_sqs_url, bucket, n, terminate ? "terminate": ""};
        Thread mgr = new Thread(()->Manager.mainMGR(args));
        mgr.start();
        return mgr;
    }
}
