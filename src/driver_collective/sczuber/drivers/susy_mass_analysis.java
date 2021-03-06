/*
 * susy_mass_analysis.java
 *
 * Created on July 2, 2016, 17:54 
 * by Summer Zuber
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
public class susy_mass_analysis extends Driver {

// changing to no angle requirement for DETECTABLE particles

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
        System.out.println("Running susy_mass_analysis");
        try {
            root = new Jroot(jrootFile, "NEW");
            root.init("TH1D","M_N_A","M_N_A_1", jrootFile,400, 0,20); //true .3-.5
            root.init("TH1D","M_N_A","M_N_A_2", jrootFile,400, 0,20); //true .5-1
            root.init("TH1D","M_N_A","M_N_A_3", jrootFile,40, 0,20); //true  >1 
            root.init("TH1D","M_n_A","M_n_A_1", jrootFile,400, 0,20); //detectable
            root.init("TH1D","M_n_A","M_n_A_2", jrootFile,400, 0,20); //detectable
            root.init("TH1D","M_n_A","M_n_A_3", jrootFile,40, 0,20); //detectable
            root.init("TH1D","M_n_C","M_n_C_1", jrootFile,400, 0,20); //detected 
            root.init("TH1D","M_n_C","M_n_C_2", jrootFile,400, 0,20); //detected 
            root.init("TH1D","M_n_C","M_n_C_3", jrootFile,40, 0,20); //detected 
                                
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
        double[][] vectors = new double[4][3]; 
        double[] scalars = new double[4]; 
        double[] energy = new double[4];
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
                scalars[0]+= scalar;  // N_A true
                vectors[0][0]+=momX;  // N_A 
                vectors[0][1]+=momY;
                vectors[0][2]+=momZ;
                energy[0]+=E;

                if (!isNeutrino ){
                    scalars[1]+=scalar;  // n_A detectable
                    vectors[1][0]+=momX; // n_A
                    vectors[1][1]+=momY;
                    vectors[1][2]+=momZ;
                    energy[1]+=E;
                }
                if (isCentral){
                    scalars[2]+=scalar;  // N_C
                    vectors[2][0]+=momX; // N_C
                    vectors[2][1]+=momY;
                    vectors[2][2]+=momZ;
                    energy[2]+=E;
                    if (!isNeutrino){
                        scalars[3]+=scalar;  // n_C //detected 
                        vectors[3][0]+=momX; // n_C
                        vectors[3][1]+=momY;
                        vectors[3][2]+=momZ;
                        energy[3]+=E;
                    }
                }
            }
                System.out.println("\n");    
        }
        double mass_0 = Math.sqrt(energy[0]*energy[0]-(Math.pow(vectors[0][0],2)+Math.pow(vectors[0][1],2)+Math.pow(vectors[0][2],2)));
        double mass_1 = Math.sqrt(energy[1]*energy[1]-(Math.pow(vectors[1][0],2)+Math.pow(vectors[1][1],2)+Math.pow(vectors[1][2],2)));
        double mass_3 = Math.sqrt(energy[3]*energy[3]-(Math.pow(vectors[3][0],2)+Math.pow(vectors[3][1],2)+Math.pow(vectors[3][2],2)));
        try{
            if (mass_0 >= 0.3 && mass_0 < 0.5){
                root.fill("M_N_A_1", mass_0);
            }
            if (mass_0 >= 0.5 && mass_0 <1){
                root.fill("M_N_A_2", mass_0);
            }
            if (mass_0 >= 1){
                root.fill("M_N_A_3", mass_0);
            }
            if (mass_1 >= 0.3 && mass_1 < 0.5){
                root.fill("M_n_A_1", mass_1);
            }
            if (mass_1 >= 0.5 && mass_1 < 1){
                root.fill("M_n_A_2", mass_1);
            }
            if (mass_1 >= 1){
                root.fill("M_n_A_3", mass_1);
            }
            if (mass_3 >= 0.3 && mass_3 < 0.5){
                root.fill("M_n_C_1", mass_3);
            }
            if (mass_3 >= 0.5 && mass_3 < 1){
                root.fill("M_n_C_2", mass_3);
            }
            if (mass_3 >= 1){
                root.fill("M_n_C_3", mass_3);
            } 
        }
        
        catch (java.io.IOException e) {
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
