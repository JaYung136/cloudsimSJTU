package com.wfc.cloudsim.cloudsim.container.containerSelectionPolicies;


import com.wfc.cloudsim.cloudsim.container.core.Container;
import com.wfc.cloudsim.cloudsim.container.core.PowerContainer;
import com.wfc.cloudsim.cloudsim.container.core.PowerContainerHost;
import com.wfc.cloudsim.cloudsim.container.core.PowerContainerPod;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by sareh on 31/07/15.
 */
public abstract class PowerContainerSelectionPolicy {

    /**
     * Gets the containers to migrate.
     *
     * @param host the host
     * @return the container to migrate
     */
    public abstract Container getContainerToMigrate(PowerContainerHost host);

    /**
     * Gets the migratable containers.
     *
     * @param host the host
     * @return the migratable containers
     */
    protected List<PowerContainer> getMigratableContainers(PowerContainerHost host) {
        List<PowerContainer> migratableContainers= new ArrayList<>();
        for (PowerContainerPod vm : host.<PowerContainerPod> getVmList()) {
            if (!vm.isInMigration()) {
                for (Container container: vm.getContainerList()){

                    if(!container.isInMigration() && !vm.getContainersMigratingIn().contains(container)){
                        migratableContainers.add((PowerContainer) container);}

                }



            }
        }
        return migratableContainers;
    }

}
