/**
 * パッケージ名：shp2lanelet.converter
 * ファイル名  ：OsmContainers.java
 * 
 * @author mbasa
 * @since Sep 18, 2024
 */
package shp2lanelet.converter;

import java.util.ArrayList;
import java.util.Collection;

import org.openstreetmap.osmosis.core.container.v0_6.NodeContainer;
import org.openstreetmap.osmosis.core.container.v0_6.RelationContainer;
import org.openstreetmap.osmosis.core.container.v0_6.WayContainer;

/**
 * 説明：
 *
 */
public class OsmContainers {

    private Collection<NodeContainer> nodeContainer = new ArrayList<NodeContainer>();
    private Collection<WayContainer> wayContainer = new ArrayList<WayContainer>();
    private Collection<RelationContainer> relContainer = new ArrayList<RelationContainer>();
    /**
     * コンストラクタ
     *
     */
    public OsmContainers() {
    }

    public OsmContainers(Collection<NodeContainer> nodeContainer,
            Collection<WayContainer> wayContainer,
            Collection<RelationContainer> relContainer) {
        this.nodeContainer = nodeContainer;
        this.wayContainer = wayContainer;
        this.relContainer = relContainer;
    }
    /**
     * @return nodeContainer を取得する
     */
    public Collection<NodeContainer> getNodeContainer() {
        return nodeContainer;
    }

    /**
     * @param nodeContainer nodeContainer を設定する
     */
    public void setNodeContainer(Collection<NodeContainer> nodeContainer) {
        this.nodeContainer = nodeContainer;
    }

    /**
     * @return wayContainer を取得する
     */
    public Collection<WayContainer> getWayContainer() {
        return wayContainer;
    }

    /**
     * @param wayContainer wayContainer を設定する
     */
    public void setWayContainer(Collection<WayContainer> wayContainer) {
        this.wayContainer = wayContainer;
    }

    /**
     * @return relContainer を取得する
     */
    public Collection<RelationContainer> getRelContainer() {
        return relContainer;
    }

    /**
     * @param relContainer relContainer を設定する
     */
    public void setRelContainer(Collection<RelationContainer> relContainer) {
        this.relContainer = relContainer;
    }

}
