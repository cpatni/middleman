package ign.middleman.helpers;

import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;
import static org.junit.Assert.assertEquals;

/**
 * User: cpatni
 * Date: Oct 23, 2010
 * Time: 2:42:22 PM
 */
public class RuleTest {
    private Rule newRule() {
        return new Rule(".*tags=.*foo.*");
    }



    @Test
    public void testSerialization() throws IOException, ClassNotFoundException {
        Rule rule = newRule();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        rule.writeExternal(oos);
        oos.close();


        Rule rt = new Rule();
        rt.readExternal(new ObjectInputStream(new ByteArrayInputStream(baos.toByteArray())));
        assertEquals(rule, rt);


    }

    @Test
    public void testEquals() throws IOException, ClassNotFoundException {
        Rule rule = newRule();
        assertTrue("true for empty rules", new Rule().equals(new Rule()));

        assertTrue("Equal to same rule", rule.equals(rule));
        assertFalse("Not equal to null rule", rule.equals(null));
        assertTrue("Same rule if pattern is same", rule.equals(new Rule(rule.rule)));

    }

    @Test
    public void testHashCode() throws IOException, ClassNotFoundException {
        Rule rule = newRule();
        assertEquals("non trivial rule",rule.rule.hashCode(), rule.hashCode());
        assertEquals("trivial rule", 0, new Rule().hashCode());
    }

    @Test
    public void testMatches() throws IOException, ClassNotFoundException {
        Rule rule = newRule();
        assertTrue("Matches URL in past", rule.matches("http://localhost/tags=foo", rule.getTimestamp()-1));
        assertTrue("Matches URL at present", rule.matches("http://localhost/tags=foo", rule.getTimestamp()));
        assertFalse("Does not match in future", rule.matches("http://localhost/tags=foo", rule.getTimestamp() +11));

        assertFalse("Does not matches URL in past", rule.matches("http://localhost/tags=bar", rule.getTimestamp()-1));
        assertFalse("Matches URL at present", rule.matches("http://localhost/tags=bar", rule.getTimestamp()));
        assertFalse("Does not match in future", rule.matches("http://localhost/tags=bar", rule.getTimestamp() +11));
    }

}
