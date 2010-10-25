package ign.middleman.controllers;

import com.google.inject.Provider;
import ign.middleman.helpers.Rule;
import ign.middleman.helpers.WebResponse;
import net.spy.memcached.CASMutator;
import org.junit.Assert;
import org.junit.Test;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static ign.middleman.helpers.ApplicationHelper.hex;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.junit.Assert.assertEquals;

/**
 * User: cpatni
 * Date: Oct 4, 2010
 * Time: 11:56:21 PM
 */
public class MiddlemanTest {
    @Test
    public void setOriginOnlyHostnameTest() {
        Middleman mm = mm();
        mm.setOrigin("origin.example.com");
        assertEquals("origin.example.com", mm.originHost);
        assertEquals(80, mm.originPort);

    }

    @Test
    public void setOriginTest() {
        Middleman mm = mm();
        mm.setOrigin("origin.example.com:9090");
        assertEquals("origin.example.com", mm.originHost);
        assertEquals(9090, mm.originPort);

    }

    private Middleman mm() {
        Middleman mm = new Middleman();
        return mm;
    }

    @Test
    public void setTimeoutTest() {
        Middleman mm = mm();
        mm.setCachedTimeout(7200, 7*24*3600, 5);
        assertEquals(7200, mm.memcachedTimeout);
        assertEquals(7*24*3600*1000, mm.refreshTimeout);
    }

    @Test
    public void makeKeyTest() {
        Middleman mm = mm();
        assertEquals("818492beabce92630298c5cc9d88fbe69a26be47", mm.makeKey("GET", "http://www.example.com/"));
    }

    @Test
    public void sha1Test() {
        Middleman mm = mm();
        assertEquals("4675a4f4a9b656c80b3329104545b5654e5d95e0", mm.sha1("http://wwww.example.com/"));
    }

    @Test
    public void urlTestNullQuery() {
        HttpServletRequest request = createMock(HttpServletRequest.class);
        expect(request.getRequestURL()).andReturn(new StringBuffer("http://localhost:5050/foo"));
        expect(request.getQueryString()).andReturn(null);
        replay(request);

        Middleman mm = mm();

        Assert.assertEquals("http://localhost:5050/foo", mm.url(request));
    }
    @Test
    public void urlTestWithQuery() {
        HttpServletRequest request = createMock(HttpServletRequest.class);
        expect(request.getRequestURL()).andReturn(new StringBuffer("http://localhost:5050/foo"));
        expect(request.getQueryString()).andReturn("x=y&a=b&c=d&").anyTimes();
        replay(request);
        Middleman mm = mm();

        Assert.assertEquals("http://localhost:5050/foo?a=b&c=d&x=y", mm.url(request));
    }

    @Test
    public void transferTest() throws IOException {
        Middleman mm = mm();
        byte[] data = new byte[1024];
        Arrays.fill(data, (byte) 3);
        ByteArrayInputStream bais = new ByteArrayInputStream(data);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        mm.transfer(bais, baos);
        Assert.assertArrayEquals(data, baos.toByteArray());
    }

    @Test
    public void setCachedRulesKeyTest() throws IOException {
        Middleman mm = mm();
        mm.setCachedRulesKey("random-key");
        assertEquals("random-key", mm.getCachedRulesKey());


    }

    @Test
    public void addRefreshRuleExceptionTest() throws IOException {
        Middleman mm = mm();
        mm.setCASMutatorProvider(new Provider<CASMutator> () {


            public CASMutator get() {
                throw new RuntimeException("for testing");
            }
        });
        assertEquals(Collections.<Rule>emptyList(), mm.addRefreshRule("foo"));


    }

    @Test
    public void nullWebResponseTest() throws IOException, ServletException {
        HttpServletRequest request = createMock(HttpServletRequest.class);
        expect(request.getRequestURL()).andReturn(new StringBuffer("http://localhost:5050/foo"));
        expect(request.getQueryString()).andReturn("x=y&a=b&c=d&").anyTimes();
        replay(request);

        Middleman mm = new Middleman() {

            @Override
            WebResponse getWebResponse(HttpServletRequest request, String url) throws IOException {
                return null;
            }

            @Override
            public WebResponse getWebResponse(String url, String method, Map<String, List<String>> headers) throws IOException {
                return null;
            }
        };
        mm.doPost(request, null);

    }



}
