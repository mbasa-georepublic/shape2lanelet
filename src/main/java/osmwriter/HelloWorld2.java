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
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Polygon;
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
public class HelloWorld2 {

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
    public HelloWorld2() {
    }

    /**
     * 
     * 
     * @param args
     */
    public static void main(String[] args) {        
        
        File file = new File("lanelet-shp/Clip_OutFeatureClass_udbx3_lane.shp");

        if (!file.exists()) {
            System.out.println("File not found. Exiting");
            System.exit(1);
        }

        Map<String,Object> map = new HashMap<String,Object>();
        
        String outFile = file.getName().split("\\.(?=[^\\.]+$)")[0];
        File out = new File(outFile + ".osm");

        XmlWriter writer = new XmlWriter(out,CompressionMethod.None,false);
        
        HelloWorld2 hw = new HelloWorld2();
        
        try {
            map.put("url", file.toURI().toURL());
            
            DataStore dataStore = DataStoreFinder.getDataStore(map);
            String typeName = dataStore.getTypeNames()[0];
            System.out.println("Processing: "+typeName);
            
            FeatureSource<SimpleFeatureType, SimpleFeature> source =
                    dataStore.getFeatureSource(typeName);
            
            String featureType = source.getSchema()
                    .getGeometryDescriptor().getType().getName().toString();
            
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
                            CRS.decode(/* "epsg:3785" */"epsg:4612"));

            System.out.println("Old Bounds: " + collection.getBounds().toString());
            System.out.println("New Bounds: " + rfc.getBounds().toString());

            BoundContainer bound = new BoundContainer(
                    new Bound(rfc.getBounds().getMaxY(),
                            rfc.getBounds().getMinY(),
                            rfc.getBounds().getMaxX(),
                            rfc.getBounds().getMinX(),
                            ""));

            writer.process(bound);

            try (FeatureIterator<SimpleFeature> features = rfc.features()) {
                long nodeId = 1;
                long wayId  = 1;
                long relId  = 1;
                
                Collection<WayContainer> ways = new ArrayList<WayContainer>();
                Collection<RelationContainer> rels = new ArrayList<RelationContainer>();
                
                while (features.hasNext()) {
                    SimpleFeature feature = features.next();
                    
                    Collection<Tag> nTags = new ArrayList<Tag>();
                    Collection<Tag> wTags = new ArrayList<Tag>();
                    Collection<Tag> rTags = new ArrayList<Tag>();
                    
                    List<RelationMember> members = new ArrayList<RelationMember>();
                    
                    rTags.add(new Tag("type", "lanelet"));
                    rTags.add(new Tag("subtype", "road"));
                    rTags.add(new Tag("location", "urban"));
                    rTags.add(new Tag("one_way", "yes"));
                    rTags.add(new Tag("region", "jp"));

                    for (int x = 0; x < attrs.size(); x++) {
                        String fn = attrs.get(x).getLocalName();

                        if (fn.compareTo("the_geom") == 0)
                            continue;

                        if (fn.compareTo("lane_type") == 0) {
                            String type = feature.getAttribute(fn).toString();
                            type = type.replace("[", "");
                            type = type.replace("]", "");
                            type = type.replace("'", "");

                            wTags.add(new Tag("type", "road_border"));
                            wTags.add(new Tag("roadtype", type));

                        }
                        // wTags.add(new Tag(fn,
                        // feature.getAttribute(fn).toString()));
                        /*
                         * if (fn.contains("left_lane"))
                         * // rTags.add(new Tag("left", feature.getAttribute(fn).toString()));
                         * members.add(new RelationMember(
                         * (long) feature.getAttribute(fn), EntityType.Way, "left"));
                         * if (fn.contains("right_lane"))
                         * // rTags.add(new Tag("right", feature.getAttribute(fn).toString()));
                         * members.add(new RelationMember(
                         * (long) feature.getAttribute(fn), EntityType.Way, "right"));
                         */
                    }

                    // wTags.add(new Tag("code" ,feature.getAttribute("code").toString()));
                    // wTags.add(new Tag("fclass",feature.getAttribute("fclass").toString()));
                    // wTags.add(new Tag("name" ,(String) feature.getAttribute("name")));
                    // wTags.add(new Tag("layer" ,feature.getAttribute("layer").toString()));
                    
                    if (feature.getDefaultGeometry() instanceof MultiPolygon) {
                        MultiPolygon mp = (MultiPolygon) feature.getDefaultGeometry();

                        for (int i = 0; i < mp.getNumGeometries(); i++) {
                            Polygon polygon = (Polygon) mp.getGeometryN(i);

                            for (Coordinate pt : polygon.getCoordinates()) {
                                nTags = new ArrayList<Tag>();
                                
                                if( ! Double.isNaN(pt.getZ())) {
                                    nTags.add(new Tag("height", 
                                            Double.toString(pt.getZ())));
                                }
                                
                                Node node = new Node(
                                        hw.createEntity(nodeId, nTags),
                                        pt.getX(),
                                        pt.getY());
                                writer.process(new NodeContainer(node));
                                nodeId += 1;
                            }

                        }
                    } else if (feature.getDefaultGeometry() instanceof MultiLineString) {

                        MultiLineString m = (MultiLineString)feature.getDefaultGeometry();
                        List<WayNode> nodes = new ArrayList<WayNode>();
                        
                        for(int i=0;i<m.getNumGeometries();i++) {
                            LineString line = (LineString )m.getGeometryN(i);
                            double sumHeight = 0d;
                            
                            for(Coordinate pt : line.getCoordinates() ) {
                                nTags = new ArrayList<Tag>();

                                if (!Double.isNaN(pt.getZ())) {
                                    double z = pt.getZ();
                                    nTags.add(new Tag("height",
                                            Double.toString(z)));

                                    sumHeight += z;
                                }

                                if (!Double.isNaN(pt.getM())) {
                                    nTags.add(new Tag("measure",
                                            Double.toString(pt.getM())));
                                }

                                Node node = new Node(
                                        hw.createEntity(nodeId, nTags),
                                        pt.getX(),
                                        pt.getY());
                                nodes.add(new WayNode(nodeId));
                                
                                writer.process(new NodeContainer(node));
                                nodeId += 1;
                            }

                            if (sumHeight > 0) {
                                double h = sumHeight / line.getCoordinates().length;
                                wTags.add(new Tag("height", Double.toString(h)));
                            }
                            Way way = new Way(hw.createEntity(wayId, wTags),
                                    nodes);
                            ways.add(new WayContainer(way));

                            members.add(new RelationMember(
                                    wayId, EntityType.Way, "left"));
                            members.add(new RelationMember(
                                    wayId, EntityType.Way, "right"));
                            wayId += 1;                            
                        }                                                
                    }
                      
                    if( members.size() > 0 ) {
                        rels.add(new RelationContainer(
                                new Relation(hw.createEntity(relId, rTags), members))
                        );
                        relId += 1;
                    }
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
            
            writer.complete();
            writer.close();
        }
        catch(Exception e) {
            e.printStackTrace();
        }

    }

}
