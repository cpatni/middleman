package ign.middleman.helpers;

import org.junit.Assert;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Arrays;

import static org.junit.Assert.assertEquals;

/**
 * User: cpatni
 * Date: Oct 23, 2010
 * Time: 5:27:51 PM
 */
public class WebClientTest {
    @Test
    public void timeoutValuesTest() {
        WebClient wc = new WebClient(500, 4000);
        assertEquals(500, wc.connectTimeout);
        assertEquals(4000, wc.readTimeout);
    }

    @Test
    public void readBytesFullyAndCloseTestWithContentLength() throws IOException {
        WebClient wc = new WebClient();

        byte[] data = new byte[1024];
        Arrays.fill(data, (byte) 3);
        ByteArrayInputStream bais = new ByteArrayInputStream(data);

        byte[] copy = wc.readBytesFullyAndClose("GET", "http://example.com/", bais, data.length);
        Assert.assertArrayEquals(data, copy);

    }

    @Test
    public void readBytesFullyAndCloseTestWithoutContentLength() throws IOException {
        WebClient wc = new WebClient();

        byte[] data = new byte[1024];
        Arrays.fill(data, (byte) 3);
        ByteArrayInputStream bais = new ByteArrayInputStream(data);

        byte[] copy = wc.readBytesFullyAndClose("GET", "http://example.com/", bais, 0);
        Assert.assertArrayEquals(data, copy);
    }

    @Test
    public void readBytesFullyANullInputStreamTest() throws IOException {
        WebClient wc = new WebClient();
        byte[] copy = wc.readBytesFullyAndClose("GET", "http://example.com/", null, 0);
        Assert.assertArrayEquals(new byte[0], copy);

    }
    
}
