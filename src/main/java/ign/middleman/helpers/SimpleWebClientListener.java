package ign.middleman.helpers;

import java.net.HttpURLConnection;
import java.io.IOException;
import java.util.Date;

/**
 * User: cpatni
 * Date: May 4, 2009
 * Time: 8:57:13 PM
 */
public class SimpleWebClientListener implements WebClientListener {
    long start;
    long end;

    public void before(HttpURLConnection hurl) {
        start = System.currentTimeMillis();
    }

    public void after(HttpURLConnection hurl, int status, String statusText) {
    }

    public void onSuccess(HttpURLConnection hurl, String response) {
    }

    public void onException(Exception e) {
    }

    public void onException(Exception e, String error) {
    }

    public void end(HttpURLConnection hurl, int status, String statusText) {
        end = System.currentTimeMillis();
    }

    public Date getStartTime() {
        return new Date(start);
    }

    public Date getEndTime() {
        return new Date(end);
    }

    public int getTime() {
        return (int) (end - start);
    }

}
