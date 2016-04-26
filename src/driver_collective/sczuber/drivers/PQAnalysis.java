/*
 * PQAnalysis.java
 *
 * Created on July 11, 2013, 10:45 AM
 * Edited on January 9, 2014, 12:31 AM
 * @author Alex Bogert and Christopher Milke
 * Modified by Jane Shtalenkova on
 * April 22, 2015
 */
package scipp_ilc.drivers;

import scipp_ilc.base.util.ScippUtils;
import scipp_ilc.base.util.PolarCoords;
import scipp_ilc.base.util.LCIOFileManager;
import scipp_ilc.base.util.Jroot;

import org.lcsim.event.EventHeader;
import org.lcsim.event.MCParticle;

import hep.physics.particle.properties.ParticleType;

import org.lcsim.util.Driver;

import java.lang.String;
import java.lang.Math;
import java.util.*;
public class PQAnalysis extends Driver {



    //DEFINE XML FUNCTIONS
    //These functions are specially fomatted functions to pull variable data from the xml file
    /*****************************************************************************************
      XML FUNCTION FORMAT

      public void //setVariablename(variable type) { //the first letter after "set" must be uppercase
    //but can (must?) be lowercase in xml file
    set variable here; 
    }
     *******************************************************************************************/

    public void setOutputfile(String s) {
        this.jrootFile = s;
    }
    //END DEFINE XML FUNCTIONS


    //This function is called when the program is first started
    //and initializes all persistant data
    public void startOfData() {
        eventNumber = 0;
        System.out.println("Running PQAnalysis");
        try {
            root = new Jroot(jrootFile, "NEW");
            root.init("TH1D","hist1", "SumPT", "PT Analysis - Low PT Zoom",200, 0,20);
            root.init("TH1D","hist2", "CheckSumPT","PT Analysis - Low PT Zoom",200,0,20);
            //root.init("TH2D","E_cos","E_cos","Energy Final State Particles of Cos(theta)", 400, -1, 1, 700, 0, 700);
                      
        }
        catch (java.io.IOException e) {
            System.out.println(e);
            System.exit(1);
        }
    }


    //This function is called after all file runs have finished,
    // and closes any necessary data
    public void endOfData(){
        try {
            root.end();
        }
        catch (java.io.IOException e) {
            System.out.println(e);
            System.exit(1);
        }
    }

    public void printElectron(EventHeader event) {

    }


    //PROCESS FUNCTION
    public void process( EventHeader event ) {
        MCParticle mcp = null;
        System.out.println("\n\n\n\n\n\n**************NEW EVENT*******************\n\n\n");
        List<Double> TransMom = new ArrayList<Double>();
        List<Double> PX = new ArrayList<Double>();
        List<Double> PY = new ArrayList<Double>();   
        //iterate through all FINAL_STATE particles in event
        for (MCParticle p : event.getMCParticles()) {    
            int state = p.getGeneratorStatus();
            if(state == MCParticle.FINAL_STATE){
                ParticleType type = p.getType();
                String name = type.getName();
                int id = p.getPDGID();
                double mom = p.getMomentum().magnitude();
                double momZ = p.getPZ();
                double cos = momZ/mom;
                double momX = p.getPX();
                double momY = p.getPY();
                double PT = Math.sqrt(momX*momX+momY*momY); 
                double energy = p.getEnergy();
                double charge = p.getCharge();
                if (
                    id != 12 && id != -12 && 
                    id != 14 && id != -14 &&
                    id != 16 && id != -16 &&
                    id != 18 && id != -18 &&
                    id != 1000022 ){ 
                    if (cos<0.9 || cos>-0.9){
                        PX.add(momX);
                        PY.add(momY);
                        TransMom.add(PT);
                    }
                }
                System.out.println("\n");    
            }
        }


        double PTCheckSum = Math.sqrt(Sum(PX)*Sum(PX)+Sum(PY)*Sum(PY)); 
        try{
            root.fill("hist1", Sum(TransMom));   
            root.fill("hist2", PTCheckSum); 
        }
        catch (java.io.IOException e){
            System.out.println(e);
            System.exit(1);
        }
             
             
        System.out.println("FINISHED EVENT "  + eventNumber++ + "\n\n\n\n\n"); 
    } // end process 

    //  sums the PT of each particle in the event
    public double Sum(List<Double> array){
        double sum = 0; 
        for (int counter = 0;counter<array.size();counter++){
            sum+=array.get(counter);
        } 

        return sum;
    }
    

        /*here all the classwide variables are declared*/
        private int eventNumber;

        private double faceZ=2500; //face of detector hard code
        public double theta;
        private double escZ=12000;
        private int eCount, pCount;
        private int morePairs = 0;

        //xml derived variables
        private String jrootFile = "";

        //variables for jroot file construction and background/signal file reading
        private LCIOFileManager lcioFileMNGR = new LCIOFileManager();
        private Jroot root;
}
