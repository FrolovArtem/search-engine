package searchengine.exception;

public class IndexingNotStartedException extends RuntimeException {
    public IndexingNotStartedException(String message) {
        super(message);
    }
}
