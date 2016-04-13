/*
 * SusyParentage.java
 *
 * Created on July 11, 2013, 10:45 AM
 * Edited on January 9, 2014, 12:31 AM
 * @author Alex Bogert and Christopher Milke
 * Modified by Jane Shtalenkova on
 * April 22, 2015
 * Modified by Summer Zuber
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
public class SusyParentage extends Driver {



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
        System.out.println("Running SusyParentage");
        System.out.println("documentation = "+MCParticle.DOCUMENTATION);
        System.out.println("final = "+MCParticle.FINAL_STATE);
        System.out.println("intermediate = "+MCParticle.INTERMEDIATE);
        try {
            root = new Jroot(jrootFile, "NEW");
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

        System.out.println( event.keys() );

        //iterate through all FINAL_STATE particles in event
        for (MCParticle p : event.getMCParticles()) {    
            int state = p.getGeneratorStatus();
            ParticleType type = p.getType();
            //System.out.println(p);
            //System.out.println(type+" "+state);
            int id = p.getPDGID();
            double momX = p.getPX();
            double momY = p.getPY();
            double PT = Math.sqrt(momX*momX+momY*momY); 
            double energy = p.getEnergy();
            List<MCParticle> parents = p.getParents();
            List<MCParticle> daughters = p.getDaughters();
            System.out.println("Parents: ");
            if(parents.size()==0){
                System.out.println("None");
            }
            for(int i = 0; i<parents.size(); i++){
                MCParticle parent_id = parents.get(i);
                ParticleType parent_type = parents.get(i).getType();
                System.out.println(parent_id +" "+parent_type);
            }
            System.out.println("   "+p);
            System.out.println("   "+type+" "+state);
            System.out.println("      Daughters: " );
            for(int i = 0; i<daughters.size(); i++){
                ParticleType daughter_type = daughters.get(i).getType();
                MCParticle daughter_id = daughters.get(i);
                System.out.println("      "+daughter_id+" "+daughter_type);
                List<MCParticle> daughters_1 = daughters.get(i).getDaughters();



               
                
            }    
        }

        System.out.println("\n");




        System.out.println("FINISHED EVENT "  + eventNumber++ + "\n\n\n\n\n"); 
    } // end process 

    //  sums the PT of each particle in the event
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
