/*
 * TrackerOccupancyAnalysis.java
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

public class TrackerOccupancyAnalysis extends Driver {
    
    
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

        for(int i=0;i<brlLyrN;i++) BrlTiles.add( new HashMap<String,Long>() );
        for(int i=0;i<ecpLyrN;i++) EcpTiles.add( new HashMap<String,Long>() );
        for(int i = 0; i<brlLyrN; i++){ tileNumBrl[i]= (int)(brlLayerArea[i]/(tileSize*tileSize));}
        for(int i = 0; i<ecpLyrN; i++){ tileNumEcp[i]= (int)(ecpLayerArea[i]/(tileSize*tileSize));}

        try {
            root = new Jroot(jrootFile,root_mode);
            for(int i = 0; i<brlLyrN;i++){
                root.init("TH1D","BrlEventsVCsHit"+i,"BrlEventsVCsHit"+i,"BrlHits"+i,50,0,0.0005);
            }
            for(int i = 0; i<ecpLyrN;i++){
                root.init("TH1D","EcapEventsVCsHit"+i,"EcapEventsVCsHit"+i,"EcapHits"+i,50,0,0.0005);
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

            for(int i=0;i<brlLyrN;i++) {
                root.proc("BrlEventsVCsHit"+i+"->SetLineColor("+(i+2)+");");
                root.proc("BrlEventsVCsHit"+i+"->SetFillColor("+(i+2)+");");
                root.proc("BrlEventsVCsHit"+i+"->SetLineWidth(3);");
                root.proc("BrlEventsVCsHit"+i+"->SetFillStyle(3002);");
            }
            root.proc("TCanvas BrlCanvas(\"BrlPlots\",\"BrlPlots\");");
            root.proc("THStack BrlStack(\"BrlStack\",\"Vertex Barrel Fractional Occupancy Over "+bunchCrossings+" Bunch Crossings(s)\");");
            for(int i=0;i<brlLyrN;i++) root.proc("BrlStack.Add(BrlEventsVCsHit"+i+",\"same\");");
            root.proc("BrlStack.Draw();");
            root.proc("BrlStack.GetHistogram()->GetXaxis()->SetTitle(\"Fraction of Pixels Hit\");");
            root.proc("BrlStack.GetHistogram()->GetYaxis()->SetTitle(\"Number of Bunches Hit X Times\");");
            root.proc("BrlCanvas.BuildLegend();");
            root.proc("BrlCanvas.SaveAs(\""+jrootFile+"_brl.png\");");
            root.proc("BrlCanvas.SaveAs(\""+jrootFile+"_brl.root\");");

            for(int i=0;i<ecpLyrN;i++) {
                root.proc("EcapEventsVCsHit"+i+"->SetLineColor("+(i+2)+");");
                root.proc("EcapEventsVCsHit"+i+"->SetFillColor("+(i+2)+");");
                root.proc("EcapEventsVCsHit"+i+"->SetLineWidth(3);");
                root.proc("EcapEventsVCsHit"+i+"->SetFillStyle(3002);");
            }
            root.proc("TCanvas EcapCanvas(\"EcapPlots\",\"EcapPlots\");");
            root.proc("THStack EcapStack(\"EcapStack\",\"Vertex Endcap Fractional Occupancy Over "+bunchCrossings+" Bunch Crossing(s)\");");
            for(int i=0;i<ecpLyrN;i++) root.proc("EcapStack.Add(EcapEventsVCsHit"+i+",\"same\");");
            root.proc("EcapStack.Draw();");
            root.proc("EcapStack.GetHistogram()->GetXaxis()->SetTitle(\"Fraction of Pixels Hit\");");
            root.proc("EcapStack.GetHistogram()->GetYaxis()->SetTitle(\"Number of Bunches Hit X Times\");");
            root.proc("EcapCanvas.BuildLegend();");
            root.proc("EcapCanvas.SaveAs(\""+jrootFile+"_ecp.png\");");
            root.proc("EcapCanvas.SaveAs(\""+jrootFile+"_ecp.root\");");

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
        int hit_count_limit = 100;
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
        if((eventNumber+1) %(bunchCrossings) == 0){
            try {
            int count =0;
            
            for( HashMap<String, Long> map : BrlTiles){
                for(String currentKey : map.keySet()){
                //plot all cells that have a hit                                                                                                                                                                 
                }
                root.fill("BrlEventsVCsHit"+count, (double) (map.size())/tileNumBrl[count]);
                count++;
            }            
            count = 0;
            for( HashMap<String, Long> map : EcpTiles){
                for(String currentKey : map.keySet()){
                //plot all cells that have a hit  
                
                }
                root.fill("EcapEventsVCsHit"+count, (double) (map.size())/tileNumEcp[count]);
                count++;
                
            }
        
        } catch(java.io.IOException e) {
            System.out.println(e);
            System.exit(1);
        }

        for(HashMap<String, Long> map : EcpTiles){ map.clear();}//Clear Hashmaps after 5 bunches!
        for(HashMap<String, Long> map : BrlTiles){ map.clear();}
    }
    
    //    System.out.println(BrlTiles.get(0));
    //Plot number of cells hit vs number of events that have this
    System.out.println("finished event "+ ++eventNumber);        
    
    }//End Process
    
    
    /*all the numbers here are very coarse. If this is to become a regular component in our driver PLEASE check
     * memory consumption and slim this down (i.e. ints instead of longs) however possible */
    private void tileBarrelHits(List<SimTrackerHit> BrlHits) {
        int BarrelLayerCount = 6; //number of Vertex Barrel Layers... I think this is several more than it needs to be, just to be safe
        double brlXseg = tileSize; //30 micron segmentation
        double brlYseg = tileSize; //same ^
        int offsetX   = 500000; //arbitrarily large number to ensure no tile ids are negative and are 6 digits long
        int offsetY   = 500000; //ditto ^

        //ArrayList< HashMap<String,Long> > BrlTiles = new ArrayList< HashMap<String,Long> >(5);
        //for(int i=0;i<BarrelLayerCount;i++) BrlTiles.add( new HashMap<String,Long>() );

        for (SimTrackerHit hit : BrlHits) {
            int layer = hit.getLayer()-1;
            double[] vec = hit.getPosition();
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
            if ( BrlTiles.get(layer).containsKey(tileID) ) {
                long new_count =  ( BrlTiles.get(layer).get(tileID).longValue() ) + 1;
                BrlTiles.get(layer).put( tileID, new Long(new_count) ); 
            } else {
                BrlTiles.get(layer).put( tileID, new Long(1) ); 
            }
        }
    }

    private void tileEndcapHits(List<SimTrackerHit> EcpHits) {
        int EndcapLayerCount = 6; //number of Vertex Endcap Layers... I think this is several more than it needs to be, just to be safe
        double EcpXseg = tileSize; //30 micron segmentation
        double EcpYseg = tileSize; //same ^
        int offsetX = 500000; //arbitrarily large number to ensure no tile ids are negative
        int offsetY = 500000; //ditto ^

        //ArrayList< HashMap<String,Long> > EcpTiles = new ArrayList< HashMap<String,Long> >(4);
        //for(int i=0;i<EndcapLayerCount;i++) EcpTiles.add( new HashMap<String,Long>() );
    
        for (SimTrackerHit hit : EcpHits) {
            int layer = hit.getLayer()-1;
            double[] vec = hit.getPosition();
            String IDx = Integer.toString( (int)(vec[0]/EcpXseg) + offsetX );
            String IDy = Integer.toString( (int)(vec[1]/EcpYseg) + offsetY );
            String side = (vec[2] < 0) ? "1" : "2";
            String tileID = side+IDx+IDy;
            //String tileID = Integer.toString( hit.getCellID() );
        
            if ( EcpTiles.get(layer).containsKey(tileID) ) {
                long new_count =  ( EcpTiles.get(layer).get(tileID).longValue() ) + 1;
                EcpTiles.get(layer).put( tileID, new Long(new_count) ); 
            } else {
                EcpTiles.get(layer).put( tileID, new Long(1) ); 
            }
        } 
    }
    /*here we declare and intialize our ArrayLists of Hashmaps of tiles*/
    private ArrayList< HashMap<String,Long> > BrlTiles = new ArrayList< HashMap<String,Long> >(5);
    private ArrayList< HashMap<String,Long> > EcpTiles = new ArrayList< HashMap<String,Long> >(4);
    
    /*here we define constants for the tiler*/
    private double pixelSize = 5.0;
    private double tileSize = 5.0;
    private double[] brlLayerArea = {14817.6,21168,31752,42336,52920};
    private double[] ecpLayerArea = {4149.8*2,4124.7*2,4124.7*2,4124.7*2};
    private int[] tileNumBrl = new int[5];
    private int[] tileNumEcp= new int[4];
    
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
