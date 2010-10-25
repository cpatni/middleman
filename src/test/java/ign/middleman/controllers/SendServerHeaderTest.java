package ign.middleman.controllers;

import ign.middleman.controllers.SendServerHeader;
import org.junit.Test;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.net.UnknownHostException;

import static junit.framework.Assert.assertNull;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.replay;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;

/**
 * User: cpatni
 * Date: Aug 9, 2010
 * Time: 11:02:38 AM
 */

public class SendServerHeaderTest {
    @Test
    public void initServerTest() throws ServletException {
        SendServerHeader ssh = new SendServerHeader();
        String host = ssh.host;
        ssh.init(null);
        assertNotSame(host, ssh.host);
    }

    @Test
    public void initExceptionTest() throws ServletException {

        //we orverride the thisHost to simulate UnknownHostException
        SendServerHeader ssh = new SendServerHeader() {
            @Override
            String thisHost() throws UnknownHostException {
                throw new UnknownHostException();
            }
        };
        assertNull(ssh.host);
        ssh.init(null);
        assertEquals("localhost", ssh.host);
    }

    @Test
    public void testDoFilter() throws Exception {
        SendServerHeader ssh = new SendServerHeader();
        ssh.init(null);
        HttpServletRequest request = createMock(HttpServletRequest.class);
        HttpServletResponse response = createMock(HttpServletResponse.class);
        FilterChain chain = createMock(FilterChain.class);
        response.setHeader("X-Via-Server", ssh.host);
        response.setHeader("Via", "Middleman/1.0");
        chain.doFilter(request, response);
        replay(request, response, chain);
        ssh.doFilter(request, response, chain);
    }

    


}
