import java.io.IOException;

public class DownloadException extends Throwable {
    private final IOException e;

    public DownloadException(IOException e) {
        this.e = e;
    }
    public String getMsg(){
        return e.toString();
    }
}
