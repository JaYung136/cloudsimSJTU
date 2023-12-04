package org.sim.service;

import javafx.util.Pair;

import java.util.List;

public class Container {
    public String image;
    public String workingDir;
    public List<String> commands;
    public List<String> args;
    public List<Pair<String, String>> labels;
}
