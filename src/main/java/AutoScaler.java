import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ec2.Ec2Client;

import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

public abstract class AutoScaler implements Runnable {
    private int n;
    protected String MGR_WKR_SQS_url;
    protected String WKR_MGR_SQS_url;
    protected String bucket;
    private ConcurrentHashMap<String, UserTask> userTasks;
    Queue<Thread> wrkrs = new LinkedList<>();

    String wrkrAmi;

    String access;

    String secret;

    protected static String wkrTag = "WKR";

    private AtomicBoolean terminated = new AtomicBoolean(false);

    public AutoScaler build(){
        assert n!=0;
        assert MGR_WKR_SQS_url!=null;
        assert WKR_MGR_SQS_url!=null;
        assert bucket!=null;
        assert userTasks!=null;
        assert wrkrAmi!=null;
        assert access!=null;
        assert secret!=null;
        return this;
    }
    private static boolean THREAD_MODE = false;

    public AutoScaler setN(int n) {
        this.n = n;
        return this;
    }

    public AutoScaler setMGR_WKR_SQS_url(String MGR_WKR_SQS_url) {
        this.MGR_WKR_SQS_url = MGR_WKR_SQS_url;
        return this;
    }

    public AutoScaler setWKR_MGR_SQS_url(String WKR_MGR_SQS_url) {
        this.WKR_MGR_SQS_url = WKR_MGR_SQS_url;
        return this;
    }

    public AutoScaler setBucket(String bucket) {
        this.bucket = bucket;
        return this;
    }

    public AutoScaler setUserTasks(ConcurrentHashMap<String, UserTask> userTasks) {
        this.userTasks = userTasks;
        return this;
    }

    public AutoScaler setWrkrs(Queue<Thread> wrkrs) {
        this.wrkrs = wrkrs;
        return this;
    }

    public AutoScaler setWrkrAmi(String wrkrAmi) {
        this.wrkrAmi = wrkrAmi;
        return this;
    }

    public AutoScaler setAccess(String access) {
        this.access = access;
        return this;
    }

    public AutoScaler setSecret(String secret) {
        this.secret = secret;
        return this;
    }



    Region region = Region.US_EAST_1;

    Ec2Client ec2;

    public static AutoScaler create(){
        if(THREAD_MODE){
            return new ThreadModeAutoScaler();
        }else{
            return new EC2AutoScaler();
        }
    }

    protected AutoScaler() {
        ec2 = Ec2Client.builder()
                .region(region)
                .build();
    }

    @Override
    public void run() {
        while (!terminated.get()){
            int pendingWorkerTasks= countPendingWorkerTasks();
            int workers = countWorkers();
            int desiredWorkersCount = calcDesridWkrs(pendingWorkerTasks, n);
            if(desiredWorkersCount > workers){
                scaleOut(desiredWorkersCount - workers);
            }else if (workers > desiredWorkersCount){
                terminate(workers - desiredWorkersCount);
            }
        }
        System.out.println("AS terminates");
        int workers = countWorkers();
        terminate(workers);
        System.out.println("AS Done");
    }

    private int calcDesridWkrs(int pendingWorkerTasks, int n) {
        if(pendingWorkerTasks >0 ){
            return 2;
        }else{
            return 0;
        }
    }

    protected abstract void terminate(int i);


    protected abstract void scaleOut(int i);


    protected abstract int countWorkers();

    private int countPendingWorkerTasks() {
        int ans  =0;
        for (UserTask ut:
             userTasks.values()) {
            ans+=ut.getNotDoneCount();
        }
        return ans;
    }

    public void terminate() {
        boolean val;
        do{
            val = terminated.get();
        }while(!terminated.compareAndSet(val, true));
    }
}
