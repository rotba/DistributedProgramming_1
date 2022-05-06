public class Common {
    public final static String TERMINATE_MSG = "TERMINATE";
    public static final String WKR_CODE = "WKR";

    public static final String FIFO_SUFFIX = ".fifo";
    public static final boolean USE_THREAD_MGR = false;
    public static final String WKR_MGR_AMI_WITHOUT_LEX_PARSER = "ami-0a7281a210a3d8ad9";
    public static final String WKR_MGR_AMI = "ami-0e2acfab2cae5602a";
    public static boolean DONT_EUNT_WKR_EC2s = false;

    public enum MGR_IDX {
        BKT(0), N(1), LA_MGR_SQS(2), MGR_LA_SQS(3), AMI(4), SECRET(5), ACCESS(6), LENGTH(7);

        public int idx;

        MGR_IDX (int idx) {
            this.idx = idx;
        }
    }

    public enum WKR_IDX {
        BKT(0), MGR_WKR_SQS(1), WKR_MGR_SQS(2), LENGTH(3);

        public int idx;

        WKR_IDX (int idx) {
            this.idx = idx;
        }
    }
}
