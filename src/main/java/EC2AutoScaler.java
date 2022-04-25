public class EC2AutoScaler extends AutoScaler {
    public EC2AutoScaler() {
        super();
    }

    @Override
    protected void terminate(int i) {
        for (int j = 0; j < i; j++) {
		wrkrsEc2.poll();
        }
    }

    @Override
    protected void scaleOut(int i) {
        for (int j = 0; j < i; j++) {
            wrkrsEc2.add(
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
                            .create());
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
        return wrkrsEc2.size();
    }
}
