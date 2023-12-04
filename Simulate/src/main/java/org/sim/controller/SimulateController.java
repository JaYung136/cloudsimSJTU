package org.sim.controller;

import javafx.util.Pair;
import org.json.JSONArray;
import org.json.JSONObject;
import org.sim.cloudbus.cloudsim.Log;
import org.sim.service.Constants;
import org.sim.service.YamlWriter;
import org.sim.workflowsim.XmlUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.sim.service.service;

import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.HashMap;


@RestController
public class SimulateController {
    @Autowired
    private service service;

    @RequestMapping(value = "/startSimulate")
    public Message startSimulate(@RequestParam("hostPath")String hostPath, @RequestParam("appPath")String appPath, @RequestParam("faultPath")String faultPath, @RequestParam("arithmetic")Integer arithmetic) {
        try{
            Message m = new Message();
            Constants.results = new ArrayList<>();
            Constants.logs = new ArrayList<>();
            Constants.resultPods = new ArrayList<>();
            Constants.id2Name = new HashMap<>();
            Constants.name2Container = new HashMap<>();
            Constants.nodeEnough = true;
            service.simulate(hostPath, appPath, faultPath, arithmetic);
            Log.printLine("1");
            JSONArray array = new JSONArray();
            for(Result result: Constants.results) {
                JSONObject obj = new JSONObject().put("name", result.name).put("host", result.host).put("start", result.start).put("end", result.finish).put("size", result.size)
                        .put("mips", result.mips).put("pes", result.pes).put("type", result.type).put("datacenter", result.datacenter).put("ram", result.ram);
                array.put(obj);
            }
            try {
                OutputStreamWriter osw = new OutputStreamWriter(new FileOutputStream(Constants.midpath), "UTF-8");
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
    public Message pauseContainer(@RequestParam("id") Integer containerId, @RequestParam("start") double start, @RequestParam("last") double last) {
        Message m = new Message();
        m.code = CODE.SUCCESS;
        m.message = "container " + containerId + " will be paused";
        Constants.pause.put(containerId, new Pair<>(start, last));
        return m;
    }

    @RequestMapping("/deletePause")
    public Message deletePause(@RequestParam("id") Integer containerId) {
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

    @RequestMapping("/inputContainer")
    public Message inputContainer(@RequestParam("path") String path) {
        Message ret = new Message();
        XmlUtil xmlUtil = new XmlUtil(-1);
        try{
            xmlUtil.parseContainerInfo(path);
            ret.code = CODE.SUCCESS;
            ret.message = "input successfully";
        }catch (Exception e) {
            ret.code = CODE.FAILED;
            ret.message = e.getMessage();
        }
        return ret;
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
