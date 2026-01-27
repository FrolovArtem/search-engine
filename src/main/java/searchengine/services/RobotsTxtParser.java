package searchengine.services;

import org.jsoup.Jsoup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class RobotsTxtParser {
    
    private static final Logger logger = LoggerFactory.getLogger(RobotsTxtParser.class);
    private final List<String> disallowedPaths;
    private final String siteUrl;
    
    public RobotsTxtParser(String siteUrl) {
        this.siteUrl = siteUrl;
        this.disallowedPaths = new ArrayList<>();
        loadRobotsTxt();
    }
    
    private void loadRobotsTxt() {
        try {
            String robotsUrl = siteUrl + "/robots.txt";
            String content = Jsoup.connect(robotsUrl)
                    .userAgent("Mozilla/5.0")
                    .timeout(5000)
                    .ignoreContentType(true)
                    .execute()
                    .body();
            
            parseRobotsTxt(content);
            logger.info("Loaded robots.txt for {}", siteUrl);
            
        } catch (Exception e) {
            logger.warn("Could not load robots.txt for {}: {}", siteUrl, e.getMessage());
        }
    }
    
    private void parseRobotsTxt(String content) {
        String[] lines = content.split("\n");
        boolean relevantSection = false;
        
        for (String line : lines) {
            line = line.trim();
            
            if (line.toLowerCase().startsWith("user-agent:")) {
                String agent = line.substring(11).trim();
                relevantSection = agent.equals("*");
            } else if (relevantSection && line.toLowerCase().startsWith("disallow:")) {
                String path = line.substring(9).trim();
                if (!path.isEmpty()) {
                    disallowedPaths.add(path);
                }
            }
        }
    }
    
    public boolean isAllowed(String url) {
        String path = url.replace(siteUrl, "");
        if (path.isEmpty()) {
            path = "/";
        }
        
        for (String disallowed : disallowedPaths) {
            if (path.startsWith(disallowed)) {
                return false;
            }
        }
        
        return true;
    }
}
