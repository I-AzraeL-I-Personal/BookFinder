package com.mycompany.searchservice.util;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import javax.xml.xpath.*;
import java.util.*;

public class XPathSearcher {

    private final Document document;
    private final String pathAccess;

    public XPathSearcher(Document document, String pathAccess) {
        this.document = document;
        this.pathAccess = pathAccess;
    }

    public Set<String> find(Map<String, String> phrases) throws XPathExpressionException {
        phrases.entrySet().removeIf(entry -> entry.getValue().isBlank());
        if (phrases.isEmpty()) {
            return Collections.emptySet();
        }
        String xPath = buildXpath(phrases);
        XPathExpression xPathExpression = XPathFactory.newInstance().newXPath().compile(xPath);

        NodeList nodes = (NodeList) xPathExpression.evaluate(document, XPathConstants.NODESET);
        Set<String> uniqueNodes = new HashSet<>();
        for (int i = 0; i < nodes.getLength(); i++) {
            uniqueNodes.add(nodes.item(i).getTextContent().trim()
                    .replaceAll("[\\s&&[^\\n]]+", " ")
                    .replaceAll("\\n ", "\n"));
        }

        return uniqueNodes;
    }

    private String buildXpath(Map<String, String> phrases) {
        StringBuilder stringBuilder = new StringBuilder("");

        phrases.forEach((node, phrase) -> {
            if (!stringBuilder.toString().equals("")) {
                stringBuilder.append(" and ");
            }
            stringBuilder.append(buildXpathByNode(node, phrase));
        });
        return pathAccess + "[" + stringBuilder.toString() + "]";
    }

    private String buildXpathByNode(String node, String phrase) {
        final String contains = "contains(translate(%s, 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz'), '%s')";
        final String orContains = " or contains(translate(%s, 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz'), '%s')";
        final String andContains = " and contains(translate(%s, 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz'), '%s')";

        List<String> words = new ArrayList<>();
        StringTokenizer stringTokenizer = new StringTokenizer(phrase, " +", true);
        while (stringTokenizer.hasMoreTokens()) {
            words.add(stringTokenizer.nextToken());
        }

        StringBuilder stringBuilder = new StringBuilder("");
        stringBuilder.append(String.format(contains, node, words.get(0).toLowerCase()));
        for (int i = 2; i < words.size(); i += 2) {
            if (words.get(i - 1).equals("+")) {
                stringBuilder.append(String.format(andContains, node, words.get(i).toLowerCase()));
            } else if (words.get(i - 1).equals(" ")) {
                stringBuilder.append(String.format(orContains, node, words.get(i).toLowerCase()));
            }
        }

        return "(" + stringBuilder.toString() + ")";
    }
}