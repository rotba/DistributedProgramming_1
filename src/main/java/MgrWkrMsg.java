public class MgrWkrMsg {
    private String file;

    public MgrWkrMsg(String taskDescriptionLoc, int i) {
        file = taskDescriptionLoc;
        idx = i;
    }

    public MgrWkrMsg(String body) {
        String[] splited = body.split("\\s+");
        file =  splited[0];
        idx = Integer.parseInt(splited[1]);
    }

    @Override
    public String toString() {
        return file+ " "+idx;
    }

    public String getFile() {
        return file;
    }

    public int getIdx() {
        return idx;
    }

    private int idx;



}
