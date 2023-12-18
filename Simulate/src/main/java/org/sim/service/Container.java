package org.sim.service;

import javafx.util.Pair;

import java.util.ArrayList;
import java.util.List;

public class Container {
    public String image = "";
    public String workingDir = "";
    public List<String> commands = new ArrayList<>();
    public List<String> args = new ArrayList<>();
    public List<Pair<String, String>> labels = new ArrayList<>();
}
