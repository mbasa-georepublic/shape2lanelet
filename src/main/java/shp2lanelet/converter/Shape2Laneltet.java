/**
 * パッケージ名：shp2lanelet.converter
 * ファイル名  ：Shape2Laneltet.java
 * 
 * @author mbasa
 * @since Sep 17, 2024
 */
package shp2lanelet.converter;

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
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.data.store.ReprojectingFeatureCollection;
import org.geotools.feature.FeatureCollection;
import org.geotools.referencing.CRS;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.MultiLineString;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Point;
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
public class Shape2Laneltet {

    private String USER_NAME = "User";
    private int USER_ID = 1;
    private int VERSION_ID = 1;

    private String shapeFile = "";
    private boolean processAsLanelet = false;

    private CommonEntityData createEntity(long idx, Collection<Tag> tags) {
        return new CommonEntityData(idx, VERSION_ID, new Date(),
                new OsmUser(USER_ID, USER_NAME), VERSION_ID, tags);
    }

    /**
     * コンストラクタ
     *
     */
    public Shape2Laneltet(String shapeFile,
            boolean processAsLanelet,
            String userName,
            String userId,
            String versionId) {
        this.shapeFile = shapeFile;
        this.processAsLanelet = processAsLanelet;

        if (userName != null && !userName.isBlank()) {
            this.USER_NAME = userName;
        }
        if (userId != null && !userId.isBlank()) {
            this.USER_ID = Integer.parseInt(userId);
        }
        if (versionId != null && !versionId.isBlank()) {
            this.VERSION_ID = Integer.parseInt(versionId);
        }
    }

    public Shape2Laneltet(String shapeFile) {
        this.shapeFile = shapeFile;
    }

    public void process() {
        File file = new File(this.shapeFile);

        if (!file.exists()) {
            System.out.println("File not found. Exiting");
            System.exit(1);
        }

        System.out.println("Processing " + this.shapeFile);
        String outFile = file.getName().split("\\.(?=[^\\.]+$)")[0];

        File out = new File(outFile + ".osm");
        XmlWriter writer = new XmlWriter(out, CompressionMethod.None, false);

        try {
            Map<String, Object> map = new HashMap<String, Object>();

            map.put("url", file.toURI().toURL());

            DataStore dataStore = DataStoreFinder.getDataStore(map);
            String typeName = dataStore.getTypeNames()[0];

            FeatureSource<SimpleFeatureType, SimpleFeature> source = dataStore.getFeatureSource(typeName);

            String featureType = source.getSchema()
                    .getGeometryDescriptor().getType().getName().toString();

            System.out.println("FeatureType:" + featureType);

            /**
             * Getting all features of the ShapeFile
             */
            Filter filter = Filter.INCLUDE; // ECQL.toFilter("BBOX(THE_GEOM, 10,20,30,40)")
            FeatureCollection<SimpleFeatureType, SimpleFeature> collection = source.getFeatures(filter);

            /**
             * Getting the Attributes of the ShapeFile
             */
            List<AttributeDescriptor> attrs = source.getSchema().getAttributeDescriptors();

            /**
             * Re-projecting into EPSG:4326 for OSM standard
             */
            ReprojectingFeatureCollection rfc = new ReprojectingFeatureCollection(collection,
                    CRS.decode("epsg:4612"));

            BoundContainer bound = new BoundContainer(
                    new Bound(rfc.getBounds().getMaxY(),
                            rfc.getBounds().getMinY(),
                            rfc.getBounds().getMaxX(),
                            rfc.getBounds().getMinX(),
                            ""));
            writer.process(bound);

            OsmContainers osm = new OsmContainers();

            switch (featureType) {
            case "LineString":
            case "MultiLineString":
                osm = processLineString(rfc, attrs);
                break;
            case "Polygon":
            case "MultiPolygon":
                osm = processPolygon(rfc, attrs);
                break;
            case "Point":
                osm = processPoint(rfc, attrs);
            }

            if (!osm.getNodeContainer().isEmpty()) {
                for (NodeContainer n : osm.getNodeContainer()) {
                    writer.process(n);
                }
            }
            if (!osm.getWayContainer().isEmpty()) {
                for (WayContainer n : osm.getWayContainer()) {
                    writer.process(n);
                }
            }
            if (!osm.getRelContainer().isEmpty()) {
                for (RelationContainer n : osm.getRelContainer()) {
                    writer.process(n);
                }
            }
            writer.complete();
            writer.close();


        } catch (Exception e) {
            System.out.println(e.toString());
        }

    }

    /**
     * 
     * Process (Multi)LineStrings
     * 
     * @param rfc
     * @param attrs
     * @return OsmContainers
     */
    private OsmContainers processLineString(
            ReprojectingFeatureCollection rfc,
            List<AttributeDescriptor> attrs) {

        long nodeId = 1;
        long wayId = 1;
        long relId = 1;

        Collection<NodeContainer> nodeContainer = new ArrayList<NodeContainer>();
        Collection<WayContainer> wayContainer = new ArrayList<WayContainer>();
        Collection<RelationContainer> relContainer = new ArrayList<RelationContainer>();

        try (SimpleFeatureIterator features = rfc.features()) {

            while (features.hasNext()) {
                SimpleFeature feature = features.next();

                Collection<Tag> nTags = new ArrayList<Tag>();
                Collection<Tag> wTags = new ArrayList<Tag>();
                Collection<Tag> rTags = new ArrayList<Tag>();


                for (int i = 0; i < attrs.size(); i++) {
                    String fn = attrs.get(i).getLocalName();

                    if (fn.compareTo("the_geom") == 0)
                        continue;

                    wTags.add(new Tag(fn,
                            feature.getAttribute(fn).toString()));
                }

                if (feature.getDefaultGeometry() instanceof MultiLineString) {

                    MultiLineString m = (MultiLineString) feature.getDefaultGeometry();
                    List<WayNode> wayNodes = new ArrayList<WayNode>();

                    for (int g = 0; g < m.getNumGeometries(); g++) {
                        LineString line = (LineString) m.getGeometryN(g);
                        double sumHeight = 0d;

                        for (Coordinate pt : line.getCoordinates()) {
                            nTags = new ArrayList<Tag>();

                            if (!Double.isNaN(pt.getZ())) {
                                nTags.add(new Tag("height",
                                        Double.toString(pt.getZ())));
                                sumHeight += pt.getZ();
                            }
                            if (!Double.isNaN(pt.getM())) {
                                nTags.add(new Tag("measure",
                                        Double.toString(pt.getM())));
                            }
                            Node node = new Node(
                                    this.createEntity(nodeId, nTags),
                                    pt.getX(),
                                    pt.getY());

                            nodeContainer.add(new NodeContainer(node));
                            wayNodes.add(new WayNode(nodeId));
                            nodeId++;
                        }
                        if (sumHeight > 0) {
                            double h = sumHeight / line.getCoordinates().length;
                            wTags.add(new Tag("height", Double.toString(h)));
                        }
                        Way way = new Way(this.createEntity(wayId, wTags),
                                wayNodes);
                        wayContainer.add(new WayContainer(way));
                        wayId += 1;
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("LineString Process Exception:" + e.toString());
        }

        return new OsmContainers(nodeContainer,
                wayContainer, relContainer);

    }

    private OsmContainers processPoint(
            ReprojectingFeatureCollection rfc,
            List<AttributeDescriptor> attrs) {

        long nodeId = 1;
        Collection<NodeContainer> nodeContainer = new ArrayList<NodeContainer>();

        try (SimpleFeatureIterator features = rfc.features()) {

            while (features.hasNext()) {
                SimpleFeature feature = features.next();
                Collection<Tag> nTags = new ArrayList<Tag>();

                for (int i = 0; i < attrs.size(); i++) {
                    String fn = attrs.get(i).getLocalName();

                    if (fn.compareTo("the_geom") == 0)
                        continue;

                    nTags.add(new Tag(fn,
                            feature.getAttribute(fn).toString()));
                }

                if (feature.getDefaultGeometry() instanceof Point) {
                    Point point = (Point) feature.getDefaultGeometry();
                    Coordinate pt = point.getCoordinate();

                    if (!Double.isNaN(pt.getZ())) {
                        nTags.add(new Tag("height",
                                Double.toString(pt.getZ())));
                    }
                    if (!Double.isNaN(pt.getM())) {
                        nTags.add(new Tag("measure",
                                Double.toString(pt.getM())));
                    }
                    Node node = new Node(
                            this.createEntity(nodeId, nTags),
                            pt.getX(),
                            pt.getY());

                    nodeContainer.add(new NodeContainer(node));
                    nodeId += 1;
                }
            }
        } catch (Exception e) {
            System.out.println("Point Process Exception:" + e.toString());
        }

        OsmContainers osm = new OsmContainers();
        osm.setNodeContainer(nodeContainer);

        return osm;
    }

    private OsmContainers processPolygon(
            ReprojectingFeatureCollection rfc,
            List<AttributeDescriptor> attrs) {

        long nodeId = 1;
        long wayId = 1;
        long relId = 1;

        Collection<NodeContainer> nodeContainer = new ArrayList<NodeContainer>();
        Collection<WayContainer> wayContainer = new ArrayList<WayContainer>();
        Collection<RelationContainer> relContainer = new ArrayList<RelationContainer>();

        try (SimpleFeatureIterator features = rfc.features()) {

            while (features.hasNext()) {
                SimpleFeature feature = features.next();

                Collection<Tag> nTags = new ArrayList<Tag>();
                Collection<Tag> wTags = new ArrayList<Tag>();
                Collection<Tag> rTags = new ArrayList<Tag>();

                List<RelationMember> members = new ArrayList<RelationMember>();

                for (int i = 0; i < attrs.size(); i++) {
                    String fn = attrs.get(i).getLocalName();

                    if (fn.compareTo("the_geom") == 0)
                        continue;

                    wTags.add(new Tag(fn,
                            feature.getAttribute(fn).toString()));
                }
                wTags.add(new Tag("area", "yes"));

                if (feature.getDefaultGeometry() instanceof MultiPolygon) {
                    MultiPolygon mp = (MultiPolygon) feature.getDefaultGeometry();
                    List<WayNode> wayNodes = new ArrayList<WayNode>();

                    for (int i = 0; i < mp.getNumGeometries(); i++) {
                        Polygon poly = (Polygon) mp.getGeometryN(i);

                        for (Coordinate pt : poly.getCoordinates()) {
                            nTags = new ArrayList<Tag>();

                            if (!Double.isNaN(pt.getZ())) {
                                nTags.add(new Tag("height",
                                        Double.toString(pt.getZ())));
                            }
                            if (!Double.isNaN(pt.getM())) {
                                nTags.add(new Tag("measure",
                                        Double.toString(pt.getM())));
                            }
                            Node node = new Node(
                                    this.createEntity(nodeId, nTags),
                                    pt.getX(),
                                    pt.getY());

                            nodeContainer.add(new NodeContainer(node));
                            wayNodes.add(new WayNode(nodeId));
                            nodeId++;
                        }
                        Way way = new Way(this.createEntity(wayId, wTags),
                                wayNodes);
                        wayContainer.add(new WayContainer(way));

                        members.add(
                                new RelationMember(wayId, EntityType.Way, "outer"));

                        wayId += 1;
                    }
                    rTags.add(new Tag("type", "multipolygon"));
                    relContainer.add(new RelationContainer(
                            new Relation(this.createEntity(relId, rTags), members)));
                    relId += 1;
                }
            }
        } catch (Exception e) {
            System.out.println("Polygon Process Exception:" + e.toString());
        }

        return new OsmContainers(nodeContainer,
                wayContainer, relContainer);
    }
}
