import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.Message;

import java.util.Arrays;
import java.util.List;

public class Manager {
    public static void mainMGR(String[] args) {
        String LA_MGR_SQS_url = args[0];
        String MGR_LA_SQS_url = args[1];
        String bucket = args[2];
        String n = args[3];
        boolean terminate = args.length>4 && args[4].equals("terminate");
        System.out.println(Arrays.toString(new String[]{"MGR started args: ",LA_MGR_SQS_url, MGR_LA_SQS_url, bucket, n}));
        Region region = Region.US_EAST_1;
        SqsClient sqsClient = SqsClient.builder()
                .region(region)
                .build();
        S3Client s3 = S3Client.builder()
                .region(region)
                .build();
        try{
            List<Message> msgs = Utils.waitForMessagesFrom(sqsClient, LA_MGR_SQS_url, "LA");
            String summary = "POS: no_where short_desc\n" +
                    "CONSTITUENCY: far_no_where very_short_desc\n";
            String summLoc = "summary" + System.currentTimeMillis();
            Utils.sendFileString(s3, bucket, summLoc, summary);
            Utils.sendMsg(sqsClient, MGR_LA_SQS_url, summLoc);
        }finally {
            //clear all the resources
        }
        System.out.println("Manager finished");
    }
}
