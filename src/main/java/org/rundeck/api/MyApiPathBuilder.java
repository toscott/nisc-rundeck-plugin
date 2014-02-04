package org.rundeck.api;

import java.util.Properties;

/**
 * Special api path builder because the existing one seems busted.
 */
public class MyApiPathBuilder extends ApiPathBuilder {

    StringBuilder sb = new StringBuilder();
    public MyApiPathBuilder(String jobId, String apptag) {
        super();
        sb.append("/job/").append(jobId).append("/run?argString=-apptag%20").append(apptag);
    }

    @Override
    public String toString() {
        return sb.toString();
    }
}
