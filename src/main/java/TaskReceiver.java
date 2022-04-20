import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.Message;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class TaskReceiver implements Runnable {
    private final S3Client s3;
    private String bucket;
    private Map<String,UserTask> userTasks;
    private final SqsClient sqsClient;

    private String LA_MGR_SQS_url;
    public TaskReceiver(String la_mgr_sqs_url, String mgr_wkr_sqs_url, String bucket, ConcurrentHashMap<String, UserTask> userTasks) {
        this.bucket = bucket;
        this.userTasks = userTasks;
        Region region = Region.US_EAST_1;
        sqsClient = SqsClient.builder()
                .region(region)
                .build();
        s3 = S3Client.builder()
                .region(region)
                .build();
        LA_MGR_SQS_url = la_mgr_sqs_url;
    }

    @Override
    public void run() {
        while(true){
            List<Message> msgs = Utils.waitForMessagesFrom(sqsClient, LA_MGR_SQS_url, "LA");
            for (Message msg:
                 msgs) {
                String body = msg.body();
                if(body.equals(Common.TERMINATE_MSG)){
                    System.out.println("TaskReceiver stops");
                    return;
                }
                System.out.println(String.format("TR got %s from LA", body));
                UserTask newUserTask = new UserTask(Utils.getFileString(s3, bucket, body).size());
                userTasks.put(body,newUserTask);
                System.out.println(String.format("TaskReceiver got task: %s: %s", body, newUserTask.toString()));
            }
            Utils.deleteMsgs(sqsClient, LA_MGR_SQS_url , msgs);
        }
    }
}
