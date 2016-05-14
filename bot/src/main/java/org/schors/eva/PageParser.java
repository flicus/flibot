package org.schors.eva;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.filter.ElementFilter;
import org.jdom2.input.SAXBuilder;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by flicus on 14.05.16.
 */
public class PageParser {

    public static List<Entry> parse(InputStream input) {
        List<Entry> list = new ArrayList<>();
        SAXBuilder parser = new SAXBuilder();
        Document xmlDoc = null;
        try {
            xmlDoc = parser.build(input);
            List<Element> elements = xmlDoc.getRootElement().getContent(new ElementFilter("entry"));
            for (Element e : elements) {
                Element title = e.getChild("title", e.getNamespace());
                if (title != null) {
                    Entry entry = new Entry();
                    entry.setTitle(title.getText());
                    Element author = e.getChild("author", e.getNamespace());
                    if (author != null && author.getChild("name", e.getNamespace()) != null) {
                        entry.setAuthor(author.getChild("name", e.getNamespace()).getText());
                    }
                    List<Element> links = e.getChildren("link", e.getNamespace());
                    for (Element link : links) {
                        Link _lnk = new Link();
                        _lnk.setTitle(link.getAttributeValue("title"));
                        _lnk.setHref(fromURL(link.getAttributeValue("href")));
                        _lnk.setType(link.getAttributeValue("type"));
                        _lnk.setRel(link.getAttributeValue("rel"));
                        entry.getLinks().add(_lnk);
                    }
                    list.add(entry);
                }
            }
        } catch (JDOMException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return list;
    }

    public static String fromURL(String url) {
        return url.replaceAll("/", "_");
    }

    public static String toURL(String cmd) {
        return cmd.replaceAll("_", "/");
    }
}
