/*
 * Bhabha.java
 *
 * Edited on April 17,  1:46 AM
 *
 * Author: Jane Shtalenkova on
 * February 18, 2016
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
            
           //theta difference plots
           root.init("TH1D", "Thit", "Thit", "Hit/Hit Theta Difference E-P Transformed", 10000, -0.02, .02);
           root.init("TH1D", "Tmiss", "Tmiss", "Miss/Miss Theta Difference E-P Transformed", 10000,-0.02, 0.02);
           root.init("TH1D", "Thitmiss", "Thitmiss", "Hit/Miss Theta Difference E-P Transformed", 10000, -0.02, 0.02);
           root.init("TH2D", "Thitmiss_cut", "ThitmissCut", "Hit/Miss Theta P v E after cut", 10000, 0.004, 0.02, 10000, 0.004, 0.02);
           
           //these are theta plots using the rotation matrix instead of transform 
          /* root.init("TH2D", "Tm_hit", "Tmhit", "Rotate: Theta of E v P, Both Hit", 10000, 0, .02, 10000, 0, 0.02);
           root.init("TH2D", "Tm_miss", "Tmmiss", "Rotate: Theta of E v P, Both Miss", 10000, 0, .02, 10000, 0, 0.02);
           root.init("TH2D", "Tm_hitmiss", "Tmhitmiss", "Rotate: Theta of E v P, Hit/Miss", 10000, 0, .02, 10000, 0, 0.02);
           */

           //position plots, unscaled and scaled to face
           root.init("TH2D", "pos_emiss", "pos_emiss", "Scaled Position, HitMiss", 1000, -150, 150, 1000, -150, 150);
           root.init("TH2D", "pos_pmiss", "pos_pmiss", "Scaled Position, HitMiss", 1000, -150, 150, 1000, -150, 150);
           root.init("TH2D", "HitXY_e", "HitXY_e", "Scaled Position, Hit", 1000, -150, 150, 1000, -150, 150);
           root.init("TH2D", "HitXY_p", "HitXY_p", "Scaled Position, Hit", 1000, -150, 150, 1000, -150, 150);
           root.init("TH2D", "MissXY_e", "MissXY_e", "Scaled Position, Miss", 1000, -150, 150, 1000, -150, 150);
           root.init("TH2D", "MissXY_p", "MissXY_p", "Scaled Position, Miss", 1000, -150, 150, 1000, -150, 150);
           
           //energy distributions
           root.init("TH1D", "Energy_e", "Energy_e", "Energy Distribution of Hit/Miss Electrons", 10000, 0, 260);
           root.init("TH1D", "Energy_p", "Energy_p", "Energy Distribution of Hit/Miss Electrons", 10000, 0, 260);
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
    
    private double[] transformLorentz(double[] p, double E){
        double theta = x_ang;
        double in_E = 250.0;
        double beta = Math.sin(theta);
        double gamma = Math.pow((1-Math.pow(beta, 2)), -0.5);
        double[] outVec = new double[4];
    
    /*
     * |gamma         -gamma*beta| |p|                       |p'|
     * |-gamma*beta         gamma|*|E| = transformed 4vector |E'|
     *
    */
        outVec[0] = p[0]*gamma - gamma*beta*E;
        outVec[1] = p[1];
        outVec[2] = p[2];
        outVec[3] = E*gamma - gamma*beta*p[0];
        return outVec;       
    
    }    
    //ROTATE: takes in momentum vector and rotates in xz plane by the crossing angle
    //returns rotated momentum vector
    private double[] rotate (double[] mom, double t){
	//crossing angle
	double[] rmom = new double [3];

	//rotation matrix
	/* 
 	|cos_t   0   sin_t| |p_x|	
 	|0       1   0    |*|p_y|= rotated p-vector
  	|-sin_t  0   cos_t| |p_z|
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
        double[] pos = new double[3];
        
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
        double theta_e_mom, theta_p_mom, rad_e_mom, rad_p_mom;
        theta_e=theta_p=rad_e=rad_p = theta_e_mom = theta_p_mom = rad_e_mom = rad_p_mom = 0;
        MCParticle fs_e = null;
        MCParticle fs_p = null;
        e_scx=e_scy=p_scx=p_scy=e_e=p_p=0;
	
	
	    //loop to check if one or both particles hit detector
        for (MCParticle p : event.getMCParticles()){
            //get generator status and PDG id
            stat = p.getGeneratorStatus();
            id = p.getPDGID();
            if(stat == MCParticle.FINAL_STATE){
		
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
                
                //transform momentum 4vector
                double [] lorT_e = transformLorentz(p.getMomentum().v(), p.getEnergy());
                //determine radius
                double angles[] = PolarCoords.CtoP(lorT_e[0], lorT_e[1]);
                //double angles[] = PolarCoords.CtoP(pos_e[0], pos_e[1]);
                rad_e = angles[0];
                theta_e = Math.atan(rad_e/Math.abs(lorT_e[2]));
                //theta_e = Math.atan(rad_e/Math.abs(pos_e[2]));

                //getMom
                mom_e = rotate(p.getMomentum().v(), -0.007);
                //determine radius
                double angles_mom[] = PolarCoords.CtoP(mom_e[0], mom_e[1]);
                //double angles[] = PolarCoords.CtoP(pos_e[0], pos_e[1]);
                rad_e_mom = angles_mom[0];
                theta_e_mom = Math.atan(rad_e_mom/Math.abs(mom_e[2]));
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
                
                //transform momentum 4vector
                double [] lorT_p = transformLorentz(p.getMomentum().v(), p.getEnergy());
                //determine radius
                double angles[] = PolarCoords.CtoP(lorT_p[0], lorT_p[1]);
                //double angles[] = PolarCoords.CtoP(pos_p[0], pos_p[1]);
                rad_p = angles[0];
                theta_p = Math.atan(rad_p/Math.abs(lorT_p[2]));
                //theta_p = Math.atan(rad_p/Math.abs(pos_p[2]));
                
                //getMom
                mom_p = rotate(p.getMomentum().v(), 0.007);
                //determine radius
                double angles_mom[] = PolarCoords.CtoP(mom_p[0], mom_p[1]);
                //double angles[] = PolarCoords.CtoP(pos_e[0], pos_e[1]);
                rad_p_mom = angles_mom[0];
                theta_p_mom = Math.atan(rad_p_mom/Math.abs(mom_p[2]));
                }	
	        }
	    }
        }
        //switch on hit
        switch (hit) {
            //both miss
            case 0:  
                try{
                    root.fill("Tmiss", (theta_e - theta_p));
                    //root.fill("Tmmiss", theta_e_mom, theta_p_mom);

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
                    //fill energy distribution plots for hit/miss electrons and positrons	
                    root.fill("Energy_e", fs_e.getEnergy());
                    root.fill("Energy_p", fs_p.getEnergy());

                    //fill scaled-to-face hit/miss position plots
                    if(pos_e[2]>12000){
                        root.fill("pos_emiss", e_scx, e_scy);
                    }
                    else{
                        root.fill("pos_pmiss", p_scx, p_scy);
                    }

                    //make a cut on the angle difference between e and p and plot remaining points
                    //these are final state e/p that are no back-to-back
                    //angle chosen: 0.005 rad
                    double tDiff = (theta_e-theta_p);
                    if(Math.abs(tDiff)>cut){
                        root.fill("Thitmiss_cut", tDiff);
                    }
                }
                catch(java.io.IOException e){
                    System.out.println(e);
                    System.exit(1);
                }
                break;
            //both hit
                case 2:  
                try{
                    root.fill("Thit", (theta_e - theta_p));
                    //root.fill("Tmhit", theta_e_mom, theta_p_mom);
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
	    eventNumber++;
        }//End Process
    

    /*here all the classwide variables are declared*/
    private int eventNumber, totEvs;
    private int hitmiss=0;
    private int totg = 0;
    private int faceZ = 2950;
    private double cut = 0.005;
    private double x_ang = 0.007;

    private String finals = "";
    //xml derived variables
    private String jrootFile = "";
    
    //variables for jroot file construction and background/signal file reading
    private LCIOFileManager lcioFileMNGR = new LCIOFileManager();
    private Jroot root;

}
