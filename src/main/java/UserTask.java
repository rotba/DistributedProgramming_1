import java.util.concurrent.atomic.AtomicBoolean;

public class UserTask {
    private AtomicBoolean[] tasksIsDone;
    private String[] taskLoc;
    private String[] tasksMsg;
    private int doneCount;
    private final int size;
    public UserTask(int tasks){
        size = tasks;
        tasksIsDone = new AtomicBoolean[size];
        taskLoc = new String[size];
        doneCount =0;
        for (int i = 0; i < size; i++) {
            tasksIsDone[i] = new AtomicBoolean(false);
        }
    }

    @Override
    public String toString() {
        return "UserTask{" +
                "tasksAmount=" + tasksIsDone.length +
                "doneCount=" + doneCount +
                '}';
    }

    public boolean isDone() {
        int dones = 0;
        for (int i = 0; i < size; i++) {
            if (tasksIsDone[i].get()){
                dones++;
            }
        }
        doneCount = dones;
        return doneCount == size();
    }

    public int size() {
        return size;
    }

    public void setDone(int i) {
        setVal(i ,true);
    }
    private void setVal(int i, boolean newVal) {
        AtomicBoolean ab = tasksIsDone[i];
        boolean val;
        do{
            val = ab.get();
        }while (!ab.compareAndSet(val, newVal));
        tasksIsDone[i].set(true);
    }
}
