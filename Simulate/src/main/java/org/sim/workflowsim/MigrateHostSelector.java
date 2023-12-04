package org.sim.workflowsim;

import org.sim.service.Constants;
import org.sim.cloudbus.cloudsim.Host;
import org.sim.cloudbus.cloudsim.ResCloudlet;


import java.util.List;

public class MigrateHostSelector {
    public Host findHostForJob(List<Integer> excludedHosts) {
        Host ret = null;
        for(Host host: Constants.hosts) {
            if(excludedHosts.contains(host.getId())) {
                continue;
            }
            if(ret == null || (ret.getUtilizationOfCpu() + ret.getUtilizationOfCpu() / 2) >
                    (host.getUtilizationOfCpu() + host.getUtilizationOfCpu() / 2)) {
                ret = host;
            }
        }
        return ret;
    }

    public ResCloudlet choseContainerToMigrate(Host host) {
        return host.choseCloudletToMigrate();
    }

}
