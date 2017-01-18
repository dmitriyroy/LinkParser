package ua.com.parser.htmlparser.parser;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import ua.com.parser.htmlparser.checker.Checker;
import ua.com.parser.htmlparser.checker.CheckerImpl;
import ua.com.parser.htmlparser.rule.Rule;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.Callable;

public class LinkParser extends Parser implements Callable<Map<Integer, String>> {
    private String url;
    private List<Rule> rules;
    private Checker checker;

    public LinkParser(String url, List<Rule> rules) {
        this.url = url;
        this.rules = rules;
        checker = new CheckerImpl();
    }

    @Override
    public Map<Integer, String> call() throws Exception {

        Map<Integer, String> result = new HashMap<>();

        try {
            result.putAll(getLinks(url));

            int start = 2; // start index to parse next page;
            for (int i = start; i <= getMaxPageNumber(url); i++) {

                String nextUrl = String.format(url + "/page%s", i);
                result.putAll(getLinks(nextUrl));
            }

        } catch (IOException e) {
            throw new RuntimeException("Failed to get a list of pages: " + e.getMessage());
        }
        return result;
    }

    private Map<Integer, String> getLinks(String url) throws IOException {

        Map<Integer, String> result = new HashMap<>();

        Document doc = Jsoup.connect(url).get();
        Elements elements = doc.getElementsByAttributeValue("class", "post post_teaser shortcuts_item");

        elements.forEach(element -> {
            Integer id = Integer.parseInt(element.attr("id").replace("post_", ""));
            Element aElement = element.child(0).child(1).child(0);
            String link = aElement.attr("href");

            for (Rule rule : rules) {
                if(checkRule(rule, element)) {
                    result.put(id, link);
                    break;
                }
            }
        });

        return result;
    }

    private boolean checkRule(Rule rule, Element element) {

        final String[] value = new String[1];

        Elements elements = element.getElementsByAttributeValue("class", checker.getParseValue(rule.getKey()));
        elements.forEach(innerElement -> value[0] = innerElement.text());

        return checker.check(rule, value[0]);
    }
}