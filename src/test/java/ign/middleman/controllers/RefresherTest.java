package ign.middleman.controllers;

import ign.middleman.helpers.Rule;
import org.junit.Test;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;

import static junit.framework.Assert.assertEquals;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.junit.Assert.assertTrue;

/**
 * User: cpatni
 * Date: Oct 14, 2010
 * Time: 1:18:56 AM
 */
public class RefresherTest {

    @Test
    public void refreshUrlTest() throws IOException, ServletException {
        HttpServletRequest request = createMock(HttpServletRequest.class);

        HttpServletResponse response = createMock(HttpServletResponse.class);
        Middleman middleman = createMock(Middleman.class);
        expect(request.getParameter("url")).andReturn("http://www.example.com/index.html").anyTimes();
        expect(request.getParameter("regex")).andReturn(null).anyTimes();
        middleman.fetchFromOriginAndRefreshCache("http://www.example.com/index.html");
        
        Refresher r = new Refresher();
        r.setMiddleman(middleman);
        replay(request, response, middleman);


        r.doGet(request, response);



    }

    @Test
    public void refreshUrlWithQueryTest() throws IOException, ServletException {
        HttpServletRequest request = createMock(HttpServletRequest.class);

        HttpServletResponse response = createMock(HttpServletResponse.class);
        Middleman middleman = createMock(Middleman.class);
        expect(request.getParameter("url")).andReturn("http://www.example.com/index.html?a=b&c=d").anyTimes();
        expect(request.getParameter("regex")).andReturn(null).anyTimes();
        middleman.fetchFromOriginAndRefreshCache("http://www.example.com/index.html?a=b&c=d");

        Refresher r = new Refresher();
        r.setMiddleman(middleman);
        replay(request, response, middleman);
        r.doGet(request, response);
    }

    @Test
    public void refreshRegexTest() throws IOException, ServletException {
        HttpServletRequest request = createMock(HttpServletRequest.class);

        HttpServletResponse response = createMock(HttpServletResponse.class);
        Middleman middleman = createMock(Middleman.class);
        expect(request.getParameter("url")).andReturn(null).anyTimes();
        expect(request.getParameter("regex")).andReturn(".*tags=foo.*").anyTimes();
        expect(middleman.addRefreshRule(".*tags=foo.*")).andReturn(Collections.singleton(new Rule(".*tags=foo.*")));

        Refresher r = new Refresher();
        r.setMiddleman(middleman);
        replay(request, response, middleman);
        r.doGet(request, response);

    }
    @Test
    public void refreshTrivialRegexTest() throws IOException, ServletException {
        HttpServletRequest request = createMock(HttpServletRequest.class);

        HttpServletResponse response = createMock(HttpServletResponse.class);
        Middleman middleman = createMock(Middleman.class);
        expect(request.getParameter("url")).andReturn(null).anyTimes();
        expect(request.getParameter("regex")).andReturn("   ").anyTimes();

        Refresher r = new Refresher();
        r.setMiddleman(middleman);
        replay(request, response, middleman);
        r.doGet(request, response);

    }

    
/*

    @Test
    public void addFirstRule2() throws IOException, ServletException {
        HttpServletRequest request = createMock(HttpServletRequest.class);
        HttpServletResponse response = createMock(HttpServletResponse.class);
        MemcachedClient memcachedClient = createMock(MemcachedClient.class);
        CASMutator casm = createMock(CASMutator.class);

        Rule rule = new Rule(".*tags=foo.*");
        final Collection<Rule> initialValue=Collections.singletonList(rule);

        //expect(casm.cas(INVALIDATION_CACHE_KEY, initialValue, 0, mu);

        //casm.cas(INVALIDATION_CACHE_KEY, initialValue,0m );


        Refresher refersher = new Refresher() {

            @Override
            Collection<Rule> cas(CASMutator<Collection<Rule>> mutator, CASMutation<Collection<Rule>> mutation, Collection<Rule> initialValue) throws Exception {
                return initialValue;
            }
        };

        refersher.setMemcachedClient(memcachedClient);
        Collection<Rule> rules = refersher.addRule(rule);
        assertEquals(1, rules.size());




    }


    @Test
    public void addFirstRule() throws IOException, ServletException {

        HttpServletRequest request = createMock(HttpServletRequest.class);
        HttpServletResponse response = createMock(HttpServletResponse.class);
        MemcachedClient memcachedClient = createMock(MemcachedClient.class);
        CASMutator casm = createMock(CASMutator.class);

        Rule rule = new Rule(".*tags=foo.*");
        final Collection<Rule> initialValue=Collections.singletonList(rule);

        //expect(casm.cas(INVALIDATION_CACHE_KEY, initialValue, 0, mu);

        //casm.cas(INVALIDATION_CACHE_KEY, initialValue,0m );


        Refresher refersher = new Refresher() {

            @Override
            Collection<Rule> cas(CASMutator<Collection<Rule>> mutator, CASMutation<Collection<Rule>> mutation, Collection<Rule> initialValue) throws Exception {
                return initialValue;
            }
        };

        refersher.setMemcachedClient(memcachedClient);
        Collection<Rule> rules = refersher.addRule(rule);
        assertEquals(1, rules.size());



    }
*/
/*
        expect(request.getParameter("toggle")).andReturn(null).anyTimes();
        expect(request.getParameter("passcode")).andReturn(null).anyTimes();

        StringWriter output = new StringWriter();
        PrintWriter out = new PrintWriter(output);
        response.setContentType("text/html");
        expect(response.getWriter()).andReturn(out).anyTimes();
        replay(request, response);

        TrafficController tc = new TrafficController();
        tc.doGet(request, response);
        assertTrue(output.toString().indexOf("Healthy") >= 0);
*/
}
