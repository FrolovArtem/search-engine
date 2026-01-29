package searchengine.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import searchengine.dto.ApiResponse;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    @ExceptionHandler(IndexingAlreadyStartedException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse handleIndexingAlreadyStarted(IndexingAlreadyStartedException ex) {
        log.warn("Indexing already started: {}", ex.getMessage());
        return new ApiResponse(false, ex.getMessage());
    }
    
    @ExceptionHandler(IndexingNotStartedException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse handleIndexingNotStarted(IndexingNotStartedException ex) {
        log.warn("Indexing not started: {}", ex.getMessage());
        return new ApiResponse(false, ex.getMessage());
    }
    
    @ExceptionHandler(PageOutOfScopeException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse handlePageOutOfScope(PageOutOfScopeException ex) {
        log.warn("Page out of scope: {}", ex.getMessage());
        return new ApiResponse(false, ex.getMessage());
    }
    
    @ExceptionHandler(EmptySearchQueryException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse handleEmptySearchQuery(EmptySearchQueryException ex) {
        log.warn("Empty search query: {}", ex.getMessage());
        return new ApiResponse(false, ex.getMessage());
    }
    
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiResponse handleGenericException(Exception ex) {
        log.error("Unexpected error occurred", ex);
        return new ApiResponse(false, "Произошла внутренняя ошибка сервера");
    }
}
