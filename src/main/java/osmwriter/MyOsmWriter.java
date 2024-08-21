/**
 * パッケージ名：osmwriter
 * ファイル名  ：MyOsmWriter.java
 * 
 * @author mbasa
 * @since Jun 22, 2023
 */
package osmwriter;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import org.openstreetmap.osmosis.core.container.v0_6.BoundContainer;
import org.openstreetmap.osmosis.core.container.v0_6.NodeContainer;
import org.openstreetmap.osmosis.core.container.v0_6.WayContainer;
import org.openstreetmap.osmosis.core.container.v0_6.RelationContainer;
import org.openstreetmap.osmosis.core.domain.v0_6.Bound;
import org.openstreetmap.osmosis.core.domain.v0_6.CommonEntityData;
import org.openstreetmap.osmosis.core.domain.v0_6.EntityType;
import org.openstreetmap.osmosis.core.domain.v0_6.Node;
import org.openstreetmap.osmosis.core.domain.v0_6.OsmUser;
import org.openstreetmap.osmosis.core.domain.v0_6.Relation;
import org.openstreetmap.osmosis.core.domain.v0_6.RelationMember;
import org.openstreetmap.osmosis.core.domain.v0_6.Tag;
import org.openstreetmap.osmosis.core.domain.v0_6.Way;
import org.openstreetmap.osmosis.core.domain.v0_6.WayNode;
import org.openstreetmap.osmosis.core.task.v0_6.Sink;
import org.openstreetmap.osmosis.core.task.v0_6.Source;
import org.openstreetmap.osmosis.xml.common.CompressionMethod;
import org.openstreetmap.osmosis.xml.v0_6.XmlWriter;
 
 
public class MyOsmWriter implements Source {
 
    private Sink sink;
 
    @Override
    public void setSink(Sink sink) {
        this.sink = sink;
    }
 
    public void write() {
        
        BoundContainer bound = new BoundContainer(new Bound(0d,0d,40d,40d,""));
        sink.process(bound);
       
        for (int idx = 1; idx <= 10; idx++) {
            sink.process(new NodeContainer(new Node(createEntity(idx), 0, 0)));
        }
        
        List<WayNode> nodes = new ArrayList<WayNode>();
        nodes.add(new WayNode(1l));
        nodes.add(new WayNode(2l));
        nodes.add(new WayNode(3l));
        sink.process(new WayContainer(new Way(createEntity(1),nodes)));
        
        List<RelationMember> members = new ArrayList<RelationMember>();
        members.add(new RelationMember(1, EntityType.Way, "outer"));
        members.add(new RelationMember(2, EntityType.Way, "left"));
        members.add(new RelationMember(3, EntityType.Way, "right"));
        
        sink.process(new RelationContainer(new Relation(createEntity(1),members)) );
    }
 
    public void complete() {
        sink.complete();
    }
 
    private CommonEntityData createEntity(int idx) {
        Tag tag1 = new Tag("Hello", "World"+idx);
        Tag tag2 = new Tag("こんにちは", "世界"+idx);
        
        Collection<Tag> tags = new ArrayList<Tag>();
        tags.add(tag1);
        tags.add(tag2);
        
        return new CommonEntityData(idx, 1, new Date(), 
                new OsmUser(idx, "User"), idx, tags);
    }
 
    public static void main(String[] args) throws FileNotFoundException {
        File file = new File("out.osm");
        MyOsmWriter writer = new MyOsmWriter();
        
        writer.setSink(new XmlWriter(file, CompressionMethod.None, false));
        //writer.setSink(new OsmosisSerializer(new BlockOutputStream(outputStream)));
       
        writer.write();
        writer.complete();
    }
}