public class WkrMgrMsg {
    public static String getString(String file, int idx, String task, String input, String output, String msg) {
        return file+" "+Integer.toString(idx)+" "+task+": "+input+" "+output+" "+msg;
    }

    public String getFile() {
        return file;
    }

    public int getIdx() {
        return idx;
    }

    public String getRes() {
        return res;
    }

    private String file;
    private int idx;
    private String res;

    private WkrMgrMsg() {
    }

    public static WkrMgrMsg parse(String body) {
        String[] splited = body.split("\\s+");
        WkrMgrMsg ans = new WkrMgrMsg();
        ans.file= splited[0];
        ans.idx= Integer.parseInt(splited[1]);
        ans.res="";
        for (int i = 2; i < splited.length; i++) {
            ans.res+=" "+splited[i];
        }
        return ans;
    }
}
