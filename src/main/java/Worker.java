import software.amazon.awssdk.core.exception.AbortedException;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.Message;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class Worker {
    public volatile static String MGR_WKR_SQS_url;
    public volatile static String WKR_MGR_SQS_url;
    public volatile static String bucket;
    public static AtomicBoolean initialized = new AtomicBoolean(false);

    public static void wrkrMain(String[] args) {
        System.out.println("WKR start");
        parseARGS(args);
        Region region = Region.US_EAST_1;
        SqsClient sqsClient = SqsClient.builder()
                .region(region)
                .build();
        S3Client s3 = S3Client.builder()
                .region(region)
                .build();
        while(!Thread.interrupted()){
            List<Message> msgs;
            try {
                msgs = Utils.waitForMessagesFrom(sqsClient, MGR_WKR_SQS_url,"TR->WKR", 1);
            } catch (InterruptedException | AbortedException e) {
                break;
            }
            MgrWkrMsg msg = MgrWkrMsg.parse(msgs.get(0).body());
            String[] task = getTask(s3, msg.getFile(), msg.getIdx());
            Utils.deleteMsgs(sqsClient, MGR_WKR_SQS_url, msgs);
            Utils.sendMsg(sqsClient, WKR_MGR_SQS_url, WkrMgrMsg.getString(msg.getFile(), msg.getIdx(), task[0],task[1],task[1], ""), "WKR->RC");
        }
        System.out.println("WKR done");
    }

    private static String[] getTask(S3Client s3, String file, int idx) {
        List<String> allTasks = Utils.getFileString(s3, bucket, file);
        String task = allTasks.get(idx);
        System.out.println("WKR: task before parsing: "+task);
        String[] split = task.split("\\s+");
        System.out.println("WKR: task after parsing : "+split[0]+" "+split[1]);
        return split;
    }

    private static void parseARGS(String[] args) {
        boolean val;
        do {
            val = initialized.get();
            if(val== true){
                while (MGR_WKR_SQS_url ==null ||WKR_MGR_SQS_url==null|| bucket==null){
                    System.out.println("WKR: shouldnt be here for too long");
                }
                return;
            }
        }while (!initialized.compareAndSet(val, true));
        MGR_WKR_SQS_url=args[Common.WKR_IDX.MGR_WKR_SQS.idx];
        WKR_MGR_SQS_url=args[Common.WKR_IDX.WKR_MGR_SQS.idx];
        bucket =args[Common.WKR_IDX.BKT.idx];
    }
}
