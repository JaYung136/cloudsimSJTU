package com.wfc.cloudsim.controller;


import com.wfc.cloudsim.wfc.core.Result;
import com.wfc.cloudsim.wfc.core.WFCConstants;
import com.wfc.cloudsim.wfc.service.WFCService;
import javafx.util.Pair;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

@RestController
public class CloudsimController {
    @Autowired
    private WFCService wfcService;

    private Integer SelectionStart = 5;

    private List<Result> results = new ArrayList<>();

    /**
     *
     * @param hostPath
     * @param appPath
     * @param arithmetic
     *
     * 1: LeastFull
     * 2: FirstFit
     * 3: HEFT
     * 4: RoundRobin
     * 5. K8s
     */
    //localhost:8080/startSimulate?hostPath=./config/dax/Host_10.xml&appPath=./config/pod/montage25.xml&arithmetic=3
    @RequestMapping(value = "/startSimulate")
    List<Result> startSimulate(@RequestParam("hostPath")String hostPath, @RequestParam("appPath")String appPath, @RequestParam("arithmetic")Integer arithmetic) {
        if(arithmetic < SelectionStart) results = wfcService.start(hostPath, appPath, "",arithmetic, false);
        else results = wfcService.start(hostPath, appPath, "", arithmetic, true);
        return results;
    }

    @RequestMapping(value = "/pauseContainer")
    void pauseContainer(@RequestParam("containerId")Integer containerId, @RequestParam("pauseStart")Double start, @RequestParam("pauseTime")Double time) {
        WFCConstants.pause.put(containerId, new Pair<Double, Double>(start, time));
    }

    @RequestMapping(value = "/clearPause")
    void clearPause(){
        WFCConstants.pause = new HashMap<>();
    }

    //localhost:8080/writeJsonFile?path=./config/results/result.json
    @RequestMapping(value = "/writeJsonFile")
    public String writeJsonFile(@RequestParam("path")String path) throws Exception {
        JSONArray array = new JSONArray();
        for(Result result: results) {
            JSONObject obj = new JSONObject().put("name", result.name).put("host", result.host).put("start", result.start).put("end", result.finish).put("size", result.size)
                    .put("mips", result.mips).put("pes", result.pes).put("type", result.type).put("datacenter", result.datacenter).put("ram", result.ram);
            array.put(obj);
        }
        OutputStreamWriter osw = new OutputStreamWriter(new FileOutputStream(path),"UTF-8");
        osw.write(array.toString(4));
        osw.flush();//清空缓冲区，强制输出数据
        osw.close();//关闭输出流
        return array.toString();
    }

    @RequestMapping("/readJsonFile")
    public String readJsonFile(String req) throws Exception {
        String content = new String(Files.readAllBytes(Paths.get("./test.json")));
        JSONArray array = new JSONArray(content);
        @SuppressWarnings("unchecked")
        Iterator<Object> iter = array.iterator();
        while(iter.hasNext()){
            System.out.println(iter.next());
        }
        return array.toString();
    }
}
