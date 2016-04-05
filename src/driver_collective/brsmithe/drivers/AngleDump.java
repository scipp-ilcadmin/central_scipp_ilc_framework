/*
 * AngleDump.java
 *
 * Created on July 11, 2013, 10:45 AM
 * Edited on January 9, 2014, 12:31 AM
 * @author Alex Bogert and Christopher Milke
 * Modified by Jane Shtalenkova on
 * April 22, 2015
 * Modified again by Ben Smithers
 * Starting April 4, 2016
 */
package scipp_ilc.drivers;

import scipp_ilc.base.util.Jroot;
import scipp_ilc.base.util.LCIOFileManager;

import org.lcsim.event.EventHeader;
import org.lcsim.event.MCParticle;

import hep.physics.particle.properties.ParticleType;

import org.lcsim.util.Driver;

import java.lang.String;

import java.util.ArrayList;

public class AngleDump extends Driver {
    
    
    
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
            root.init("TH1D","posz","posz", "Z Position", 18000, 0, 25000);
	    root.init("TH2D", "theta", "theta", "Theta Z Position", 18000, 0, 1800, 180, -6, 6);
            
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
        //System.out.println("\n\n\n\n\n\n**************NEW EVENT*******************\n\n\n");

        
        //System.out.println( event.keys() );


        
        //iterate through all FINAL_STATE particles in event
	ArrayList<Double> anglelist=new ArrayList<Double>();
        for (MCParticle p : event.getMCParticles()) {
            
            int state = p.getGeneratorStatus();
            if(state == MCParticle.FINAL_STATE){
                ParticleType type = p.getType();

                String name = type.getName();
                int id = p.getPDGID();
               
            
                //System.out.println(type);
                //System.out.println(name);
                //System.out.println(id);

                //get endpoint and scale to face
	        double[] org =p.getOrigin().v();
		double[] pos = p.getEndPoint().v();
		double[] momentum = p.getMomentum().v();
		//System.out.println("px: " + pos[0] + "py: " + pos[1] + "pz: " + pos[2]);
		//System.out.println("mx: " + momentum[0] + "my: " + momentum[1] + "mz: " +momentum[2]);
		//System.out.println("ox: " + org[0] + "oy: " + org[1] + "oz: " +org[2]);
		
		double theta;
		double term1=(momentum[1]*pos[0]-pos[1]*momentum[0]);
		double term2=(pos[0]*pos[0]+pos[1]*pos[1]);
		theta = term1/term2;
		anglelist.add(theta);

		// First, finding the magnitude squared of the momentum, rsq. Then, finding its root. 
		double rsq = momentum[0]*momentum[0] + momentum[1]*momentum[1] + momentum[2]*momentum[2];
		double arr = Math.sqrt(rsq);
	 
     
		
		// Calclating the transverse angle based on particle momentum.
    
	        double angle = Math.acos(momentum[2]/arr);
		
		
                //fill position plot
                try {
                     root.fill("posXY",pos[0], pos[1]);
                     root.fill("posz",pos[2]);
		     root.fill("theta",pos[2],angle);
                }
                catch (java.io.IOException e) {
                     System.out.println(e);
                     System.exit(1);
                }
                //System.out.println("\n");
            }
        }
	double sum = 0;
	for (double angle: anglelist){
	    sum += angle;
	}
	double aver;
	aver= sum / anglelist.size();
	System.out.println("The Average Spin is: "+ aver);
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
