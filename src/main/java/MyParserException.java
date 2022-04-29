import java.io.IOException;

public class MyParserException extends Throwable {
    public IOException e;

    public MyParserException(IOException e) {
        this.e = e;
    }

}
