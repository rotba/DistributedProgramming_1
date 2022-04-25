import java.util.Arrays;

public class TheMain {
    public static void main(String[] args) {
        String[] subArgs = Arrays.asList(args)
                .subList(1, args.length)
                .toArray(new String[0]);
        if(args[0].equals("LA")){
            LocalApp.mainLA(subArgs);
        }else if(args[0].equals("TEAR")){
            Utils.tearDown();
        }else if (args[0].equals("MLT")){
            String[] args1 = new String[]{"LA", "/home/rotemb271/Code/School/bgu/extern/DistributedProgramming_1/input-sample.txt", "/home/rotemb271/Code/School/bgu/extern/DistributedProgramming_1/out.html", "10"};
            String[] args2 = new String[]{"LA", "/home/rotemb271/Code/School/bgu/extern/DistributedProgramming_1/input-sample.txt", "/home/rotemb271/Code/School/bgu/extern/DistributedProgramming_1/out.html", "10" ,"terminate"};
            Thread t1 = new Thread(()->main(args1));
            Thread t2 = new Thread(()->main(args2));
            t1.start();
            try {
                t1.join();
                t2.start();
                t2.join();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }else if (args[0].equals("LST")){
            Utils.list();
        }else if(args[0].equals("CLR")){
            Utils.clearResources();
        }else if(args[0].equals("WKR")){
            Worker.wrkrMain(subArgs);
        }
    }
}
