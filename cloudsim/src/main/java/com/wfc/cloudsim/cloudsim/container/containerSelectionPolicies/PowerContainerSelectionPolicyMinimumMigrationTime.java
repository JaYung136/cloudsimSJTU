package com.wfc.cloudsim.cloudsim.container.containerSelectionPolicies;

import com.wfc.cloudsim.cloudsim.container.core.Container;
import com.wfc.cloudsim.cloudsim.container.core.PowerContainer;
import com.wfc.cloudsim.cloudsim.container.core.PowerContainerHost;

import java.util.List;

/**
 * Created by sareh on 3/08/15.
 */
public class PowerContainerSelectionPolicyMinimumMigrationTime extends PowerContainerSelectionPolicy {


    /*
     * (non-Javadoc)
     * @see
     * PowerContainerSelectionPolicy#getContainerToMigrate
     */
    @Override
    public Container getContainerToMigrate(PowerContainerHost host) {
        List<PowerContainer> migratableContainers = getMigratableContainers(host);
        if (migratableContainers.isEmpty()) {
            return null;
        }
        Container containerToMigrate = null;
        double minMetric = Double.MAX_VALUE;
        for (Container container : migratableContainers) {
            if (container.isInMigration()) {
                continue;
            }
            double metric = container.getRam();
            if (metric < minMetric) {
                minMetric = metric;
                containerToMigrate = container;
            }
        }
        return containerToMigrate;
    }

}
