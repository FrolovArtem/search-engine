package searchengine.controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import searchengine.dto.ApiResponse;
import searchengine.dto.search.SearchResponse;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.services.*;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ApiController {
    
    private final StatisticsService statisticsService;
    private final IndexingService indexingService;
    private final SearchService searchService;
    private final PageIndexingService pageIndexingService;
    
    @GetMapping("/statistics")
    public StatisticsResponse statistics() {
        return statisticsService.getStatistics();
    }
    
    @GetMapping("/startIndexing")
    public ApiResponse startIndexing() {
        indexingService.startIndexing();
        return new ApiResponse(true);
    }
    
    @GetMapping("/stopIndexing")
    public ApiResponse stopIndexing() {
        indexingService.stopIndexing();
        return new ApiResponse(true);
    }
    
    @PostMapping("/indexPage")
    public ApiResponse indexPage(@RequestParam String url) {
        pageIndexingService.indexPage(url);
        return new ApiResponse(true);
    }
    
    @GetMapping("/search")
    public SearchResponse search(
            @RequestParam String query,
            @RequestParam(required = false) String site,
            @RequestParam(defaultValue = "0") int offset,
            @RequestParam(defaultValue = "20") int limit) {
        
        return searchService.search(query, site, offset, limit);
    }
}
