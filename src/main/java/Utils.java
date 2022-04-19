import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.*;
import software.amazon.awssdk.core.waiters.WaiterResponse;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.waiters.S3Waiter;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

public class Utils {
    public static String createQueue(SqsClient sqsClient, String queueName) {

        try {
            // snippet-start:[sqs.java2.sqs_example.create_queue]

            CreateQueueRequest createQueueRequest = CreateQueueRequest.builder()
                    .queueName(queueName)
                    .build();

            sqsClient.createQueue(createQueueRequest);
            // snippet-end:[sqs.java2.sqs_example.create_queue]



            // snippet-start:[sqs.java2.sqs_example.get_queue]
            GetQueueUrlResponse getQueueUrlResponse =
                    sqsClient.getQueueUrl(GetQueueUrlRequest.builder().queueName(queueName).build());
            String queueUrl = getQueueUrlResponse.queueUrl();
            System.out.println(String.format("Created queue url%s", queueUrl));
            return queueUrl;

        } catch (SqsException e) {
            System.err.println(e.awsErrorDetails().errorMessage());
            System.exit(1);
        }
        return "";
        // snippet-end:[sqs.java2.sqs_example.get_queue]
    }

    public static void deleteAllQs(SqsClient sqsClient) {

        try {
            ListQueuesRequest listQueuesRequest = ListQueuesRequest.builder().build();
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
    public static String deleteQueue(SqsClient sqsClient, String queueUrl) {

        try {
            // snippet-start:[sqs.java2.sqs_example.create_queue]

            DeleteQueueRequest deleteQueueRequest = DeleteQueueRequest.builder()
                    .queueUrl(queueUrl)
                    .build();

            sqsClient.deleteQueue(deleteQueueRequest);
            // snippet-end:[sqs.java2.sqs_example.create_queue]

            System.out.println(String.format("Deleted sqs:%s", queueUrl));

        } catch (SqsException e) {
            System.err.println(e.awsErrorDetails().errorMessage());
            System.exit(1);
        }
        return "";
        // snippet-end:[sqs.java2.sqs_example.get_queue]
    }

    public static void createBucket(S3Client s3Client, String bucketName) {

        try {
            S3Waiter s3Waiter = s3Client.waiter();
            CreateBucketRequest bucketRequest = CreateBucketRequest.builder()
                    .bucket(bucketName)
                    .build();

            s3Client.createBucket(bucketRequest);
            HeadBucketRequest bucketRequestWait = HeadBucketRequest.builder()
                    .bucket(bucketName)
                    .build();


            // Wait until the bucket is created and print out the response
            WaiterResponse<HeadBucketResponse> waiterResponse = s3Waiter.waitUntilBucketExists(bucketRequestWait);
            waiterResponse.matched().response().ifPresent(System.out::println);
            System.out.println(bucketName + " is ready");

        } catch (S3Exception e) {
            System.err.println(e.awsErrorDetails().errorMessage());
            throw e;
        }
    }

    public static void deleteBucket(S3Client s3, String bucket) {

        try {
            // To delete a bucket, all the objects in the bucket must be deleted first
            ListObjectsV2Request listObjectsV2Request = ListObjectsV2Request.builder().bucket(bucket).build();
            ListObjectsV2Response listObjectsV2Response;

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

            } while(listObjectsV2Response.isTruncated());
            // snippet-end:[s3.java2.s3_bucket_ops.delete_bucket]

            DeleteBucketRequest deleteBucketRequest = DeleteBucketRequest.builder().bucket(bucket).build();
            s3.deleteBucket(deleteBucketRequest);
            System.out.println(bucket + " is deleted");

        } catch (S3Exception e) {
            System.err.println(e.awsErrorDetails().errorMessage());
            throw e;
        }
    }

    public static void deleteAllBuckets(S3Client s3) {

        ListBucketsRequest listBucketsRequest = ListBucketsRequest.builder().build();
        ListBucketsResponse listBucketsResponse = s3.listBuckets(listBucketsRequest);
        listBucketsResponse.buckets().stream().forEach(x -> deleteBucket(s3, x.name()));
    }

    public static void putFileInBucket(S3Client s3, String bucket, String inputLoc, String inputPath) {
        PutObjectRequest objectRequest = PutObjectRequest.builder()
                .bucket(bucket)
                .key(inputLoc)
                .build();
        try {
            s3.putObject(objectRequest, RequestBody.fromFile(new File(inputPath)));
            System.out.println(String.format("put file %s in bucket", inputPath));
        } catch (S3Exception e) {
            e.printStackTrace();
            System.err.println(e.awsErrorDetails().errorMessage());
            throw  e;
        }
    }

    public static void sendMsg(SqsClient sqsClient, String queueUrl, String msg) {

        System.out.println("Send a msg multiple messages");

        try {
            SendMessageRequest req = SendMessageRequest.builder()
                    .queueUrl(queueUrl)
                    .messageBody(msg)
                    .build();
            sqsClient.sendMessage(req);
            System.out.println(String.format(" sent %s though %s", msg, queueUrl));
            // snippet-end:[sqs.java2.sqs_example.send__multiple_messages]

        } catch (SqsException e) {
            System.err.println(e.awsErrorDetails().errorMessage());
            throw e;
        }
    }
    public static List<Message> receiveMessages(SqsClient sqsClient, String queueUrl) {

        try {
            // snippet-start:[sqs.java2.sqs_example.retrieve_messages]
            ReceiveMessageRequest receiveMessageRequest = ReceiveMessageRequest.builder()
                    .queueUrl(queueUrl)
                    .maxNumberOfMessages(5)
                    .build();
            List<Message> messages = sqsClient.receiveMessage(receiveMessageRequest).messages();
            return messages;
        } catch (SqsException e) {
            System.err.println(e.awsErrorDetails().errorMessage());
            throw e;
        }
        // snippet-end:[sqs.java2.sqs_example.retrieve_messages]
    }

    public static String getFileString(S3Client s3, String bucket, String resultS3Loc) {
        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(bucket)
                .key(resultS3Loc)
                .build();
        ResponseInputStream inputStream = s3.getObject(getObjectRequest);
        return new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8)).lines().collect(Collectors.joining("\n"));
    }

    static List<Message> waitForMessagesFrom(SqsClient sqs, String MGR_LA_SQS_url, String from) {
        List<Message> msgs = null;
        System.out.println(String.format("waiting for msgs from %s", from));
        do {
            msgs = receiveMessages(sqs, MGR_LA_SQS_url);
        }while (msgs == null || msgs.size() < 1);
        System.out.println(String.format("got %d msgs from %s", msgs.size(), from));
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
}
