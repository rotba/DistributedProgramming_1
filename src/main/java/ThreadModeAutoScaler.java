
public class ThreadModeAutoScaler extends AutoScaler {
    public ThreadModeAutoScaler() {
        super();
    }

    @Override
    protected void terminate(int i) {
        for (int j = 0; j < i; j++) {
            Thread wkr = wrkrs.poll();
            wkr.interrupt();
            try {
                wkr.join();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    protected void scaleOut(int i) {
        for (int j = 0; j < i; j++) {
            Thread wkr = new Thread(() -> new Worker().wrkrMain(new String[]{MGR_WKR_SQS_url, WKR_MGR_SQS_url, bucket}));
            wkr.start();
            wrkrs.add(wkr);
        }

    }

    @Override
    protected int countWorkers() {
        return wrkrs.size();
    }
}
