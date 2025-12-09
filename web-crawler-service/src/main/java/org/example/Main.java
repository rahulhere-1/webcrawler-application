package org.example;

import org.jsoup.Connection;
import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class Main {
    private static final Set<String> visited = Collections.synchronizedSet(new HashSet<>());
    private static final Set<String> binaryExtensions = Set.of(
            ".pdf", ".jpg", ".jpeg", ".png", ".gif", ".webp", ".svg",
            ".mp4", ".mp3", ".zip", ".rar", ".exe", ".css", ".js",
            ".ico", ".woff", ".woff2", ".ttf", ".doc", ".docx", ".xls",
            ".xlsx", ".ppt", ".pptx", ".bin", ".iso"
    );

    private static String domain;
    private static final int POLITENESS_DELAY_MS = 1000; // 1 second
    private static final int MAX_DEPTH = 3;
    private static final int TIMEOUT_MS = 10_000;

    public static void main(String[] args) {
        String startUrl = "https://crawler-test.com/"; // Safe test page


        try {
            domain = new URI(startUrl).getHost();
            System.out.println("Starting crawl from: " + startUrl);
            System.out.println("Domain restricted to: " + domain);
            crawl(startUrl, 0);
            System.out.println("\nCrawl finished. Total unique HTML pages: " + visited.size());
        } catch (URISyntaxException e) {
            System.err.println("Invalid start URL");
        }
    }

    private static void crawl(String url, int depth) {
        if (depth > MAX_DEPTH || visited.contains(url)) {
            return;
        }

        if (isLikelyBinary(url)) {
            System.out.println("Skipped (binary extension): " + url);
            return;
        }

        /*if (!isAllowedByRobots(url)) {
            System.out.println("Blocked by robots.txt: " + url);
            return;
        }*/

        try {
            Connection headConn = Jsoup.connect(url)
                    .method(Connection.Method.HEAD)
                    .timeout(TIMEOUT_MS)
                    .followRedirects(true);

            Connection.Response headResponse;
            try {
                headResponse = headConn.execute();
            } catch (HttpStatusException e) {
                if (e.getStatusCode() == 405 || e.getStatusCode() == 501) {
                    // HEAD not supported → fallback to GET later
                    headResponse = null;
                } else {
                    return; // 404, 403, etc.
                }
            }

            String contentType = (headResponse != null) ?
                    headResponse.contentType() : null;

            if (contentType != null && !isHtmlContentType(contentType)) {
                System.out.println("Skipped (non-HTML): " + url + " | " + contentType);
                return;
            }

            Connection.Response response = Jsoup.connect(url)
                    .timeout(TIMEOUT_MS)
                    .execute();

            if (!isHtmlContentType(response.contentType())) {
                System.out.println("Skipped (confirmed non-HTML): " + url);
                return;
            }

            Document doc = response.parse();
            visited.add(url);

            System.out.printf("[%d] Crawled (%d): %s | Title: %s%n",
                    visited.size(), response.statusCode(), url,
                    doc.title().isEmpty() ? "(no title)" : doc.title());

            if (depth < MAX_DEPTH) {
                Elements links = doc.select("a[href]");
                for (Element link : links) {
                    String nextUrl = link.absUrl("href");
                    if (isSameDomain(nextUrl) && !visited.contains(nextUrl)) {
                        sleepPolitely();
                        crawl(nextUrl, depth + 1);
                    }
                }
            }

        } catch (IOException e) {
            System.out.println("Error crawling " + url + " → " + e.getMessage());
        }
    }

    private static boolean isHtmlContentType(String contentType) {
        return contentType != null &&
                (contentType.contains("text/html") ||
                        contentType.contains("application/xhtml+xml"));
    }

    private static boolean isLikelyBinary(String url) {
        String lower = url.toLowerCase();
        return binaryExtensions.stream().anyMatch(lower::endsWith) ||
                lower.contains("/wp-content/uploads/") ||
                lower.contains("/images/") ||
                lower.contains("?download=");
    }

    private static boolean isSameDomain(String url) {
        try {
            String host = new URI(url).getHost();
            return host != null && host.endsWith(domain);
        } catch (URISyntaxException e) {
            return false;
        }
    }

    private static boolean isAllowedByRobots(String url) {
        try {
            String robotsUrl = "https://" + domain + "/robots.txt";
            Document robots = Jsoup.connect(robotsUrl)
                    .timeout(5000)
                    .get();

            String text = robots.text().toLowerCase();
            String path = new URI(url).getPath();
            return !text.contains("disallow: " + path) &&
                    !text.contains("disallow: /");
        } catch (Exception e) {
            return true;
        }
    }

    private static void sleepPolitely() {
        try {
            TimeUnit.MILLISECONDS.sleep(POLITENESS_DELAY_MS + new Random().nextInt(500));
        } catch (InterruptedException ignored) {}
    }
}