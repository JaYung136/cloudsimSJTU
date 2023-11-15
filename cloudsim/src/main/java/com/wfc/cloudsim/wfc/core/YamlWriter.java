package com.wfc.cloudsim.wfc.core;


import com.wfc.cloudsim.cloudsim.Log;
import com.wfc.cloudsim.cloudsim.Pod;
import com.wfc.cloudsim.cloudsim.PodAllocationPolicy;
import com.wfc.cloudsim.cloudsim.container.core.ContainerPod;
import com.wfc.cloudsim.cloudsim.container.resourceAllocators.ContainerAllocationPolicy;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class YamlWriter {
    /*public static com.wfc.cloudsim.workflowsim.k8s.Pod ParsePodFromPath(String path) throws FileNotFoundException {
        Yaml yaml = new Yaml();
        InputStream input = new FileInputStream(new File(path));
        Scanner s = new Scanner(input).useDelimiter("\\A");
        String result = s.hasNext() ? s.next() : "";
        Log.printLine(result);
        Pod pod;
        pod = yaml.loadAs(input, Pod.class);
        return pod;
    }

    public static void main(String[] args) throws FileNotFoundException {
        Pod pod = YamlUtil.ParsePodFromPath("config/pod/pod.yml");

    }*/

    public void writeYaml(String path, List<ContainerPod> pods) {
        Log.printLine("YamlWriter: write " + pods.size() + " pod");
        for(int i = 0; i < pods.size(); i++) {
            if(pods.get(i).getId() == WFCConstants.WFC_NUMBER_VMS)
                continue;
            Map<String, Object> podConfig = new HashMap<>();
            podConfig.put("apiVersion", "v1");
            podConfig.put("kind", "Pod");

            /* metadata */
            Map<String, Object> metaData = new HashMap<>();
            metaData.put("name", "pod_" + pods.get(i).getId());
            podConfig.put("metadata", metaData);

            /* spec */
            Map<String, Object> spec = new HashMap<>();
            List<Map<String, Object>> containers = new ArrayList<>();
            String nodeName = "Host " + pods.get(i).getHost().getId();
            for(int j = 0; j < 1; j++) {
                Map<String, Object> container = new HashMap<>();
                container.put("name", "container_" + i + "_" + j);
                container.put("image", "");
                Map<String, Object> resources = new HashMap<>();
                Map<String, Object> limits = new HashMap<>();
                limits.put("cpu", pods.get(i).getNumberOfPes());
                limits.put("memory", pods.get(i).getRam());
                resources.put("limits", limits);
                container.put("resources", resources);
                containers.add(container);
            }
            spec.put("nodeName", nodeName);
            spec.put("containers", containers);
            podConfig.put("spec", spec);

            DumperOptions options = new DumperOptions();
            options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);

            Yaml yaml = new Yaml(options);
            String yamlString = yaml.dump(podConfig);
            String pathFile = path+"/pod_" + pods.get(i).getId() + ".yml";
            try {
                FileWriter writer = new FileWriter(pathFile);
                writer.write(yamlString);
                writer.close();
                System.out.println("YAML file generated successfully.");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

}
