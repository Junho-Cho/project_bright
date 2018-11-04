package com.example.lgpc.project_bright;

/**
 * Created by LGPC on 2018-10-22.
 */

public class NodeInfo {
    public int x;
    public int y;
    public int floor;
    public String name;
    public boolean state;

    private NodeInfo() {

    }

    public NodeInfo(int x, int y, int floor, String name, boolean state) {
        this.x = x;
        this.y = y;
        this.floor = floor;
        this.name = name;
        this.state = state;
    }
}
