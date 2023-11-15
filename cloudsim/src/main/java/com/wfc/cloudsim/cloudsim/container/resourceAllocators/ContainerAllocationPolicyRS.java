package com.wfc.cloudsim.cloudsim.container.resourceAllocators;

import com.wfc.cloudsim.cloudsim.container.containerPlacementPolicies.ContainerPlacementPolicy;
import com.wfc.cloudsim.cloudsim.container.core.Container;
import com.wfc.cloudsim.cloudsim.container.core.ContainerPod;

import java.util.HashSet;
import java.util.Set;

/**
 * Created by sareh on 16/12/15.
 */
public class ContainerAllocationPolicyRS extends  PowerContainerAllocationPolicySimple{
    /** The vm table. */


    private ContainerPlacementPolicy containerPlacementPolicy;


    public ContainerAllocationPolicyRS(ContainerPlacementPolicy containerPlacementPolicy1) {
        super();
        setContainerPlacementPolicy(containerPlacementPolicy1);
    }


    @Override
    public ContainerPod findVmForContainer(Container container) {

        Set<ContainerPod> excludedVmList = new HashSet<>();
        int tries = 0;
        boolean found = false;
        do{

            ContainerPod containerPod = getContainerPlacementPolicy().getContainerVm(getContainerVmList(), container,excludedVmList);
            if(containerPod == null){

                return null;
            }
            if (containerPod.isSuitableForContainer(container)) {
                found = true;
                return containerPod;
            }
            else {
                    excludedVmList.add(containerPod);
                    tries ++;
                }

            } while (!found & tries < getContainerVmList().size());

        return null;
    }



    public ContainerPlacementPolicy getContainerPlacementPolicy() {
        return this.containerPlacementPolicy;
    }

    public void setContainerPlacementPolicy(ContainerPlacementPolicy containerPlacementPolicy) {
        this.containerPlacementPolicy = containerPlacementPolicy;
    }


}
