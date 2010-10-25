package ign.middleman.helpers;

import org.junit.Test;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.junit.Assert.assertEquals;

/**
 * User: cpatni
 * Date: Oct 23, 2010
 * Time: 3:49:00 PM
 */
public class WebResponseTest {
    private WebResponse newWebResponse() {
        Map<String, List<String>> headers = new LinkedHashMap<String, List<String>>();
        headers.put("Content-Type", Arrays.asList("text/plain"));
        return new WebResponse("GET", 200, "OK", headers, "simple body".getBytes());
    }



    @Test
    public void testSerialization() throws IOException, ClassNotFoundException {
        WebResponse wr = newWebResponse();
        WebResponse wrt = roundtrip(wr);
        assertEquals(wr.getTimestamp(), wrt.getTimestamp());
        assertEquals(wr.status, wrt.status);
        //assertEquals(wr.message, wrt.message);
        assertEquals(new String(wr.body), new String(wrt.body));


    }

    private WebResponse roundtrip(WebResponse wr) throws IOException, ClassNotFoundException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        wr.writeExternal(oos);
        oos.close();


        WebResponse wrt = new WebResponse();
        wrt.readExternal(new ObjectInputStream(new ByteArrayInputStream(baos.toByteArray())));
        return wrt;
    }

    @Test
    public void testSerializationNoBody() throws IOException, ClassNotFoundException {
        Map<String, List<String>> headers = new LinkedHashMap<String, List<String>>();

        WebResponse wr = new WebResponse("GET", 200, "OK", headers, "".getBytes());
        WebResponse wrt = roundtrip(wr);
        assertEquals(new String(wr.body), new String(wrt.body));
    }

    @Test
    public void toStringTest() {
        WebResponse wr = newWebResponse();
        String expected = "200 OK\r\n" +
                "simple body";
        assertEquals(expected, wr.toString());


    }

    @Test
    public void getHeadersTest() {
        Map<String, List<String>> headers = new LinkedHashMap<String, List<String>>();
        headers.put("Content-Type", Arrays.asList("text/plain"));
        WebResponse wr = new WebResponse("GET", 200, "OK", headers, "simple body".getBytes());
        assertEquals(headers, wr.getHeaders());
    }

    @Test
    public void okTest() {
        WebResponse wr = newWebResponse();
        assertTrue(wr.isOK());
        wr.status = 299;
        assertTrue(wr.isOK());
        wr.status = 301;
        assertTrue(wr.isOK());
        wr.status = 300;
        assertFalse(wr.isOK());
        wr.status = 302;
        assertFalse(wr.isOK());
        wr.status = 400;
        assertFalse(wr.isOK());
        wr.status = 500;
        assertFalse(wr.isOK());
    }

    @Test
    public void is4XXTest() {
        WebResponse wr = newWebResponse();
        assertFalse(wr.is4XX());
        wr.status = 299;
        assertFalse(wr.is4XX());
        wr.status = 301;
        assertFalse(wr.is4XX());
        wr.status = 300;
        assertFalse(wr.is4XX());
        wr.status = 302;
        assertFalse(wr.is4XX());
        wr.status = 400;
        assertTrue(wr.is4XX());
        wr.status = 500;
        assertFalse(wr.is4XX());
    }

    @Test
    public void writeToTestNullHeaders() throws IOException {
        WebResponse wr = newWebResponse();
        wr.headers = null;
        HttpServletResponse hr = createMock(HttpServletResponse.class);
        ServletOutputStream sos = createMock(ServletOutputStream.class);
        hr.setStatus(wr.status);
        expect(hr.getOutputStream()).andReturn(sos);

        replay(hr);
        wr.writeTo(hr);


    }
    @Test
    public void writeToTest() throws IOException {
        WebResponse wr = newWebResponse();
        HttpServletResponse hr = createMock(HttpServletResponse.class);
        ServletOutputStream sos = createMock(ServletOutputStream.class);
        hr.setStatus(wr.status);
        hr.addHeader("Content-Type","text/plain");
        expect(hr.getOutputStream()).andReturn(sos);

        replay(hr);
        wr.writeTo(hr);


    }

    @Test
    public void writeToTestIncludingDate() throws IOException {
        WebResponse wr = newWebResponse();
        wr.headers.put("Date", Arrays.asList("today"));
        HttpServletResponse hr = createMock(HttpServletResponse.class);
        ServletOutputStream sos = createMock(ServletOutputStream.class);
        hr.setStatus(wr.status);
        hr.addHeader("Content-Type","text/plain");
        expect(hr.getOutputStream()).andReturn(sos);

        replay(hr);
        wr.writeTo(hr);


    }

    @Test
    public void writeToTestIncludingNullHeader() throws IOException {
        WebResponse wr = newWebResponse();
        wr.headers.put(null, Arrays.asList("today"));
        wr.headers.put("Null Keys", Arrays.asList(""));
        HttpServletResponse hr = createMock(HttpServletResponse.class);
        ServletOutputStream sos = createMock(ServletOutputStream.class);
        hr.setStatus(wr.status);
        hr.addHeader("Content-Type","text/plain");
        expect(hr.getOutputStream()).andReturn(sos);

        replay(hr);
        wr.writeTo(hr);


    }

    @Test
    public void writeToTestWithHeaderString() throws IOException {
        WebResponse wr = newWebResponse();
        wr.headers = null;
        wr.headersString = "Content-Type: text/plain\r\n"+
                "Server: Jetty";
        HttpServletResponse hr = createMock(HttpServletResponse.class);
        ServletOutputStream sos = createMock(ServletOutputStream.class);
        hr.setStatus(wr.status);
        hr.addHeader("Content-Type","text/plain");
        hr.addHeader("Server","Jetty");
        expect(hr.getOutputStream()).andReturn(sos);

        replay(hr);
        wr.writeTo(hr);


    }

    @Test
    public void writeToTestWithHeaderStringIncludingDate() throws IOException {
        WebResponse wr = newWebResponse();
        wr.headers = null;
        wr.headersString = "Content-Type: text/plain\r\n"+
                "Server: Jetty\r\n"+"" +
                "Date: today";
        HttpServletResponse hr = createMock(HttpServletResponse.class);
        ServletOutputStream sos = createMock(ServletOutputStream.class);
        hr.setStatus(wr.status);
        hr.addHeader("Content-Type","text/plain");
        hr.addHeader("Server","Jetty");
        expect(hr.getOutputStream()).andReturn(sos);

        replay(hr);
        wr.writeTo(hr);


    }

/*
    @Test
    public void writeToTestWithHeaderStringIncludingNullHeader() throws IOException {
        WebResponse wr = newWebResponse();
        wr.headers = null;
        wr.headersString = "Content-Type: text/plain\r\n"+
                "Server: Jetty\r\n"+"" +
                ": Something\r\n"+"" +
                "Date: today";
        HttpServletResponse hr = createMock(HttpServletResponse.class);
        ServletOutputStream sos = createMock(ServletOutputStream.class);
        hr.setStatus(wr.status);
        hr.addHeader("Content-Type","text/plain");
        hr.addHeader("Server","Jetty");
        expect(hr.getOutputStream()).andReturn(sos);

        replay(hr);
        wr.writeTo(hr);
    }
*/

    @Test
    public void addH() {
        WebResponse wr = newWebResponse();
        wr.headers.put("Server", Arrays.asList("Jetty"));
        wr.headers.put("  ", Arrays.asList("blank value"));
        wr.headers.put("Simple", Arrays.asList("  "));
        assertEquals("Content-Type: text/plain\r\nServer: Jetty\r\n", wr.serializeHeadersToString());



    }

}
