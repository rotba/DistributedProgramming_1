import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.Message;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

public class LocalApp {
    public static final String LA_TR_GID = "LA_TR";
    private static final String MGR_TAG = "MGR";
    public static final String MGR_JOB_CODE = "MGR";
    static String LA_MGR_SQS_url = null;
    static String MGR_LA_SQS_url = null;
    static String bucket = null;
    public static String LA_resourcePrefix = "xxLAxx";
    static String inputLoc;
    static Region region = Region.US_EAST_1;
    static SqsClient sqsClient;

    static S3Client s3;

    static Ec2Client ec2;

    static String input;
    static String output;
    static String n;
    static boolean terminate;

    static Thread tmgr;

    public static String LA_MGR_ID_PREF = LA_resourcePrefix + "LAxMGR";
    public static String MGR_LA_ID_PREF = LA_resourcePrefix + "MGRxLA";
    static String BUCKET_ID = "robarakbucket";

    static String SECRET;
    static String ACCESS;

    public static void mainLA(String[] args) {
        input = args[0];
        output = args[1];
        n = args[2];
        terminate = args.length > 3;

        System.out.println(Arrays.toString(new String[]{"LA args: ", input, output, n, Boolean.toString(terminate)}));
        sqsClient = SqsClient.builder()
                .region(region)
                .build();
        s3 = S3Client.builder()
                .region(region)
                .build();
        ec2 = Ec2Client.builder()
                .region(region)
                .build();
        try {
            read_credentials();
            init_resources();
            initMGR();
            Utils.putFileInBucket(s3, bucket, inputLoc, input);
            Utils.sendMsg(sqsClient, LA_MGR_SQS_url, inputLoc, "LA->TR", LA_TR_GID);
            if (terminate) {
                Utils.sendMsg(sqsClient, LA_MGR_SQS_url, Common.TERMINATE_MSG + System.currentTimeMillis(), "LA->TR", LA_TR_GID);
            }
            List<Message> msgs = Utils.waitForMessagesFrom(sqsClient, MGR_LA_SQS_url, "MGR->LA", 1);
            String resultS3Loc = msgs.get(0).body();
            List<String> summaryFile = Utils.getFileString(s3, bucket, resultS3Loc);
            generateHTML(summaryFile, output);
            if (terminate) {
                termMgr();
            }
            System.out.println(String.format("LA complete"));
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            if (terminate) {
                Utils.purgeQ(sqsClient, LA_MGR_SQS_url);
                Utils.purgeQ(sqsClient, MGR_LA_SQS_url);
                //Utils.emptyBucket(s3, bucket);
            }
        }

    }

    private static void termMgr() throws InterruptedException {
        if (Common.USE_THREAD_MGR) {
            tmgr.join();
        } else {
            tearMGREc2();
        }
    }

    private static void tearMGREc2() {
        List<String> mgrSingelton = Utils.getActiveEC2s(ec2, MGR_TAG);
        if (mgrSingelton.size() == 0) {
            System.err.println("A MGR should be active");
        } else {
            if (mgrSingelton.size() > 1) System.err.println("Only one MGR should be active");
            for (String mgrId :
                    mgrSingelton) {
                Utils.terminateInstance(ec2, mgrId);
            }
        }
    }

    private static void read_credentials() {
        try {
            File myObj = new File("/home/rotemb271/.aws/credentials");
            Scanner myReader = new Scanner(myObj);
            while (myReader.hasNextLine()) {
                String data = myReader.nextLine();
                if (data.contains("aws_access_key_id")) {
                    ACCESS = data.split("\\s+")[2];
                } else if (data.contains("aws_secret_access_key")) {
                    SECRET = data.split("\\s+")[2];
                }
            }
            myReader.close();
        } catch (FileNotFoundException e) {
            System.out.println("An error occurred.");
            e.printStackTrace();
            throw new RuntimeException(e);
        }
        System.out.println(ACCESS);
        System.out.println(SECRET);
    }

    private static void initMGR() {
        if (MGRisUp()) {
            return;
        }
        createMGR(LA_MGR_SQS_url, MGR_LA_SQS_url, bucket, n, terminate);
    }

    private static boolean MGRisUp() {
        if (Common.USE_THREAD_MGR) {
            return Manager.IS_UP;
        } else {
            return Utils.getActiveEC2s(ec2, MGR_TAG).size() > 0;
        }
    }

    private static void init_resources() {
        LA_MGR_SQS_url = Utils.createUniqueQueue(sqsClient, LA_MGR_ID_PREF);
        MGR_LA_SQS_url = Utils.createUniqueQueue(sqsClient, MGR_LA_ID_PREF);
        bucket = Utils.createUniqueBucket(s3, BUCKET_ID);
        inputLoc = "inputLoc" + System.currentTimeMillis();
    }

    public static void generateHTML(List<String> summaryFile, String output) {
        Document doc = Jsoup.parse("<html></html>");
        doc.body().addClass("body-styles-cls");
        for (String s :
                summaryFile) {
            doc.body().appendElement("div").text(s);
        }
        try {
            FileWriter myWriter = new FileWriter(output);
            myWriter.write(doc.toString());
            myWriter.close();
            System.out.println("OUTPUT at " + output);
        } catch (IOException e) {
            System.out.println("An error occurred.");
            e.printStackTrace();
        }
    }

    private static void createMGR(String la_mgr_sqs_url, String mgr_la_sqs_url, String bucket, String n, boolean terminate) {
        String[] args = new String[Common.MGR_IDX.LENGTH.idx];
        args[Common.MGR_IDX.LA_MGR_SQS.idx] = LA_MGR_SQS_url;
        args[Common.MGR_IDX.MGR_LA_SQS.idx] = MGR_LA_SQS_url;
        args[Common.MGR_IDX.AMI.idx] = Common.WKR_MGR_AMI;
        args[Common.MGR_IDX.BKT.idx] = bucket;
        args[Common.MGR_IDX.N.idx] = n;
        args[Common.MGR_IDX.SECRET.idx] = SECRET;
        args[Common.MGR_IDX.ACCESS.idx] = ACCESS;
        for (int i = 0; i < args.length; i++) {
            assert args[i] != null;
        }
        if (Common.USE_THREAD_MGR) {
            tmgr = new Thread(() -> Manager.mainMGR(args));
            tmgr.start();
        } else {
            CreateEC2Builder.builder()
                    .setArgs(args)
                    .setBucket(bucket)
                    .setSecret(SECRET)
                    .setAccess(ACCESS)
                    .setAmi(Common.WKR_MGR_AMI)
                    .setTagString(MGR_TAG)
                    .setEc2(ec2)
                    .setJobCode(MGR_JOB_CODE)
                    .build()
                    .create();
        }
    }
}
