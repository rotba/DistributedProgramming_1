import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.Message;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class TaskReceiver implements Runnable {
    private final S3Client s3;
    private String bucket;
    private Map<String,UserTask> userTasks;
    private AtomicInteger pendingTasksCount;
    private final SqsClient sqsClient;

    private String LA_MGR_SQS_url;
    private String MGR_WKR_SQS_url;
    public TaskReceiver(String la_mgr_sqs_url, String mgr_wkr_sqs_url, String bucket, ConcurrentHashMap<String, UserTask> userTasks, AtomicInteger pendingTasksCount) {
        this.bucket = bucket;
        this.userTasks = userTasks;
        this.pendingTasksCount = pendingTasksCount;
        Region region = Region.US_EAST_1;
        sqsClient = SqsClient.builder()
                .region(region)
                .build();
        s3 = S3Client.builder()
                .region(region)
                .build();
        LA_MGR_SQS_url = la_mgr_sqs_url;
        MGR_WKR_SQS_url= mgr_wkr_sqs_url;
    }

    @Override
    public void run() {
        System.out.println("TR start");
        boolean terminated = false;
        while(!Thread.interrupted()){
            List<Message> msgs = null;
            try {
                msgs = Utils.waitForMessagesFrom(sqsClient, LA_MGR_SQS_url, "LA->TR", Integer.MAX_VALUE);
            } catch (InterruptedException e) {
                return;
            }
            for (Message msg:
                 msgs) {
                String body = msg.body();
                if(body.contains(Common.TERMINATE_MSG)){
                    System.out.println("TR term with "+body);
                    Utils.deleteMsgs(sqsClient, LA_MGR_SQS_url , msgs);
                    return;
                }
                String taskDescriptionLoc= body;
                pendingTasksCount.incrementAndGet();
                System.out.println(String.format("TR got %s from LA", taskDescriptionLoc));
                UserTask newUserTask = new UserTask(Utils.getFileString(s3, bucket, taskDescriptionLoc).size());
                userTasks.put(taskDescriptionLoc,newUserTask);
                sendToWrkrs(taskDescriptionLoc, newUserTask.size());
                System.out.println(String.format("TaskReceiver got task: %s: %s", taskDescriptionLoc, newUserTask.toString()));
            }
            Utils.deleteMsgs(sqsClient, LA_MGR_SQS_url , msgs);
        }
    }

    private void sendToWrkrs(String taskDescriptionLoc, int size) {
        for (int i = 0; i < size; i++) {
            Utils.sendMsg(sqsClient, MGR_WKR_SQS_url, MgrWkrMsg.getString(taskDescriptionLoc,i), "TR->WKR");
        }
    }
}
