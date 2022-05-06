import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.*;

import java.io.UnsupportedEncodingException;
import java.util.Base64;

public class CreateEC2Builder {
    Ec2Client ec2;
    String tagString;
    String jobCode;
    String ami;
    String access;
    String secret;
    String[] args;

    String bucket;

    public CreateEC2Builder setBucket(String bucket) {
        this.bucket = bucket;
        return this;
    }

    public CreateEC2Builder setEc2(Ec2Client ec2) {
        this.ec2 = ec2;
        return this;
    }

    public CreateEC2Builder setTagString(String tagString) {
        this.tagString = tagString;
        return this;
    }

    public CreateEC2Builder setJobCode(String jobCode) {
        this.jobCode = jobCode;
        return this;
    }

    public CreateEC2Builder setAmi(String ami) {
        this.ami = ami;
        return this;
    }

    public CreateEC2Builder setAccess(String access) {
        this.access = access;
        return this;
    }

    public CreateEC2Builder setSecret(String secret) {
        this.secret = secret;
        return this;
    }

    public CreateEC2Builder setArgs(String[] args) {
        this.args = args;
        return this;
    }

    private CreateEC2Builder() {
    }

    public static CreateEC2Builder builder(){
        return new CreateEC2Builder();
    }

    public CreateEC2Builder build() {
        assert ec2!=null;
        assert tagString!=null;
        assert jobCode!=null;
        assert ami!=null;
        assert access!=null;
        assert secret!=null;
        assert args!=null;
        for (int i = 0; i < args.length; i++) {
            assert args[i]!=null;
        }
        assert bucket!=null;
        return this;
    }

    private String getUserData() {
        String argsString = "";
        for (int i = 0; i < args.length; i++) {
            argsString+=" "+args[i];
        }
        String userData = "";
        userData = userData + "#!/bin/bash" + "\n";
        userData = userData + "echo HEYYYYYYYYYYYYYYYYYY" + "\n";
        userData = userData + "cd /home/ubuntu\n";
        userData = userData + String.format("export AWS_ACCESS_KEY_ID=%s\n",access);
        userData = userData + String.format("export AWS_SECRET_ACCESS_KEY=%s\n",secret);
        userData = userData + String.format("java -jar -mx600m DistributedProgramming_1/target/DistributedProgramming_1-1.0-SNAPSHOT-jar-with-dependencies.jar %s %s\n",jobCode, argsString);
//        for (int i = 0; i < Common.WRKRS_PER_VM; i++) {
//            userData = userData + String.format("java -jar -mx200m DistributedProgramming_1/target/DistributedProgramming_1-1.0-SNAPSHOT-jar-with-dependencies.jar %s %s &\n",jobCode, argsString);
//        }
//        for (int i = 0; i < Common.WRKRS_PER_VM; i++) {
//            userData = userData + "fg\n";
//        }
        userData = userData + String.format("sudo halt\n");
        userData = userData + String.format("echo THIS LINE SHOULDNT BE PRINTED\n");
        System.out.println(userData);
        String base64UserData = null;
        try {
            base64UserData = new String( Base64.getEncoder().encode(userData.getBytes("UTF-8")), "UTF-8" );
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return base64UserData;
    }

    public String create() {
        RunInstancesRequest runRequest = RunInstancesRequest.builder()
                .imageId(ami)
                .instanceType(InstanceType.T2_MICRO)
                .maxCount(1)
                .minCount(1)
                .userData(getUserData())
                .keyName("dsp222_kv0")
                .build();
        if(Common.DONT_RUN_EC2s){
            System.out.println(runRequest.userData());
            return null;
        }

        RunInstancesResponse response = ec2.runInstances(runRequest);
        String instanceId = response.instances().get(0).instanceId();

        software.amazon.awssdk.services.ec2.model.Tag tag = Tag.builder()
                .key("Name")
                .value(tagString)
                .build();

        CreateTagsRequest tagRequest = CreateTagsRequest.builder()
                .resources(instanceId)
                .tags(tag)
                .build();

        try {
            ec2.createTags(tagRequest);
            System.out.printf(
                    "Successfully started EC2 Instance %s based on AMI %s",
                    instanceId, ami);

            return instanceId;

        } catch (Ec2Exception e) {
            System.err.println(e.awsErrorDetails().errorMessage());
            System.exit(1);
        }
        return "";
    }

}
