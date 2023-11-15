package com.wfc.cloudsim.cloudsim.container.containerPlacementPolicies;

import com.wfc.cloudsim.cloudsim.Log;
import com.wfc.cloudsim.cloudsim.container.core.ContainerPod;
import com.wfc.cloudsim.cloudsim.container.utils.RandomGen;

import java.util.List;
import java.util.Set;

/**
 * Created by sareh fotuhi Piraghaj on 16/12/15.
 * For container placement Random policy.
 */
public class ContainerPlacementPolicyRandomSelection extends ContainerPlacementPolicy {
    @Override
    public ContainerPod getContainerVm(List<ContainerPod> vmList, Object obj, Set<? extends ContainerPod> excludedVmList) {
        ContainerPod containerPod = null;
        while (true) {
            if (vmList.size() > 0) {
                int randomNum = new RandomGen().getNum(vmList.size());
                containerPod = vmList.get(randomNum);
                if (excludedVmList.contains(containerPod)) {
                    continue;
                }
            } else {

                Log.print(String.format("Error: The VM list Size is: %d", vmList.size()));
            }

            return containerPod;
        }
    }
}
