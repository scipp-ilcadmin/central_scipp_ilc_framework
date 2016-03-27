/*
 * SusyKinematicAnalysis.java
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
public class SusyKinematicAnalysis extends Driver {



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
        System.out.println("documentation = "+MCParticle.DOCUMENTATION);
        System.out.println("final = "+MCParticle.FINAL_STATE);
        System.out.println("intermediate = "+MCParticle.INTERMEDIATE);
        try {
            root = new Jroot(jrootFile, "NEW");
            //root.init("TH2D","posXY","posXY", "XYPosition", 1000, -500, 500, 1000, -500, 500);
            //root.init("TH1D","posz","posz", "Z Position", 18000, 0, 18000);
            root.init("TH1D","hist1", "SumPT", "Total PT of Final State Observable Particles with |cos(theta)|> 0.9",1000, 0,500);
            //root.init("TH1D","hist2", "SumPT", "Total PT of Final State Observable Particles with |cos(theta)|<0.9",1000, 0,500);
            root.init("TH1D","hist3","mult_1","Multiplicity of Charged Final State Observable Particles with |cos(theta)| > 0.9",500,0,500); //change to charged only
            root.init("TH1D","hist4","mult_2","Multiplicity of Charged Final State Observable Particles with |cos(theta)| < 0.9",500,0,500); //change to charged only       
            root.init("TH2D","P_cos", "P_cos","PT Final State (Observable) Particles vs Cosine(theta)", 400, -1, 1, 700, 0, 700);
            root.init("TH2D","E_cos","E_cos","Energy Final State Particles of Cos(theta)", 400, -1, 1, 700, 0, 700);
            root.init("TH2D","E_cos_gamma","E_cos_gamma","Energy FS Gamma of cos_theta",400,-1,1,1000,0,500);
            root.init("TH2D","E_cos_ep","E_cos_ep","Energy FS e/p of cos_theta",400,-1,1,1000,0,500);
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
    //This is where the vast bulk of the program is run and controlled
    public void process( EventHeader event ) {
        MCParticle mcp = null;
        System.out.println("\n\n\n\n\n\n**************NEW EVENT*******************\n\n\n");

        List<Double> TransMom = new ArrayList<Double>();   
        System.out.println( event.keys() );
        int mult_1 = 0; // > 0.9
        int mult_2 = 0; // < 0.9


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
                List<MCParticle> parents = p.getParents();
                List<MCParticle> daughters = p.getDaughters();
		        double charge = p.getCharge();
                //System.out.println(type+" "+state);
                //System.out.println("costheta: "+cos);
                //System.out.println("P magnitude: "+mom);
                //System.out.println("Energy: "+energy);
                //System.out.println("Parents: "+parents);
                for(int i = 0; i<parents.size(); i++){
                    ParticleType parent = parents.get(i).getType();
                    //System.out.println(parent);
                }
               // System.out.println("Daughters: " + daughters);
                for(int i = 0; i<daughters.size(); i++){
                    ParticleType daughter = daughters.get(i).getType();
                    //System.out.println(daughter);
                }
                
                if (id != 12 && id != -12 && 
                    id != 14 && id != -14 &&
                    id != 16 && id != -16 &&
                    id != 18 && id != -18 &&
                    id != 1000022 ){ 
                    
                    if(charge != 0){ // charged particles  
                        if ( cos >= 0.9 || cos <= -0.9){  
                            mult_1 = mult_1 + 1;  
                        }     
                        if ( cos<=0.9 || cos>=-0.9){
                            mult_2 = mult_2 + 1;
                        }
                    }  
                    if ( cos >= 0.9 || cos <= -0.9){
                        TransMom.add(PT);
                    }
                }
                try {
                    //get endpoint and scale to face
                    //double[] pos = p.getEndPoint().v();         
                    //fill position plot
                    //root.fill("posXY",pos[0], pos[1]);
                    //root.fill("posz",pos[2]);
                    if (mom != 0 && 
                        id != 12 && id != -12 &&
                        id != 14 && id != -14 &&
                        id != 16 && id != -16 &&
                        id != 18 && id != -18 &&
                        id != 1000022 && mom != 0) { 
                        root.fill("E_cos",cos,energy);
                    }
                    if (id == 22 && mom != 0){
                        root.fill("E_cos_gamma",cos,energy);
                        }
                    if (id == 11 || id == -11){
                        root.fill("E_cos_ep",cos,energy);
                        }
                    }
                
                catch (java.io.IOException e) {
                    System.out.println(e);
                    System.exit(1);
                }
                catch (java.lang.RuntimeException e){
                    System.out.println("Found No endpoint");
                }

                System.out.println("\n");
            
            
            //System.out.println("PT list "+TransMom);
            }
        }
        
            try{
                root.fill("hist1", Sum(TransMom));  // |cos| > 0.9
                root.fill("hist3", mult_1);
                root.fill("hist4", mult_2);
            }
            catch (java.io.IOException e){
                System.out.println(e);
                System.exit(1);
            }
             
             
            //System.out.println("FINISHED EVENT "  + eventNumber++ + "\n\n\n\n\n"); 
        } // end process 

    // function sums the PT of each particle in the event
    public double Sum(List<Double> array){
        double sum = 0; 
        for (int counter = 0;counter<array.size();counter++){
            sum+=array.get(counter);
        } 
        //System.out.println("Total PT " + sum);
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
