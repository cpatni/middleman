package ign.middleman.helpers;

import org.junit.Test;

import java.util.Date;

import static junit.framework.Assert.assertTrue;
import static org.junit.Assert.assertEquals;

/**
 * User: cpatni
 * Date: Oct 23, 2010
 * Time: 5:36:55 PM
 */
public class SimpleWebClientListenerTest {
    @Test
    public void sanityTest() throws InterruptedException {
        SimpleWebClientListener wcl = new SimpleWebClientListener();
        wcl.before(null);
        Thread.sleep(3);
        wcl.end(null, 200, "OK");
        assertEquals(new Date(wcl.start), wcl.getStartTime());
        assertEquals(new Date(wcl.end), wcl.getEndTime());
        assertTrue(wcl.getTime() >= 3);

    }


}
