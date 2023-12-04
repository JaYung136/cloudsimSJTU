package org.sim.service;



import javafx.util.Pair;
import org.sim.cloudbus.cloudsim.Cloudlet;
import org.sim.cloudbus.cloudsim.Log;
import org.sim.workflowsim.Job;
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

    public void writeYaml(String path, List<? extends Cloudlet> pods) throws Exception {
        Log.printLine("YamlWriter: write " + pods.size() + " pod");
        for(int i = 0; i < pods.size(); i++) {
            if(((Job)pods.get(i)).getTaskList().size() < 1) {
                Log.printLine("no need to write");
                continue;
            }
            int cId = ((Job)pods.get(i)).getTaskList().get(0).getCloudletId();
            String name = Constants.id2Name.get(cId);
            Container c = Constants.name2Container.get(name);
            Map<String, Object> podConfig = new HashMap<>();
            List<Map<String, String>> labels = new ArrayList<>();
            podConfig.put("apiVersion", "v1");
            podConfig.put("kind", "Pod");

            /* metadata */
            Map<String, Object> metaData = new HashMap<>();
            metaData.put("name", "pod_" + pods.get(i).getCloudletId());
            for(Pair<String, String> p: c.labels) {
                Map<String, String> l = new HashMap<>();
                l.put(p.getKey(), p.getValue());
                labels.add(l);
            }
            metaData.put("labels", labels);
            podConfig.put("metadata", metaData);

            /* spec */
            Map<String, Object> spec = new HashMap<>();
            List<Map<String, Object>> containers = new ArrayList<>();
            String nodeName = "Host " + pods.get(i).getVmId();
            for(int j = 0; j < 1; j++) {
                Map<String, Object> container = new HashMap<>();
                container.put("name", "container_" + i + "_" + j);
                container.put("image", c.image);
                Map<String, Object> resources = new HashMap<>();
                Map<String, Object> limits = new HashMap<>();
                limits.put("cpu", pods.get(i).getNumberOfPes());
                limits.put("memory", ((Job)pods.get(i)).getTaskList().get(0).getRam());
                resources.put("limits", limits);
                container.put("resources", resources);
                if(!c.commands.isEmpty()) {
                    List<String> coms = new ArrayList<>();
                    for(String s: c.commands) {
                        coms.add(s);
                    }
                    container.put("command", coms);
                }
                if(!c.args.isEmpty()) {
                    List<String> as = new ArrayList<>();
                    for(String s: c.args) {
                        as.add(s);
                    }
                    container.put("args", as);
                }
                if(!c.workingDir.isEmpty()) {
                    container.put("workingDir", c.workingDir);
                }
                containers.add(container);

            }
            spec.put("nodeName", nodeName);
            spec.put("containers", containers);
            podConfig.put("spec", spec);

            DumperOptions options = new DumperOptions();
            options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);

            Yaml yaml = new Yaml(options);
            String yamlString = yaml.dump(podConfig);
            String pathFile = path+"/pod_" + pods.get(i).getCloudletId() + ".yml";
            try {
                FileWriter writer = new FileWriter(pathFile);
                writer.write(yamlString);
                writer.close();
                System.out.println("YAML file generated successfully.");
            } catch (IOException e) {
                e.printStackTrace();
                throw e;
            }
        }
    }

}
