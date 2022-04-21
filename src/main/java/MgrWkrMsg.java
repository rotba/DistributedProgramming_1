public class MgrWkrMsg {
    private String file;

    public static String getString(String taskDescriptionLoc, int i) {
        return taskDescriptionLoc+ " "+i;
    }

    public String getFile() {
        return file;
    }

    public int getIdx() {
        return idx;
    }

    private int idx;

    private MgrWkrMsg() {
    }

    public static MgrWkrMsg parse(String body) {
        String[] splited = body.split("\\s+");
        MgrWkrMsg ans= new MgrWkrMsg();
        ans.file =  splited[0];
        ans.idx = Integer.parseInt(splited[1]);
        return ans;
    }
}
