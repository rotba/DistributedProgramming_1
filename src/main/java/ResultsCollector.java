import software.amazon.awssdk.core.exception.AbortedException;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.Message;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class ResultsCollector implements Runnable {
    public static final String RC_LA_GID = "RC_LA";
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
            List<Message> msgs;
            try {
                msgs = Utils.waitForMessagesFrom(sqsClient, WKR_MGR_SQS_url, "WKR->RC", Integer.MAX_VALUE);
            } catch (InterruptedException | AbortedException e) {
                return;
            }
            for (Message msg:
                 msgs) {
                WkrMgrMsg parsedMsg = new WkrMgrMsg(msg.body());
                System.out.println(String.format("RC got %s", parsedMsg.toString()));
                UserTask ut = userTasks.get(parsedMsg.getFile());
                int wkrTaskIdx = parsedMsg.getIdx();
                if(!ut.isDone(wkrTaskIdx)){
                    ut.setDone(wkrTaskIdx, parsedMsg.getRes(bucket));
                    Utils.deleteMsgs(sqsClient, WKR_MGR_SQS_url, Arrays.asList(msg));
                    if (ut.isDone()){
                        String resLoc = "RES" + parsedMsg.getFile();
                        Utils.sendFileString(s3, bucket, resLoc, ut.getRes());
                        Utils.sendMsg(sqsClient, MGR_LA_SQS_url, resLoc, "RC->LA", RC_LA_GID);
                        System.out.println("RC done handling a usertask: "+parsedMsg.getFile());
                        pendingTasksCount.decrementAndGet();
                    }
                }else{
                    Utils.deleteMsgs(sqsClient, WKR_MGR_SQS_url, Arrays.asList(msg));//might fail because the tasks can complete and interrupt will be sent
                }
            }
        }
        System.out.println("RC stopped");
    }
}
