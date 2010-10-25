package ign.middleman.helpers;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URI;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * User: cpatni
 * Date: May 4, 2009
 * Time: 8:03:07 PM
 */
public class WebClient {
    private static Logger httpLogger = Logger.getLogger("WebClientLogger");
    private static final String UTF_8 = "UTF-8";

    int connectTimeout = 3000;
    int readTimeout = 80000;

    public WebClient(int connectTimeout, int readTimeout) {
        this.connectTimeout = connectTimeout;
        this.readTimeout = readTimeout;
    }

    public WebClient() {
    }


    byte[] readBytesFullyAndClose(String method, String url, InputStream is, int contentLength) throws IOException {
        if (is == null) {
            return new byte[0];
            //throw new IOException("Null input stream for HTTP " + method + " " + url);
        }
        try {
            //httpLogger.warning("Buffering response body");
            ByteArrayOutputStream baos = new ByteArrayOutputStream(
                    contentLength > 0 ? contentLength : 4096);
            byte[] buffer = new byte[4096];
            int len;
            while ((len = is.read(buffer)) > 0) {
                baos.write(buffer, 0, len);
            }
            return baos.toByteArray();
        }
        finally {
            is.close();
        }
    }


    public WebResponse raw(String method, String url, Proxy via, WebClientListener wcl) throws IOException {
        URI uri = URI.create(url);
        if (via != null && via.address() instanceof InetSocketAddress) {
            InetSocketAddress sock = (InetSocketAddress) via.address();
            if (sock.getHostName().equals(uri.getHost())) {
                uri = URI.create(url.replace(String.valueOf(uri.getPort()), String.valueOf(sock.getPort())));

            }
        }
        URL u = uri.toURL();
        HttpURLConnection hurl = (HttpURLConnection) (via == null ? u.openConnection() : u.openConnection(via));
        hurl.setConnectTimeout(connectTimeout);
        hurl.setReadTimeout(readTimeout);
        hurl.setRequestMethod(method);

        if (wcl != null) {
            wcl.before(hurl);
        }
        long start = System.currentTimeMillis();
        int status = 0;
        String statusText = null;
        try {
            try {
                status = hurl.getResponseCode();
                statusText = hurl.getResponseMessage();

                if (wcl != null) {
                    wcl.after(hurl, status, statusText);
                }
                byte[] response = readBytesFullyAndClose(method, url, hurl.getInputStream(), hurl.getContentLength());
                if (wcl != null) {
                    wcl.onSuccess(hurl, "<<<Reading raw>>");

/*
                    if (status < 400) {
                        wcl.onSuccess(hurl, "<<<Reading raw>>");
                    } else {
                        wcl.onError(hurl, "<<<Reading raw>>");
                    }
*/
                }
                return new WebResponse(method, status, statusText, hurl.getHeaderFields(), response);
            } catch (Exception e) {
                try {
                    byte[] response = readBytesFullyAndClose("GET", url, hurl.getErrorStream(), hurl.getContentLength());
                    if (wcl != null) {
                        wcl.onException(e, "<<Reading raw>>");
                    }
                    return new WebResponse(hurl.getRequestMethod(), status, statusText, hurl.getHeaderFields(), response);
                } catch (Exception e1) {
                    if (wcl != null) {
                        wcl.onException(e1);
                    }
                }
            }
        } finally {
            if (wcl != null) {
                wcl.end(hurl, status, statusText);
            }
            if (httpLogger.isLoggable(Level.INFO)) {
                long time = System.currentTimeMillis();
                httpLogger.info("Status: " + status + " Time: " + (time - start) + " ms URL: " + url);
            }
        }
        return null;
    }


}
