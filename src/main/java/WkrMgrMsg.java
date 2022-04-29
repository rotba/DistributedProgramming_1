public class WkrMgrMsg {
    public WkrMgrMsg(String file, int idx, String task, String input, String output, String msg) {
        this.file = file;
        this.idx = idx;
        this.task = task;
        this.input = input;
        this.output = output;
        this.msg = msg;
    }

    @Override
    public String toString() {
        return file+" "+Integer.toString(idx)+" "+task+": "+input+" "+output+" "+msg;
    }

    public WkrMgrMsg(String body) {
        String[] splited = body.split("\\s+");
        file= splited[0];
        idx= Integer.parseInt(splited[1]);
        task=splited[2];
        input = splited[3];
        output = splited[4];
        msg = splited.length>5 ? splited[5] : "";
    }

    public String getFile() {
        return file;
    }

    public int getIdx() {
        return idx;
    }

    public String getRes() {
        return task+": "+input+" "+ (msg.equals("") ? output: msg);
    }

    private String file;
    private int idx;

    private String task;
    private String input;
    private String output;
    private String msg;

}
