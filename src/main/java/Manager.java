import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.Message;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Manager {

    static String MGR_WKR_SQS_url = null;
    static Region region = Region.US_EAST_1;
    static String LA_MGR_SQS_url;
    static String MGR_LA_SQS_url;
    static String bucket;
    static String n;
    public static void mainMGR(String[] args) {
        parseARGS(args);
        System.out.println(Arrays.toString(new String[]{"MGR started args: ",LA_MGR_SQS_url, MGR_LA_SQS_url, bucket, n}));

        SqsClient sqsClient = SqsClient.builder()
                .region(region)
                .build();
        S3Client s3 = S3Client.builder()
                .region(region)
                .build();
        String MGR_resourcePrefix = "__MGR__";
        ConcurrentHashMap<String, UserTask> userTasks = new ConcurrentHashMap<>();
        try{
            init_resources(sqsClient, MGR_resourcePrefix);
            Thread taskReciever = new Thread(new TaskReceiver(LA_MGR_SQS_url, MGR_WKR_SQS_url, bucket, userTasks));
            taskReciever.start();
            taskReciever.join();
            System.out.println("There are "+ userTasks.size()+ " user tasks");
            String summary = "POS: no_where short_desc\n" +
                    "CONSTITUENCY: far_no_where very_short_desc\n";
            String summLoc = "summary" + System.currentTimeMillis();
            Utils.sendFileString(s3, bucket, summLoc, summary);
            Utils.sendMsg(sqsClient, MGR_LA_SQS_url, summLoc);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            //clear all the resources
            Utils.deleteAllQs(sqsClient, MGR_resourcePrefix);
        }
        System.out.println("Manager finished");
    }

    private static void init_resources(SqsClient sqsClient, String MGR_resourcePrefix) {
        MGR_WKR_SQS_url = Utils.createQueue(sqsClient, MGR_resourcePrefix +"MGR_WKR"+System.currentTimeMillis());
    }

    private static void parseARGS(String[] args) {
        LA_MGR_SQS_url = args[0];
        MGR_LA_SQS_url = args[1];
        bucket = args[2];
        n = args[3];
    }
}
