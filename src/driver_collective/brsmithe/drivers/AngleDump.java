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
import scipp_ilc.base.util.ScippUtils;

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
	
            //root.init("TH2D","posXY","posXY", "XYPosition", 300, -150, 150, 300, -150, 150);
            //root.init("TH1D","posz","posz", "Z Position", 18000, 0, 25000);
	    root.init("TH2D", "theta", "theta", "Theta Z Position", 8200, 0, 8200, 200, 0, 3);
	    root.init("TH2D", "thetaT","thetaT", "Theta V Z With Trans", 8200, 0, 8200, 200, 0 ,3);
	    root.init("TH2D","angle1","angle1","Theta vs R 3000",4000,0,4000,200,0,3);
	    root.init("TH2D","angle2","angle2","Theta vs R 8000",12000,8000,20000,200,0,3);

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
        for (MCParticle p : event.getMCParticles()) {
            
            int state = p.getGeneratorStatus();
            if(state == MCParticle.FINAL_STATE){
                ParticleType type = p.getType();

                String name = type.getName();
                int id = p.getPDGID();
               
            

                //get endpoint and scale to face
	        double[] org =p.getOrigin().v();
		double[] pos = p.getEndPoint().v();
		double[] momentum = p.getMomentum().v();

		//Get energy
		double energy = p.getEnergy();
		

		//Applying the transform to the momentum
		double[] transm = ScippUtils.transform(momentum,energy);
		
		/*
		 *double theta;
		 *double term1=(momentum[1]*pos[0]-pos[1]*momentum[0]);
		 *double term2=(pos[0]*pos[0]+pos[1]*pos[1]);
		 *theta = term1/term2;
		 *anglelist.add(theta);
		 */

		// First, finding the magnitude squared of the momentum, rsq. Then, finding its root.
		// T indicates it is transformed 
		double rsq = momentum[0]*momentum[0] + momentum[1]*momentum[1] + momentum[2]*momentum[2];
		double arr = Math.sqrt(rsq);
		double rsqT = transm[0]*transm[0] + transm[1]*transm[1] + transm[2]*transm[2];
		double arrT = Math.sqrt(rsqT);
	 
		//Finding the R distance to the endpoint
		double dist = pos[0]*pos[0]+pos[1]*pos[1]+pos[2]*pos[2];
		dist= Math.sqrt(dist);

		// Calclating the transverse angle based on particle momentum.
    
	        double angleT = Math.acos(transm[2]/arrT);
		double angle = Math.acos(momentum[2]/arr);
		
		
                // Focus options
		boolean neglect = false;
		int lower = 0;
		int upper = 10000;
		
		//Defining an upper cutoff. Values past this point are ignored. 
		boolean dozcutoff = true;
		int zcutoff = 8000;
		if(pos[2]>zcutoff && !neglect && dozcutoff){
		    pos[2]=zcutoff;
		}
		boolean dorcutoff = true;
		int rcutoff = 19999;
		if(dist>rcutoff && !neglect && dorcutoff){
		    dist=rcutoff;
		}
		

		// Fill position plot
                try {
		    if(!(neglect && (pos[2]<2800 ||pos[2]>3600)) ){ 
		     //root.fill("posXY",pos[0], pos[1]);
                     //root.fill("posz",pos[2]);
		     root.fill("theta",pos[2],angle);
		     root.fill("thetaT",pos[2],angleT);
		     //If we are in the peak around 3000, we put it in the Angle1 Plot
		     if((pos[2]<3100)&&(pos[2]>2900)){
			 root.fill("angle1",dist,angle);
		     }
		     // If we are in the peak of 8000 and beyond, we put it in the Angle2 Plot
		     if(pos[2]>7900){
			 root.fill("angle2",dist,angle);
		     }
		     
		    }
                }
                catch (java.io.IOException e) {
                     System.out.println(e);
                     System.exit(1);
                }
                //System.out.println("\n");
            }
        }
	/*
	 *double sum = 0;
	 *for (double angle: anglelist){
	 *   sum += angle;
	 *}
	 *
	 *double aver;
	 *aver= sum / anglelist.size();
	 *System.out.println("The Average Spin is: "+ aver);
	 */
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
