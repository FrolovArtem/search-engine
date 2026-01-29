package searchengine.exception;

public class PageOutOfScopeException extends RuntimeException {
    public PageOutOfScopeException(String message) {
        super(message);
    }
}
