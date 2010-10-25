package ign.middleman.helpers;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.regex.Pattern;

/**
 * Created by IntelliJ IDEA.
 * User: kgupta
 * Date: Oct 7, 2010
 */
public class Rule implements Externalizable {
    private static final long serialVersionUID = 1L;

    String rule;
    long timestamp;
    volatile transient Pattern pattern;

    public Rule() {
    }

    public Rule(String rule) {
        this.timestamp = System.currentTimeMillis();
        this.rule = rule;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void writeExternal(ObjectOutput out) throws IOException {
        out.writeLong(timestamp);
        out.writeUTF(rule);
    }

    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        this.timestamp = in.readLong();
        this.rule = in.readUTF();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if(!(o instanceof Rule)) {
            return false;
        }

        Rule that = (Rule) o;
        return rule != null ? rule.equals(that.rule) : that.rule == null ;

    }

    @Override
    public int hashCode() {
        return rule != null ? rule.hashCode() : 0;
    }

    public boolean matches(String url, long timestamp) {
        //if the url is cached after the regex is added then it we should
        //not call the origin server
        if(timestamp > this.timestamp) {
            return false;
        }
        if(pattern == null) {
            pattern = Pattern.compile(rule);
        }
        return pattern.matcher(url).matches();
    }
}
