package ign.middleman.helpers;

import org.junit.Assert;
import org.junit.Test;

import javax.servlet.http.HttpServletRequest;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;

import static ign.middleman.helpers.ApplicationHelper.getIP;
import static ign.middleman.helpers.ApplicationHelper.getTrueClientIPFromXFF;
import static ign.middleman.helpers.ApplicationHelper.isNonTrivial;
import static ign.middleman.helpers.ApplicationHelper.isTrivial;
import static ign.middleman.helpers.ApplicationHelper.tryNonNull;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertTrue;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;


/**
 * User: cpatni
 * Date: Aug 8, 2010
 * Time: 1:45:46 PM
 */
public class ApplicationHelperTest {

    @Test
    public void privateConstructorTest() throws InstantiationException,
            NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        Constructor<ApplicationHelper> cons = ApplicationHelper.class.getDeclaredConstructor();
        assertFalse(cons.isAccessible());
        cons.setAccessible(true);
        assertEquals(ApplicationHelper.class, cons.newInstance().getClass());

    }

    @Test
    public void tryNonNullFirstItem() throws Exception {
        String s1 = "foo";
        String s2 = tryNonNull(s1, "bar");
        assertEquals(s1, s2);

    }

    @Test
    public void testNonNullSecondItem() {
        Integer n1 = 42;
        Integer n2 = tryNonNull(null, n1);
        assertEquals(n1, n2);
    }

    @Test
    public void testNonNullAllNull() {
        assertNull(tryNonNull(null, null));
    }

    @Test
    public void specifiedIPTest() {
        assertEquals("212.101.97.206", getIP("212.101.97.206", null));
    }

    @Test
    public void xffTest() {
        HttpServletRequest request = createMock(HttpServletRequest.class);
        expect(request.getHeader("X-Forwarded-For")).andReturn("45.43.4.5,212.101.97.206,127.0.0.1");
        replay(request);
        assertEquals("45.43.4.5", getIP(null, request));
    }


    @Test
    public void socketIpTestEmptyXFF() {
        HttpServletRequest request = createMock(HttpServletRequest.class);
        expect(request.getHeader("X-Forwarded-For")).andReturn("  ");
        expect(request.getRemoteAddr()).andReturn("212.101.97.206");
        replay(request);
        assertEquals("212.101.97.206", getIP(null, request));
    }

    @Test
    public void selfAsIp() {
        HttpServletRequest request = createMock(HttpServletRequest.class);
        expect(request.getHeader("X-Forwarded-For")).andReturn("  ");
        expect(request.getRemoteAddr()).andReturn("212.101.97.206");
        replay(request);
        assertEquals("212.101.97.206", getIP("self", request));
    }

    @Test
    public void currentAsIp() {
        HttpServletRequest request = createMock(HttpServletRequest.class);
        expect(request.getHeader("X-Forwarded-For")).andReturn("  ");
        expect(request.getRemoteAddr()).andReturn("212.101.97.206");
        replay(request);
        assertEquals("212.101.97.206", getIP("current", request));
    }

    @Test
    public void socketIpTestNullXFF() {
        HttpServletRequest request = createMock(HttpServletRequest.class);
        expect(request.getHeader("X-Forwarded-For")).andReturn(null);
        expect(request.getRemoteAddr()).andReturn("212.101.97.206");
        replay(request);
        assertEquals("212.101.97.206", getIP(null, request));
    }

    @Test
    public void getTrueClientIPFromXFFTest() {
        HttpServletRequest request = createMock(HttpServletRequest.class);
        expect(request.getHeader("X-Forwarded-For")).andReturn("212.101.97.206");
        replay(request);
        assertEquals("212.101.97.206", getTrueClientIPFromXFF(request));

    }

    @Test
    public void testNullAsTrivial() {
        assertTrue(isTrivial(null));
    }

    @Test
    public void testBlankAsTrivial() {
        assertTrue(isTrivial("   "));
    }

    @Test
    public void testTrivialTueNegatives() {
        assertFalse(isTrivial("Hello"));
    }

    @Test
    public void testNullAsNonTrivialNegative() {
        assertFalse(isNonTrivial(null));
    }

    @Test
    public void testBlankAsNonTrivialNegative() {
        assertFalse(isNonTrivial("   "));
    }

    @Test
    public void testHelloAsTrivial() {
        assertTrue(isNonTrivial("Hello"));
    }

    @Test
    public void urlTestNullQuery() {
        HttpServletRequest request = createMock(HttpServletRequest.class);
        expect(request.getRequestURL()).andReturn(new StringBuffer("http://localhost:5050/foo"));
        expect(request.getQueryString()).andReturn(null);
        replay(request);

        assertEquals("http://localhost:5050/foo", ApplicationHelper.url(request));
    }
    @Test
    public void urlTestWithQuery() {
        HttpServletRequest request = createMock(HttpServletRequest.class);
        expect(request.getRequestURL()).andReturn(new StringBuffer("http://localhost:5050/foo"));
        expect(request.getQueryString()).andReturn("a=b&c=d").anyTimes();
        replay(request);

        assertEquals("http://localhost:5050/foo?a=b&c=d", ApplicationHelper.url(request));
    }

    @Test
    public void equalsTest() {
        assertTrue(ApplicationHelper.equals(null, null));
        assertTrue(ApplicationHelper.equals("s1", "s1"));
        assertFalse(ApplicationHelper.equals("s1", "s2"));
        assertFalse(ApplicationHelper.equals("s1", null));
        assertFalse(ApplicationHelper.equals(null, "s1"));
        assertFalse(ApplicationHelper.equals("s1", "S1"));

    }

    @Test
    public void equalsIgnoreTestTest() {
        assertTrue(ApplicationHelper.equalsIgnoreCase(null, null));
        assertTrue(ApplicationHelper.equalsIgnoreCase("s1", "s1"));
        assertFalse(ApplicationHelper.equalsIgnoreCase("s1", "s2"));
        assertFalse(ApplicationHelper.equalsIgnoreCase("s1", null));
        assertFalse(ApplicationHelper.equalsIgnoreCase(null, "s1"));
        assertTrue(ApplicationHelper.equalsIgnoreCase("s1", "S1"));

    }

    @Test
    public void hexTest() {
        byte[] cafebabe = {-54, -2, -70, -66};
        assertArrayEquals(cafebabe, ApplicationHelper.hex("cafebabe"));
        assertArrayEquals(new byte[0], ApplicationHelper.hex(""));
        assertEquals("cafebabe", ApplicationHelper.hex(cafebabe));


        //System.out.println(Arrays.toString(ApplicationHelper.hex("cafebabe")));
        //assertEquals();
    }

    @Test (expected=IllegalArgumentException.class)
    public void invalidHexTest() {
        ApplicationHelper.hex("cafebabee");
    }

    @Test
    public void reorderQueryStringTest() {
        assertEquals(null, ApplicationHelper.reorderQueryString(null));
        assertEquals("a=&c=d", ApplicationHelper.reorderQueryString("a=&c=d"));
        assertEquals("a=&c=d", ApplicationHelper.reorderQueryString("c=d&a="));

        assertEquals("a=b&c=d", ApplicationHelper.reorderQueryString("a=b&c=d"));
        assertEquals("a=b&c=d", ApplicationHelper.reorderQueryString("c=d&a=b"));
    }

    @Test
    public void convertRequestHeadersTestEmptyHeaders() {

        Enumeration<String> en = Collections.enumeration(Collections.<String>emptyList());
        HttpServletRequest request = createMock(HttpServletRequest.class);
        expect(request.getHeaderNames()).andReturn(en);
        replay(request);
        Map<String,List<String>> map = ApplicationHelper.convertRequestHeaders(request);
        assertEquals(0, map.size());
    }

    @Test
    public void convertRequestHeadersTestNotAllowedHeaders() {
        Enumeration<String> en = Collections.enumeration(Arrays.asList("Host", "Cookie"));
        HttpServletRequest request = createMock(HttpServletRequest.class);
        expect(request.getHeaderNames()).andReturn(en);
        replay(request);
        Map<String,List<String>> map = ApplicationHelper.convertRequestHeaders(request);
        assertEquals(0, map.size());
    }
    @Test
    public void convertRequestHeadersTestAllowedHeaders() {
        Enumeration<String> en = Collections.enumeration(Arrays.asList("Host", "Cookie", "Accept", "Accept-Language", "Accept-Language", "Accept-Charset"));
        HttpServletRequest request = createMock(HttpServletRequest.class);
        expect(request.getHeaderNames()).andReturn(en);
        expect(request.getHeader("Accept")).andReturn("text/xml");
        expect(request.getHeader("Accept-Language")).andReturn("en");
        expect(request.getHeader("Accept-Language")).andReturn("fr");
        expect(request.getHeader("Accept-Charset")).andReturn("utf-8");
        replay(request);
        Map<String,List<String>> map = ApplicationHelper.convertRequestHeaders(request);
        assertEquals(3, map.size());
        assertEquals("{Accept=[text/xml], Accept-Language=[en, fr], Accept-Charset=[utf-8]}", map.toString());
    }
}
