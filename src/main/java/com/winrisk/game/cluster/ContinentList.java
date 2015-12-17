package com.winrisk.game.cluster;

import com.winrisk.game.object.Continent;

import java.util.ArrayList;

public class ContinentList extends ArrayList<Continent> {

    private static final long serialVersionUID = 7895873526888990052L;

    public ContinentList(ContinentList continents) {
        continents.forEach(this::add);
    }

    public ContinentList() {

    }

}
