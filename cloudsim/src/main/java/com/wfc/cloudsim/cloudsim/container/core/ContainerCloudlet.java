package com.wfc.cloudsim.cloudsim.container.core;

import com.wfc.cloudsim.cloudsim.Cloudlet;
import com.wfc.cloudsim.cloudsim.UtilizationModel;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by sareh on 10/07/15.
 */
public class ContainerCloudlet extends Cloudlet {
    public int containerId = -1;

    public int hostId = -1;
    public Boolean ifJob() {
        return false;
    }


    public ContainerCloudlet(int cloudletId, long cloudletLength, int pesNumber, long cloudletFileSize, long cloudletOutputSize, UtilizationModel utilizationModelCpu, UtilizationModel utilizationModelRam, UtilizationModel utilizationModelBw) {

        super(cloudletId, cloudletLength, pesNumber, cloudletFileSize, cloudletOutputSize, utilizationModelCpu, utilizationModelRam, utilizationModelBw);

    }

    public ContainerCloudlet(int cloudletId, long cloudletLength, int pesNumber, long cloudletFileSize, long cloudletOutputSize, UtilizationModel utilizationModelCpu, UtilizationModel utilizationModelRam, UtilizationModel utilizationModelBw, boolean record, List<String> fileList) {
        super(cloudletId, cloudletLength, pesNumber, cloudletFileSize, cloudletOutputSize, utilizationModelCpu, utilizationModelRam, utilizationModelBw, record, fileList);

    }

    public ContainerCloudlet(int cloudletId, long cloudletLength, int pesNumber, long cloudletFileSize, long cloudletOutputSize, UtilizationModel utilizationModelCpu, UtilizationModel utilizationModelRam, UtilizationModel utilizationModelBw, List<String> fileList) {
        super(cloudletId, cloudletLength, pesNumber, cloudletFileSize, cloudletOutputSize, utilizationModelCpu, utilizationModelRam, utilizationModelBw, fileList);
    }

    public ContainerCloudlet(int cloudletId, long cloudletLength, int pesNumber, long cloudletFileSize, long cloudletOutputSize, UtilizationModel utilizationModelCpu, UtilizationModel utilizationModelRam, UtilizationModel utilizationModelBw, boolean record) {
        super(cloudletId, cloudletLength, pesNumber, cloudletFileSize, cloudletOutputSize, utilizationModelCpu, utilizationModelRam, utilizationModelBw, record);
    }

    public int getContainerId() {
        return containerId;
    }

    public void setContainerId(int containerId) {
        if(ifJob()) {
            Logger.getGlobal().log(Level.INFO, "set job " + this.getCloudletId() + "'s container ID " + containerId);
        }
        this.containerId = containerId;       
    }

    public int getHostId() {
        return hostId;
    }

    public void setHostId(int hostId) {
        if(ifJob()) {
            Logger.getGlobal().log(Level.INFO, "set job " + this.getCloudletId() + "'s host ID " + hostId);
        }
        this.hostId = hostId;
    }
    
}
