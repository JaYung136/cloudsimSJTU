package com.wfc.cloudsim.cloudsim.container.resourceAllocators;


import com.wfc.cloudsim.cloudsim.Log;
import com.wfc.cloudsim.cloudsim.container.core.ContainerHost;
import com.wfc.cloudsim.cloudsim.container.core.ContainerPod;
import com.wfc.cloudsim.cloudsim.container.core.PowerContainerHost;
import com.wfc.cloudsim.cloudsim.core.CloudSim;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
/**
 * Created by sareh on 14/07/15.
 */
public abstract  class PowerContainerPodAllocationAbstract extends ContainerPodAllocationPolicy {

        /** The vm table. */
        private final Map<Integer, ContainerHost> vmTable = new HashMap<Integer, ContainerHost>();

        /**
         * Instantiates a new power vm allocation policy abstract.
         *
         * @param list the list
         */
        public PowerContainerPodAllocationAbstract(List<? extends ContainerHost> list) {
            super(list);
        }

        /*
         * (non-Javadoc)
         * @see com.wfc.cloudsim.cloudsim.PodAllocationPolicy#allocateHostForVm(com.wfc.cloudsim.cloudsim.Pod)
         */
        @Override
        public boolean allocateHostForVm(ContainerPod containerPod) {
            //Log.formatLine("||||||||||||||||asasasa");
            return allocateHostForVm(containerPod, findHostForVm(containerPod));
        }

        @Override
        public boolean allocateHostForPods(List<ContainerPod> containerPods) {
            //Log.formatLine("||||||||||||||||asasasa");
            List<PowerContainerHost> ret = findHostForPods(containerPods);
            for(int i = 0; i < ret.size(); i++) {
                allocateHostForVm(containerPods.get(i), ret.get(i));
            }
            return true;
        }

        /*
         * (non-Javadoc)
         * @see com.wfc.cloudsim.cloudsim.PodAllocationPolicy#allocateHostForVm(com.wfc.cloudsim.cloudsim.Pod,
         * com.wfc.cloudsim.cloudsim.Host)
         */
        @Override
        public boolean allocateHostForVm(ContainerPod containerPod, ContainerHost host) {
            if (host == null) {
                Log.formatLine("%.2f: No suitable host found for VM #" + containerPod.getId() + "\n", CloudSim.clock());
                return false;
            }
            if (host.containerVmCreate(containerPod)) { // if vm has been succesfully created in the host
                getVmTable().put(containerPod.getId(), host);
                Log.formatLine(
                        "%.2f: VM #" + containerPod.getId() + " has been allocated to the host #" + host.getId(),
                        CloudSim.clock());
                return true;
            }
            Log.formatLine(
                    "%.2f: Creation of VM #" + containerPod.getId() + " on the host #" + host.getId() + " failed\n",
                    CloudSim.clock());
            return false;
        }

        /**
         * Find host for vm.
         *
         * @param containerPod the vm
         * @return the power host
         */
        public ContainerHost findHostForVm(ContainerPod containerPod) {
            for (ContainerHost host : this.<ContainerHost> getContainerHostList()) {
                Log.formatLine("host: " + host.getId());
                if (host.isSuitableForContainerVm(containerPod)) {
                    return host;
                }
            }
            return null;
        }

        public List<PowerContainerHost> findHostForPods(List<ContainerPod> containerPods) {
            List<PowerContainerHost> ret = new ArrayList<>();
            for(ContainerPod containerPod: containerPods) {
                for (ContainerHost host : this.<ContainerHost>getContainerHostList()) {
                    Log.formatLine("host: " + host.getId());

                    if (host.isSuitableForContainerVm(containerPod)) {
                        ret.add((PowerContainerHost) host);
                    }

                }
            }
            return ret;
        }

        /*
         * (non-Javadoc)
         * @see com.wfc.cloudsim.cloudsim.PodAllocationPolicy#deallocateHostForVm(com.wfc.cloudsim.cloudsim.Pod)
         */
        @Override
        public void deallocateHostForVm(ContainerPod containerPod) {
            ContainerHost host = getVmTable().remove(containerPod.getUid());
            if (host != null) {
                host.containerVmDestroy(containerPod);
            }
        }

        /*
         * (non-Javadoc)
         * @see com.wfc.cloudsim.cloudsim.PodAllocationPolicy#getHost(com.wfc.cloudsim.cloudsim.Pod)
         */
        @Override
        public ContainerHost getHost(ContainerPod vm) {
            return getVmTable().get(vm.getId());
        }

        /*
         * (non-Javadoc)
         * @see com.wfc.cloudsim.cloudsim.PodAllocationPolicy#getHost(int, int)
         */
        @Override
        public ContainerHost getHost(int vmId, int userId) {
           // return getVmTable().get(ContainerPod.getUid(userId, vmId));
            return getVmTable().get(vmId);
        }

        /**
         * Gets the vm table.
         *
         * @return the vm table
         */
        public Map<Integer, ContainerHost> getVmTable() {
            return vmTable;
        }

    public List<ContainerPod> getOverUtilizedVms() {
        List<ContainerPod> vmList = new ArrayList<ContainerPod>();
        for (ContainerHost host : getContainerHostList()) {
            for (ContainerPod vm : host.getVmList()) {
                if (vm.getTotalUtilizationOfCpuMips(CloudSim.clock()) > vm.getTotalMips()) {
                    vmList.add(vm);

                }

            }

        }
        return vmList;
    }


}