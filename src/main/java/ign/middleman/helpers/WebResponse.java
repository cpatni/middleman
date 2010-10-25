package ign.middleman.helpers;

import javax.servlet.http.HttpServletResponse;
import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.List;
import java.util.Map;

import static ign.middleman.helpers.ApplicationHelper.isNonTrivial;

/**
 * User: cpatni
 * Date: Sep 22, 2010
 * Time: 5:49:26 PM
 */
public class WebResponse implements Externalizable {
    private static final long serialVersionUID = 1L;

    String method;
    int status;
    long timestamp;
    String message;
    transient Map<String, List<String>> headers;
    byte[] body;
    String headersString;

    WebResponse(String method, int status, String message, Map<String, List<String>> headers, byte[] body) {
        this.method = method;
        this.status = status;
        this.message = message;
        this.headers = headers;
        this.body = body;
        this.timestamp = System.currentTimeMillis();
    }

    public WebResponse() {
    }

    @Override
    public String toString() {
        return status + " " +message + "\r\n" + new String(body);
    }

    String serializeHeadersToString() {
        StringBuilder sb = new StringBuilder(512);
        for (Map.Entry<String, List<String>> header : headers.entrySet()) {
            if(isNonTrivial(header.getKey())) {
                for (String val : header.getValue()) {
                    if(isNonTrivial(val)) {
                        sb.append(header.getKey()).append(": ").append(val).append("\r\n");
                    }
                }
            }
        }
        return sb.toString();
    }

/*
    Map<String, List<String>> deserializeHeadersFromString(String s) {
    }
*/

    public void writeExternal(ObjectOutput out) throws IOException {
        out.writeInt(1);
        out.writeInt(status);
        out.writeUTF(method);
        out.writeLong(timestamp);
        out.writeUTF(serializeHeadersToString());
        out.writeInt(body.length);
        if(body.length > 0) {
            out.write(body);
        }
    }

    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        in.readInt(); //ignore version for now
        this.status = in.readInt();
        this.method = in.readUTF();
        this.timestamp = in.readLong();
        this.headersString = in.readUTF();
        int bodyLength = in.readInt();
        this.body = new byte[bodyLength];
        in.readFully(body);
    }

    public void writeTo(HttpServletResponse response) throws IOException {
        response.setStatus(this.status);
        if(isNonTrivial(headersString)) {
            for (String hs : headersString.split("\r\n")) {
                String[] parts = hs.split(": ", 2);
                if(!"Date".equalsIgnoreCase(parts[0])) {
                    response.addHeader(parts[0], parts[1]);
                }
            }
        }
        else if(headers != null) {
            for (Map.Entry<String, List<String>> header : headers.entrySet()) {
                if(isNonTrivial(header.getKey())) {
                    for (String val : header.getValue()) {
                        if(isNonTrivial(val)) {
                            if(!"Date".equalsIgnoreCase(header.getKey())) {
                                response.addHeader(header.getKey(), val);
                            }
                        }
                    }
                }
            }
        }
        response.getOutputStream().write(this.body);

    }

    public String getMethod() {
        return method;
    }

    public int getStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }

    public byte[] getBody() {
        return body;
    }


    public boolean isOK() {
        return status < 300 || status == 301;
    }

    public boolean is4XX(){
        return status >= 400 && status < 500;
    }

    public Map<String, List<String>> getHeaders() {
        return headers;
    }

    public long getTimestamp() {
        return timestamp;
    }
}
