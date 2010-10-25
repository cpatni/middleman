package ign.middleman.boot;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Provider;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.Stage;
import com.google.inject.name.Names;
import com.google.inject.servlet.GuiceServletContextListener;
import com.sun.jersey.guice.JerseyServletModule;
import ign.middleman.controllers.Middleman;
import ign.middleman.controllers.Refresher;
import ign.middleman.controllers.SendServerHeader;
import net.spy.memcached.CASMutator;
import net.spy.memcached.MemcachedClient;
import net.spy.memcached.transcoders.SerializingTranscoder;

import javax.servlet.ServletContextEvent;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * User: cpatni
 * Date: Aug 7, 2010
 * Time: 9:51:21 PM
 */
public class Bootstrap extends GuiceServletContextListener {

    private ExecutorService executorService;
    private ExecutorService refreshWorkerService;
    private Properties config = new Properties();
    Logger logger = Logger.getLogger("Middleman");


    @Override
    protected Injector getInjector() {

        return Guice.createInjector(getStage(), new JerseyServletModule() {

            @Override
            protected void configureServlets() {
                filter("/*").through(SendServerHeader.class);

                Map<String, String> params = new HashMap<String, String>();
                params.put("javax.ws.rs.Application", MiddlemanApplication.class.getName());
                //serve("/*").with(GuiceContainer.class, params);
                serve("/_refresh").with(Refresher.class);
                serve("/*").with(Middleman.class);
                bind(ExecutorService.class).annotatedWith(Names.named("refreshWorkers"))
                        .toProvider(new Provider<ExecutorService>() {
                            public ExecutorService get() {
                                return createRefreshWorkers();
                            }
                        });

                bindConstant().annotatedWith(Names.named("origin")).to(getOrigin());
                bindConstant().annotatedWith(Names.named("memcachedTimeout")).to(getDefaultMemcachedTimeout());
                bindConstant().annotatedWith(Names.named("refreshTimeout")).to(getDefaultRefreshTimeout());
                bindConstant().annotatedWith(Names.named("connectTimeout")).to(getDefaultConnectTimeout());
                bindConstant().annotatedWith(Names.named("readTimeout")).to(getDefaultReadTimeout());
                bindConstant().annotatedWith(Names.named("cachedRulesTimeout")).to(getDefaultCacheRulesTimeout());
                bindConstant().annotatedWith(Names.named("cacheKeyPrefix")).to(getCacheKeyPrefix());

            }


            @Provides
            @Singleton
            public ExecutorService createScheduledExecutorService() {
                executorService = Executors.newCachedThreadPool();
                return executorService;
            }

            public ExecutorService createRefreshWorkers() {
                refreshWorkerService = Executors.newFixedThreadPool(3);
                return refreshWorkerService;
            }

            @Provides
            @Singleton
            public MemcachedClient createMemcachedClient() throws IOException {
                return new MemcachedClient(getMemcachedNodes());
            }

            @Provides
            public CASMutator createCASMutator(MemcachedClient memcachedClient) {
                return new CASMutator(memcachedClient, new SerializingTranscoder());
            }


        });
    }

    private String getCacheKeyPrefix() {
        return config.getProperty("cacheKeyPrefix", System.getProperty("cacheKeyPrefix", ""));
    }


    public List<InetSocketAddress> getMemcachedNodes() {
        List<InetSocketAddress> nodes = new LinkedList<InetSocketAddress>();

        String csv = config.getProperty("memecahed", System.getProperty("memecahed", "localhost"));
        String[] all = csv.split(",");
        for (String s : all) {
            String[] tuple = s.trim().split(":");
            if (tuple.length == 2) {
                nodes.add(new InetSocketAddress(tuple[0].trim(), Integer.parseInt(tuple[1].trim())));
            } else if (tuple.length == 1) {
                nodes.add(new InetSocketAddress(tuple[0].trim(), 11211));
            } else {
                System.err.println("Could not add memcacehd node " + s);
            }

        }
        return nodes;
    }


    public String getOrigin() {
        return config.getProperty("origin", System.getProperty("origin", "localhost"));
    }

    //in seconds

    private int getDefaultRefreshTimeout() {
        return Integer.parseInt(config.getProperty("refreshTimeout", "10"));
    }

    //in seconds

    public int getDefaultMemcachedTimeout() {
        return Integer.parseInt(config.getProperty("memcachedTimeout", "20"));
    }

    public Stage getStage() {
        try {
            return Stage.valueOf(getEnvironment().toUpperCase());
        } catch (Exception e) {
            return Stage.DEVELOPMENT;
        }
    }

    public String getEnvironment() {
        return System.getProperty("environment", "development");
    }


    @Override
    public void contextInitialized(ServletContextEvent servletContextEvent) {
        try {
            String configFile = configFile();
            if (logger.isLoggable(Level.CONFIG)) {
                logger.config("Config file " + configFile);
            }
            try {
                InputStream in = getClass().getClassLoader().getResourceAsStream(configFile);
                config.load(in);
            } catch (IOException e) {
                e.printStackTrace();
            }

        } finally {
            super.contextInitialized(servletContextEvent);
        }

    }

    private String configFile() {
        return "ign/middleman/config/middleman-" + getEnvironment() + ".properties";
    }

    public void contextDestroyed(ServletContextEvent servletContextEvent) {
        super.contextDestroyed(servletContextEvent);
        if (executorService != null) {
            executorService.shutdownNow();
        }
        if (refreshWorkerService != null) {
            refreshWorkerService.shutdownNow();
        }
    }

    private int getDefaultReadTimeout() {
        return Integer.parseInt(config.getProperty("readTimeout", "30000"));
    }

    public int getDefaultConnectTimeout() {
        return Integer.parseInt(config.getProperty("connectTimeout", "5000"));
    }

    public int getDefaultCacheRulesTimeout() {
        return Integer.parseInt(config.getProperty("cachedRulesTimeout", "5"));
    }
}
