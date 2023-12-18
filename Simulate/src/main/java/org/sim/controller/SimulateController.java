package org.sim.controller;

import javafx.util.Pair;
import org.json.JSONArray;
import org.json.JSONObject;
import org.sim.cloudbus.cloudsim.Log;
import org.sim.service.Constants;
import org.sim.service.YamlWriter;
import org.sim.workflowsim.XmlUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.sim.service.service;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.xml.XMLConstants;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;


@RestController
@CrossOrigin
public class SimulateController {
    @Autowired
    private service service;



    @RequestMapping("/uploadhost")
    public Message uploadhost(MultipartFile file, HttpServletRequest req) throws IOException {
        System.out.println("上传host.xml文件");
        Message r = new Message();
        try {
            String InputDir = System.getProperty("user.dir")+"\\InputFiles";
            System.out.println(InputDir);
            File hostfile = new File(InputDir,"Input_Hosts_a_try.xml");
            boolean dr = hostfile.getParentFile().mkdirs(); //创建目录
            file.transferTo(hostfile);
            Constants.hostFile = hostfile;
            try {
                SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
                File schemaFile = new File(System.getProperty("user.dir") + "\\Schema\\Host.xsd");
                Schema schema = factory.newSchema(schemaFile);
                Validator validator = schema.newValidator();
                StreamSource source = new StreamSource(hostfile);
                validator.validate(source);
                System.out.println("校验成功");
            } catch (Exception e) {
                e.printStackTrace();
                System.out.println("校验失败：" + e.getMessage());
                Constants.hostFile = null;
                r.message = e.getMessage();
                r.code = CODE.FAILED;
                return r;
            }
        }catch (IOException e){
            r.message = e.getMessage();
            r.code = CODE.FAILED;
            System.out.print(e.getMessage());
        }
        r.message = "upload successfully";
        r.code = CODE.SUCCESS;
        return r;
    }

    @RequestMapping("/uploadApp")
    public Message uploadApp(MultipartFile file, HttpServletRequest req) throws IOException {
        System.out.println("上传AppInfo.xml文件");
        Message r = new Message();
        try {
            String InputDir = System.getProperty("user.dir")+"\\InputFiles";
            System.out.println(InputDir);
            File hostfile = new File(InputDir,"Input_Apps_a_try.xml");
            boolean dr = hostfile.getParentFile().mkdirs(); //创建目录
            file.transferTo(hostfile);
            Constants.appFile = hostfile;
        }catch (IOException e){
            r.message = e.getMessage();
            r.code = CODE.FAILED;
            System.out.print(e.getMessage());
        }
        r.message = "upload successfully";
        r.code = CODE.SUCCESS;
        return r;
    }

    @RequestMapping("/uploadContainer")
    public Message uploadContainer(MultipartFile file, HttpServletRequest req) throws IOException {
        System.out.println("上传ContainerInfo.xml文件");
        Message r = new Message();
        try {
            Constants.name2Container = new HashMap<>();
            XmlUtil xmlUtil = new XmlUtil(-1);
            String InputDir = System.getProperty("user.dir")+"\\InputFiles";
            System.out.println(InputDir);
            File hostfile = new File(InputDir,"Input_Containers_a_try.xml");
            boolean dr = hostfile.getParentFile().mkdirs(); //创建目录
            file.transferTo(hostfile);
            Constants.containerFile = hostfile;
            xmlUtil.parseContainerInfo(hostfile);
        }catch (IOException e){
            r.message = e.getMessage();
            r.code = CODE.FAILED;
            System.out.print(e.getMessage());
        } catch (Exception e) {
            r.message = e.getMessage();
            r.code = CODE.FAILED;
            e.printStackTrace();
        }
        r.message = "upload successfully";
        r.code = CODE.SUCCESS;
        return r;
    }

    @RequestMapping("/uploadFault")
    public Message uploadFault(MultipartFile file, HttpServletRequest req) throws IOException {
        System.out.println("上传FaultInject.xml文件");
        Message r = new Message();
        try {
            String InputDir = System.getProperty("user.dir")+"\\InputFiles";
            System.out.println(InputDir);
            File hostfile = new File(InputDir,"Input_Fault_a_try.xml");
            boolean dr = hostfile.getParentFile().mkdirs(); //创建目录
            file.transferTo(hostfile);
            Constants.faultFile = hostfile;
        }catch (IOException e){
            r.message = e.getMessage();
            r.code = CODE.FAILED;
            System.out.print(e.getMessage());
        }
        r.message = "upload successfully";
        r.code = CODE.SUCCESS;
        return r;
    }

    @RequestMapping(value = "/startSimulate")
    public Message startSimulate(@RequestBody Map<String, Integer> req) {
        try{
            Message m = new Message();
            Constants.results = new ArrayList<>();
            Constants.logs = new ArrayList<>();
            Constants.resultPods = new ArrayList<>();
            Constants.id2Name = new HashMap<>();
            Constants.nodeEnough = true;
            Constants.faultNum = new HashMap<>();
            Constants.records = new ArrayList<>();
            Integer arithmetic = req.get("arithmetic");
            service.simulate(arithmetic);
            JSONArray array = new JSONArray();
            for(Result result: Constants.results) {
                JSONObject obj = new JSONObject().put("name", result.name).put("host", result.host).put("start", result.start).put("end", result.finish).put("size", result.size)
                        .put("mips", result.mips).put("pes", result.pes).put("type", result.type).put("datacenter", result.datacenter).put("ram", result.ram);
                array.put(obj);
            }
            try {
                String InputDir = System.getProperty("user.dir")+"\\Intermediate\\assign.json";
                OutputStreamWriter osw = new OutputStreamWriter(new FileOutputStream(InputDir), "UTF-8");
                osw.write(array.toString());

                osw.flush();//清空缓冲区，强制输出数据
                osw.close();//关闭输出流*/
            } catch (Exception e) {
                m.code = CODE.FAILED;
                m.message = e.getMessage();
                Log.printLine(e);
                return m;
            }

            m.code = CODE.SUCCESS;
            m.message = Constants.results;
            if(!Constants.nodeEnough) {
                m.code = CODE.FAILED;
                m.message = "node is not enough";
            } YamlWriter writer = new YamlWriter();
            try {
                String path = System.getProperty("user.dir")+"\\OutputFiles\\yaml";
                File dir = new File(path);
                dir.delete();
                dir.mkdirs();
                writer.writeYaml(path, Constants.resultPods);
                m.code = CODE.SUCCESS;
                m.message = "generate successfully";
            } catch (Exception e) {
                m.code = CODE.FAILED;
                m.message = e.getMessage();
            }

            return m;
        }catch (Exception e) {
            Message m = new Message();
            m.message = e.getMessage();
            m.code = CODE.FAILED;
            return m;
        }
    }

    @RequestMapping("/pauseContainer")
    public Message pauseContainer(@RequestBody String req) {
        Log.printLine("pauseContainer");
        JSONObject content = new JSONObject(req);
        Integer containerId = content.getInt("id");
        double start = content.getDouble("start");
        double last = content.getDouble("last");
        Message m = new Message();
        m.code = CODE.SUCCESS;
        m.message = "container " + containerId + " will be paused";
        Constants.pause.put(containerId, new Pair<>(start, last));
        return m;
    }

    @RequestMapping("/deletePause")
    public Message deletePause(@RequestBody String req) {
        Log.printLine("deletePause");
        JSONObject content = new JSONObject(req);
        Integer containerId = content.getInt("id");
        if(containerId == -1) {
            Constants.pause = new HashMap<>();
        } else {
            Constants.pause.remove(containerId);
        }
        Message m = new Message();
        m.code = CODE.SUCCESS;
        m.message = "delete pause";
        return m;
    }


    @RequestMapping("/writeYaml")
    public Message writeYaml(@RequestParam("path") String path) {
        Message ret = new Message();
        YamlWriter writer = new YamlWriter();
        try {
            writer.writeYaml(path, Constants.resultPods);
            ret.code = CODE.SUCCESS;
            ret.message = "generate successfully";
        } catch (Exception e) {
            ret.code = CODE.FAILED;
            ret.message = e.getMessage();
        }
        return ret;
    }

    @RequestMapping(value = "/hello")
    public String hello() {
        return "hello \n  ------ from CloudSim";
    }

}
