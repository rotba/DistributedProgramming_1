import java.util.Arrays;

public class TheMain {
    public static void main(String[] args) {
        String[] subArgs = Arrays.asList(args)
                .subList(1, args.length)
                .toArray(new String[0]);
        if(args[0].equals("LA")){
            LocalApp.mainLA(subArgs);
        }
    }
}
