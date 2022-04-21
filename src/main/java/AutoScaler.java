import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;

public class AutoScaler implements Runnable {
    private int n;
    private String MGR_WKR_SQS_url;
    private String WKR_MGR_SQS_url;
    private String bucket;
    private ConcurrentHashMap<String, UserTask> userTasks;
    Queue<Thread> wrkrs = new LinkedList<>();

    public AutoScaler(int n, String MGR_WKR_SQS_url, String WKR_MGR_SQS_url, String bucket, ConcurrentHashMap<String, UserTask> userTasks) {
        this.n = n;
        this.MGR_WKR_SQS_url = MGR_WKR_SQS_url;
        this.WKR_MGR_SQS_url = WKR_MGR_SQS_url;
        this.bucket = bucket;
        this.userTasks = userTasks;
    }

    @Override
    public void run() {
        while (!Thread.interrupted()){
            int pendingWorkerTasks= countPendingWorkerTasks();
            int workers = countWorkers();
            int desiredWorkersCount = calcDesridWkrs(pendingWorkerTasks, n);
            if(desiredWorkersCount > workers){
                scaleOut(desiredWorkersCount - workers);
            }else if (workers > desiredWorkersCount){
                terminate(workers - desiredWorkersCount);
            }
        }
    }

    private int calcDesridWkrs(int pendingWorkerTasks, int n) {
        if(pendingWorkerTasks > 0)
            return 1;
        else
            return 0;
    }

    private void terminate(int i) {
        for (int j = 0; j <i; j++) {
            Thread wkr = wrkrs.poll();
            wkr.interrupt();
            try {
                wkr.join();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private void scaleOut(int i) {
        for (int j = 0; j < i; j++) {
            Thread wkr = new Thread(() -> new Worker().wrkrMain(new String[]{MGR_WKR_SQS_url, WKR_MGR_SQS_url, bucket}));
            wkr.start();
            wrkrs.add(wkr);
        }

    }

    private int countWorkers() {
        return wrkrs.size();
    }

    private int countPendingWorkerTasks() {
        int ans  =0;
        for (UserTask ut:
             userTasks.values()) {
            ans+=ut.getNotDoneCount();
        }
        return ans;
    }
}
