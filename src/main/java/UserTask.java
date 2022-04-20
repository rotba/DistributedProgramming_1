import java.util.concurrent.atomic.AtomicBoolean;

public class UserTask {
    private AtomicBoolean[] tasksIsDone;
    private String[] taskLoc;
    private String[] tasksMsg;
    private int doneCount;
    public UserTask(int tasks){
        tasksIsDone = new AtomicBoolean[tasks];
        taskLoc = new String[tasks];
        doneCount =0;
    }

    @Override
    public String toString() {
        return "UserTask{" +
                "tasksAmount=" + tasksIsDone.length +
                "doneCount=" + doneCount +
                '}';
    }
}
