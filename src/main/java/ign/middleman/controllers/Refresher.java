package ign.middleman.controllers;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

import static ign.middleman.helpers.ApplicationHelper.isNonTrivial;

/**
 * User: thomas
 * Date: Sep 22, 2010
 * Time: 5:33:16 PM
 */
@Singleton
public class Refresher extends HttpServlet {

    private Middleman middleman;

    @Inject
    public void setMiddleman(Middleman middleman) {
        this.middleman = middleman;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String url = req.getParameter("url");
        if (isNonTrivial(url)) {
            middleman.fetchFromOriginAndRefreshCache(url);

        } else {
            String regex = req.getParameter("regex");
            if (isNonTrivial(regex)) {
                //store the set of invalidations in memcached
                middleman.addRefreshRule(regex);
            }
        }


    }


}