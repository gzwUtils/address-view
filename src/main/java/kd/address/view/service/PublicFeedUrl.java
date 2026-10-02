package kd.address.view.service;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.URI;
import java.util.Locale;

final class PublicFeedUrl {
    private PublicFeedUrl() {}

    static URI parse(String value) {
        if (value == null || value.length() > 500) throw new IllegalArgumentException("来源地址无效");
        URI uri;
        try {
            uri = URI.create(value.trim());
        } catch (IllegalArgumentException error) {
            throw new IllegalArgumentException("来源地址无效");
        }
        String host = uri.getHost();
        String normalizedHost = host == null ? "" : host.toLowerCase(Locale.ROOT);
        if (!"https".equalsIgnoreCase(uri.getScheme()) || host == null || uri.getUserInfo() != null
                || (uri.getPort() != -1 && uri.getPort() != 443) || !normalizedHost.contains(".")
                || normalizedHost.endsWith(".") || normalizedHost.endsWith(".local")
                || normalizedHost.endsWith(".internal") || normalizedHost.endsWith(".localhost")
                || normalizedHost.matches("[0-9.]+") || normalizedHost.contains(":")) {
            throw new IllegalArgumentException("仅支持公开 HTTPS 订阅地址");
        }
        return uri;
    }

    static URI checkedForRequest(String value) throws Exception {
        URI uri = parse(value);
        for (InetAddress address : InetAddress.getAllByName(uri.getHost())) {
            if (address.isAnyLocalAddress() || address.isLoopbackAddress() || address.isLinkLocalAddress()
                    || address.isSiteLocalAddress() || address.isMulticastAddress() || isRestricted(address)) {
                throw new IllegalArgumentException("来源地址必须解析到公网地址");
            }
        }
        return uri;
    }

    private static boolean isRestricted(InetAddress address) {
        byte[] bytes = address.getAddress();
        if (address instanceof Inet6Address) return (bytes[0] & 0xfe) == 0xfc;
        if (!(address instanceof Inet4Address)) return true;
        int first = bytes[0] & 0xff;
        int second = bytes[1] & 0xff;
        return first == 0 || first == 10 || first == 127 || first >= 224
                || (first == 100 && second >= 64 && second <= 127)
                || (first == 169 && second == 254) || (first == 172 && second >= 16 && second <= 31)
                || (first == 192 && (second == 0 || second == 168))
                || (first == 198 && (second == 18 || second == 19));
    }
}
