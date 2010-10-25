package ign.middleman.helpers;

import java.net.HttpURLConnection;
import java.io.IOException;

/**
 * User: cpatni
 * Date: May 4, 2009
 * Time: 8:04:27 PM
 */
public interface WebClientListener {

    /**
     * Callback before the http request is sent. Hook to add request headers
     * @param hurl
     */
    public void before(HttpURLConnection hurl);

    /**
     * Callback after the response is received. Hook to read response headers
     * @param hurl
     * @param status
     * @param statusText
     */
    public void after(HttpURLConnection hurl, int status, String statusText);

    /**
     * Callback if the http request is successful and response is returned.
     *  i.e. the status code is < 400
     * @param hurl
     * @param response
     */
    public void onSuccess(HttpURLConnection hurl, String response);

    /**
     * Callback if the http response is returned but the request didn't succeed
     *  i.e. the status code is >= 400
     * @param e
     */
    public void onException(Exception e);

    public void onException(Exception e, String error);

    public int getTime();

    /**
     * A finally callback to indicate the end of http call
     * @param hurl
     * @param status
     * @param statusText
     */
    public void end(HttpURLConnection hurl, int status, String statusText);
}
