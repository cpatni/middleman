package ign.middleman.helpers;

import javax.servlet.http.HttpServletRequest;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * User: cpatni
 * Date: Aug 8, 2010
 * Time: 11:42:21 AM
 */
public class ApplicationHelper {
    private static Logger httpLogger = Logger.getLogger("ApplicationHelperLogger");

    private ApplicationHelper() {
    }

    /**
     * Returns the first of two given parameters that is not {@code null}, if
     * either is, or returns {@code null}.
     *
     * @param first first object
     * @param second second object
     * @return {@code first} if {@code first} is not {@code null}, or
     *         {@code second} if {@code first} is {@code null} and {@code second} is
     *         not {@code null}
     */
    public static <T> T tryNonNull(T first, T second) {
        return first != null ? first : second;
    }


    public static String getIP(String ip, HttpServletRequest request) {
        if("self".equals(ip) || "current".equals(ip) || isTrivial(ip)) {
            return getTrueClientIPFromXFF(request);
        } else {
            return ip;
        }

    }


    public static String getTrueClientIPFromXFF(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (isNonTrivial(xff)) {
            xff = xff.trim();
            int index = xff.indexOf(",");
            if (index > 0) {
                return xff.substring(0, index).trim();
            } else {
                return xff;
            }

        }
        return request.getRemoteAddr();
    }

    
    public static String url(HttpServletRequest request) {
        StringBuffer url = request.getRequestURL();
        if(request.getQueryString() != null) {
            url.append("?").append(request.getQueryString());
        }
        return url.toString();
    }

       /**
     * Returns true if the specified string is a trivial string.
     * A trivial string is either null or "" after trimming
     *
     * @param s String to compare
     * @return true if the specified string is trivial, false otherwise
     */
    public static boolean isTrivial(String s) {
        return s == null || "".equals(s.trim());
    }

    /**
     * Returns true if the specified string is a non trivial string.
     * A non trivial string is neither null nor "" after trimming
     *
     * @param s String to compare
     * @return true if the specified string is non trivial, false otherwise
     */
    public static boolean isNonTrivial(String s) {
        return s != null && !"".equals(s.trim());
    }

    /**
     * Returns true if two strings are considered equal, false
     * otherwise
     *
     * @param s1 the first string
     * @param s2 the second string
     * @return the equality of the two Strings
     */
    @SuppressWarnings({"StringEquality"})
    public static boolean equals(String s1, String s2) {
        return s1 == s2 || s1 != null && s1.equals(s2);
    }

    /**
     * Returns true if two strings are considered case blind equal, false
     * otherwise
     *
     * @param s1 the first string
     * @param s2 the second string
     * @return the equality of the two Strings
     */
    @SuppressWarnings({"StringEquality"})
    public static boolean equalsIgnoreCase(String s1, String s2) {
        return s1 == s2 || s1 != null && s1.equalsIgnoreCase(s2);
    }

    private static final char hex[] = "0123456789abcdef".toCharArray();
    private static List<String> allowedHeaders = Arrays.asList("Accept","Accept-Language","Accept-Charset"); 

    /**
     * Converts hex representation of a byte array. The output string
     * length would be (2 * bytes.length )
     *
     * @param bytes bytes for hex encoding
     * @return hex representation of the byte data
     */
    public static String hex(byte bytes[]) {
        StringBuilder sb = new StringBuilder(2 * bytes.length);
        for (byte x : bytes) {
            sb.append(hex[(x >> 4) & 0xf]).append(hex[x & 0xf]);
        }
        return sb.toString();
    }

    /**
     * Converts a String hex representation to its byte array from. The output string
     * length would be string.length() / 2
     *
     * @param string hex string for decoding
     * @return byte array
     */
    public static byte[] hex(String string) {
        if (isTrivial(string)) return new byte[0];
        int len = string.length();
        if (len % 2 == 1) throw new IllegalArgumentException("string size must be even");
        byte array[] = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            String s = string.substring(i, i + 2);
            array[i / 2] = (byte) Integer.parseInt(s, 16);
        }
        return array;
    }

    public static String reorderQueryString(String queryString) {
        try {
            StringBuilder sb = new StringBuilder();
            String[] params = queryString.split("&");
            List paramsList = Arrays.asList(params);
            Collections.sort(paramsList);

            for(int i = 0; i < paramsList.size(); i++) {
                sb.append(paramsList.get(i));
                if(i < paramsList.size()-1) {
                    sb.append("&");
                }
            }

            return sb.toString();
        } catch(Exception e) {
            ignore(e);
        }

        return queryString;
    }

    public static Map<String, List<String>> convertRequestHeaders(HttpServletRequest req) {
        Map<String, List<String>> headers = new LinkedHashMap<String, List<String>>();

        Enumeration enumeration = req.getHeaderNames();
        while (enumeration.hasMoreElements()) {
            String name = (String) enumeration.nextElement();

            if (!"Host".equalsIgnoreCase(name) && allowedHeaders.contains(name)) {
                List<String> headerValues = headers.get(name);
                if(headerValues == null) {
                    headerValues = new LinkedList<String>();
                    headers.put(name, headerValues);
                }
                headerValues.add(req.getHeader(name));
            }
        }
        return headers;
    }

    public static void ignore(Exception e) {
        // Ignoring for now
/*
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        e.printStackTrace(pw);
*/

        httpLogger.log(Level.FINEST, "ignorable exception", e);

    }
}
