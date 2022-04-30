import java.io.IOException;

public class MyParserException extends Throwable {
    public Exception e;

    public MyParserException(Exception e) {
        this.e = e;
    }

    @Override
    public String getMessage() {
        return e.getMessage();
    }
}
