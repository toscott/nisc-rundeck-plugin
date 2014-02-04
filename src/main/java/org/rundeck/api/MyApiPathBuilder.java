package org.rundeck.api;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

/**
 * Special api path builder because the existing one seems busted.
 */
public class MyApiPathBuilder extends ApiPathBuilder {

    StringBuilder sb = new StringBuilder();
    public MyApiPathBuilder(String jobId, String apptag) {
        super();
        sb.append("/job/").append(jobId).append("/run?argString=-apptag%20");
        try {
            sb.append(URLEncoder.encode(apptag.trim(), "UTF-8"));
        } catch (UnsupportedEncodingException e) {
            //Oh well..
        }
    }

    @Override
    public String toString() {
        return sb.toString();
    }
}
