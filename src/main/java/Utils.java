import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.*;
import software.amazon.awssdk.services.ec2.model.Tag;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.*;
import software.amazon.awssdk.core.waiters.WaiterResponse;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.waiters.S3Waiter;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

public class Utils {

    public static String createUniqueQueue(SqsClient sqsClient, String uniqueGivenName) {
        String uniqueName = uniqueGivenName + Common.FIFO_SUFFIX;
        try {
            String ans = getQueueIfExist(sqsClient, uniqueName);
            if (ans != null) {
                System.out.println("Q " + ans + " exits, returns it");
                return ans;
            }
            // snippet-start:[sqs.java2.sqs_example.create_queue]
            Map<QueueAttributeName, String> queueAttributes = new HashMap<>();
            queueAttributes.put(QueueAttributeName.FIFO_QUEUE, "true");
            queueAttributes.put(QueueAttributeName.VISIBILITY_TIMEOUT, "1800");
            CreateQueueRequest createQueueRequest = CreateQueueRequest.builder()
                    .queueName(uniqueName)
                    .attributes(queueAttributes)
                    .build();

            sqsClient.createQueue(createQueueRequest);
            // snippet-end:[sqs.java2.sqs_example.create_queue]

            // snippet-start:[sqs.java2.sqs_example.get_queue]
            GetQueueUrlResponse getQueueUrlResponse =
                    sqsClient.getQueueUrl(GetQueueUrlRequest.builder().queueName(uniqueName).build());
            String queueUrl = getQueueUrlResponse.queueUrl();
            System.out.println(String.format("Created queue url%s", queueUrl));
            return queueUrl;

        } catch (SqsException e) {
            System.err.println(e.awsErrorDetails().errorMessage());
        }
        return "";
        // snippet-end:[sqs.java2.sqs_example.get_queue]
    }

    private static String getQueueIfExist(SqsClient sqsClient, String name) {
        try {

            GetQueueUrlRequest getQueueRequest = GetQueueUrlRequest.builder()
                    .queueName(name)
                    .build();

            return sqsClient.getQueueUrl(getQueueRequest).queueUrl();

        } catch (SqsException e) {
            return null;
        }
    }

    private static String getQueueIfContains_DEPRECATED(SqsClient sqsClient, String subName) {
        try {
            ListQueuesRequest listQueuesRequest = ListQueuesRequest.builder().build();
            ListQueuesResponse listQueuesResponse = sqsClient.listQueues(listQueuesRequest);
            List<String> urls = listQueuesResponse.queueUrls();
            for (String url : urls) {
                if (url.contains(subName)) {
                    return url;
                }
            }

        } catch (SqsException e) {
            return null;
        }
        return null;
    }

    public static void deleteAllQs(SqsClient sqsClient, String prefix) {

        try {
            ListQueuesRequest listQueuesRequest = ListQueuesRequest.builder().queueNamePrefix(prefix).build();
            ListQueuesResponse listQueuesResponse = sqsClient.listQueues(listQueuesRequest);

            for (String url : listQueuesResponse.queueUrls()) {
                deleteQueue(sqsClient, url);
            }

        } catch (SqsException e) {
            System.err.println(e.awsErrorDetails().errorMessage());
            throw e;
        }
        // snippet-end:[sqs.java2.sqs_example.get_queue]
    }

    public static void deleteQueue(SqsClient sqsClient, String queueUrl) {
        if (queueUrl == null) return;
        try {
            // snippet-start:[sqs.java2.sqs_example.create_queue]

            DeleteQueueRequest deleteQueueRequest = DeleteQueueRequest.builder()
                    .queueUrl(queueUrl)
                    .build();

            sqsClient.deleteQueue(deleteQueueRequest);
            // snippet-end:[sqs.java2.sqs_example.create_queue]

            System.out.println(String.format("Deleted sqs:%s", queueUrl));

        } catch (QueueDoesNotExistException e) {
            System.out.println("q url : " + queueUrl + " doesnt exist. hope not bug");
        } catch (SqsException e) {
            System.err.println(e.awsErrorDetails().errorMessage());
            throw e;
        }
        // snippet-end:[sqs.java2.sqs_example.get_queue]
    }

    public static String createUniqueBucket(S3Client s3Client, String prefix) {

        try {
            ListBucketsRequest listBucketsRequest = ListBucketsRequest.builder().build();
            ListBucketsResponse listBucketsResponse = s3Client.listBuckets(listBucketsRequest);
            for (Bucket b :
                    listBucketsResponse.buckets()) {
                if (b.name().contains(prefix)) {
                    System.out.println(prefix + " already exist");
                    return b.name();
                }
            }
            String bName = prefix + System.currentTimeMillis();
            S3Waiter s3Waiter = s3Client.waiter();
            CreateBucketRequest bucketRequest = CreateBucketRequest.builder()
                    .bucket(bName)
                    .build();

            s3Client.createBucket(bucketRequest);
            HeadBucketRequest bucketRequestWait = HeadBucketRequest.builder()
                    .bucket(bName)
                    .build();


            // Wait until the bucket is created and print out the response
            WaiterResponse<HeadBucketResponse> waiterResponse = s3Waiter.waitUntilBucketExists(bucketRequestWait);
            waiterResponse.matched().response().ifPresent((x) -> {
            });
            System.out.println("created bucket " + bName);
            return bName;

        } catch (S3Exception e) {
            System.err.println(e.awsErrorDetails().errorMessage());
            throw e;
        }
    }

    public static void deleteBucket(S3Client s3, String bucket) {
        if (bucket == null) return;
        try {
            // To delete a bucket, all the objects in the bucket must be deleted first
            emptyBucket(s3, bucket);
            // snippet-end:[s3.java2.s3_bucket_ops.delete_bucket]

            DeleteBucketRequest deleteBucketRequest = DeleteBucketRequest.builder().bucket(bucket).build();
            s3.deleteBucket(deleteBucketRequest);
            System.out.println(bucket + " is deleted");

        } catch (S3Exception e) {
            System.err.println(e.awsErrorDetails().errorMessage());
            throw e;
        }
    }

    public static void emptyBucket(S3Client s3, String bucket) {
        if(bucket.contains("2robarakbucket") || bucket.contains("3robarakbucket")){
            //dont erase their content
            return;
        }
        ListObjectsV2Request listObjectsV2Request = ListObjectsV2Request.builder().bucket(bucket).build();
        ListObjectsV2Response listObjectsV2Response;
        System.out.println("Clearing the bucket " + bucket);
        do {
            listObjectsV2Response = s3.listObjectsV2(listObjectsV2Request);
            for (S3Object s3Object : listObjectsV2Response.contents()) {
                s3.deleteObject(DeleteObjectRequest.builder()
                        .bucket(bucket)
                        .key(s3Object.key())
                        .build());
            }

            listObjectsV2Request = ListObjectsV2Request.builder().bucket(bucket)
                    .continuationToken(listObjectsV2Response.nextContinuationToken())
                    .build();

        } while (listObjectsV2Response.isTruncated());
    }

    public static void deleteAllBuckets(S3Client s3) {

        ListBucketsRequest listBucketsRequest = ListBucketsRequest.builder().build();
        ListBucketsResponse listBucketsResponse = s3.listBuckets(listBucketsRequest);
        listBucketsResponse.buckets().stream().forEach(x -> deleteBucket(s3, x.name()));
    }

    public static void clearAllBuckets(S3Client s3) {

        ListBucketsRequest listBucketsRequest = ListBucketsRequest.builder().build();
        ListBucketsResponse listBucketsResponse = s3.listBuckets(listBucketsRequest);
        listBucketsResponse.buckets().stream().forEach(x -> emptyBucket(s3, x.name()));
    }

    public static void putFileInBucket(S3Client s3, String bucket, String inputLoc, String inputPath) {
        PutObjectRequest objectRequest = PutObjectRequest.builder()
                .bucket(bucket)
                .key(inputLoc)
                .acl(ObjectCannedACL.PUBLIC_READ)
                .build();
        try {
            s3.putObject(objectRequest, RequestBody.fromFile(new File(inputPath)));
            System.out.println(String.format("put file %s in bucket", inputPath));
        } catch (S3Exception e) {
            e.printStackTrace();
            System.err.println(e.awsErrorDetails().errorMessage());
            throw e;
        }
    }

    public static void sendMsg(SqsClient sqsClient, String queueUrl, String msg, String fromTo, String msgGroupId) {


        try {
            SendMessageRequest req = SendMessageRequest.builder()
                    .queueUrl(queueUrl)
                    .messageGroupId(msgGroupId)
                    .messageBody(msg)
                    .messageDeduplicationId("" + System.currentTimeMillis())
                    .build();
            sqsClient.sendMessage(req);
            System.out.println(String.format(fromTo + ": %s through %s", msg, queueUrl));
            // snippet-end:[sqs.java2.sqs_example.send__multiple_messages]

        } catch (SqsException e) {
            System.err.println(e.awsErrorDetails().errorMessage());
            throw e;
        }
    }

    public static List<Message> receiveMessages(SqsClient sqsClient, String queueUrl, int amount) {

        try {
            if (amount == Integer.MAX_VALUE) {
                amount = 5;
            }
            // snippet-start:[sqs.java2.sqs_example.retrieve_messages]
            ReceiveMessageRequest receiveMessageRequest = ReceiveMessageRequest.builder()
                    .queueUrl(queueUrl)
                    .maxNumberOfMessages(amount)
                    .build();
            List<Message> messages = sqsClient.receiveMessage(receiveMessageRequest).messages();
            return messages;
        } catch (QueueDoesNotExistException e) {
            System.out.println("problematic q " + queueUrl);
            System.err.println(e.awsErrorDetails().errorMessage());
            throw e;
        } catch (SqsException e) {
            System.err.println(e.awsErrorDetails().errorMessage());
            throw e;
        }
        // snippet-end:[sqs.java2.sqs_example.retrieve_messages]
    }

    public static List<String> getFileString(S3Client s3, String bucket, String resultS3Loc) {
        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(bucket)
                .key(resultS3Loc)
                .build();
        ResponseInputStream inputStream = s3.getObject(getObjectRequest);
        String fileString = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8)).lines().collect(Collectors.joining("\n"));
        return Arrays.asList(fileString.split("\\s*\n\\s*"));
    }

    static List<Message> waitForMessagesFrom(SqsClient sqs, String MGR_LA_SQS_url, String fromTo, int amount) throws InterruptedException {
        List<Message> msgs = null;
        System.out.println(String.format("Waiting for msgs from %s", fromTo));
        do {
            if (Thread.interrupted()) {
                throw new InterruptedException();
            }
            msgs = receiveMessages(sqs, MGR_LA_SQS_url, amount);
        } while (msgs == null || msgs.size() < 1);
        System.out.println(String.format("Got %d msgs from %s", msgs.size(), fromTo));
        return msgs;
    }

    public static void sendFileString(S3Client s3, String bucket, String loc, String content) {
        PutObjectRequest objectRequest = PutObjectRequest.builder()
                .bucket(bucket)
                .key(loc)
                .build();
        try {
            s3.putObject(objectRequest, RequestBody.fromString(content));
        } catch (S3Exception e) {
            System.err.println(e.awsErrorDetails().errorMessage());
            throw e;
        }
    }

    public static void deleteMsgs(SqsClient sqsClient, String la_mgr_sqs_url, List<Message> msgs) {
        // snippet-start:[sqs.java2.sqs_example.delete_message]

        try {
            for (Message message : msgs) {
                DeleteMessageRequest deleteMessageRequest = DeleteMessageRequest.builder()
                        .queueUrl(la_mgr_sqs_url)
                        .receiptHandle(message.receiptHandle())
                        .build();
                sqsClient.deleteMessage(deleteMessageRequest);
            }
            // snippet-end:[sqs.java2.sqs_example.delete_message]

        } catch (SqsException e) {
            System.err.println(e.awsErrorDetails().errorMessage());
        }

    }

    public static void tearDown() {
        Region region = Region.US_EAST_1;
        SqsClient sqsClient = SqsClient.builder()
                .region(region)
                .build();
        S3Client s3 = S3Client.builder()
                .region(region)
                .build();
        Utils.deleteAllQs(sqsClient, LocalApp.LA_resourcePrefix);
        Utils.deleteAllQs(sqsClient, Manager.MGR_resourcePrefix);
        Utils.deleteAllBuckets(s3);
    }

    public static void clearResources() {
        Region region = Region.US_EAST_1;
        SqsClient sqsClient = SqsClient.builder()
                .region(region)
                .build();
        S3Client s3 = S3Client.builder()
                .region(region)
                .build();
        Utils.purgeAllQs(sqsClient, LocalApp.LA_resourcePrefix);
        Utils.purgeAllQs(sqsClient, Manager.MGR_resourcePrefix);
        Utils.clearAllBuckets(s3);
    }

    public static void purgeAllQs(SqsClient sqsClient, String prefix) {

        try {
            ListQueuesRequest listQueuesRequest = ListQueuesRequest.builder().queueNamePrefix(prefix).build();
            ListQueuesResponse listQueuesResponse = sqsClient.listQueues(listQueuesRequest);

            for (String url : listQueuesResponse.queueUrls()) {
                purgeQ(sqsClient, url);
            }

        } catch (SqsException e) {
            System.err.println(e.awsErrorDetails().errorMessage());
            throw e;
        }
        // snippet-end:[sqs.java2.sqs_example.get_queue]
    }

    public static void list() {
        Region region = Region.US_EAST_1;
        SqsClient sqsClient = SqsClient.builder()
                .region(region)
                .build();
        S3Client s3 = S3Client.builder()
                .region(region)
                .build();
        try {
            ListQueuesRequest listQueuesRequest = ListQueuesRequest.builder().build();
            ListQueuesResponse listQueuesResponse = sqsClient.listQueues(listQueuesRequest);
            List<String> urls = listQueuesResponse.queueUrls();
            for (String url : urls) {
                System.out.println(url);
            }

        } catch (SqsException e) {
            System.err.println(e.awsErrorDetails().errorMessage());
        }
    }

    public static void purgeQ(SqsClient sqsClient, String url) {
        try {
            PurgeQueueRequest purgeQ = PurgeQueueRequest.builder()
                    .queueUrl(url)
                    .build();
            sqsClient.purgeQueue(purgeQ);
            System.out.println("Purged " + url);
            // snippet-end:[sqs.java2.sqs_example.delete_message]

        } catch (SqsException e) {
            System.err.println(e.awsErrorDetails().errorMessage());
        }
    }


    public static void terminateInstance(Ec2Client ec2, String instanceID) {
        try {
            TerminateInstancesRequest ti = TerminateInstancesRequest.builder()
                    .instanceIds(instanceID)
                    .build();

            TerminateInstancesResponse response = ec2.terminateInstances(ti);
            List<InstanceStateChange> list = response.terminatingInstances();

            for (int i = 0; i < list.size(); i++) {
                InstanceStateChange sc = (list.get(i));
                System.out.println("The ID of the terminated instance is " + sc.instanceId());
            }
        } catch (Ec2Exception e) {
            System.err.println(e.awsErrorDetails().errorMessage());
            System.err.println("BAD BAD BAD - ec2 not terminated $$$$$$$$$$$$$, Maybe he died?");
        }
    }

    public static List<String> getActiveEC2s(Ec2Client ec2, String wkrTag) {
        List<String> ans = new ArrayList<>();
        boolean done = false;
        String nextToken = null;

        try {

            do {
                DescribeInstancesRequest request = DescribeInstancesRequest.builder().maxResults(6).nextToken(nextToken).build();
                DescribeInstancesResponse response = ec2.describeInstances(request);

                for (Reservation reservation : response.reservations()) {
                    for (Instance instance : reservation.instances()) {
                        if ((!ans.contains(instance.instanceId())) &&
                                isActiveEc2(instance, wkrTag)) {
                            ans.add(instance.instanceId());
                        }
                    }
                }
                nextToken = response.nextToken();
            } while (nextToken != null);

        } catch (Ec2Exception e) {
            System.err.println(e.awsErrorDetails().errorMessage());
            System.err.println("ERROR getActiveEC2s");
        }
        return ans;
    }

    private static boolean isActiveEc2(Instance instance, String wkrTag) {
        return isTaggedAs(instance.tags(), wkrTag) && inActiveState(instance.state().name());
    }

    private static boolean isTaggedAs(List<Tag> tags, String wkrTag) {
        return tags.size() > 0 &&
                tags.stream().anyMatch(t -> t.key().equals("Name") && t.value().equals(wkrTag));
    }

    private static boolean inActiveState(InstanceStateName name) {
        return name.equals(InstanceStateName.PENDING) || name.equals(InstanceStateName.RUNNING);
    }
}
