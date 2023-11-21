package backend.controller;

//import com.reins.bookstore.service.LoginService;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.sdn.main.SimpleExampleInterCloud;
import org.cloudbus.cloudsim.sdn.workload.Workload;
import org.cloudbus.cloudsim.sdn.workload.WorkloadResultWriter;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.XML;
import org.json.simple.JSONValue;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

@RestController
//@Scope(value = "singleton")
@CrossOrigin
public class Controller {
    private SimpleExampleInterCloud simulator;

    private String physicalf = "Intermediate/physical.json";
    private String virtualf = "Intermediate/virtual.json";
    private String workloadf = "Intermediate/workload.csv";
    private boolean halfDuplex = false;

    @RequestMapping("/visit")
    public ResultDTO login(@RequestBody String req){
        System.out.println("访问后端");
        System.out.println(CloudSim.HalfDuplex);
        System.out.println(CloudSim.HalfDuplex);
        System.out.println(CloudSim.HalfDuplex);
        return ResultDTO.success("This is simulator backend");
    }

    @RequestMapping("/halfduplex")
    public ResultDTO halfduplex(@RequestBody String req) {
        JSONObject state = new JSONObject(req);
        halfDuplex = state.getBoolean("switchstate");
        System.out.println(String.valueOf(halfDuplex));
        CloudSim.HalfDuplex = halfDuplex;
        CloudSim.wirelessBw = 10000000; //10M
        return ResultDTO.success("ok");
    }

    @RequestMapping("/convertphytopo")
    public ResultDTO convertphytopo() throws IOException {
        String xml = Files.readString(
                Path.of("./InputFiles/Input_TopoInfo.xml"));
        JSONObject json = XML.toJSONObject(xml).getJSONObject("NetworkTopo");
        JSONArray swches = json.getJSONObject("Switches").getJSONArray("Switch");
        JSONArray links = json.getJSONObject("Links").getJSONArray("Link");

        // 计算多少dc
        Set<String> dcnames = new HashSet<>();
        for(Object obj : swches){
            JSONObject swch = (JSONObject) obj;
            String dcname = swch.getString("Network");
            dcnames.add(dcname);
        }

        JSONObject topo = new JSONObject();
        // 新建wirelessnetwork dc、interswitch
        topo.accumulate("datacenters", new JSONObject()
                .put("name","net").put("type", "wirelessnetwork"));
        topo.accumulate("nodes", new JSONObject()
                .put("upports", 0)
                .put("downports", 0)
                .put("iops", 1000000000)
                .put("name","inter")
                .put("type","intercloud")
                .put("datacenter","net")
                .put("bw", 100000000) //100M
        );
        // 新建普通dc、gateways
        for(String dcname : dcnames){
            topo.accumulate("datacenters", new JSONObject()
                    .put("name",dcname).put("type", "cloud"));
            topo.accumulate("nodes", new JSONObject()
                    .put("upports", 0)
                    .put("downports", 0)
                    .put("iops", 1000000000)
                    .put("name","gw"+dcname)
                    .put("type","gateway")
                    .put("datacenter","net")
                    .put("bw", 100000000) //100M
            );
            topo.accumulate("nodes", new JSONObject()
                    .put("upports", 0)
                    .put("downports", 0)
                    .put("iops", 1000000000)
                    .put("name","gw"+dcname)
                    .put("type","gateway")
                    .put("datacenter",dcname)
                    .put("bw", 100000000) //100M
            );
        }
        // 新建所有的交换机、links
        for(Object obj : swches){
            JSONObject swch = (JSONObject) obj;
            topo.accumulate("nodes", new JSONObject()
                    .put("upports", 0)
                    .put("downports", 0)
                    .put("iops", 1000000000)
                    .put("name",swch.getString("Name"))
                    .put("type",swch.getString("Type"))
                    .put("datacenter",swch.getString("Network"))
                    .put("ports",swch.getInt("PortNum"))
                    .put("bw",(long) swch.getDouble("Speed")*1000000));
        }
        for(Object obj : links){
            JSONObject link = (JSONObject) obj;
            topo.accumulate("links", new JSONObject()
                    .put("source",link.getString("Src"))
                    .put("destination",link.getString("Dst"))
                    .put("latency",String.valueOf(link.getDouble("Latency")))
                    .put("name", link.getString("Name"))
            );
        }
        // 补建links：gateway<->interswitch
        for(String dcname : dcnames){
            topo.accumulate("links", new JSONObject()
                    .put("source","inter")
                    .put("destination","gw"+dcname)
                    .put("latency","0")
                    .put("name", "gw"+dcname+"-inter")
            );
        }
        // 补建links：core<->gateway
        for(Object obj : swches){
            JSONObject swch = (JSONObject) obj;
            if( swch.getString("Type").equals("core")){
                topo.accumulate("links", new JSONObject()
                        .put("source",swch.getString("Name"))
                        .put("destination","gw"+swch.getString("Network"))
                        .put("latency","0")
                        .put("name", "gw"+swch.getString("Network")+"-core")
                );
            }
        }
        // 新建所有的主机
        xml = Files.readString(Path.of("./InputFiles/Input_Host8.xml"));
        json = XML.toJSONObject(xml);
        JSONArray hosts = json.getJSONObject("adag").getJSONArray("node");
        for(Object obj : hosts){
            JSONObject host = (JSONObject) obj;
            topo.accumulate("nodes", new JSONObject()
                    .put("name",host.getString("name"))
                    .put("type","host")
                    .put("datacenter",host.getString("network"))
                    .put("bw",(long) host.getDouble("bandwidth")*1000000)
                    .put("pes",host.getInt("cores"))
                    .put("mips",host.getLong("mips"))
                    .put("ram", host.getInt("memory"))
                    .put("storage",host.getLong("storage")));
        }
        String jsonPrettyPrintString = topo.toString(4);
        //保存格式化后的json
        FileWriter writer = new FileWriter("Intermediate/physical.json");
        writer.write(jsonPrettyPrintString);
        writer.close();
        return ResultDTO.success("ok");
    }


    // 必需保持 hostname = “host” + hostid 对应关系。flows字段在解析workload文件时添加
    @RequestMapping("/convertvirtopo")
    public ResultDTO convertvirtopo() throws Exception{
        String content = Files.readString(Path.of("./InputFiles/result1.json"));
        JSONArray json = new JSONArray(content);
        JSONObject vir = new JSONObject();
        for(Object obj : json){
            JSONObject vm = (JSONObject) obj;
            vir.accumulate("nodes", vm);
        }
        JSONArray vms = vir.getJSONArray("nodes");
        for(int i=0; i<vms.length(); ++i){
            for(int j=0; j<vms.length(); ++j){
                if(j==i) {
                    continue;
                }
                vir.accumulate("flows", new JSONObject()
                        .put("name", "default")
                        .put("source", vms.getJSONObject(j).getString("name"))
                        .put("destination",vms.getJSONObject(i).getString("name"))
                );
            }
        }
        vir.put("policies", new JSONArray());
        String jsonPrettyPrintString = vir.toString(4);
        //保存格式化后的json
        FileWriter writer = new FileWriter("Intermediate/virtual.json");
        writer.write(jsonPrettyPrintString);
        writer.close();
        return ResultDTO.success(jsonPrettyPrintString);
    }

    @RequestMapping("/convertworkload")
    public ResultDTO convertworkload() throws Exception{
        //读result1制作ip->starttime/endtime的字典
        String content = Files.readString(Path.of("./InputFiles/result1.json"));
        JSONArray json = new JSONArray(content);
        Map<String, String> startmap = new HashMap<>();
        Map<String, String> endmap = new HashMap<>();

        for(Object obj : json) {
            JSONObject host = (JSONObject) obj;
            startmap.put(host.getString("name"), host.getString("start"));
            endmap.put(host.getString("name"), host.getString("end"));
        }

        //读appinfo.xml 写workload.csv
        String xml = Files.readString(
                Path.of("./InputFiles/Input_AppInfo10.xml"));
        JSONArray apps = XML.toJSONObject(xml).getJSONObject("AppInfo").getJSONArray("Application");
        String filePath = "./Intermediate/workload.csv";

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath))) {
            writer.write("count,period,atime,name.1,zeros,w.1.1,link.1.2,name.2,p.1.2,w.2.1,link.2.3,name.3,p.2.3,w.3\n");
            for(Object obj : apps){
                JSONObject app = (JSONObject) obj;
                String src = app.getString("IpAddress");
                JSONObject tem = app.getJSONObject("A653SamplingPort").getJSONObject("A664Message");
                Object dataField = tem.opt("A653SamplingPort");

                //case1:大于等于2条msg
                if (dataField instanceof JSONArray) {
                    JSONArray msgs = (JSONArray) dataField;
                    for (Object obj2 : msgs) {
                        JSONObject msg = (JSONObject) obj2;
                        String dst = msg.getString("IpAddress");
                        int period = msg.getInt("SamplePeriod");
                        int pktsize = msg.getInt("MessageSize");
                        int count = 0;
                        for (double t = Double.parseDouble(startmap.get(src)); t < Double.parseDouble(endmap.get(src)); t += period) {
                            count++;
                        }
                        writer.write(count + "," + period + "," + startmap.get(src) + "," + src + ",0,0,default," + dst + "," + pktsize + ",0,,,,\n");
                    }
                }
                //case2:1条msg
                else {
                    JSONObject msg = (JSONObject) dataField;
                    String dst = msg.getString("IpAddress");
                    int period = msg.getInt("SamplePeriod");
                    int pktsize = msg.getInt("MessageSize");
                    int count = 0;
                    for (double t = Double.parseDouble(startmap.get(src)); t < Double.parseDouble(endmap.get(src)); t += period) {
                        count++;
                    }
                    writer.write(count + "," + period + "," + startmap.get(src) + "," + src + ",0,0,default," + dst + "," + pktsize + ",0,,,,\n");
                }
            }
        } catch (IOException e) {
            System.out.println("An error occurred while writing the CSV file: " + e.getMessage());
        }
        return ResultDTO.success("ok");
    }


    @RequestMapping("/run")
    public ResultDTO run() throws IOException {
        System.out.println("\n开始仿真");
//        String args[] = {"LFF","example-intercloud/intercloud.physical2.xml","example-intercloud/intercloud.virtual2.json", "example-intercloud/one-workload.csv"};
        String args[] = {"",physicalf,virtualf,workloadf};
        simulator = new SimpleExampleInterCloud();
        List<Workload> wls = simulator.main(args);
        List<WorkloadResult> wrlist = new ArrayList<>();
        for(Workload workload:wls){
//------------------------------------------ calculate total time
            double finishTime = -1;
            double startTime = -1;
            double Time = -1;
            finishTime = WorkloadResultWriter.getWorkloadFinishTime(workload);
            startTime = WorkloadResultWriter.getWorkloadStartTime(workload);
            if (finishTime > 0)
                Time = finishTime - startTime;
//------------------------------------------
            WorkloadResult wr = new WorkloadResult();
            wr.jobid = workload.jobId;
            wr.workloadid = workload.workloadId;
            wr.vmid = workload.submitVmName;
            wr.destid = workload.destVmName;
            if(workload.failed)
                wr.status = "timeout";
            else
                wr.status = "arrived";
            wr.finishtime = String.format("%.4f", finishTime);
            wr.starttime = String.format("%.4f", startTime);
            wr.time = String.format("%.4f", Time);
            wrlist.add(wr);
        }
        WorkloadResult[] wrarray = wrlist.toArray(new WorkloadResult[wrlist.size()]);
        return ResultDTO.success(wrarray);
    }
}