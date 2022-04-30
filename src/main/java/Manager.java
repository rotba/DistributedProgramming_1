import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.sqs.SqsClient;

import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class Manager {

    private static String ACCESS;
    static String MGR_WKR_SQS_url = null;
    static String WKR_MGR_SQS_url = null;//TODO init
    static Region region = Region.US_EAST_1;
    static String LA_MGR_SQS_url;
    static String MGR_LA_SQS_url;
    static String bucket;
    static int n;
    static public boolean IS_UP = false;
    public static String MGR_resourcePrefix = "xxMGRxx";
    static AtomicInteger pendingTasksCount = new AtomicInteger(0);
    private static String SECRET;
    private static String AMI;

    public static void mainMGR(String[] args) {
        IS_UP=true;
        parseARGS(args);
        System.out.println(Arrays.toString(new String[]{"MGR started args: ",AMI, SECRET, ACCESS, LA_MGR_SQS_url, MGR_LA_SQS_url, bucket, Integer.toString(n)}));

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
            AutoScaler autoScalerObject = AutoScaler.create()
                    .setAccess(ACCESS)
                    .setSecret(SECRET)
                    .setBucket(bucket)
                    .setMGR_WKR_SQS_url(MGR_WKR_SQS_url)
                    .setWKR_MGR_SQS_url(WKR_MGR_SQS_url)
                    .setN(n)
                    .setWrkrAmi(AMI)
                    .setUserTasks(userTasks)
                    .build();
            Thread autoScaler =new Thread(autoScalerObject);
            Thread resultsCollector = new Thread(new ResultsCollector(MGR_LA_SQS_url, WKR_MGR_SQS_url, bucket, userTasks, pendingTasksCount));
            taskReciever.start();
            resultsCollector.start();
            autoScaler.start();;
            taskReciever.join();
            while (pendingTasksCount.get() >0){}
            resultsCollector.interrupt();
            resultsCollector.join();
            autoScalerObject.terminate();
            autoScaler.join();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            //clear all the resources
            Utils.purgeQ(sqsClient, MGR_WKR_SQS_url);
            Utils.purgeQ(sqsClient, WKR_MGR_SQS_url);
        }
        System.out.println("Manager finished");
    }

    private static void init_resources(SqsClient sqsClient) {
        MGR_WKR_SQS_url = Utils.createUniqueQueue(sqsClient, MGR_resourcePrefix +"MGRxWKR");
        WKR_MGR_SQS_url = Utils.createUniqueQueue(sqsClient, MGR_resourcePrefix +"WKRxMGR");
    }

    private static void parseARGS(String[] args) {
        ACCESS=args[Common.MGR_IDX.ACCESS.idx];
        SECRET=args[Common.MGR_IDX.SECRET.idx];
        AMI=args[Common.MGR_IDX.AMI.idx];
        bucket = args[Common.MGR_IDX.BKT.idx];
        n = Integer.parseInt(args[Common.MGR_IDX.N.idx]);
        LA_MGR_SQS_url = args[Common.MGR_IDX.LA_MGR_SQS.idx];
        MGR_LA_SQS_url = args[Common.MGR_IDX.MGR_LA_SQS.idx];
    }
}
