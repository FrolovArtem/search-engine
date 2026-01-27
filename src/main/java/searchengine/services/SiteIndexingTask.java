package searchengine.services;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import searchengine.model.*;
import searchengine.repository.*;

import java.util.*;
import java.util.concurrent.RecursiveAction;
import java.util.concurrent.atomic.AtomicBoolean;

public class SiteIndexingTask extends RecursiveAction {
    
    private final String url;
    private final SiteEntity site;
    private final PageRepository pageRepository;
    private final LemmaRepository lemmaRepository;
    private final IndexRepository indexRepository;
    private final LemmaService lemmaService;
    private final PageIndexer pageIndexer;
    private final AtomicBoolean indexingInProgress;
    private final Set<String> visitedUrls;
    private final RobotsTxtParser robotsTxtParser;
    
    public SiteIndexingTask(String url, SiteEntity site,
                           PageRepository pageRepository,
                           LemmaRepository lemmaRepository,
                           IndexRepository indexRepository,
                           LemmaService lemmaService,
                           PageIndexer pageIndexer,
                           AtomicBoolean indexingInProgress,
                           Set<String> visitedUrls,
                           RobotsTxtParser robotsTxtParser) {
        this.url = url;
        this.site = site;
        this.pageRepository = pageRepository;
        this.lemmaRepository = lemmaRepository;
        this.indexRepository = indexRepository;
        this.lemmaService = lemmaService;
        this.pageIndexer = pageIndexer;
        this.indexingInProgress = indexingInProgress;
        this.visitedUrls = visitedUrls;
        this.robotsTxtParser = robotsTxtParser;
    }
    
    @Override
    protected void compute() {
        if (!indexingInProgress.get() || !visitedUrls.add(url)) {
            return;
        }
        
        try {
            Thread.sleep(500);
            
            Connection.Response response = connectToPage();
            Document doc = response.parse();
            String path = getPath(url);
            
            PageEntity page = savePage(path, response.statusCode(), doc.html());
            
            if (response.statusCode() == 200) {
                pageIndexer.indexPageContent(page, doc, site);
            }
            
            List<SiteIndexingTask> childTasks = createChildTasks(doc);
            invokeAll(childTasks);
            
        } catch (Exception e) {
            // Игнорируем ошибки отдельных страниц
        }
    }
    
    private Connection.Response connectToPage() throws Exception {
        return Jsoup.connect(url)
                .userAgent("Mozilla/5.0")
                .timeout(10000)
                .execute();
    }
    
    private List<SiteIndexingTask> createChildTasks(Document doc) {
        List<SiteIndexingTask> tasks = new ArrayList<>();
        Elements links = doc.select("a[href]");
        
        for (Element link : links) {
            String childUrl = link.absUrl("href");
            if (isValidUrl(childUrl)) {
                SiteIndexingTask task = new SiteIndexingTask(
                    childUrl, site, pageRepository, lemmaRepository,
                    indexRepository, lemmaService, pageIndexer,
                    indexingInProgress, visitedUrls, robotsTxtParser
                );
                tasks.add(task);
            }
        }
        
        return tasks;
    }
    
    private PageEntity savePage(String path, int code, String content) {
        PageEntity page = new PageEntity();
        page.setSite(site);
        page.setPath(path);
        page.setCode(code);
        page.setContent(content);
        return pageRepository.save(page);
    }
    
    private String getPath(String url) {
        String path = url.replace(site.getUrl(), "");
        return path.isEmpty() ? "/" : path;
    }
    
    private boolean isValidUrl(String url) {
        return url.startsWith(site.getUrl())
                && !url.contains("#")
                && !url.matches(".*\\.(jpg|jpeg|png|gif|pdf|zip)$")
                && !visitedUrls.contains(url)
                && robotsTxtParser.isAllowed(url);
    }
}
