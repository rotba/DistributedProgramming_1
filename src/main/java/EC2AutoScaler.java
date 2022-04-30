import java.util.*;

public class EC2AutoScaler extends AutoScaler {
    List<String> lastKnownActiveWorkers = null;
    public EC2AutoScaler() {
        super();
    }

    @Override
    protected void terminate(int i) {
        if(lastKnownActiveWorkers == null){
            System.err.println("EC2AutoScaler: terminate should call usually after count was called, therefore the track of active wrkrs should be non-null");
            lastKnownActiveWorkers = Utils.getActiveEC2s(ec2, wkrTag);
        }
        for (int j = 0; j < i && lastKnownActiveWorkers.size() >0; j++) {
            String instanceId = lastKnownActiveWorkers.remove(0);
            Utils.terminateInstance(ec2, instanceId);
        }
        lastKnownActiveWorkers = null;
    }

    @Override
    protected void scaleOut(int i) {
        for (int j = 0; j < i; j++) {
            CreateEC2Builder.builder()
                    .setAmi(wrkrAmi)
                    .setAccess(access)
                    .setSecret(secret)
                    .setArgs(wrkrArgs())
                    .setJobCode(Common.WKR_CODE)
                    .setEc2(ec2)
                    .setTagString(wkrTag)
                    .setBucket(bucket)
                    .setArgs(wrkrArgs())
                    .build()
                    .create();

        }

    }

    private String[] wrkrArgs() {
        String[] ans = new String[Common.WKR_IDX.LENGTH.idx];
        ans[Common.WKR_IDX.BKT.idx] = bucket;
        ans[Common.WKR_IDX.WKR_MGR_SQS.idx] = WKR_MGR_SQS_url;
        ans[Common.WKR_IDX.MGR_WKR_SQS.idx] = MGR_WKR_SQS_url;
        return ans;
    }

    @Override
    protected int countWorkers() {
        lastKnownActiveWorkers = Utils.getActiveEC2s(ec2, wkrTag);
        return lastKnownActiveWorkers.size();
    }
}
