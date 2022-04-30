import java.io.IOException;

public class MyParserException extends Throwable {
    public Throwable t;

    public MyParserException(Throwable t) {
        this.t = t;
    }

    @Override
    public String getMessage() {
        return t.getMessage();
    }
}
