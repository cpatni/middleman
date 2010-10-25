package ign.middleman.integration;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.servlet.Context;
import org.mortbay.jetty.servlet.ServletHolder;
import org.mortbay.jetty.webapp.WebAppContext;

import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.security.SecureRandom;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

import static junit.framework.Assert.assertTrue;
import static org.junit.Assert.assertEquals;

/**
 * User: cpatni
 * Date: Aug 14, 2010
 * Time: 12:05:30 PM
 */
public class MiddlemanIntegrationTest {
    private static Server server;
    private static int port;

    private static Server origin;

    public static String generateRandomPrefix(int size) {
        StringBuilder sb = new StringBuilder(size);

        SecureRandom sr = new SecureRandom();
        sr.nextInt(26);
        for (int i = 0; i < size; i++) {
            sb.append((char) ('a' + sr.nextInt(26)));
        }
        return sb.toString();

    }

    public static void main(String[] args) throws Exception {
        init();

    }

    static StatusServlet statusServlet = new StatusServlet();

    static class StatusServlet extends HttpServlet {
        AtomicInteger counter = new AtomicInteger(0);

        int status = 200;

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
            counter.incrementAndGet();
            resp.setStatus(status);
            resp.setContentType("text/plain; charset=utf-8");
            resp.getWriter().print("Hello World");
        }

        public void setStatus(int status) {
            this.status = status;
        }
    }

    static OKServlet okServlet = new OKServlet();

    static class OKServlet extends HttpServlet {
        AtomicInteger counter = new AtomicInteger(0);

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
            counter.incrementAndGet();
            resp.setContentType("text/plain; charset=utf-8");
            resp.getWriter().print("Hello World");

        }
    }

    static CountingServlet countingServlet = new CountingServlet();

    static class CountingServlet extends HttpServlet {
        AtomicInteger counter = new AtomicInteger(0);

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
            resp.setContentType("text/plain; charset=utf-8");
            resp.getWriter().print("Hello World " + counter.intValue());

        }

        @Override
        public void service(ServletRequest req, ServletResponse res) throws ServletException, IOException {
            counter.incrementAndGet();
            super.service(req, res);
        }

        @Override
        protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
            resp.setContentType("text/plain; charset=utf-8");
            resp.getWriter().print("Hello World " + counter.intValue());
        }

        @Override
        protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
            resp.setContentType("text/plain; charset=utf-8");
            resp.getWriter().print("Hello World " + counter.intValue());
        }

        @Override
        protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
            resp.setContentType("text/plain; charset=utf-8");
            resp.getWriter().print("Hello World " + counter.intValue());
        }
    }

    @BeforeClass
    public static void init() throws Exception {
        Logger.getLogger("Middleman").setLevel(Level.FINEST);
        System.setProperty("environment", "test");
        // Port 0 means "assign arbitrarily port number"
        origin = new Server(0);
        Context root = new Context(origin, "/", Context.SESSIONS);
        root.addServlet(new ServletHolder(okServlet), "/ok");
        root.addServlet(new ServletHolder(countingServlet), "/count");
        root.addServlet(new ServletHolder(statusServlet), "/status");
        origin.start();
        int originPort = origin.getConnectors()[0].getLocalPort();
        System.setProperty("origin", "localhost:" + originPort);
        System.err.println("origin port " + originPort);
        System.setProperty("memcached", "localhost:11211");
        System.setProperty("cacheKeyPrefix", generateRandomPrefix(12));


        // Port 0 means "assign arbitrarily port number"
        server = new Server(0);
        //Context origin = new Context(server, "/origin", Context.SESSIONS);
        //root.addServlet(new ServletHolder(new OKServlet()), "/ok");
        //root.addServlet(new ServletHolder(new NotFoundServlet()), "/not-found");


        WebAppContext origin = new WebAppContext();
        origin.setContextPath("/");

        origin.setResourceBase("src/main/java");
        server.setHandler(origin);

        WebAppContext webapp = new WebAppContext();
        webapp.setContextPath("/");
        webapp.setWar("src/main/webapp");
        server.setHandler(webapp);

        server.start();
        port = server.getConnectors()[0].getLocalPort();

    }


    static class Response {
        int status;
        String message;
        Map<String, List<String>> headers;
        String contentType;
        String body;

        Response(int status, String message, String contentType, Map<String, List<String>> headers, String body) {
            this.status = status;
            this.message = message;
            this.contentType = contentType;
            this.headers = headers;
            this.body = body;
        }

        @Override
        public String toString() {
            return status + message + "\r\n" + body;
        }
    }

    private Response http(String method, String uri, byte[] data) throws IOException {
        URL url = new URL("http", "localhost", port, uri);
        HttpURLConnection request = (HttpURLConnection) url.openConnection();
        request.setRequestMethod(method);
        if ("POST".equalsIgnoreCase(method) || "PUT".equalsIgnoreCase(method)) {
            if (data != null && data.length > 0) {
                request.setDoOutput(true);
                request.getOutputStream().write(data);
            }
        }
        InputStream in;
        try {
            in = request.getInputStream();
        } catch (IOException e) {
            in = request.getErrorStream();
        }
        return new Response(request.getResponseCode(), request.getResponseMessage(), request.getContentType(), request.getHeaderFields(), asString(in));
    }

    private Response get(String uri) throws IOException {
        URL url = new URL("http", "localhost", port, uri);
        HttpURLConnection request = (HttpURLConnection) url.openConnection();
        InputStream in;
        try {
            in = request.getInputStream();
        } catch (IOException e) {
            in = request.getErrorStream();
        }
        return new Response(request.getResponseCode(), request.getResponseMessage(), request.getContentType(), request.getHeaderFields(), asString(in));
    }

    public String asString(InputStream in) throws IOException {
        try {
            StringBuilder fileData = new StringBuilder(1000);
            BufferedReader reader = new BufferedReader(new InputStreamReader(in));
            char[] buf = new char[1024];
            int numRead;
            while ((numRead = reader.read(buf)) != -1) {
                String readData = String.valueOf(buf, 0, numRead);
                fileData.append(readData);
            }
            return fileData.toString();

        }
        finally {
            in.close();
        }
    }

    @Test
    public void addRuleTest() throws IOException, InterruptedException {
        //okServlet.counter.set(0);
        Response response = get("/_refresh?regex=.*tags%3Drotten.*");
        assertEquals(200, response.status);
        response = get("/_refresh?regex=.*tags%3Ddirty.*");
        assertEquals(200, response.status);
    }


    @Test
    public void simpleTest() throws IOException, InterruptedException {
        okServlet.counter.set(0);
        Response response = get("/ok");
        assertEquals(200, response.status);
        assertEquals(1, okServlet.counter.intValue());
        Thread.sleep(15L);
        response = get("/ok");
        assertEquals(200, response.status);
        assertEquals(1, okServlet.counter.intValue());
        Thread.sleep(30L);
    }

    @Test
    public void refreshTimeoutTest() throws IOException, InterruptedException {
        countingServlet.counter.set(0);
        Response response = get("/count");
        assertEquals(200, response.status);
        assertEquals(1, countingServlet.counter.intValue());
        //wait for 15 ms and we should have in the cache
        Thread.sleep(15L);
        response = get("/count");
        assertEquals(200, response.status);
        assertEquals(1, countingServlet.counter.intValue());
        //wait for 5+ seconds and it should be refreshing
        Thread.sleep(5500L);
        response = get("/count");
        assertEquals(200, response.status);
        //wait for 15 ms and we should have in the cache
        Thread.sleep(15L);
        //it should have been refreshed by now
        response = get("/count");
        assertEquals(200, response.status);
        assertEquals(2, countingServlet.counter.intValue());
    }

    @Test
    public void cacheInvalidationTest() throws IOException, InterruptedException {
        countingServlet.counter.set(0);
        Response response = get("/count?tags=silly");
        assertEquals(200, response.status);
        assertEquals(1, countingServlet.counter.intValue());
        //wait for 15 ms and we should have in the cache
        response = get("/count?tags=silly");
        assertEquals(200, response.status);
        assertEquals(1, countingServlet.counter.intValue());

        response = get("/_refresh?regex=.*tags%3Dsilly.*");
        assertEquals(200, response.status);
        Thread.sleep(15L);
        response = get("/count?tags=silly");
        assertEquals(200, response.status);
        Thread.sleep(15L);
        response = get("/count?tags=silly");
        assertEquals(200, response.status);
        assertEquals(2, countingServlet.counter.intValue());
    }

    @Test
    public void fetchFromOriginAndRefreshCacheTest() throws IOException, InterruptedException {
        countingServlet.counter.set(0);
        Response response = get("/count?tags=ffo");
        assertEquals(200, response.status);
        assertEquals(1, countingServlet.counter.intValue());
        response = get("/_refresh?url=" + URLEncoder.encode(new URL("http", "localhost", port, "/count?tags=ffo").toString(), "utf-8"));
        assertEquals(200, response.status);
        Thread.sleep(15L);
        response = get("/count?tags=ffo");
        assertEquals(200, response.status);
        assertEquals(2, countingServlet.counter.intValue());
    }

    @Test
    public void nonGetTests() throws IOException, InterruptedException {
        countingServlet.counter.set(0);
        Response response = http("POST", "/count", "foo=bar&baz=fooz".getBytes("utf-8"));
        assertEquals(200, response.status);
        assertEquals(1, countingServlet.counter.intValue());

        response = http("POST", "/count", "foo=bar&baz=fooz".getBytes("utf-8"));
        assertEquals(200, response.status);
        assertEquals(2, countingServlet.counter.intValue());

        response = http("PUT", "/count", "foo=bar&baz=fooz".getBytes("utf-8"));
        assertEquals(200, response.status);
        assertEquals(3, countingServlet.counter.intValue());


        response = http("PUT", "/count", "foo=bar&baz=fooz".getBytes("utf-8"));
        assertEquals(200, response.status);
        assertEquals(4, countingServlet.counter.intValue());


        response = http("DELETE", "/count", "foo=bar&baz=fooz".getBytes("utf-8"));
        assertEquals(200, response.status);
        assertEquals(5, countingServlet.counter.intValue());


        response = http("DELETE", "/count", "foo=bar&baz=fooz".getBytes("utf-8"));
        assertEquals(200, response.status);
        assertEquals(6, countingServlet.counter.intValue());
    }

    @Test
    public void flushOnClienErrortTest() throws IOException, InterruptedException {
        statusServlet.counter.set(0);
        statusServlet.setStatus(200);


        Response response = get("/status");
        assertEquals(200, response.status);
        assertEquals(1, statusServlet.counter.intValue());

        response = get("/status");
        assertEquals(200, response.status);
        assertEquals(1, statusServlet.counter.intValue());


        response = get("/status");
        assertEquals(200, response.status);
        assertEquals(1, statusServlet.counter.intValue());

        statusServlet.setStatus(404);
        response = get("/_refresh?url=" + URLEncoder.encode(new URL("http", "localhost", port, "/status").toString(), "utf-8"));
        assertEquals(200, response.status);
        Thread.sleep(15L);
        assertEquals(2, statusServlet.counter.intValue());

        response = get("/status");
        assertEquals(404, response.status);
        assertEquals(3, statusServlet.counter.intValue());

        statusServlet.setStatus(200);


        response = get("/status");
        assertEquals(200, response.status);
        assertEquals(4, statusServlet.counter.intValue());

        response = get("/status");
        assertEquals(200, response.status);
        assertEquals(4, statusServlet.counter.intValue());
    }


    @Test
    public void queryParameterOrderTest() throws IOException, InterruptedException {
        countingServlet.counter.set(0);

        Response response = get("/count?a=b&c=d&e=f");
        assertEquals(200, response.status);
        assertEquals(1, countingServlet.counter.intValue());

        response = get("/count?a=b&e=f&c=d");
        assertEquals(200, response.status);
        assertEquals(1, countingServlet.counter.intValue());

        response = get("/count?c=d&a=b&e=f");
        assertEquals(200, response.status);
        assertEquals(1, countingServlet.counter.intValue());

        response = get("/count?e=f&a=b&c=d");
        assertEquals(200, response.status);
        assertEquals(1, countingServlet.counter.intValue());
    }


    @AfterClass
    public static void shutdown() throws Exception {
        Thread.sleep(3000L);
        if (server != null) {
            server.stop();
        }

        if (origin != null) {
            origin.stop();
        }
    }

}
