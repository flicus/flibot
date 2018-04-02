/*
 *  The MIT License (MIT)
 *
 *  Copyright (c) 2016  schors
 *
 *   Permission is hereby granted, free of charge, to any person obtaining a copy
 *  of this software and associated documentation files (the "Software"), to deal
 *  in the Software without restriction, including without limitation the rights
 *  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the Software is
 *  furnished to do so, subject to the following conditions:
 *
 *   The above copyright notice and this permission notice shall be included in all
 *  copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 *  SOFTWARE.
 */

package org.schors.flibot;

import org.apache.log4j.Logger;
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

    private static final Logger log = Logger.getLogger(PageParser.class);

    public static Page parse(InputStream input) {
        SAXBuilder parser = new SAXBuilder();
        Document xmlDoc = null;
        final Page page = new Page();
        try {
            xmlDoc = parser.build(input);
            log.warn(xmlDoc.toString());
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
            log.error(e, e);
        } catch (IOException e) {
            log.error(e, e);
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
