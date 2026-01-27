package searchengine.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ApiResponse {
    private boolean result;
    private String error;
    
    public ApiResponse(boolean result) {
        this.result = result;
    }
}
