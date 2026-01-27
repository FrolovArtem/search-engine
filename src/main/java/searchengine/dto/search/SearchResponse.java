package searchengine.dto.search;

import lombok.Data;
import java.util.List;

@Data
public class SearchResponse {
    private boolean result;
    private int count;
    private List<SearchItem> data;
    private String error;
}
