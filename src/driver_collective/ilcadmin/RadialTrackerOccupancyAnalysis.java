/*
 * RadialTrackerOccupancyAnalysis.java
 *
 * Created on October 12, 2015 12:51 AM
 * @author Christopher Milke & Luc d'Hauthuille
 *
 */
package scipp_ilc.drivers;

import org.lcsim.geometry.compact.Detector;
import org.lcsim.geometry.Subdetector;
import org.lcsim.detector.IGeometryInfo;
import hep.physics.vec.BasicHep3Vector;

import scipp_ilc.base.util.LCIOFileManager;
import scipp_ilc.base.util.PolarCoords;
import scipp_ilc.base.util.Jroot;

import org.lcsim.event.EventHeader;
import org.lcsim.event.MCParticle;
import org.lcsim.event.SimCalorimeterHit;
import org.lcsim.event.SimTrackerHit;

import org.lcsim.util.Driver;
import org.lcsim.util.Driver.NextEventException;

import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.ListIterator;
import java.util.Collections;

import java.lang.String;
import java.lang.Long;

public class RadialTrackerOccupancyAnalysis extends Driver {


    //DEFINE XML FUNCTIONS
    //These functions are specially formatted functions to pull variable data from the xml file
    /*****************************************************************************************
      XML FUNCTION FORMAT

    public void //setVariablename(variable type)  { //the first letter after "set" must be uppercase
    //but can (must?) be lowercase in xml file
    set variable here; 
    }
     *******************************************************************************************/


    public void setBunchCrossings(int c) {
        this.bunchCrossings = c;
    } 

    public void setPixelSize(double s) {
        this.pixelSize = s;//pixel size is in microns
    } 

    public void setOutputfile(String s) {
        this.jrootFile = s;
    } 

    //END DEFINE XML FUNCTIONS




    //This function is called when the program is first started
    //and initializes all persistent data
    public void startOfData() {
        eventNumber = 0;
        String root_mode = "NEW";
        tileSize = pixelSize/1000.0;
        //Initializes BrlTiles, a data structure of ArrayLists(of layers),
        //which in turn contain ArraysLists of Hashmap(for each layer, there
        //is Rbins(number of radial bins) Hashmaps that each contain hit tiles.
        for(int i=0;i<brlLyrN;i++) {
            BrlTiles.add( new ArrayList< HashMap<String,Long>>() );
            tileNumBrl[i] = (int) ( brlLayerArea[i]/(tileSize*tileSize) );
            for(int x=0;x<Rbins;x++) {
                BrlTiles.get(i).add( new HashMap<String,Long>() );
            }
        }
        for(int i=0;i<ecpLyrN;i++) {
            EcpTiles.add( new ArrayList< HashMap<String,Long>>() );
            //int totaltiles = (int) (ecpLayerArea[i]/(tileSize*tileSize));
            for(int r=0;r<Rbins;r++) {
                EcpTiles.get(i).add( new HashMap<String,Long>() );
                tileNumEcp[i][r] = 2 * (int) (ecpLayerArea[i][r]/(tileSize*tileSize)); //multiply by two because of -z detector
            }
        }
        try {
            root = new Jroot(jrootFile,root_mode);
            for(int i = 0; i<brlLyrN;i++){
                root.init("TProfile","BrlFractionVz"+i,"BrlFractionVz"+i,"BrlHitsByZ"+i,10,0,brlLayerLength,0,1.0);
            }
            for(int i = 0; i<ecpLyrN;i++){
                root.init("TProfile","EcapFractionVRadius"+i,"EcapFractionVRadius"+i,"EcapHits"+i,10,0,EcapRout,0,1.0);
            }
        } catch(java.io.IOException e) {
            System.out.println(e);
            System.exit(1);
        }
    }


    //This function is called after all file runs have finished,
    // and closes any necessary data
    public void endOfData(){
        System.out.println("Number of shingle errors = " + shingleError);
        try {
            root.end();
        }
        catch (java.io.IOException e) {
            System.out.println(e);
            System.exit(1);
        }
    }

    //PROCESS FUNCTION
    //This is where the vast bulk of the program is run and controlled
    public void process( EventHeader event ) {
        super.process( event );

        int check_layer = 0;
        boolean use_limit = false;
        boolean reject_negative = true;

        int hit_count = 0;

        String subdetectorName = "SiVertexBarrel";
        Detector detector = event.getDetector();
        Subdetector subdetector = detector.getSubdetector(subdetectorName);
        List<SimTrackerHit> BrlHits = event.get(SimTrackerHit.class, "SiVertexBarrelHits");
        List<SimTrackerHit> EcpHits = event.get(SimTrackerHit.class, "SiVertexEndcapHits");
        tileBarrelHits(BrlHits);
        tileEndcapHits(EcpHits);
        if( ((eventNumber+1) % bunchCrossings) == 0) {
            try {
                int count =0;
                double zBin = 0;
                for(ArrayList<HashMap<String,Long>> lyrs : BrlTiles){
                    zBin = 0;
                    for( HashMap<String, Long> map : lyrs){
                        double tilesNum = (double) (tileNumBrl[count]) / Rbins;
                        System.out.println("Layer "+ count + " has " + map.size() + " tiles hit ");
                        root.fill("BrlFractionVz"+count,zBin,map.size()/tilesNum);
                        zBin += brlLayerLength/Rbins;
                    }
                    count++;
                }            
                count = 0;
                int radialBin = 0;
                for(ArrayList<HashMap<String,Long>> lyrs : EcpTiles){
                    radialBin =0;
                    for( HashMap<String, Long> map : lyrs){
                        double tilesNum =  (tileNumEcp[count][radialBin]);
                        root.fill("EcapFractionVRadius"+count, radialBin*(EcapRout/Rbins),(map.size()/tilesNum));
                        radialBin++;
                    }
                    count++;
                }

            } catch(java.io.IOException e) {
                System.out.println(e);
                System.exit(1);
            }

            for(ArrayList<HashMap<String,Long>> lyrs : EcpTiles){
                for(HashMap<String, Long> map : lyrs){ map.clear();}//Clear Hashmaps after n bunches!
            }

            for(ArrayList<HashMap<String, Long>> lyrs : BrlTiles){ 
                for(HashMap<String, Long> map : lyrs){ map.clear();}//Clear Hashmaps after n bunches!
            }
        }

        System.out.println("finished event "+ ++eventNumber);        

    }//End Process


    /*all the numbers here are very coarse. If this is to become a regular component in our driver PLEASE check
     * memory consumption and slim this down (i.e. ints instead of longs) however possible */
    private void tileBarrelHits(List<SimTrackerHit> BrlHits) {
        double brlXseg = tileSize; //30 micron segmentation
        double brlYseg = tileSize; //same ^
        int offsetX   = 500000; //arbitrarily large number to ensure no tile ids are negative and are 6 digits long
        int offsetY   = 500000; //ditto ^
        double xBinner = Rbins/brlLayerLength;//Constant used to produce a radial bin number
        for (SimTrackerHit hit : BrlHits) {
            int layer = hit.getLayer()-1;
            double[] vec = hit.getPosition();
            //Get the phi, figure out which radial bin it's in.
            double z  = vec[2]+(brlLayerLength/2);
            int binNum = (int) (z*xBinner);

            //this transforms the global x,y,z coordinates of the vertex hits to local coordinates that are easier to tile
            double[] vecT = hit.getDetectorElement().getGeometry().transformGlobalToLocal( new BasicHep3Vector(vec) ).v();
            String shingle = hit.getDetectorElement().getName();
            String module;
            try{
                //string is assumed to have this form: SiVertexBarrel_layer1_module7_sensor0
                //I'm getting the number following "module" (this refers to the shingle in question)
                module = shingle.split("_")[2].substring(6);
            }catch(java.lang.StringIndexOutOfBoundsException e){
                shingleError++;
                module = "-1";
            }
            String IDx = Integer.toString( (int)(vecT[0]/brlXseg) + offsetX );
            String IDy = Integer.toString( (int)(vecT[1]/brlYseg) + offsetY );
            String tileID = module+IDx+IDy;
            //String tileID = Integer.toString( hit.getCellID() );
            if ( BrlTiles.get(layer).get(binNum).containsKey(tileID) ) {
                long new_count =  ( BrlTiles.get(layer).get(binNum).get(tileID).longValue() ) + 1;
                BrlTiles.get(layer).get(binNum).put( tileID, new Long(new_count) ); 
            } else {
                BrlTiles.get(layer).get(binNum).put( tileID, new Long(1) ); 
                //System.out.println("Tile on layer " + layer + ", bin" + binNum);
            }
        }
    }

    private void tileEndcapHits(List<SimTrackerHit> EcpHits) {
        double EcpXseg = tileSize; //30 micron segmentation
        double EcpYseg = tileSize; //same ^
        int offsetX = 500000; //arbitrarily large number to ensure no tile ids are negative
        int offsetY = 500000; //ditto ^

        for (SimTrackerHit hit : EcpHits) {
            int layer = hit.getLayer()-1;
            double[] vec = hit.getPosition();
            //Obtain radius, place in appropriate radial bin.
            double radius = PolarCoords.CtoP(vec[0],vec[1])[0];
            int radialBin = (int) (radius*((double) (Rbins)/EcapRout));
            String IDx = Integer.toString( (int)(vec[0]/EcpXseg) + offsetX );
            String IDy = Integer.toString( (int)(vec[1]/EcpYseg) + offsetY );
            String side = (vec[2] < 0) ? "1" : "2";
            String tileID = side+IDx+IDy;
            //String tileID = Integer.toString( hit.getCellID() );

            if ( EcpTiles.get(layer).get(radialBin).containsKey(tileID) ) {
                long new_count =  ( EcpTiles.get(layer).get(radialBin).get(tileID).longValue() ) + 1;
                EcpTiles.get(layer).get(radialBin).put( tileID, new Long(new_count) ); 
            } else {
                EcpTiles.get(layer).get(radialBin).put( tileID, new Long(1) ); 
            }
        } 
    }
    /*here we declare and intialize our ArrayLists of Hashmaps of tiles*/
    private ArrayList< ArrayList< HashMap<String,Long> > > BrlTiles = new ArrayList< ArrayList< HashMap<String,Long> > >(5);
    private ArrayList< ArrayList< HashMap<String,Long> > > EcpTiles = new ArrayList< ArrayList< HashMap<String,Long> > >(4);

    /*here we define constants for the tiler*/
    private double pixelSize = 5.0;
    private double tileSize = 5.0;
    private int Rbins = 10;
    private int xBarrelEnd = 70;
    private int EcapRout = 80;
    private double[] brlLayerArea = {14817.6,21168.0,31752.0,42336.0,52920.0};
    private double brlLayerLength = 126;
    //TODO:Words cannot express how much it pained me
    //to hardcode so many numbers into this...
    //PLEASE fix this by reading the eventHeader
    //geometry information and calculating this
    //properly
    private double[][] ecpLayerArea = { {10000.0,    3.4,124.3,175.3,226.2,277.1,328.0,378.9,429.9,131.8,},
                                        {10000.0,10000.0,115.2,175.3,226.2,277.1,328.0,378.9,429.9,131.8,},
                                        {10000.0,10000.0, 96.9,168.9,219.8,270.7,321.6,372.6,423.5,188.3,},
                                        {10000.0,10000.0, 70.9,159.3,210.3,261.2,312.1,363.0,413.9,271.6,} };
    private int[] tileNumBrl = new int[5];
    private int[][] tileNumEcp= new int[4][Rbins];

    /*here all the classwide variables are declared*/
    private int eventNumber;
    private int shingleError = 0;
    private int brlLyrN = 5;
    private int ecpLyrN = 4;
    //xml derived variables
    private String jrootFile = "";
    private int bunchCrossings = 1;

    //variables for jroot file construction and background/signal file reading
    private LCIOFileManager lcioFileMNGR = new LCIOFileManager();
    private Jroot root;
    private boolean aligned;
}
