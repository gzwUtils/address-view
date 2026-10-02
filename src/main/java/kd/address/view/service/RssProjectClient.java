package kd.address.view.service;

import kd.address.view.entity.ExternalProject;
import kd.address.view.entity.ExternalSource;
import org.springframework.stereotype.Component;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
public class RssProjectClient {
    private static final int MAX_BYTES = 2_000_000;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();

    public List<ExternalProject> fetch(ExternalSource source) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(PublicFeedUrl.checkedForRequest(source.getFeedUrl()))
                .timeout(Duration.ofSeconds(8))
                .header("Accept", "application/rss+xml, application/atom+xml, application/xml, text/xml")
                .header("User-Agent", "Zaichang-Portal")
                .GET().build();
        HttpResponse<byte[]> response = http.send(request, HttpResponse.BodyHandlers.ofByteArray());
        if (response.statusCode() != 200) throw new IOException("订阅源返回 HTTP " + response.statusCode());
        if (response.body().length > MAX_BYTES) throw new IOException("订阅源内容超过 2 MB");
        return parse(response.body(), source);
    }

    List<ExternalProject> parse(byte[] body, ExternalSource source) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        factory.setXIncludeAware(false);
        factory.setExpandEntityReferences(false);
        Element root = factory.newDocumentBuilder().parse(new ByteArrayInputStream(body)).getDocumentElement();
        NodeList entries = root.getElementsByTagName("item");
        if (entries.getLength() == 0) entries = root.getElementsByTagName("entry");
        List<ExternalProject> projects = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (int index = 0; index < entries.getLength() && projects.size() < source.getMaxItems(); index++) {
            Element entry = (Element) entries.item(index);
            String title = text(entry, "title");
            String link = link(entry);
            if (title.isBlank() || !seen.add(link)) continue;
            try {
                PublicFeedUrl.parse(link);
            } catch (IllegalArgumentException ignored) {
                continue;
            }
            ExternalProject project = new ExternalProject();
            project.setSourcePlatform(source.getCode());
            project.setSourceRepoId(idFor(link));
            project.setFullName(shorten(title, 255));
            project.setSourceUrl(shorten(link, 500));
            String summary = text(entry, "description");
            if (summary.isBlank()) summary = text(entry, "summary");
            project.setDescription(shorten(summary.replaceAll("<[^>]+>", " ").replaceAll("\\s+", " ").trim(), 1000));
            project.setLanguage("");
            project.setLicenseSpdx("");
            project.setStarCount(0);
            project.setForkCount(0);
            project.setRepoCreatedAt(parseDate(entry));
            projects.add(project);
        }
        return projects;
    }

    private static String link(Element entry) {
        NodeList links = entry.getElementsByTagName("link");
        for (int i = 0; i < links.getLength(); i++) {
            Element element = (Element) links.item(i);
            String relation = element.getAttribute("rel");
            if (relation.isBlank() || "alternate".equals(relation)) {
                String href = element.getAttribute("href");
                return href.isBlank() ? element.getTextContent().trim() : href.trim();
            }
        }
        return "";
    }

    private static String text(Element element, String tag) {
        NodeList nodes = element.getElementsByTagName(tag);
        return nodes.getLength() == 0 ? "" : nodes.item(0).getTextContent().trim();
    }

    private static LocalDateTime parseDate(Element entry) {
        String value = text(entry, "pubDate");
        if (value.isBlank()) value = text(entry, "published");
        if (value.isBlank()) value = text(entry, "updated");
        try {
            Instant instant = value.contains(",")
                    ? ZonedDateTime.parse(value, DateTimeFormatter.RFC_1123_DATE_TIME).toInstant()
                    : Instant.parse(value);
            return LocalDateTime.ofInstant(instant, ZoneOffset.UTC);
        } catch (Exception ignored) {
            return LocalDateTime.now(ZoneOffset.UTC);
        }
    }

    private static long idFor(String value) throws Exception {
        byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
        return ByteBuffer.wrap(digest).getLong() & Long.MAX_VALUE;
    }

    private static String shorten(String value, int length) {
        return value.length() > length ? value.substring(0, length) : value;
    }
}
