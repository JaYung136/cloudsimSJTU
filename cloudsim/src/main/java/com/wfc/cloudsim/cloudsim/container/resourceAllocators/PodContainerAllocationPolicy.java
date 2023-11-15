package com.wfc.cloudsim.cloudsim.container.resourceAllocators;

import com.wfc.cloudsim.cloudsim.Log;
import com.wfc.cloudsim.cloudsim.container.core.Container;
import com.wfc.cloudsim.cloudsim.container.core.ContainerPod;
import com.wfc.cloudsim.cloudsim.core.CloudSim;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PodContainerAllocationPolicy extends PowerContainerAllocationPolicy{

    private final Map<String, ContainerPod> containerTable = new HashMap<>();

    public PodContainerAllocationPolicy() {
        super();
    }

    @Override
    public boolean allocateVmForContainer(Container container, List<ContainerPod> containerPodList) {
        setContainerVmList(containerPodList);
        Log.printLine("Allocation: pods size: " + containerPodList.size());
        return allocateVmForContainer(container, findPodForContainer(container));
    }


    public ContainerPod findPodForContainer(Container container) {
        Log.printLine("Allocation: container " + container.getId() + " should be allocated in Pod " + container.getUid());
        for (ContainerPod containerPod : getContainerVmList()) {
            if (containerPod.getUid().equals(container.getPodId())) {
                Log.printLine("Allocation: find pod");
                return containerPod;
            }
        }
        Log.printLine("Allocation: failed to find pod");
        return null;
    }

    @Override
    public boolean allocateVmForContainer(Container container, ContainerPod containerPod) {
        if (containerPod == null) {
            Log.formatLine("%.2f: No suitable VM found for Container#" + container.getId() + "\n", CloudSim.clock());
            return false;
        }
        if (containerPod.containerCreate(container)) { // if vm has been succesfully created in the host
            getContainerTable().put(container.getUid(), containerPod);
//                container.setVm(containerPod);
            Log.formatLine(
                    "%.2f: Container #" + container.getId() + " has been allocated to the VM #" + containerPod.getId(),
                    CloudSim.clock());
            return true;
        }
        Log.formatLine(
                "%.2f: Creation of Container #" + container.getId() + " on the Pod #" + containerPod.getId() + " failed\n",
                CloudSim.clock());
        return false;
    }

    @Override
    public List<Map<String, Object>> optimizeAllocation(List<? extends Container> containerList) {
        return null;
    }

    @Override
    public void deallocateVmForContainer(Container container) {
        ContainerPod containerPod = getContainerTable().remove(container.getUid());
        if (containerPod != null) {
            containerPod.containerDestroy(container);
        }
    }

    @Override
    public ContainerPod getContainerVm(Container container) {
        return getContainerTable().get(container.getUid());
    }

    @Override
    public ContainerPod getContainerVm(int containerId, int userId) {
        return getContainerTable().get(Container.getUid(userId, containerId));
    }

    /**
     * Gets the vm table.
     *
     * @return the vm table
     */
    public Map<String, ContainerPod> getContainerTable() {
        return containerTable;
    }

}
