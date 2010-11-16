package ign.middleman.controllers;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import ign.middleman.helpers.ApplicationHelper;
import ign.middleman.helpers.Rule;
import ign.middleman.helpers.SimpleWebClientListener;
import ign.middleman.helpers.Trace;
import ign.middleman.helpers.WebClient;
import ign.middleman.helpers.WebResponse;
import net.spy.memcached.CASMutation;
import net.spy.memcached.CASMutator;
import net.spy.memcached.MemcachedClient;

import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.logging.Level;
import java.util.logging.Logger;

import static ign.middleman.helpers.ApplicationHelper.convertRequestHeaders;
import static ign.middleman.helpers.ApplicationHelper.hex;
import static ign.middleman.helpers.ApplicationHelper.reorderQueryString;


/**
 * User: cpatni
 * Date: Sep 22, 2010
 * Time: 1:49:04 PM
 */
@Singleton
public class Middleman extends HttpServlet {
    Logger logger = Logger.getLogger("Middleman");

    MemcachedClient memcachedClient;
    Provider<CASMutator> casmp;
    String cacheKeyPrefix = "";


    String originHost;
    int originPort;
    int memcachedTimeout;
    long refreshTimeout;
    volatile long cachedRulesTimeout;
    private ExecutorService executor;
    private Provider<WebClient> wcp;
    private String cachedRulesKey = "mm_refresh_rules";

    @Inject
    public void setOrigin(@Named("origin") String origin) {
        String[] parts = origin.split(":");
        if (parts.length == 2) {
            this.originHost = parts[0];
            this.originPort = Integer.parseInt(parts[1]);
        } else {
            this.originHost = origin;
            this.originPort = 80;
        }
    }

    @Inject
    public void setCachedTimeout(@Named("memcachedTimeout") int memcachedTimeoutSeconds, @Named("refreshTimeout") int refreshTimeoutSeconds, @Named("cachedRulesTimeout") int cachedRulesTimeout) {
        this.memcachedTimeout = memcachedTimeoutSeconds;
        this.refreshTimeout = refreshTimeoutSeconds * 1000L;
        this.cachedRulesTimeout = cachedRulesTimeout* 1000L;
    }


    @Inject
    public void setCacheKeyPrefix(@Named("cacheKeyPrefix") String cacheKeyPrefix) {
        this.cacheKeyPrefix = cacheKeyPrefix;
    }

    @Inject
    public void setMemcachedClient(MemcachedClient memcachedClient) {
        this.memcachedClient = memcachedClient;
    }

    @Inject
    public void setCASMutatorProvider(Provider<CASMutator> casmp) {
        this.casmp = casmp;
    }


    @Inject
    public void setExecutorService(ExecutorService executorService) {
        this.executor = executorService;
    }

    @Inject
    public void setWebClientProvider(Provider<WebClient> wcp) {
        this.wcp = wcp;
    }


    public String getCachedRulesKey() {
        return cacheKeyPrefix + cachedRulesKey;
    }

    public void setCachedRulesKey(String cachedRulesKey) {
        this.cachedRulesKey = cachedRulesKey;
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        WebResponse r = getWebResponse(request, url(request));
        if (r != null) {
            r.writeTo(response);
        }
    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        doPost(req, resp);
    }

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        doPost(req, resp);
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        final String method = request.getMethod();
        final String url = url(request);

        //note that head and get requests call doGet
        final String cacheKey = makeKey(request.getMethod(), url);
        Object o = getCached(cacheKey);
        if (o != null) {

            if (logger.isLoggable(Level.FINE)) {
                logger.fine("cache hit: " + url);
            }

            try {
                ((WebResponse) o).writeTo(response);
            } finally {
                final Map<String, List<String>> headers = convertRequestHeaders(request);
                if (shouldRefresh((WebResponse) o)) {
                    executor.execute(new RefreshWorker(request.getMethod(), url, cacheKey, headers));
                } else {
                    executor.execute(new InvalidationWorker(request.getMethod(), url, cacheKey, headers, ((WebResponse) o).getTimestamp()));
                }
            }
        } else {
            if (logger.isLoggable(Level.FINE)) {
                logger.fine("cache miss: " + url);
            }
            final WebResponse r = getWebResponse(request, url);
            if (r != null) {
                try {
                    r.writeTo(response);
                } finally {
                    if (r.isOK()) {
                        final Map<String, List<String>> headers = convertRequestHeaders(request);
                        executor.execute(new Runnable() {
                            public void run() {
                                cacheAside(method, headers, url, cacheKey, r);
                            }
                        });
                    }
                }
            } else {
                if (logger.isLoggable(Level.INFO)) {
                    logger.info("no response for " + url);
                }
            }
        }

    }

    @Trace(metricName = "MemCache/read")
    private Object getCached(String cacheKey) {
        return memcachedClient.get(cacheKey);
    }

    void cacheAside(String method, Map<String, List<String>> headers, String url, String cacheKey, WebResponse r) {
        try {
            cacheWebResponse(cacheKey, r);
            //saveRequestToMongo(method, headers, url, cacheKey);
        } catch (Exception e) {
            ApplicationHelper.ignore(e);
        }
    }


    WebResponse getWebResponse(final HttpServletRequest request, String url) throws IOException {
        return wcp.get().raw(request.getMethod(), url, getProxy(url), new SimpleWebClientListener() {
            @Override
            public void before(HttpURLConnection hurl) {
                super.before(hurl);
                try {
                    Enumeration enumeration = request.getHeaderNames();
                    while (enumeration.hasMoreElements()) {
                        String name = (String) enumeration.nextElement();
//                        System.out.println(name + ": " + request.getHeader(name));
                        if (!"Host".equalsIgnoreCase(name)) {
                            hurl.addRequestProperty(name, request.getHeader(name));
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                if (!("GET".equalsIgnoreCase(request.getMethod()) || "HEAD".equalsIgnoreCase(request.getMethod()) || "DELETE".equalsIgnoreCase(request.getMethod()))) {
                    try {
                        ServletInputStream is = request.getInputStream();
                        if (is != null) {
                            hurl.setDoOutput(true);
                            OutputStream os = hurl.getOutputStream();
                            transfer(is, os);
                        }
                    } catch (IOException e) {
                        ApplicationHelper.ignore(e);
                    }
                }
            }
        });
    }


    public WebResponse getWebResponse(String url, final String method, final Map<String, List<String>> headers) throws IOException {
        Proxy via = getProxy(url);
        return wcp.get().raw(method, url, via, new SimpleWebClientListener() {
            @Override
            public void before(HttpURLConnection hurl) {
                super.before(hurl);
                if (headers != null) {
                    try {
                        for (Map.Entry<String, List<String>> header : headers.entrySet()) {
                            List<String> values = header.getValue();
                            for (String value : values) {
                                hurl.addRequestProperty(header.getKey(), value);
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        });
    }

    @Trace(metricName = "MemCache/write")
    protected void cacheWebResponse(String cacheKey, WebResponse r) {
        int timeout = memcachedTimeout;

        if (timeout > 0) {
            memcachedClient.set(cacheKey, timeout, r);
        }
    }


    public void fetchFromOriginAndRefreshCache(String url) {
        // Reorder query string
        int idx = url.indexOf("?");
        if (idx > 0 && idx < url.length() - 1) {
            StringBuilder sb = new StringBuilder(url.substring(0, idx));
            sb.append("?").append(reorderQueryString(url.substring(idx + 1)));
            url = sb.toString();
        }
        String getKey = makeKey("GET", url);
        new RefreshWorker("GET", url, getKey, null).run();
    }


    void transfer(InputStream in, OutputStream out) throws IOException {
        // Transfer bytes from in to out
        byte[] buf = new byte[4096];
        int len;
        while ((len = in.read(buf)) > 0) {
            out.write(buf, 0, len);
        }
    }

    String makeKey(String method, String url) {
        return sha1(cacheKeyPrefix + method + url);
    }


    String sha1(String url) {
        try {
            MessageDigest sha1 = MessageDigest.getInstance("sha1");
            byte[] bytes = sha1.digest(url.getBytes("utf-8"));
            return hex(bytes);
        } catch (Exception e) {
            return url;
        }
    }


    String url(HttpServletRequest request) {
        StringBuffer url = request.getRequestURL();
        if (request.getQueryString() != null) {
            url.append("?").append(reorderQueryString(request.getQueryString()));
        }
        return url.toString();
    }

    boolean shouldRefresh(WebResponse r) {
        return System.currentTimeMillis() - r.getTimestamp() >= refreshTimeout;
    }

    public Proxy getProxy(String url) {
        return new Proxy(Proxy.Type.HTTP, new InetSocketAddress(originHost, originPort));
    }

    class RefreshWorker implements Runnable {
        private String method;
        private String url;
        private String cacheKey;
        private Map<String, List<String>> headers;


        private RefreshWorker(String method, String url, String cacheKey, Map<String, List<String>> headers) {
            this.method = method;
            this.url = url;
            this.cacheKey = cacheKey;
            this.headers = headers;
        }

        @Trace(dispatcher = true)
        public void run() {
            try {

                if (logger.isLoggable(Level.FINE)) {
                    logger.fine("Refreshing " + url);
                }

                WebResponse r = getWebResponse(url, method, headers);
                if (r != null && r.isOK()) {
                    cacheWebResponse(cacheKey, r);
                } else if (r != null && r.is4XX()) {
                    flushWebResponse(cacheKey);
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    private void flushWebResponse(String cacheKey) {
        memcachedClient.delete(cacheKey);
    }


    private class InvalidationWorker implements Runnable {
        private String method;
        private String url;
        private String cacheKey;
        private Map<String, List<String>> headers;
        private long timestamp;

        private InvalidationWorker(String method, String url, String cacheKey, Map<String, List<String>> headers, long timestamp) {
            this.method = method;
            this.url = url;
            this.cacheKey = cacheKey;
            this.headers = headers;
            this.timestamp = timestamp;
        }


        @Trace(dispatcher = true)
        public void run() {
            Collection<Rule> rules = fetchRulesInLastTwoHours();
            if (rules != null) {
                for (Rule rule : rules) {
                    try {
                        if (rule.matches(url, timestamp)) {
                            WebResponse r = getWebResponse(url, method, headers);
                            if (r != null && r.isOK()) {
                                cacheWebResponse(cacheKey, r);
                                return;
                            }
                        }
                    } catch (IOException e) {
                        ApplicationHelper.ignore(e);
                    }

                }
            }

        }

    }


    //cache this for 10 seconds
    volatile long lastTime;
    volatile Collection<Rule> cachedRules;

    private Collection<Rule> fetchRulesInLastTwoHours() {
        if (lastTime < System.currentTimeMillis() - cachedRulesTimeout || cachedRules == null) {
            return filterAndSetRulesInLastTwoHoursFromCache();
        } else {
            return cachedRules;
        }

    }

    /**
     * Returns a set consisting of invalidation regular expressions in the last two hours and sets them in the cache
     */
    private synchronized Collection<Rule> filterAndSetRulesInLastTwoHoursFromCache() {
        if (lastTime < System.currentTimeMillis() - cachedRulesTimeout) {
            Collection<Rule> rules = (Collection<Rule>) getCached(getCachedRulesKey());
            boolean update = false;
            if (rules != null) {
                for (Rule rule : rules) {
                    if (rule.getTimestamp() < System.currentTimeMillis() - refreshTimeout) {
                        update = true;
                        break;
                    }
                }
            }

            if (update) {
                try {
                    CASMutation<Collection<Rule>> mutation = new CASMutation<Collection<Rule>>() {
                        public Collection<Rule> getNewValue(Collection<Rule> current) {

                            ArrayList<Rule> currentRules = new ArrayList<Rule>(current);
                            for (Iterator<Rule> iterator = currentRules.iterator(); iterator.hasNext();) {
                                Rule rule = iterator.next();
                                if (rule.getTimestamp() < System.currentTimeMillis() - refreshTimeout) {
                                    iterator.remove();
                                }
                            }
                            return currentRules;
                        }
                    };
                    casmp.get().cas(getCachedRulesKey(), null, 0, mutation);
                } catch (Exception e) {
                    ApplicationHelper.ignore(e);
                }
            }
            if (rules == null) {
                rules = Collections.emptyList();
            }
            cachedRules = rules;
            lastTime = System.currentTimeMillis();
        }
        return cachedRules;
    }


    public Collection<Rule> addRefreshRule(String regex) {
        final Rule rule = new Rule(regex);

        try {
// This is how we modify a list when we find one in the cache.
            CASMutation<Collection<Rule>> mutation = new CASMutation<Collection<Rule>>() {

                // This is only invoked when a value actually exists.
                public Collection<Rule> getNewValue(Collection<Rule> current) {

                    LinkedHashMap<Rule, Object> rules = new LinkedHashMap<Rule, Object>() {
                        @Override
                        protected boolean removeEldestEntry(Map.Entry<Rule, Object> eldest) {
                            return eldest.getKey().getTimestamp() < System.currentTimeMillis() - refreshTimeout;
                        }
                    };

                    rules.put(rule, rule);
                    for (Rule r : current) {
                        rules.put(r, r);
                    }
                    return new ArrayList<Rule>(rules.keySet());
                }

            };
            // The initial value -- only used when there's no list stored under
            // the key.
            Collection<Rule> initialValue = Collections.singletonList(rule);

            // This returns whatever value was successfully stored within the
            // cache -- either the initial list as above, or a mutated existing
            // one
            return (Collection<Rule>) casmp.get().cas(getCachedRulesKey(), initialValue, 0, mutation);
        } catch (Exception e) {
            ApplicationHelper.ignore(e);
        }
        return Collections.emptyList();
    }


}
