package org.sim.service.result;

public class LogEntity {
    public String time;
    public String cpuUtilization;
    public String ramUtilization;

    public LogEntity(String t, String c, String r) {
        this.time = t;
        this.cpuUtilization = c;
        this.ramUtilization = r;
    }
}
