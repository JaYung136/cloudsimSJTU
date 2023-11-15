package com.wfc.cloudsim.cloudsim.container.resourceAllocators;

import com.wfc.cloudsim.cloudsim.Log;
import com.wfc.cloudsim.cloudsim.container.core.Container;
import com.wfc.cloudsim.cloudsim.container.core.ContainerPod;
import com.wfc.cloudsim.cloudsim.core.CloudSim;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by sareh on 16/07/15.
 */
public abstract class PowerContainerAllocationPolicy extends ContainerAllocationPolicy{

        /** The container table. */
        private final Map<String, ContainerPod> containerTable = new HashMap<>();

        /**
         * Instantiates a new power vm allocation policy abstract.
         *
         */
        public PowerContainerAllocationPolicy() {
            super();
        }

        /*
         * (non-Javadoc)
         * @see com.wfc.cloudsim.cloudsim.PodAllocationPolicy#allocateHostForVm(com.wfc.cloudsim.cloudsim.Pod)
         */
        @Override
        public boolean allocateVmForContainer(Container container, List<ContainerPod> containerPodList) {
            setContainerVmList(containerPodList);
            return allocateVmForContainer(container, findVmForContainer(container));
        }

        /*
         * (non-Javadoc)
         * @see com.wfc.cloudsim.cloudsim.PodAllocationPolicy#allocateHostForVm(com.wfc.cloudsim.cloudsim.Pod,
         * com.wfc.cloudsim.cloudsim.Host)
         */
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

        /**
         * Find host for vm.
         *
         * @param container the vm
         * @return the power host
         */
        public ContainerPod findVmForContainer(Container container) {
            for (ContainerPod containerPod : getContainerVmList()) {
//                Log.printConcatLine("Trying vm #",containerPod.getId(),"For container #", container.getId());
                if (containerPod.isSuitableForContainer(container)) {
                    return containerPod;
                }
            }
            return null;
        }

        /*
         * (non-Javadoc)
         * @see com.wfc.cloudsim.cloudsim.PodAllocationPolicy#deallocateHostForVm(com.wfc.cloudsim.cloudsim.Pod)
         */
        @Override
        public void deallocateVmForContainer(Container container) {
            ContainerPod containerPod = getContainerTable().remove(container.getUid());
            if (containerPod != null) {
                containerPod.containerDestroy(container);
            }
        }

        /*
         * (non-Javadoc)
         * @see com.wfc.cloudsim.cloudsim.PodAllocationPolicy#getHost(com.wfc.cloudsim.cloudsim.Pod)
         */
        @Override
        public ContainerPod getContainerVm(Container container) {
            return getContainerTable().get(container.getUid());
        }

        /*
         * (non-Javadoc)
         * @see com.wfc.cloudsim.cloudsim.PodAllocationPolicy#getHost(int, int)
         */
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



