package com.wfc.cloudsim.cloudsim.container.resourceAllocatorMigrationEnabled;

import com.wfc.cloudsim.cloudsim.Log;
import com.wfc.cloudsim.cloudsim.container.core.*;
import com.wfc.cloudsim.cloudsim.container.hostSelectionPolicies.HostSelectionPolicy;
import com.wfc.cloudsim.cloudsim.container.podSelectionPolicies.PowerContainerPodSelectionPolicy;
import com.wfc.cloudsim.wfc.core.WFCDatacenter;

import java.util.*;

/**
 * Created by sareh on 17/11/15.
 */
public class PowerContainerPodAllocationPolicyMigrationAbstractHostSelection extends PowerContainerPodAllocationPolicyMigrationAbstract {

    private HostSelectionPolicy hostSelectionPolicy;
    private double utilizationThreshold = 0.9;
    private double underUtilizationThreshold = 0.7;

    /**
     * Instantiates a new power vm allocation policy migration abstract.
     *
     * @param hostSelectionPolicy
     * @param hostList            the host list
     * @param vmSelectionPolicy   the vm selection policy
     */
    public PowerContainerPodAllocationPolicyMigrationAbstractHostSelection(List<? extends ContainerHost> hostList, PowerContainerPodSelectionPolicy vmSelectionPolicy, HostSelectionPolicy hostSelectionPolicy, double OlThreshold, double UlThreshold) {
        super(hostList, vmSelectionPolicy);
        setHostSelectionPolicy(hostSelectionPolicy);
        setUtilizationThreshold(OlThreshold);
        setUnderUtilizationThreshold(UlThreshold);
    }

    @Override
    public List<PowerContainerHost> findHostForPods(List<ContainerPod> pods) {
        Map<Integer, ContainerHost> ret = getHostSelectionPolicy().getHosts(getContainerHostList(), pods);
        List<PowerContainerHost> re = new ArrayList<>();
        for(int i = 0; i < pods.size(); i++) {
            re.add((PowerContainerHost) ret.get(pods.get(i).getId()));
        }
        return re;
    }

    @Override
    /**
     * Find host for vm.
     *
     * @param vm            the vm
     * @param excludedHosts the excluded hosts
     * @return the power host
     */
    public PowerContainerHost findHostForVm(ContainerPod vm, Set<? extends ContainerHost> excludedHosts) {
        PowerContainerHost allocatedHost = null;
        Boolean find = false;
        Set<ContainerHost> excludedHost1 = new HashSet<>();
        excludedHost1.addAll(excludedHosts);
        while (!find) {
            Log.printLine("excludedHost's size: " + excludedHost1.size());
            ContainerHost host = getHostSelectionPolicy().getHost(getContainerHostList(), vm, excludedHost1);
            if (host == null) {
                Log.printLine("pod's id: " + vm.getId());
                return allocatedHost;
            }
            if (host.isSuitableForContainerVm(vm)) {
                Log.printLine("find a suitable host: host " + host.getId());
                find = true;
                allocatedHost = (PowerContainerHost) host;
            } else {
                excludedHost1.add(host);
                if (getContainerHostList().size() == excludedHost1.size()) {

                    return null;

                }
            }

        }
        return allocatedHost;
    }


    public HostSelectionPolicy getHostSelectionPolicy() {
        return hostSelectionPolicy;
    }

    public void setHostSelectionPolicy(HostSelectionPolicy hostSelectionPolicy) {
        this.hostSelectionPolicy = hostSelectionPolicy;
    }


    /**
     * Checks if is host over utilized.
     *
     * @param host the _host
     * @return true, if is host over utilized
     */
    @Override
    protected boolean isHostOverUtilized(PowerContainerHost host) {
        addHistoryEntry(host, getUtilizationThreshold());
        double totalRequestedMips = 0;
        double totalRequestedCores = 0;
        for (ContainerPod vm : host.getVmList()) {
            totalRequestedMips += vm.getCurrentRequestedTotalMips();
            totalRequestedCores += vm.getNumberOfPes();
        }
        double utilization = totalRequestedMips / host.getTotalMips();
        double coreUtilization = totalRequestedCores / host.getNumberOfPes();
        host.getUtilizationMips();
        return coreUtilization > getUtilizationThreshold();
    }

    @Override
    protected boolean isHostUnderUtilized(PowerContainerHost host) {
        return false;
    }

    /**
     * Sets the utilization threshold.
     *
     * @param utilizationThreshold the new utilization threshold
     */
    protected void setUtilizationThreshold(double utilizationThreshold) {
        this.utilizationThreshold = utilizationThreshold;
    }

    /**
     * Gets the utilization threshold.
     *
     * @return the utilization threshold
     */
    protected double getUtilizationThreshold() {
        return utilizationThreshold;
    }

    public double getUnderUtilizationThreshold() {
        return underUtilizationThreshold;
    }

    public void setUnderUtilizationThreshold(double underUtilizationThreshold) {
        this.underUtilizationThreshold = underUtilizationThreshold;
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
            if (!areAllVmsMigratingOutOrAnyVmMigratingIn(host) && utilization < getUnderUtilizationThreshold() && !areAllContainersMigratingOutOrAnyContainersMigratingIn(host)) {
                underUtilizedHostList.add(host);
            }
        }
        return underUtilizedHostList;
    }


    @Override
    public void setDatacenter(WFCDatacenter datacenter) {
    }
}
