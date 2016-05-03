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
            root.init("TH1D","V_n_C","V_n_C", jrootFile,40, 0,20);
            root.init("TH1D","V_n_A","V_n_A", jrootFile,40, 0,20);
            root.init("TH1D","V_N_C","V_N_C", jrootFile,40, 0,20);
            root.init("TH1D","V_N_A","V_N_A", jrootFile,40, 0,20);
            root.init("TH1D","S_n_C","S_n_C", jrootFile,40, 0,20);
            root.init("TH1D","S_n_A","S_n_A", jrootFile,40, 0,20);
            root.init("TH1D","S_N_C","S_N_C", jrootFile,40, 0,20);
            root.init("TH1D","S_N_A","S_N_A", jrootFile,40, 0,20);
            root.init("TH1D","M_N_A","M_N_A", jrootFile,40, 0,20);
            root.init("TH1D","M_n_C","M_n_C", jrootFile,40, 0,20);
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
                double E = p.getEnergy();
                double charge = p.getCharge();
                boolean isNeutrino = (
                    id == 12 || id == -12 || 
                    id == 14 || id == -14 ||
                    id == 16 || id == -16 ||
                    id == 18 || id == -18 );
                boolean isCentral = (cos<0.9 || cos>-0.9);
                scalars[0]+= scalar;  // S_N_A
                vectors[0][0]+=momX;  // V_N_A 
                vectors[0][1]+=momY;
                vectors[0][2]+=momZ;
                energy[0]+=E;
                if (!isNeutrino ){
                    scalars[1]+=scalar;  // S_n_A
                    vectors[1][0]+=momX; // V_n_A
                    vectors[1][1]+=momY;
                    vectors[1][2]+=momZ;
                    energy[1]+=E;
                }
                if (isCentral){
                    scalars[2]+=scalar;  // S_N_C
                    vectors[2][0]+=momX; // V_N_C
                    vectors[2][1]+=momY;
                    vectors[2][2]+=momZ;
                    energy[2]+=E;
                    if (!isNeutrino){
                        scalars[3]+=scalar;  // S_n_C
                        vectors[3][0]+=momX; // V_n_C
                        vectors[3][1]+=momY;
                        vectors[3][2]+=momZ;
                        energy[3]+=E;
                    }
                }
            }
                System.out.println("\n");    
        }
        try{
            root.fill("S_N_A", scalars[0]);
            root.fill("S_n_A", scalars[1]);
            root.fill("S_N_C", scalars[2]);
            root.fill("S_n_C", scalars[3]);
            root.fill("V_N_A", Math.sqrt(vectors[0][0]*vectors[0][0]+vectors[0][1]*vectors[0][1])); 
            root.fill("V_n_A", Math.sqrt(vectors[1][0]*vectors[1][0]+vectors[1][1]*vectors[1][1]));
            root.fill("V_N_C", Math.sqrt(vectors[2][0]*vectors[2][0]+vectors[2][1]*vectors[2][1]));
            root.fill("V_n_C", Math.sqrt(vectors[3][0]*vectors[3][0]+vectors[3][1]*vectors[3][1]));
            root.fill("M_N_A", Math.sqrt(energy[0]*energy[0]-(Math.pow(vectors[0][0],2)+Math.pow(vectors[0][1],2)+Math.pow(vectors[0][2],2)))
            root.fill("M_n_C", Math.sqrt(energy[3]*energy[3]-(Math.pow(vectors[3][0],2)+Math.pow(vectors[3][1],2)+Math.pow(vectors[3][2],2)))
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
