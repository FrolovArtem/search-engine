package searchengine.controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
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
    public ResponseEntity<StatisticsResponse> statistics() {
        return ResponseEntity.ok(statisticsService.getStatistics());
    }
    
    @GetMapping("/startIndexing")
    public ResponseEntity<ApiResponse> startIndexing() {
        boolean started = indexingService.startIndexing();
        
        if (started) {
            return ResponseEntity.ok(new ApiResponse(true));
        } else {
            return ResponseEntity.ok(
                new ApiResponse(false, "Индексация уже запущена")
            );
        }
    }
    
    @GetMapping("/stopIndexing")
    public ResponseEntity<ApiResponse> stopIndexing() {
        boolean stopped = indexingService.stopIndexing();
        
        if (stopped) {
            return ResponseEntity.ok(new ApiResponse(true));
        } else {
            return ResponseEntity.ok(
                new ApiResponse(false, "Индексация не запущена")
            );
        }
    }
    
    @PostMapping("/indexPage")
    public ResponseEntity<ApiResponse> indexPage(@RequestParam String url) {
        boolean indexed = pageIndexingService.indexPage(url);
        
        if (indexed) {
            return ResponseEntity.ok(new ApiResponse(true));
        } else {
            return ResponseEntity.ok(
                new ApiResponse(false, "Данная страница находится за пределами сайтов, указанных в конфигурационном файле")
            );
        }
    }
    
    @GetMapping("/search")
    public ResponseEntity<SearchResponse> search(
            @RequestParam String query,
            @RequestParam(required = false) String site,
            @RequestParam(defaultValue = "0") int offset,
            @RequestParam(defaultValue = "20") int limit) {
        
        return ResponseEntity.ok(searchService.search(query, site, offset, limit));
    }
}
