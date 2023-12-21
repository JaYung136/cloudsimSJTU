package org.cloudbus.backend.controller;

//import com.reins.bookstore.service.LoginService;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.sdn.Configuration;
import org.cloudbus.cloudsim.sdn.LogWriter;
import org.cloudbus.cloudsim.sdn.main.SimpleExampleInterCloud;
import org.cloudbus.cloudsim.sdn.workload.Workload;
import org.cloudbus.cloudsim.sdn.workload.WorkloadResultWriter;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.XML;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import java.io.*;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

@RestController
//@Scope(value = "singleton")
@CrossOrigin
public class Controller {
    private SimpleExampleInterCloud simulator;
    private String input_topo = "./InputFiles/Input_TopoInfo.xml";
    private String input_host = "./InputFiles/Input_Hosts.xml";
    private String input_container = "./InputFiles/assign.json";
    private String input_app = "./InputFiles/Input_AppInfo.xml";
    private String physicalf = "Intermediate/physical.json";
    private String virtualf = "Intermediate/virtual.json";
    private String workloadf = "Intermediate/messages.csv";
    private String workload_result = "./Intermediate/result_messages.csv";
    private String latency_result = "./OutputFiles/latency/output_latency.xml";
    private String bwutil_result = "./OutputFiles/bandwidthUtil/link_utilization.xml";
    private Map<String, Long> wirelessChan_bw = new HashMap<>();
    private boolean halfDuplex = false;

    @RequestMapping("/visit")
    public ResultDTO hello(){
        System.out.println("simulator可访问");
        return ResultDTO.success("This is simulator backend");
    }

    @RequestMapping("/halfduplex")
    public ResultDTO halfduplex(@RequestBody String req) {
        JSONObject state = new JSONObject(req);
        halfDuplex = state.getBoolean("switchstate");
        System.out.println(String.valueOf(halfDuplex));
        CloudSim.HalfDuplex = halfDuplex;
        return ResultDTO.success("ok");
    }

    @RequestMapping("/uploadhost")
    public ResultDTO uploadhost(MultipartFile file, HttpServletRequest req) throws IOException {
        System.out.println("上传host.xml文件");
        try {
            String InputDir = System.getProperty("user.dir")+"\\InputFiles";
            System.out.println(InputDir);
            File hostfile = new File(InputDir,"Input_Hosts.xml");
            boolean dr = hostfile.getParentFile().mkdirs(); //创建目录
            file.transferTo(hostfile);
        }catch (IOException e){
            System.out.print(e.getMessage());
        }
        return ResultDTO.success("上传成功");
    }

    @RequestMapping("/uploadtopo")
    public ResultDTO uploadtopo(MultipartFile file, HttpServletRequest req) throws IOException {
        System.out.println("上传topo.xml文件");
        try {
            String InputDir = System.getProperty("user.dir")+"\\InputFiles";
            System.out.println(InputDir);
            File topofile = new File(InputDir,"Input_TopoInfo.xml");
            boolean dr = topofile.getParentFile().mkdirs(); //创建目录
            file.transferTo(topofile);
        }catch (IOException e){
            System.out.print(e.getMessage());
        }
        return ResultDTO.success("上传成功");
    }

    @RequestMapping("/uploadapp")
    public ResultDTO uploadapp(MultipartFile file, HttpServletRequest req) throws IOException {
        System.out.println("上传app.xml文件");
        try {
            String InputDir = System.getProperty("user.dir")+"\\InputFiles";
            System.out.println(InputDir);
            File appfile = new File(InputDir,"Input_AppInfo.xml");
            boolean dr = appfile.getParentFile().mkdirs(); //创建目录
            file.transferTo(appfile);
        } catch (IOException e){
            System.out.print(e.getMessage());
        }
        return ResultDTO.success("上传成功");
    }

    @RequestMapping("/getassign")
    public ResultDTO getassign(@RequestBody String req) throws IOException {
        String content = Files.readString(Path.of(input_container));
        JSONArray array = new JSONArray(content);
        return ResultDTO.success(array.toString());
    }

    @RequestMapping("/modifyassign")
    public ResultDTO modifyassign(@RequestBody String req) throws IOException {
        JSONArray array = new JSONArray(req);
        System.out.println("用户修改容器分配方案:");
        for (Object obj:array){
            JSONObject container = (JSONObject)obj;
            System.out.printf("\tapp: %s | ", container.getString("app"));
            System.out.printf("host: %s | ", container.getString("host"));
            System.out.printf("container: %s\n", container.getString("name"));
        }
        String jsonPrettyPrintString = array.toString(4);
        FileWriter writer = new FileWriter(input_container);
        writer.write(jsonPrettyPrintString);
        writer.close();
        return ResultDTO.success(array.toString());
    }

    @RequestMapping("/convertphytopo")
    public ResultDTO convertphytopo() throws IOException {
        String xml = Files.readString(Path.of(input_topo));
        JSONObject topojson = XML.toJSONObject(xml).getJSONObject("NetworkTopo");
        JSONObject swes = topojson.getJSONObject("Switches");
        JSONArray swches = new JSONArray();
        try {
            swches = swes.getJSONArray("Switch");
        } catch (Exception e){
            swches.clear();
            swches.put(swes.getJSONObject("Switch"));
        }
        JSONArray links = topojson.getJSONObject("Links").getJSONArray("Link");
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
        xml = Files.readString(Path.of(input_host));
        JSONObject hostjson = XML.toJSONObject(xml);
        JSONArray hosts = hostjson.getJSONObject("adag").getJSONArray("node");
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
        FileWriter writer = new FileWriter(physicalf);
        writer.write(jsonPrettyPrintString);
        writer.close();
        try {
            JSONObject endsys = topojson
                    .getJSONObject("EndSystems")
                    .getJSONObject("EndSystem")
                    .getJSONObject("AesPhysPorts");
            JSONArray sys = endsys.getJSONArray("AesPhysPort");
            for (Object obj : sys) {
                JSONObject wirelesschan = (JSONObject) obj;
                String name = wirelesschan.getString("Network");
                long bw = (long) (wirelesschan.getDouble("Speed") * 1000); //MB
                wirelessChan_bw.put(name, bw);
            }
        }catch (Exception e) {
            System.out.println("以太网络仿真");
        }
        CloudSim.bwLimit = 1.0;

        try {
            JSONArray ups = hostjson.getJSONObject("adag").getJSONArray("utilization");
            for (Object obj : ups) {
                JSONObject up = (JSONObject) obj;
                String name = up.getString("type");
                double upvalue =  up.getDouble("up");
                if(name.equals("bandwidth")){
                    CloudSim.bwLimit = upvalue;
                }
            }
        } catch (Exception e){
            try {
                JSONArray ups = new JSONArray();
                ups.put(hostjson.getJSONObject("adag").getJSONObject("utilization"));
                for (Object obj : ups) {
                    JSONObject up = (JSONObject) obj;
                    String name = up.getString("type");
                    double upvalue =  up.getDouble("up");
                    if(name.equals("bandwidth")){
                        CloudSim.bwLimit = upvalue;
                    }
                }
            }
            catch (Exception e1){
            }
        }
        System.out.println("带宽利用率:"+CloudSim.bwLimit);
        return ResultDTO.success("ok");
    }

    // 必需保持 hostname = “host” + hostid 对应关系。flows字段在解析workload文件时添加
    @RequestMapping("/convertvirtopo")
    public ResultDTO convertvirtopo() throws IOException{
        String content = Files.readString(Path.of(input_container));
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
        FileWriter writer = new FileWriter(virtualf);
        writer.write(jsonPrettyPrintString);
        writer.close();
        return ResultDTO.success(jsonPrettyPrintString);
    }

    @RequestMapping("/convertworkload")
    public ResultDTO convertworkload() throws IOException{
        //读result1制作ip->starttime/endtime的字典
        String content = Files.readString(Path.of(input_container));
        JSONArray json = new JSONArray(content);
        Map<String, String> startmap = new HashMap<>();
        Map<String, String> endmap = new HashMap<>();

        for(Object obj : json) {
            JSONObject host = (JSONObject) obj;
            startmap.put(host.getString("name"), host.getString("start"));
            endmap.put(host.getString("name"), host.getString("end"));
        }

        //读appinfo.xml 写workload.csv
        String xml = Files.readString(Path.of(input_app));
        JSONArray apps = XML.toJSONObject(xml).getJSONObject("AppInfo").getJSONArray("application");
        String filePath = workloadf;

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath))) {
            writer.write("count,period,atime,name.1,zeros,w.1.1,link.1.2,name.2,p.1.2,w.2.1,link.2.3,name.3,p.2.3,w.3\n");
            for(Object obj : apps){
                JSONObject app = (JSONObject) obj;
                String src = app.getString("IpAddress");
                JSONObject tem = new JSONObject();
                try {
                    tem = app.getJSONObject("A653SamplingPort").getJSONObject("A664Message");
                }catch (Exception e){
                    continue;
                }

                Object dataField = tem.opt("A653SamplingPort");

                //case1:大于等于2条msg
                if (dataField instanceof JSONArray) {
                    JSONArray msgs = (JSONArray) dataField;
                    for (Object obj2 : msgs) {
                        JSONObject msg = (JSONObject) obj2;
                        String dst = msg.getString("IpAddress");
                        double period = msg.getDouble("SamplePeriod");
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
                    double period = msg.getDouble("SamplePeriod");
                    int pktsize = msg.getInt("MessageSize");
                    int count = 0;
                    double st = Double.parseDouble(startmap.get(src));
                    double ed = Double.parseDouble(endmap.get(src));
                    for (double t = st; t <= ed; t += period) {
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

    @RequestMapping("/outputdelay")
    public ResultDTO outputdelay() throws IOException{
        // 读取CSV文件
        CSVReader csvReader = new CSVReaderBuilder(new FileReader(workload_result)).build();
        List<String[]> csvData = csvReader.readAll();

        //创建xml
        File file = new File(latency_result);
        file.createNewFile();

        // 写入
        FileWriter fw = new FileWriter(file.getAbsoluteFile());
        BufferedWriter bw = new BufferedWriter(fw);
        bw.write("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
                "<NetworkDelay>\n<Messages>\n");

        int count = 0;
        double totaltime = 0.0;
        for (int i = 1; i < csvData.size(); i++) {
            String[] row = csvData.get(i);
            try {
                bw.write("\t<Message Src=\"" + row[1].trim() + "\" Dst=\"" + row[2].trim() + "\" StartTime=\"" + row[4].trim() + "\" EndTime=\"" + row[15].trim() + "\" NetworkTime=\"" + row[18].trim() + "\" PkgSizeKB=\"" + row[12].trim() + "\">\n\t</Message>\n");
                ++count;
                totaltime += Double.parseDouble(row[18].trim());
            }
            catch (Exception e) {
                bw.write("\t<Message Src=\"" + row[1].trim() + "\" Dst=\"" + row[2].trim() + "\" StartTime=\"" + row[4].trim() + "\" EndTime=\"TimeOut\" NetworkTime=\"TimeOut\" PkgSizeKB=\"" + row[12].trim() + "\">\n\t</Message>\n");
            }
        }

        bw.write("</Messages>\n");
        bw.write("<TotalNetworkTime Time=\""+String.valueOf(totaltime)+"\"/>\n");
        bw.write("<AvgNetworkTime Time=\""+String.valueOf(totaltime/count)+"\"/>\n");
        bw.write("</NetworkDelay>");
        bw.close();
        return ResultDTO.success("ok");
    }

    @RequestMapping("/run")
    public ResultDTO run() throws IOException {
        System.out.println("\n开始仿真");
        CloudSim.HalfDuplex = false;
        CloudSim.wirelesschan_bw = wirelessChan_bw;

        convertphytopo();
        convertvirtopo();
        convertworkload();
        String args[] = {"",physicalf,virtualf,workloadf};
        LogWriter.resetLogger(bwutil_result);
        LogWriter log = LogWriter.getLogger(bwutil_result);
        log.printLine("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        log.printLine("<Links Timespan=\"" + Configuration.monitoringTimeInterval+ "\">");
        simulator = new SimpleExampleInterCloud();
        List<Workload> wls = simulator.main(args);
        log = LogWriter.getLogger(bwutil_result);
        log.printLine("</Links>");
        outputdelay();
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
            wr.finishtime = String.format("%.8f", finishTime);
            wr.starttime = String.format("%.8f", startTime);
            wr.time = String.format("%.8f", Time);
            wrlist.add(wr);
        }
        WorkloadResult[] wrarray = wrlist.toArray(new WorkloadResult[wrlist.size()]);
        return ResultDTO.success(wrarray);
    }
}
