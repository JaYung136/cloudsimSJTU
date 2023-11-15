package com.wfc.cloudsim.cloudsim.container.resourceAllocatorMigrationEnabled;

import com.wfc.cloudsim.cloudsim.container.containerSelectionPolicies.PowerContainerSelectionPolicy;
import com.wfc.cloudsim.cloudsim.container.core.ContainerHost;
import com.wfc.cloudsim.cloudsim.container.core.ContainerHostList;
import com.wfc.cloudsim.cloudsim.container.core.PowerContainerHost;
import com.wfc.cloudsim.cloudsim.container.hostSelectionPolicies.HostSelectionPolicy;
import com.wfc.cloudsim.cloudsim.container.podSelectionPolicies.PowerContainerPodSelectionPolicy;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Created by sareh on 13/08/15.
 */
public abstract class PowerContainerPodAllocationPolicyMigrationAbstractContainerHostSelectionUnderUtilizedAdded extends PowerContainerPodAllocationPolicyMigrationAbstractContainerHostSelection {

    private double underUtilizationThr;

    public PowerContainerPodAllocationPolicyMigrationAbstractContainerHostSelectionUnderUtilizedAdded(
            List<? extends ContainerHost> hostList, PowerContainerPodSelectionPolicy vmSelectionPolicy,
            PowerContainerSelectionPolicy containerSelectionPolicy, HostSelectionPolicy hostSelectionPolicy,
            double underUtilizationThr,
            int numberOfVmTypes, int[] vmPes, float[] vmRam, long vmBw, long vmSize, double[] vmMips) {
        super(hostList, vmSelectionPolicy, containerSelectionPolicy, hostSelectionPolicy,
        		 numberOfVmTypes, vmPes, vmRam, vmBw, vmSize, vmMips);
        setUnderUtilizationThr(underUtilizationThr);
    }


    @Override
    /**
     * Gets the under utilized host.
     *Checks if the utilization is under the threshold then counts it as underUtilized :)
     * @param excludedHosts the excluded hosts
     * @return the under utilized host
     */
    protected PowerContainerHost getUnderUtilizedHost(Set<? extends ContainerHost> excludedHosts) {

        List<ContainerHost> underUtilizedHostList = getUnderUtilizedHostList(excludedHosts);
        if (underUtilizedHostList.size() == 0) {

            return null;
        }
        ContainerHostList.sortByCpuUtilizationDescending(underUtilizedHostList);
//        Log.print(String.format("The under Utilized Hosts are %d", underUtilizedHostList.size()));
        PowerContainerHost underUtilizedHost = (PowerContainerHost) underUtilizedHostList.get(0);

        return underUtilizedHost;
    }

    @Override
    /**
     * Gets the under utilized host.
     *
     * @param excludedHosts the excluded hosts
     * @return the under utilized host
     */
    protected List<ContainerHost> getUnderUtilizedHostList(Set<? extends ContainerHost> excludedHosts) {
        List<ContainerHost> underUtilizedHostList = new ArrayList<>();
        for (PowerContainerHost host : this.<PowerContainerHost>getContainerHostList()) {
            if (excludedHosts.contains(host)) {
                continue;
            }
            double utilization = host.getUtilizationOfCpu();
            if (!areAllVmsMigratingOutOrAnyVmMigratingIn(host) && utilization < getUnderUtilizationThr() && !areAllContainersMigratingOutOrAnyContainersMigratingIn(host)) {
                underUtilizedHostList.add(host);
            }
        }
        return underUtilizedHostList;
    }

    public double getUnderUtilizationThr() {
        return underUtilizationThr;
    }

    public void setUnderUtilizationThr(double underUtilizationThr) {
        this.underUtilizationThr = underUtilizationThr;
    }
}
