import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.sqs.SqsClient;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class ResultsCollector implements Runnable {
    String MGR_LA_SQS_url;
    String WKR_MGR_SQS_url;
    private String bucket;
    private AtomicInteger pendingTasksCount;
    ConcurrentHashMap<String, UserTask> userTasks;
    private final SqsClient sqsClient;
    private final S3Client s3;
    public ResultsCollector(String mgr_la_sqs_url, String wkr_mgr_sqs_url, String bucket, ConcurrentHashMap<String, UserTask> userTasks, AtomicInteger pendingTasksCount) {
        MGR_LA_SQS_url = mgr_la_sqs_url;
        WKR_MGR_SQS_url = wkr_mgr_sqs_url;
        this.bucket = bucket;
        this.pendingTasksCount = pendingTasksCount;
        this.userTasks = userTasks;
        Region region = Region.US_EAST_1;
        sqsClient = SqsClient.builder()
                .region(region)
                .build();
        s3 = S3Client.builder()
                .region(region)
                .build();
    }

    @Override
    public void run() {
        while (!Thread.interrupted()){
            for (String k:
                 userTasks.keySet()) {
                UserTask ut = userTasks.get(k);
                if(!ut.isDone()){
                    for (int i = 0; i < ut.size(); i++) {
                        ut.setDone(i);
                    }
                    String summary = "POS: no_where short_desc\n" +
                            "CONSTITUENCY: far_no_where very_short_desc\n";
                    String summLoc = "summary" + System.currentTimeMillis();
                    Utils.sendFileString(s3, bucket, summLoc, summary);
                    Utils.sendMsg(sqsClient, MGR_LA_SQS_url, summLoc);
                    pendingTasksCount.decrementAndGet();
                    System.out.println("RC done handling a usertask: "+k);
                }
            }
        }
        System.out.println("RC stopped");
    }
}
