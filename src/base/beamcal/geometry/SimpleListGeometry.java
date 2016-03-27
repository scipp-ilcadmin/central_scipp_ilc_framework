/**
 * SimpleListGeometry.java.
 *
 * Created on March 26, 2016
 *
 * @author Christopher Milke
 * @version 1.0
 * 
 * This class handles all of the coordinate changing, tile parameters,
 * and tile labeling needed for the radial tiling scheme. It allows users
 * to set the radius of individual rings of the detector, and to control
 * the number of sectors on a per ring basis.
 *
 *
 */
 
package scipp_ilc.base.beamcal.geometry;

import scipp_ilc.base.util.PolarCoords;

import java.lang.String;
import java.lang.Short;
import java.lang.Math;
import java.lang.Integer;

import java.util.ArrayList;
import java.util.HashMap;

import java.io.File;
import javax.xml.parsers.DocumentBuilder; 
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;


public class SimpleListGeometry{
    
    //CONSTRUCTOR
    public SimpleListGeometry(String geom_file) {
        System.out.println("Initializing geometry");
        readGeomFile(geom_file);
        makeGraph();
        System.out.println("Geometry initialized");
    }
    
    
    private void readGeomFile(String geom_file) {    
        try {
            Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse( new File(geom_file) );
            doc.getDocumentElement().normalize();
            
            String ring_edges = doc.getElementsByTagName("ring_edges").item(0).getTextContent();
            String sector_count = doc.getElementsByTagName("sector_count").item(0).getTextContent();
            String offset = doc.getElementsByTagName("offset").item(0).getTextContent();

            String[] ring_edge_list = ring_edges.trim().split(",");
            String[] sector_count_list = sector_count.trim().split(",");

            sectorOffset = Float.parseFloat(offset);
            LastRing = ring_edge_list.length - 1;

            if ( ring_edge_list.length != sector_count_list.length ) {
                throw new Exception("sector parameter does not match ring parameter number!");
            }
            
            RingToRadTable = new float[LastRing+1];
            for (int i = 0; i <= LastRing; i++) {
                RingToRadTable[i] = Float.parseFloat( ring_edge_list[i].trim() );
            }
            
            SectorCountTable = new short[LastRing+1];
            for (int i = 0; i <= LastRing; i++) {
                SectorCountTable[i] = Short.parseShort( sector_count_list[i].trim() );
            }

            float lastOffset = sectorOffset * RingToRadTable[LastRing];
            if (lastOffset > 2*Math.PI) {
                String e = "Sector Offset is too large!\n";
                e += "Offset will cause pixels to rotate over 360 degrees by last ring.\n";
                e += "This program is not designed to handle this.\n";
                e += "Please make your offset value smaller.\n";
                throw new Exception(e);
            }
        }catch (Exception e) {
            e.printStackTrace();
        }        
    }

    /* 
     * Identifying pixels that surround other pixels with a radial pixel scheme is hard.
     * So, in order to avoid painfully repeating the process for every pixel that is
     * looked at, I am creating a hashmap here which caches that information.
     * The hashmap's keys correspond to the ID of the pixel you want to know the 
     * sorrounding pixels of. The values are the IDs of the surrounding pixels...
     * sort of. The values are actually arraylists of only two elements.
     * These two elements are themselves arraylists, and consist of the IDs of 
     * surrounding pixels. The reason the IDs are broken up into two seperate
     * lists is that it allows you to rapidly identify pixels on a ring outside
     * the current one, or inside it. 
     *
     * NOTE: The inner list is the 0th element, the outer list is the 1st
     *
     * p.s. I'm calling this a graph because 'graph' is a computer science term.
     *      look it up.
     */
    private void makeGraph() {
        Graph = new HashMap<  Integer, ArrayList< ArrayList<Integer> >  >();
        for( int ring = 0; ring <= LastRing; ring++) {
            for ( int sector = 0; sector < SectorCountTable[ring]; sector++ ) {
                int ID = ring*IDlimit + sector;
                /*Identify boundries*/

                //create ring variables
                int outer_ring = ring + 1;
                int inner_ring = ring - 1;

                //create sector variables
                int clockwiseMost_sector = sector-1;
                if (clockwiseMost_sector > SectorCountTable[ring]) clockwiseMost_sector -= SectorCountTable[ring];
                int anticlockwiseMost_sector = sector+1;
                if (anticlockwiseMost_sector < 0) anticlockwiseMost_sector += SectorCountTable[ring];

                //get phi boundries
                float clockwise_boundry = getPhi(ring, clockwiseMost_sector);
                float anticlockwise_boundry = getPhi(ring, sector);


                /*create ID lists*/

                //create outer ID list.
                //Note that the clockwise-most sector on the same ring is included at index 0.
                ArrayList outerlist = new ArrayList<Integer>();
                int clockwiseMost_ID = ring*IDlimit + clockwiseMost_sector;
                outerlist.add(clockwiseMost_ID);


                //create inner ID list.
                //Note that the anticlockwise-most sector on the same ring is included
                ArrayList innerlist = new ArrayList<Integer>();
                int anticlockwiseMost_ID = ring*IDlimit + anticlockwiseMost_sector;
                innerlist.add(clockwiseMost_ID);


                if (inner_ring > 0) {
                    //identify first and last inner sectors
                    int inner_anticlockwiseMost_sector = getSector(inner_ring, anticlockwise_boundry);
                    int inner_clockwiseMost_sector = getSector(inner_ring, clockwise_boundry);

                    //add inner sectors to innerlist
                    for (int inner_sector  = inner_anticlockwiseMost_sector;
                             inner_sector <= inner_clockwiseMost_sector;
                             inner_sector++) {

                        int inner_ID = (inner_ring)*IDlimit + inner_sector;
                        innerlist.add(inner_ID);
                    }
                }

                if (outer_ring < LastRing) {
                    //identify first and last outer sectors
                    int outer_anticlockwiseMost_sector = getSector(outer_ring, anticlockwise_boundry);
                    int outer_clockwiseMost_sector = getSector(outer_ring, clockwise_boundry);

                    //add outer sectors to outerlist
                    for (int outer_sector  = outer_clockwiseMost_sector;
                             outer_sector <= outer_anticlockwiseMost_sector;
                             outer_sector++) {

                        int outer_ID = (outer_ring)*IDlimit + outer_sector;
                        outerlist.add(outer_ID);
                    }
                }


                //create surrounding pixel list, which just combines the previous two
                ArrayList surrounding_pixels = new ArrayList< ArrayList<Integer> >();
                surrounding_pixels.add(innerlist);
                surrounding_pixels.add(outerlist);

                //add surrounding pixel list to the hashmap
                Graph.put(ID,surrounding_pixels);
            }
        }
    }


    //use binary search to find appropriate ring for a given radius
    private int getRing(float radius) {
        if (radius <= RingToRadTable[0]) return 0;

        int start = 0;
        int end = LastRing;
        while (end - start > 1) {
            int center = (int) (end-start)/2 + start;
            if ( radius < RingToRadTable[center] ) end = center;
            else start = center;
        }
        return end;
    }


    private int getSector(int ring, float phi) {
        int sectorCount = SectorCountTable[ring];
        float offset_phi = phi - sectorOffset*ring;
        if (offset_phi < 0)       offset_phi += 2*Math.PI;
        if (Math.PI < offset_phi) offset_phi -= 2*Math.PI;

        int sector = (int) (offset_phi/Math.PI) * sectorCount;
        return sector;
    }

    private float getPhi(int ring, int sector) {
        int sectorCount = SectorCountTable[ring];
        if( sector < 0 )           sector += sectorCount;
        if( sectorCount < sector ) sector -= sectorCount;

        double offset_phi = (sector / sectorCount) * Math.PI;
        float phi = (float) (offset_phi + sectorOffset*ring);
        return phi;
    }

    //IDs are of the form RRRRSSSS,
    //where R is a ring digit and S is a sector digit.
    //This implicitly assumes you will never have more than
    //IDlimit-1 Sectors or Rings for any geometric configuration.
    private int getIDpolar(float radius, float phi) {
        int ring = getRing(radius);
        int sector = getSector(ring,phi);

        int ID = IDlimit*ring + sector;
        return ID;
    }


    public int getID(float x, float y) {
        double[] polars = PolarCoords.CtoP(x,y);
        float r = (float) polars[0];
        float phi = (float) polars[1];
        int ID = getIDpolar(r,phi);
        return ID;
    }
    

    private int LastRing;
    private float sectorOffset;
    private float sectorMultiplier;
    private float[] RingToRadTable;
    private short[] SectorCountTable;

    public HashMap Graph;
    public static int IDlimit = 10000;
}
