/*
 * susy_cutflow_detectable.java
 *
 * Created on July 11, 2013, 10:45 AM
 * Edited on January 9, 2014, 12:31 AM
 * @author Alex Bogert and Christopher Milke
 * Modified by Jane Shtalenkova on
 * April 22, 2015
 * Modified by Summer Zuber 
 * June, 2016
 *
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
public class susy_cutflow_detectable extends Driver {



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
        System.out.println("Running Susy Cutflow Detectable");
    //    try {
      //      root = new Jroot(jrootFile, "NEW");
            //initialize root plots
                      
       // }
        //catch (java.io.IOException e) {
          //  System.out.println(e);
           // System.exit(1);
       // }
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
        //System.out.println("\n\n\n\n\n\n**************NEW EVENT*******************\n\n\n");
        double[][] vectors = new double[2][3]; //0:1 true:detected
        double[] scalars = new double[2]; 
        double[] energy = new double[2];
        //iterate through all FINAL_STATE particles in event
        for (MCParticle p : event.getMCParticles()) {    
            int state = p.getGeneratorStatus();
            if(state == MCParticle.FINAL_STATE){
                ParticleType type = p.getType();
                String name = type.getName();
                int id = p.getPDGID();
                boolean isDarkMatter = ( id == 1000022 );
                if (isDarkMatter) continue;
                double E = p.getEnergy(); 
                double mom = p.getMomentum().magnitude();
                double momZ = p.getPZ();
                double cos = momZ/mom;
                double momX = p.getPX();
                double momY = p.getPY();
                double scalar =  Math.sqrt(momX*momX+momY*momY);
                //double PT = Math.sqrt(momX*momX+momY*momY); 
                boolean isNeutrino = (
                    id == 12 || id == -12 || 
                    id == 14 || id == -14 ||
                    id == 16 || id == -16 ||
                    id == 18 || id == -18 );
                boolean isCentral = (cos<0.9 || cos>-0.9);
                scalars[0]+= scalar;  // S_N_A //true
                vectors[0][0]+=momX;  // V_N_A //true 
                vectors[0][1]+=momY;
                vectors[0][2]+=momZ;
                energy[0]+=E;
                if (!isNeutrino){
                    scalars[1]+=scalar;  // S_n_A //detectable
                    vectors[1][0]+=momX; // V_n_A //detectable 
                    vectors[1][1]+=momY;
                    vectors[1][2]+=momZ;
                    energy[1]+=E; 
                }
            }
                System.out.println("\n");    
        }
        double total_detect_scalar = scalars[1];
        double total_detect_mass_squared = energy[1]*energy[1]-
        (vectors[1][0]*vectors[1][0]+vectors[1][1]*vectors[1][1]
        +vectors[1][2]*vectors[1][2]);
        double total_detect_mass = Math.sqrt(total_detect_mass_squared);
        cuts[0]+=1;
        if(total_detect_scalar > 0.5){
            cuts[1]+=1;
            if(total_detect_mass > 0.5){
                cuts[2]+=1;
                if(total_detect_scalar > 1){
                    cuts[3]+=1;
                    if(total_detect_mass >1){
                        cuts[4]+=1;
                    }
                }
            }
        }
    //---------------------------------------------------------------------------
       // try{
       //     //fill root plots   
       // }
        
       // catch (java.io.IOException e) {
       //     System.out.println(e);
       //     System.exit(1);
       // }
    //---------------------------------------------------------------------------  
    System.out.println("cut_0  "+cuts[0]);
    System.out.println("cut_1  "+cuts[1]);
    System.out.println("cut_2  "+cuts[2]);
    System.out.println("cut_3  "+cuts[3]);
    System.out.println("cut_4  "+cuts[4]);
             
             
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
    
        double[] cuts = { 0, 0, 0, 0, 0}; // 5 categories of cuts
        
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
