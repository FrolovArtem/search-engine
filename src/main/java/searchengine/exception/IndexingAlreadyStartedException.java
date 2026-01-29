package searchengine.exception;

public class IndexingAlreadyStartedException extends RuntimeException {
    public IndexingAlreadyStartedException(String message) {
        super(message);
    }
}
