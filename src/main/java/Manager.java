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
import java.util.concurrent.atomic.AtomicInteger;

public class Manager {

    static String MGR_WKR_SQS_url = null;
    static String WKR_MGR_SQS_url = null;//TODO init
    static Region region = Region.US_EAST_1;
    static String LA_MGR_SQS_url;
    static String MGR_LA_SQS_url;
    static String bucket;
    static String n;
    static public boolean IS_UP = false;
    public static String MGR_resourcePrefix = "__MGR__";
    static AtomicInteger pendingTasksCount = new AtomicInteger(0);
    public static void mainMGR(String[] args) {
        IS_UP=true;
        parseARGS(args);
        System.out.println(Arrays.toString(new String[]{"MGR started args: ",LA_MGR_SQS_url, MGR_LA_SQS_url, bucket, n}));

        SqsClient sqsClient = SqsClient.builder()
                .region(region)
                .build();
        S3Client s3 = S3Client.builder()
                .region(region)
                .build();

        ConcurrentHashMap<String, UserTask> userTasks = new ConcurrentHashMap<>();
        try{
            init_resources(sqsClient);
            Thread taskReciever = new Thread(new TaskReceiver(LA_MGR_SQS_url, MGR_WKR_SQS_url, bucket, userTasks, pendingTasksCount));
            Thread resultsCollector = new Thread(new ResultsCollector(MGR_LA_SQS_url, WKR_MGR_SQS_url, bucket, userTasks, pendingTasksCount));
            taskReciever.start();
            resultsCollector.start();
            taskReciever.join();
            while (pendingTasksCount.get() >0){}
            resultsCollector.interrupt();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            //clear all the resources
            Utils.deleteAllQs(sqsClient, MGR_resourcePrefix);
        }
        System.out.println("Manager finished");
    }

    private static void init_resources(SqsClient sqsClient) {
        MGR_WKR_SQS_url = Utils.createUniqueQueue(sqsClient, MGR_resourcePrefix +"MGR_WKR");
    }

    private static void parseARGS(String[] args) {
        LA_MGR_SQS_url = args[0];
        MGR_LA_SQS_url = args[1];
        bucket = args[2];
        n = args[3];
    }
}
