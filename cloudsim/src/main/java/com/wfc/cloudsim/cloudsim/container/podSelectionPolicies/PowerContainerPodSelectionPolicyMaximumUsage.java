package com.wfc.cloudsim.cloudsim.container.podSelectionPolicies;

import com.wfc.cloudsim.cloudsim.Log;
import com.wfc.cloudsim.cloudsim.container.core.ContainerPod;
import com.wfc.cloudsim.cloudsim.container.core.PowerContainerHost;
import com.wfc.cloudsim.cloudsim.container.core.PowerContainerPod;

import java.util.List;

/**
 * Created by sareh on 16/11/15.
 */
public class PowerContainerPodSelectionPolicyMaximumUsage extends PowerContainerPodSelectionPolicy {
    /*
     * (non-Javadoc)
     * @see
     * com.wfc.cloudsim.cloudsim.experiments.power.PowerPodSelectionPolicy#getVmsToMigrate(com.wfc.cloudsim
     * .cloudsim.power.PowerHost)
     */
    @Override
    public ContainerPod getVmToMigrate(PowerContainerHost host) {
        Log.printLine("Get Pod to Migrate");
        List<PowerContainerPod> migratableContainers = getMigratableVms(host);
        if (migratableContainers.isEmpty()) {
            return null;
        }
        ContainerPod VmsToMigrate = null;
        double maxMetric = Double.MIN_VALUE;
        for (ContainerPod vm : migratableContainers) {
            if (vm.isInMigration()) {
                continue;
            }
            double metric = vm.getCurrentRequestedTotalMips();
            if (maxMetric < metric) {
                maxMetric = metric;
                VmsToMigrate = vm;
            }
        }
//        Log.formatLine("The Container To migrate is #%d from VmID %d from host %d", containerToMigrate.getId(),containerToMigrate.getVm().getId(), host.getId());
        return VmsToMigrate;
    }


}
