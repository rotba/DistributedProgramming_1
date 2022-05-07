import software.amazon.awssdk.core.exception.AbortedException;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.Message;

import java.io.BufferedInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Paths;
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
        List<Message> msgs;
        try {
            msgs = Utils.waitForMessagesFrom(sqsClient, MGR_WKR_SQS_url,"TR->WKR", 1);
        } catch (InterruptedException | AbortedException e) {
            return;
        }
        MgrWkrMsg msg = new MgrWkrMsg(msgs.get(0).body());
        System.out.println(String.format("WKR got task %s", msg.toString()));
        String[] task = getTask(s3, msg.getFile(), msg.getIdx());
        WkrMgrMsg result = handleMsg(s3, bucket ,task[0], task[1], msg.getIdx(), msg.getFile());
        System.out.println("WKR done parsing");
        Utils.deleteMsgs(sqsClient, MGR_WKR_SQS_url, msgs);
        Utils.sendMsg(sqsClient, WKR_MGR_SQS_url, result.toString(), "WKR->RC", ""+System.currentTimeMillis());
        System.out.println("WKR done");
    }

    public static WkrMgrMsg handleMsg(S3Client s3, String theBucket,String taskType, String pasringSubject, int idx, String inputFile) {
        String inPath;
        String outPath = null;
        String outPutLoc;
        try{
            inPath = download(pasringSubject);
            outPath = MyParser.parse(taskType, inPath);
            outPutLoc = inputFile+"_"+Integer.toString(idx);
            Utils.putFileInBucket(s3, theBucket, outPutLoc, outPath);
        }catch (MyParserException| DownloadException e){
            return new WkrMgrMsg(inputFile,idx, taskType, pasringSubject, "NO_OUTPUT", e.getMessage());
        }
        return new WkrMgrMsg(inputFile,idx, taskType, pasringSubject, outPutLoc, "");
    }

    private static String download(String url) throws DownloadException {
        String name = Paths.get(url).getFileName().toString();
        try (BufferedInputStream in = new BufferedInputStream(new URL(url).openStream());
             FileOutputStream fileOutputStream = new FileOutputStream(name)) {
            byte dataBuffer[] = new byte[1024];
            int bytesRead;
            while ((bytesRead = in.read(dataBuffer, 0, 1024)) != -1) {
                fileOutputStream.write(dataBuffer, 0, bytesRead);
            }
        } catch (IOException e) {
            throw new DownloadException(e);
            // handle exception
        }
        return name;
    }

    private static String[] getTask(S3Client s3, String file, int idx) {
        List<String> allTasks = Utils.getFileString(s3, bucket, file);
        String task = allTasks.get(idx);
        String[] split = task.split("\\s+");
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
