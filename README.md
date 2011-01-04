Middleman
-----------
Middleman is a reverse proxy which supports regex refreshes and employes memcached as the backend. Middleman was written because varnish didn't work very well for our use case. 

 - Varnish employs local RAM hence cannot hold large working sets
 - More Varnish servers means more origin traffic due to cache misses. It adds up. 
 - Local cache also create cache coherency problems
 - Varnish monitoring is not at par with what we get from newrelic

Features
-------
 - Middleman employs distributed memory (memcached) 
 - Supports regex invalidation
 - Supports url refresh
 - Awesome performance
 - Quickly enable memcached cache on apps without coding

Design Principles
--------
 - As much as possible, do things in background 
 - Use memcached to exploit distributed memory 

User guide
------
To run middleman configure middleman webapp in Jetty, Tomcat, Resin, etc. Middleman is configured via a properties file. Specify origin server and memcached nodes in the configuration file. Origin servers is where Middleman will get the content from when it's not found it cache or it needs to refresh a url. Memcached nodes are used for caching. 

###Refresh a URL
Middleman allows a url to be refreshed on-demand 

   /_refresh?url=http://app.example.com/index.html

###Invalidate via regex
Middleman allows urls to be invalidated by specifying a regex. Middleman maintains a list of regex rules for `refreshTimeout` period and if the url matches a regex, then it asynchronously refreshes the url when it was being requested.

   /_refresh?regex=.*/posts/tags/code.*


Configuration
------
Middleman is configured via `ign/middleman/config/middleman-${environment}.properties` under `WEB-INF/classes` where `environment` can be specified via System Property `environment` (Example: `/ign/middleman/config/middleman-development.properties`). The default value of `environment` is `development`.

###middleman-development.properties 

    #memcached hosts comma separated list of host:port
    memecahed=localhost:11211
    #memcached cache timeout
    #memcachedTimeout=200
    memcachedTimeout=604800
    #After refresh timeout has expired, the middleman proxy will return the 
    # stale data from memcached and schedule a background update
    refreshTimeout=120
    #refreshTimeout=7200
    origin=127.0.0.1:80
    #http connect and read timeouts in ms
    connectTimeout=5000
    readTimeout=30000
    #cached rules time out in s
    cachedRulesTimeout=5
