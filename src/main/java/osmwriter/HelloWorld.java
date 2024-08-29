/**
 * パッケージ名：osmwriter
 * ファイル名  ：HelloWorld.java
 * 
 * @author mbasa
 * @since Jun 19, 2023
 */
package osmwriter;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.geotools.data.DataStore;
import org.geotools.data.DataStoreFinder;
import org.geotools.data.FeatureSource;
import org.geotools.data.store.ReprojectingFeatureCollection;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.geotools.referencing.CRS;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.MultiLineString;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeDescriptor;
import org.opengis.filter.Filter;
import org.openstreetmap.osmosis.core.container.v0_6.BoundContainer;
import org.openstreetmap.osmosis.core.container.v0_6.NodeContainer;
import org.openstreetmap.osmosis.core.container.v0_6.RelationContainer;
import org.openstreetmap.osmosis.core.container.v0_6.WayContainer;
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
import org.openstreetmap.osmosis.xml.common.CompressionMethod;
import org.openstreetmap.osmosis.xml.v0_6.XmlWriter;

/**
 * 説明：
 *
 */
public class HelloWorld {

    private final String USER_NAME = "User";
    private final int USER_ID      = 1;
    private final int VERSION_ID   = 1;
    
    private CommonEntityData createEntity(long idx,Collection<Tag> tags) {
        return new CommonEntityData(idx, VERSION_ID, new Date(), 
                new OsmUser(USER_ID, USER_NAME), VERSION_ID, tags);
    }
    
    /**
     * コンストラクタ
     *
     */
    public HelloWorld() {
    }

    /**
     * 
     * 
     * @param args
     */
    public static void main(String[] args) {        
        
        // File file = new File("shikoku-latest-free/gis_osm_roads_free_1.shp");
        File file = new File("lanelet-shp/Clip_OutFeatureClass_udbx3_lane.shp");

        Map<String,Object> map = new HashMap<String,Object>();
        
        File out = new File("test.osm");
        XmlWriter writer = new XmlWriter(out,CompressionMethod.None,false);
        
        HelloWorld hw = new HelloWorld();
        
        try {
            map.put("url", file.toURI().toURL());
            
            DataStore dataStore = DataStoreFinder.getDataStore(map);
            String typeName = dataStore.getTypeNames()[0];
            System.out.println("Processing: "+typeName);
            
            FeatureSource<SimpleFeatureType, SimpleFeature> source =
                    dataStore.getFeatureSource(typeName);
            
            Filter filter = Filter.INCLUDE; // ECQL.toFilter("BBOX(THE_GEOM, 10,20,30,40)")

            FeatureCollection<SimpleFeatureType, SimpleFeature> collection = 
                    source.getFeatures(filter);
            
            String s = source.getInfo().getCRS().toString();
            System.out.println(s);
            
            List<AttributeDescriptor> attrs = source.getSchema().getAttributeDescriptors();

            for(int x=0;x<attrs.size();x++) {
                System.out.println(attrs.get(x).getLocalName());
            }
            
            ReprojectingFeatureCollection rfc =
                    new ReprojectingFeatureCollection(collection, 
                            CRS.decode("epsg:3875"));

            BoundContainer bound = new BoundContainer(
                    new Bound(collection.getBounds().getMaxX(),
                            collection.getBounds().getMinX(),
                            collection.getBounds().getMaxY(),
                            collection.getBounds().getMinY(),
                            ""));

            System.out.println("New Bounds: " + bound.getEntity().toString());
            writer.process(bound);
            
            try (FeatureIterator<SimpleFeature> features = collection.features()) {
                long nodeId = 1;
                long wayId  = 1;
                long relId  = 1;
                
                Collection<WayContainer> ways = new ArrayList<WayContainer>();
                Collection<RelationContainer> rels = new ArrayList<RelationContainer>();
                
                int count = 0;
                
                while (features.hasNext()) {
                    SimpleFeature feature = features.next();
                    
                    Collection<Tag> nTags = new ArrayList<Tag>();
                    Collection<Tag> wTags = new ArrayList<Tag>();
                    Collection<Tag> rTags = new ArrayList<Tag>();
                    
                    List<RelationMember> members = new ArrayList<RelationMember>();
                    
                    rTags.add(new Tag("type", "lanelet"));

                    for (int x = 0; x < attrs.size(); x++) {
                        String fn = attrs.get(x).getLocalName();

                        if (fn.compareTo("the_geom") == 0)
                            continue;

                        wTags.add(new Tag(fn,
                                feature.getAttribute(fn).toString()));

                        if (fn.contains("left_lane"))
                            rTags.add(new Tag("left", feature.getAttribute(fn).toString()));
                        if (fn.contains("right_lane"))
                            rTags.add(new Tag("right", feature.getAttribute(fn).toString()));
                        if (fn.contains("lane_numbe"))
                            rTags.add(new Tag("lane_number", feature.getAttribute(fn).toString()));
                        if (fn.contains("lane_type"))
                            rTags.add(new Tag("lane_type", feature.getAttribute(fn).toString()));
                    }

                    // wTags.add(new Tag("code" ,feature.getAttribute("code").toString()));
                    // wTags.add(new Tag("fclass",feature.getAttribute("fclass").toString()));
                    // wTags.add(new Tag("name" ,(String) feature.getAttribute("name")));
                    // wTags.add(new Tag("layer" ,feature.getAttribute("layer").toString()));
                    
                    if (feature.getDefaultGeometry() instanceof MultiLineString) {

                        MultiLineString m = (MultiLineString)feature.getDefaultGeometry();
                        List<WayNode> nodes = new ArrayList<WayNode>();
                        
                        for(int i=0;i<m.getNumGeometries();i++) {
                            LineString line = (LineString )m.getGeometryN(i);
                            
                            for(Coordinate pt : line.getCoordinates() ) {
                                Node node = new Node(
                                        hw.createEntity(nodeId, nTags),
                                        pt.getY(),
                                        pt.getX());
                                nodes.add(new WayNode(nodeId));
                                
                                writer.process(new NodeContainer(node));
                                nodeId += 1;
                            }
                            
                            Way way = new Way(hw.createEntity(wayId, wTags),
                                    nodes);
                            ways.add(new WayContainer(way));
                            members.add(new RelationMember(
                                    wayId, EntityType.Way, ""));
                            
                            wayId += 1;                            
                        }                                                
                    }
                      
                    if( members.size() > 0 ) {
                        rels.add(new RelationContainer(
                                new Relation(hw.createEntity(relId, rTags), members))
                        );
                        relId += 1;
                    }
                    
                    count++;
                    // if (count >= 40) {
                    // break;
                    // }

                }

                if( ways.size() > 0 ) {
                    for( WayContainer w : ways ) {
                        writer.process(w);
                    }
                } 
                if( rels.size() > 0 ) {
                    for(RelationContainer r : rels ) {
                        writer.process(r);
                    }
                }
            }                                    
                                   
            Tag tag1 = new Tag("Hello", "World");
            Tag tag2 = new Tag("こんにちは", "世界");
            
            Collection<Tag> tags = new ArrayList<Tag>();
            tags.add(tag1);
            tags.add(tag2);
            
            CommonEntityData ce = new CommonEntityData(1, 2, 
                    new Date(), OsmUser.NONE, 3,tags);
            
            Node n = new Node(ce, 35d, 135d);
            NodeContainer node = new NodeContainer(n);
                   
            List<WayNode> nodes = new ArrayList<WayNode>();
            nodes.add(new WayNode(1l));
            nodes.add(new WayNode(2l));
            nodes.add(new WayNode(3l));
            
            WayContainer ways = new WayContainer(new Way(ce,nodes));

            //writer.process(bound);
            //writer.process(node);   
            //writer.process(ways);
            
            writer.complete();
            writer.close();
        }
        catch(Exception e) {
            e.printStackTrace();
        }

    }

}
