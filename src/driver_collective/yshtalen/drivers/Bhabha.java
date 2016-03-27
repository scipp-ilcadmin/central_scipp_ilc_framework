/*
 * Bhabha.java
 *
 * Created on July 11, 2013, 10:45 AM
 * Edited on January 9, 2014, 12:31 AM
 * @author Alex Bogert and Christopher Milke
 * Modified by Jane Shtalenkova on
 * April 22, 2015
 */
package scipp_ilc.drivers;

import scipp_ilc.base.util.PolarCoords;
import scipp_ilc.base.util.LCIOFileManager;
import scipp_ilc.base.util.Jroot;

import org.lcsim.event.EventHeader;
import org.lcsim.event.MCParticle;

import hep.physics.particle.properties.ParticleType;

import org.lcsim.util.Driver;

import java.lang.String;
import java.util.Arrays;

public class Bhabha extends Driver {
	

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
            //root.init("TH1D","posz","posz", "Z Position", 18000, 0, 18000);

	   root.init("TH2D", "T_hit", "Thit", "Theta of E v P, Both Hit", 10000, 0, .02, 10000, 0, 0.02);
	   root.init("TH2D", "T_miss", "Tmiss", "Theta of E v P, Both Miss", 10000, 0, .02, 10000, 0, 0.02);
	   root.init("TH2D", "T_hitmiss", "Thitmiss", "Theta of E v P, Hit/Miss", 10000, 0, .02, 10000, 0, 0.02);

	   root.init("TH2D", "pos_emiss", "pos_emiss", "Scaled Position, HitMiss", 1000, -150, 150, 1000, -150, 150);
	   root.init("TH2D", "pos_pmiss", "pos_pmiss", "Scaled Position, HitMiss", 1000, -150, 150, 1000, -150, 150);
	
	   root.init("TH2D", "HitXY_e", "HitXY_e", "Scaled Position, Hit", 1000, -150, 150, 1000, -150, 150);
	   root.init("TH2D", "HitXY_p", "HitXY_p", "Scaled Position, Hit", 1000, -150, 150, 1000, -150, 150);
	   root.init("TH2D", "MissXY_e", "MissXY_e", "Scaled Position, Miss", 1000, -150, 150, 1000, -150, 150);
	   root.init("TH2D", "MissXY_p", "MissXY_p", "Scaled Position, Miss", 1000, -150, 150, 1000, -150, 150);
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
  //      	System.out.print("Total Photons: " + totg);   
	root.end();
        }
        catch (java.io.IOException e) {
           System.out.println(e);
           System.exit(1);
        }
    }

    //rotate: takes in momentum vector and rotates in xz plane by the crossing angle
    //returns rotated momentum vector
    private double[] rotate (double[] mom, double t){
	//crossing angle
	double[] rmom = new double [3];

	//rotation matrix
	/* 
 *	|cos_t   0   sin_t| |p_x|	
 *	|0       1   0    |*|p_y|= rotated p-vector
 * 	|-sin_t  0   cos_t| |p_z|
 	*/
	rmom[0] = mom[0]*Math.cos(t)+mom[2]*Math.sin(t);
	rmom[1] = mom[1];
	rmom[2] = mom[2]*Math.cos(t)-mom[0]*Math.sin(t);

	return rmom;
    }
    //PROCESS FUNCTION
    //This is where the vast bulk of the program is run and controlled

    public void process( EventHeader event ) {
        super.process(event);
	int hit = 0;
	int g = 0;

	double pos [] = new double [3];
	
	double e_e;
	double pos_e [] = new double [3];
	double e_scx, e_scy;
	double mom_e [] = new double [3];
	
	double p_p;
	double pos_p [] = new double [3];
	double p_scx, p_scy;
	double mom_p [] = new double [3];

	int id, stat;
	double theta_e, theta_p, rad_e, rad_p;
	theta_e=theta_p=rad_e=rad_p = 0;
	MCParticle fs_e = null;
	MCParticle fs_p = null;
	e_scx=e_scy=p_scx=p_scy=e_e=p_p=0;
	
	
	//loop to check if one or both particles hit detector
        for (MCParticle p : event.getMCParticles()){
            //get generator status and PDG id
            stat = p.getGeneratorStatus();
            id = p.getPDGID();
            if(stat == MCParticle.FINAL_STATE){
		
		if(id==22){
			g++;
		}
		//get endpoint, but verify that it exists
		try{
			pos = p.getEndPoint().v();
		}
		catch (RuntimeException ex){
			//System.out.println(ex);
        	}
		

		if(id==11){
			fs_e = p;
			//System.out.println("Parents:\n");
			for(MCParticle r : p.getParents()){
			//	System.out.print(r + "   Type:" + r.getPDGID() + "   Energy:" + r.getEnergy() + "\n");
			}
			e_e = p.getEnergy();
			if (pos[2]<12000){
				hit++;
			}
			
			//get position	
			try{
				pos_e = p.getEndPoint().v();
			}
			catch (RuntimeException ex){
				//System.out.println(ex);
			}
			e_scx = faceZ*pos_e[0]/pos_e[2];
			e_scy = faceZ*pos_e[1]/pos_e[2];
			//get momentum vector
			mom_e = rotate(p.getMomentum().v(), -x_ang);
			//determine radius
			double angles[] = PolarCoords.CtoP(mom_e[0], mom_e[1]);
			//double angles[] = PolarCoords.CtoP(pos_e[0], pos_e[1]);
			rad_e = angles[0];
			theta_e = Math.atan(rad_e/Math.abs(mom_e[2]));
			//theta_e = Math.atan(rad_e/Math.abs(pos_e[2]));
		}
		else if (id==-11){
			fs_p = p;
			p_p = p.getEnergy();
			if (pos[2]>-12000){
				hit++;
			}
			
			//get position
			try{
				pos_p = p.getEndPoint().v();
			}
			catch (RuntimeException ex){
				//System.out.println(ex);
			}
			p_scx = -faceZ*pos_p[0]/pos_p[2];
			p_scy = -faceZ*pos_p[1]/pos_p[2];
			//get momentum vector
			mom_p = rotate(p.getMomentum().v(), x_ang);
			//determine radius
			double angles[] = PolarCoords.CtoP(mom_p[0], mom_p[1]);
			//double angles[] = PolarCoords.CtoP(pos_p[0], pos_p[1]);
			rad_p = angles[0];
			theta_p = Math.atan(rad_p/Math.abs(mom_p[2]));
			//theta_p = Math.atan(rad_p/Math.abs(pos_p[2]));
		}	
	    }
	}
	//switch on hit
	switch (hit) {
	    //both miss
            case 0:  
			try{
				root.fill("Tmiss", theta_e, theta_p);
				root.fill("MissXY_e", e_scx, e_scy);
				root.fill("MissXY_p", p_scx, p_scy);
			}
			catch(java.io.IOException e){
				System.out.println(e);
                		System.exit(1);
			}
                break;
	    //either E or P miss
            case 1:  
		hitmiss++;
		try{	
			/*System.out.println("Electron: " + fs_e);
			if (fs_e.getParents()!= null){
				for(MCParticle p : fs_e.getParents()){
					//System.out.println("Parent: " + p);
					if(p.getDaughters().size()>1){
						System.out.println("	Children: " + p.getDaughters());
					}
				}
			}

			System.out.println("Positron: " + fs_p);
			if (fs_p.getParents()!= null){
				for(MCParticle p : fs_p.getParents()){
					//System.out.println("Parent: " + p);
					if(p.getDaughters().size()>1){
						System.out.println("	Children: " + p.getDaughters());
					}
				}
			}*/
			if(pos_e[2]>12000){
				root.fill("pos_emiss", e_scx, e_scy);
			}
			else{
				root.fill("pos_pmiss", p_scx, p_scy);
			}

			root.fill("Thitmiss", theta_e, theta_p);
		}
		catch(java.io.IOException e){
			System.out.println(e);
			System.exit(1);
		}
                break;
	    //both hit
            case 2:  
		try{
			root.fill("Thit", theta_e, theta_p);
			root.fill("HitXY_e", e_scx, e_scy);
			root.fill("HitXY_p", p_scx, p_scy);
		}
		catch(java.io.IOException e){
			System.out.println(e);
			System.exit(1);
		}
                break;
	}
	System.out.println(eventNumber);
//	System.out.println("Photons this event: " + g);
        totg+=g;
	eventNumber++;
    }//End Process

    /*here all the classwide variables are declared*/
    private int eventNumber, totEvs;
    private int hitmiss=0;
    private int totg = 0;
    private int faceZ = 2950;
    private double x_ang = 0.007;

    private String finals = "";
    //xml derived variables
    private String jrootFile = "";
    
    //variables for jroot file construction and background/signal file reading
    private LCIOFileManager lcioFileMNGR = new LCIOFileManager();
    private Jroot root;

}
