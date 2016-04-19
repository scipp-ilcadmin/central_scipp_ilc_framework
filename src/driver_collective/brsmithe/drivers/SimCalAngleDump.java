/*
 * ParticleDump.java
 *
 * Created on July 11, 2013, 10:45 AM
 * Edited on January 9, 2014, 12:31 AM
 * @author Alex Bogert and Christopher Milke
 * Modified by Jane Shtalenkova on
 * April 22, 2015
 */
package scipp_ilc.drivers;

import scipp_ilc.base.util.Jroot;
import scipp_ilc.base.util.LCIOFileManager;

import org.lcsim.event.EventHeader;
import org.lcsim.event.MCParticle;
import org.lcsim.event.SimCalorimeterHit;

import hep.physics.particle.properties.ParticleType;

import org.lcsim.util.Driver;

import java.lang.String;
import java.util.List;


public class SimCalAngleDump extends Driver {
    
    
    
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

        try {
            root = new Jroot(jrootFile, "NEW");
	
            root.init("TH2D","posXY","posXY", "XYPosition", 300, -150, 150, 300, -150, 150);
            root.init("TH1D","posz","posz", "Z Position", 18000, 0, 18000);
	    
            
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
	super.process( event );
        MCParticle mcp = null;
	

	List<SimCalorimeterHit> hits = event.get(SimCalorimeterHit.class, "BeamCalHits");
        
        //iterate through all FINAL_STATE particles in event
        for (SimCalorimeterHit hit : hits) {

	    //Find the number of MC particles responsible for a hit
	    int particles = hit.getMCParticleCount();
	    
	    //Create a list of the indices to cycle through MCparticles responsible for hit.  
	    int[] particleindex = new int[particles];
	    
	    for (int i : particleindex) {
		MCParticle p = hit.getMCParticle(i);

		int state = p.getGeneratorStatus();
		if(state == MCParticle.FINAL_STATE){
		    
		    
            	     //get endpoint and scale to face
		    double[] pos = p.getEndPoint().v();

		    boolean ignoremisses = true;

		    //fill position plot
		    try {
			// If the particle missed and we are ignoring them, returns false 
			if(!((pos[2]>3600 || pos[2]<2800) && ignoremisses) ){ 
			    root.fill("posXY",pos[0], pos[1]);
			    root.fill("posz",pos[2]);
			}
		    }
		    catch (java.io.IOException e) {
			System.out.println(e);
			System.exit(1);
		    }
		    //System.out.println("\n");
		    
		}// End the if statement
	  
	    }//End the new For loop
	    
        }//End the main for loop
        
     System.out.println("FINISHED EVENT "  + eventNumber++ + "\n\n\n\n\n");
     
    }//End Process
    

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
