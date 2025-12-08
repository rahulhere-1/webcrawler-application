package org.example;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.logging.LogManager;

public class Main {

    private static final int MAX_DEPTH = 5;

    private LogManager log = LogManager.getLogManager();
    public static void main(String[] args) {
        Properties prop = new Properties();
        try (InputStream input = Main.class.getClassLoader()
                .getResourceAsStream("application.properties")) {
            prop.load(input);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        String url =  prop.getProperty("url");
        int level = Integer.parseInt(prop.getProperty("level"));
        crawl(1,url,new ArrayList<>(),level);

    }

    private static void crawl(int level, String url, List<String> visited, int maxSetLevel){

        if(level<MAX_DEPTH && level<maxSetLevel){
            System.out.println("LEVEL "+level);
            Document doc = request(url);
            if(doc!=null){
                for(Element element: doc.select("a[href]")){
                    String sublink = element.absUrl("href");
                    boolean isValid = validate(sublink.toLowerCase());
                    if(isValid && !visited.contains(sublink)){
                        visited.add(sublink);
                        crawl(level+1,sublink,visited,maxSetLevel);
                    }
                }
            }
        }

    }
    private static boolean validate(String url){
        return url.contains("https") && !url.contains("login")
                && !url.contains("signup") && !url.contains("auth") && url.indexOf('.')==-1;
    }
    private static Document request(String url){
        try {
            Thread.sleep(1000L);
            Connection con =  Jsoup.connect(url);
            Document doc = con.get();
            if(con.response().statusCode()==200){
                System.out.println("Link : ["+url+"]");
                System.out.println("Website name: ["+doc.title()+"]");
                return doc;
            }
        } catch (RuntimeException | IOException e) {
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        return null;
    }

}