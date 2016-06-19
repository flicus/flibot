package org.schors.flibot;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.filter.ElementFilter;
import org.jdom2.input.SAXBuilder;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 * Created by flicus on 14.05.16.
 */
public class PageParser {

    public static Page parse(InputStream input) {
        SAXBuilder parser = new SAXBuilder();
        Document xmlDoc = null;
        final Page page = new Page();
        try {
            xmlDoc = parser.build(input);
            String title = xmlDoc.getRootElement().getAttributeValue("title");
            page.setTitle(title);

            List<Element> links = xmlDoc.getRootElement().getContent(new ElementFilter("link"));
            links.stream().forEach(link -> {
                Link _lnk = new Link();
                _lnk.setTitle(link.getAttributeValue("title"));
                _lnk.setHref(link.getAttributeValue("href"));
                _lnk.setType(link.getAttributeValue("type"));
                _lnk.setRel(link.getAttributeValue("rel"));
                page.getLinks().add(_lnk);
            });

            List<Element> elements = xmlDoc.getRootElement().getContent(new ElementFilter("entry"));
            elements.stream().forEach(element -> {
                Element _title = element.getChild("title", element.getNamespace());
                if (_title != null) {
                    Entry entry = new Entry();
                    entry.setTitle(_title.getText());
                    Element author = element.getChild("author", element.getNamespace());
                    if (author != null && author.getChild("name", element.getNamespace()) != null) {
                        entry.setAuthor(author.getChild("name", element.getNamespace()).getText());
                    }
                    List<Element> _links = element.getChildren("link", element.getNamespace());
                    _links.stream().forEach(link -> {
                        Link _lnk = new Link();
                        _lnk.setTitle(link.getAttributeValue("title"));
                        _lnk.setHref(link.getAttributeValue("href"));
                        _lnk.setType(link.getAttributeValue("type"));
                        _lnk.setRel(link.getAttributeValue("rel"));
                        entry.getLinks().add(_lnk);
                    });
                    page.getEntries().add(entry);
                }
            });
        } catch (JDOMException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return page;
    }

//    public static String fromURL(String url) {
//        String res = null;
//        try {
//            res = URLDecoder.decode(url
//                    .replace("/opds", "")
//                    .replace("authorsequences", "as")
//                    .replace("authorsequenceless", "asl")
//                    .replace("author", "au")
//                    .replace("alphabet", "al")
//                    .replace("time", "t")
//                    .replaceAll("/", "_"), "UTF-8");
//        } catch (UnsupportedEncodingException e) {
//            e.printStackTrace();
//        }
//        return res;
//    }
//
//    public static String toURL(String cmd, boolean direct) {
//        if (direct) return cmd
//                .replaceAll("_", "/")
//                .replace("t", "time")
//                .replace("al", "alphabet")
//                .replace("au", "author")
//                .replace("asl", "authorsequenceless")
//                .replace("as", "authorsequences");
//        else return "/opds" + cmd
//                .replaceAll("_", "/")
//                .replace("t", "time")
//                .replace("al", "alphabet")
//                .replace("au", "author")
//                .replace("asl", "authorsequenceless")
//                .replace("as", "authorsequences");
//    }
}
