package com.wfc.cloudsim.cloudsim.container.core;

import com.wfc.cloudsim.cloudsim.container.containerPodProvisioners.ContainerPodBwProvisioner;
import com.wfc.cloudsim.cloudsim.container.containerPodProvisioners.ContainerPodPe;
import com.wfc.cloudsim.cloudsim.container.containerPodProvisioners.ContainerPodRamProvisioner;
import com.wfc.cloudsim.cloudsim.container.schedulers.ContainerPodScheduler;
import com.wfc.cloudsim.cloudsim.power.models.PowerModel;
import com.wfc.cloudsim.cloudsim.util.MathUtil;

import java.util.List;

/**
 * Created by sareh on 15/07/15.
 */
public class PowerContainerHostUtilizationHistory extends PowerContainerHost {

    /**
     * Instantiates a new power host utilization history.
     *
     * @param id             the id
     * @param ramProvisioner the ram provisioner
     * @param bwProvisioner  the bw provisioner
     * @param storage        the storage
     * @param peList         the pe list
     * @param vmScheduler    the vm scheduler
     * @param powerModel     the power model
     */
    public PowerContainerHostUtilizationHistory(
            int id,
            ContainerPodRamProvisioner ramProvisioner,
            ContainerPodBwProvisioner bwProvisioner,
            long storage,
            List<? extends ContainerPodPe> peList,
            ContainerPodScheduler vmScheduler,
            PowerModel powerModel) {
        super(id, ramProvisioner, bwProvisioner, storage, peList, vmScheduler, powerModel);
    }

    /**
     * Gets the host utilization history.
     *
     * @return the host utilization history
     */
    public double[] getUtilizationHistory() {
        double[] utilizationHistory = new double[PowerContainerPod.HISTORY_LENGTH];
        double hostMips = getTotalMips();
        for (PowerContainerPod vm : this.<PowerContainerPod>getVmList()) {
            for (int i = 0; i < vm.getUtilizationHistory().size(); i++) {
                utilizationHistory[i] += vm.getUtilizationHistory().get(i) * vm.getMips() / hostMips;
            }
        }
        return MathUtil.trimZeroTail(utilizationHistory);
    }

}

