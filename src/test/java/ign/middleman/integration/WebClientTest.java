package ign.middleman.integration;

import ign.middleman.helpers.SimpleWebClientListener;
import ign.middleman.helpers.WebClient;
import ign.middleman.helpers.WebResponse;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.servlet.Context;
import org.mortbay.jetty.servlet.ServletHolder;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Proxy;

import static org.junit.Assert.assertEquals;

/**
 * User: cpatni
 * Date: Oct 24, 2010
 * Time: 3:44:21 PM
 */
public class WebClientTest {
    private static Server server;
    private static int port;

    //private static Server origin;
    //private static int originPort;

    @BeforeClass
    public static void init() throws Exception {

        // Port 0 means "assign arbitrarily port number"
        server = new Server(0);

        Context root = new Context(server, "/", Context.SESSIONS);
        root.addServlet(new ServletHolder(new OKServlet()), "/ok");
        root.addServlet(new ServletHolder(new NotFoundServlet()), "/not-found");
        root.addServlet(new ServletHolder(new NotModifiedServlet()), "/not-modified");
        root.addServlet(new ServletHolder(new ClientErrorServlet()), "/client-error");
        root.addServlet(new ServletHolder(new ServerErrorServlet()), "/server-error");
        root.addServlet(new ServletHolder(new SlowServlet()), "/slow");

        server.start();
        port = server.getConnectors()[0].getLocalPort();

    }

    @AfterClass
    public static void shutdown() throws Exception {
        if (server != null) {
            server.stop();
        }
    }

    @Test
    public void proxyTest() throws IOException {
        WebClient wc = new WebClient();
        Proxy via = new Proxy(Proxy.Type.HTTP, new InetSocketAddress("localhost", port));
        WebResponse wr = wc.raw("GET", "http://localhost:" + port + "/ok", via, null);
        assertEquals(200, wr.getStatus());
    }

    @Test
    public void okTest() throws IOException {
        WebClient wc = new WebClient();
        WebResponse wr = wc.raw("GET", "http://localhost:" + port + "/ok", null, null);
        assertEquals(200, wr.getStatus());
        assertEquals("GET", wr.getMethod());
        assertEquals("OK", wr.getMessage());
        assertEquals("text/plain; charset=utf-8", wr.getHeaders().get("Content-Type").get(0));
        assertEquals("Hello World", new String(wr.getBody()));

    }

    @Test
    public void okTestWithListener() throws IOException {
        WebClient wc = new WebClient();
        WebResponse wr = wc.raw("GET", "http://localhost:" + port + "/ok", null, new SimpleWebClientListener());
        assertEquals(200, wr.getStatus());
        assertEquals("OK", wr.getMessage());
        assertEquals("text/plain; charset=utf-8", wr.getHeaders().get("Content-Type").get(0));
        assertEquals("Hello World", new String(wr.getBody()));

    }

    @Test
    public void notModifiedTest() throws IOException {
        WebClient wc = new WebClient();
        WebResponse wr = wc.raw("GET", "http://localhost:" + port + "/not-modified", null, null);
        assertEquals(304, wr.getStatus());

    }

    @Test
    public void notModifiedTestWithListener() throws IOException {
        WebClient wc = new WebClient();
        WebResponse wr = wc.raw("GET", "http://localhost:" + port + "/not-modified", null, new SimpleWebClientListener());
        assertEquals(304, wr.getStatus());

    }

    @Test
    public void notFoundTest() throws IOException {
        WebClient wc = new WebClient();
        WebResponse wr = wc.raw("GET", "http://localhost:" + port + "/not-found", null, null);
        assertEquals(404, wr.getStatus());
        assertEquals("Not Found", wr.getMessage());
        assertEquals("text/html; charset=utf-8", wr.getHeaders().get("Content-Type").get(0));
        assertEquals("Hello World", new String(wr.getBody()));

    }

    @Test
    public void notFoundTestWithListener() throws IOException {
        WebClient wc = new WebClient();
        WebResponse wr = wc.raw("GET", "http://localhost:" + port + "/not-found", null, new SimpleWebClientListener());
        assertEquals(404, wr.getStatus());
        assertEquals("Not Found", wr.getMessage());
        assertEquals("text/html; charset=utf-8", wr.getHeaders().get("Content-Type").get(0));
        assertEquals("Hello World", new String(wr.getBody()));

    }

    @Test
    public void clientError() throws IOException {
        WebClient wc = new WebClient();
        WebResponse wr = wc.raw("GET", "http://localhost:" + port + "/client-error", null, null);
        
        assertEquals(400, wr.getStatus());

    }

    @Test
    public void clientErrorTestWithListener() throws IOException {
        WebClient wc = new WebClient();
        WebResponse wr = wc.raw("GET", "http://localhost:" + port + "/client-error", null, new SimpleWebClientListener());
        assertEquals(400, wr.getStatus());

    }

    @Test
    public void serverError() throws IOException {
        WebClient wc = new WebClient();
        WebResponse wr = wc.raw("GET", "http://localhost:" + port + "/server-error", null, null);
        assertEquals(503, wr.getStatus());

    }

    @Test
    public void serverErrorTestWithListener() throws IOException {
        WebClient wc = new WebClient();
        WebResponse wr = wc.raw("GET", "http://localhost:" + port + "/server-error", null, new SimpleWebClientListener());
        assertEquals(503, wr.getStatus());

    }


    @Test
    public void slowServerTest() throws IOException {
        WebClient wc = new WebClient(5, 5);
        WebResponse wr = wc.raw("GET", "http://localhost:" + port + "/slow", null, null);
        assertEquals(0, wr.getStatus());

    }

    @Test
    public void slowServerTestWithListener() throws IOException {
        WebClient wc = new WebClient(5, 5);
        WebResponse wr = wc.raw("GET", "http://localhost:" + port + "/slow", null, new SimpleWebClientListener() {
            

            @Override
            public void onException(Exception e, String error) {
                super.onException(e, error);
                throw new NullPointerException();
            }
        });
        assertEquals(null, wr);

    }

    public static class OKServlet extends HttpServlet {
        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
            resp.setContentType("text/plain; charset=utf-8");
            resp.getWriter().print("Hello World");

        }
    }


    public static class NotModifiedServlet extends HttpServlet {
        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
            resp.setStatus(304);

        }
    }

    public static class NotFoundServlet extends HttpServlet {
        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
            resp.setStatus(404);
            resp.setContentType("text/html; charset=utf-8");
            resp.getWriter().print("Hello World");

        }
    }
    public static class ClientErrorServlet extends HttpServlet {
        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
            resp.setStatus(400);

        }
    }
    public static class ServerErrorServlet extends HttpServlet {
        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
            resp.setStatus(503);
            resp.getWriter().print("Service is not available");

        }
    }
    public static class SlowServlet extends HttpServlet {
        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
            resp.setStatus(503);
            resp.getWriter().print("Hello World");
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                //e.printStackTrace();
            }


        }
    }
}
