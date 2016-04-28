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
import scipp_ilc.base.util.ScippUtils;

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
            

           //PLOTS 
           //naming convention: content_hitType_axes
           
           //theta difference plots
           root.init("TH1D", "T_hitDiff", "T_hitDiff", "Hit/Hit Theta Difference E-P", 1000, -0.02, 0.02);
           root.init("TH1D", "T_hitDiff_c1", "T_hitDiff_c1", "Hit/Hit Theta Difference E-P tDiff<0.001", 1000, -0.02, 0.02);
           root.init("TH1D", "T_hitDiff_c2", "T_hitDiff_c2", "Hit/Hit Theta Difference E-P tDiff<0.0005", 1000, -0.02, 0.02);
           root.init("TH2D", "T_2hit_PvE", "T_2hit_PvE", "Hit/Hit Theta P v E Transformed", 10000, 0.0, 0.02, 10000, 0.0, 0.02);
           //root.init("TH1D", "Thitmiss", "Thitmiss", "Hit/Miss Theta Difference E-P Transformed", 10000, -0.02, 0.02);
           root.init("TH2D", "T_2hit_cut_PvE", "T_2hit_cut_PvE", "Hit/Hit Theta P v E after cut", 10000, 0.0, 0.02, 10000, 0.0, 0.02);
           root.init("TH2D", "T_2hit_cutc_PvE", "T_2hit_cutc_PvE", "Hit/Hit Theta P v E after cut complement", 10000, 0.0, 0.02, 10000, 0.0, 0.02);
           root.init("TH2D", "T_hit_PvE", "T_hit_PvE", "Hit/Miss Theta P v E after cut", 10000, 0.0, 0.2, 10000, 0.0, 0.2);

        
           //position plots, unscaled and scaled to face
           root.init("TH2D", "pos_emiss", "pos_emiss", "Scaled Position, HitMiss", 1000, -150, 150, 1000, -150, 150);
           root.init("TH2D", "pos_pmiss", "pos_pmiss", "Scaled Position, HitMiss", 1000, -150, 150, 1000, -150, 150);
           root.init("TH2D", "HitXY_e", "HitXY_e", "Scaled Position, Hit", 1000, -50, 50, 1000, -50, 50);
           root.init("TH2D", "HitXY_p", "HitXY_p", "Scaled Position, Hit", 1000, -50, 50, 1000, -50, 50);
           root.init("TH2D", "MissXY_e", "MissXY_e", "Scaled Position, Miss", 1000, -150, 150, 1000, -150, 150);
           root.init("TH2D", "MissXY_p", "MissXY_p", "Scaled Position, Miss", 1000, -150, 150, 1000, -150, 150);
           

           //energy distributions
           root.init("TH2D", "EvT_2hit_E", "EvT_2hit_E", "Energy v Theta of Hit/Hit Electrons", 1000, 0, 0.02, 1000, 0, 260);
           root.init("TH2D", "EvT_2hit_P", "EvT_2hit_P", "Energy v Theta of Hit/Hit Positrons", 1000, 0, 0.02, 1000, 0, 260);
           root.init("TH2D", "EvT_2hit_cut_E", "EvT_2hit_cut_E", "Energy v Theta of Hit/Hit Electrons after cut", 1000, 0, 0.02, 1000, 0, 260);
           root.init("TH2D", "EvT_2hit_cut_P", "EvT_2hit_cut_P", "Energy v Theta of Hit/Hit Positrons after cut", 1000, 0, 0.02, 1000, 0, 260);
           root.init("TH2D", "EvT_2hit_cutc_E", "EvT_2hit_cutc_E", "Energy v Theta of Hit/Hit Electrons after cut complement", 1000, 0, 0.02, 1000, 0, 260);
           root.init("TH2D", "EvT_2hit_cutc_P", "EvT_2hit_cutc_P", "Energy v Theta of Hit/Hit Positrons after cut complement", 1000, 0, 0.02, 1000, 0, 260);

           root.init("TH2D", "E_2hit_PvE", "E_2hit_PvE", "Energy Hit/Hit P v E", 1000, 0.0, 260, 1000, 0.0, 260);
           root.init("TH2D", "E_2hit_cut_PvE", "E_2hit_cut_PvE", "Energy Hit/Hit P v E after cut", 1000, 0.0, 260, 1000, 0.0, 260);
           root.init("TH2D", "E_2hit_cutc_PvE", "E_2hit_cutc_PvE", "Energy Hit/Hit P v E after cut complement", 1000, 0.0, 260, 1000, 0.0, 260);
           root.init("TH2D", "E_hit_PvE", "E_hit_PvE", "Energy Hit/Miss P v E ", 1000, 0.0, 260, 1000, 0.0, 260);
           root.init("TH2D", "EvT_hit_E", "EvT_hit_E", "Energy v Theta of Hit/Miss Electrons", 1000, 0, 0.02, 1000, 0, 260);
           root.init("TH2D", "EvT_hit_P", "EvT_hit_P", "Energy v Theta of Hit/Miss Positrons", 1000, 0, 0.02, 1000, 0, 260);

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
            //cut results
            System.out.println(cut1count + " events remaining after tDiff < " + cut1 + " cut.");
            double ratio1 = 100*(1-(cut1count/3085));
            System.out.println(ratio1 + " percent hithit events remaining.");
            System.out.println(cut2count + " events remaining after tDiff < " + cut2 + " cut.");
            double ratio2 = 100*(1-(cut2count/3085));
            System.out.println(ratio2 + " percent hithit events remaining.");
            //.cxx edits
	        root.end();
        }
        catch (java.io.IOException e) {
           System.out.println(e);
           System.exit(1);
        }
    }


    
    // TRANSFORM: takes in momentum vector and Energy double
    // returns Lorentz transformed 4 element {momentum[], energy}  array
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
    
    
    /*    
    / /ROTATE: takes in momentum vector and rotates in xz plane by the crossing angle
    //returns rotated momentum vector
    private double[] rotate (double[] mom, double t){
	//crossing angle
	double[] rmom = new double [3];

	//rotation matrix
	/* 
 	|cos_t   0   sin_t| |p_x|	
 	|0       1   0    |*|p_y|= rotated p-vector
  	|-sin_t  0   cos_t| |p_z|
 	
	rmom[0] = mom[0]*Math.cos(t)+mom[2]*Math.sin(t);
	rmom[1] = mom[1];
	rmom[2] = mom[2]*Math.cos(t)-mom[0]*Math.sin(t);

	return rmom;
    }
    */




    //PROCESS FUNCTION
    //This is where the vast bulk of the program is run and controlled

    public void process( EventHeader event ) {
        super.process(event);
        
        int hit = 0;
        
        double e_e;
        double [] T_e  = new double [4];;
        double [] pos_e  = new double [3];
        double e_scx, e_scy;
        double [] mom_e = new double [3];
        
        double p_p;
        double [] T_p = new double [4];;
        double [] pos_p  = new double [3];
        double p_scx, p_scy;
        double [] mom_p  = new double [3];

        int id, stat;
        double theta_e, theta_p, rad_e, rad_p;
        //double theta_e_mom, theta_p_mom, rad_e_mom, rad_p_mom;
        theta_e=theta_p=rad_e=rad_p = 0;
        MCParticle fs_e = null;
        MCParticle fs_p = null;
        e_scx=e_scy=p_scx=p_scy=e_e=p_p=0;
	        
        
	
	    //loop to check if one or both particles hit detector
        for (MCParticle p : event.getMCParticles()){
            //get generator status and PDG id
            stat = p.getGeneratorStatus();
            if(stat == MCParticle.FINAL_STATE){
		
                if(p.getPDGID()==11){
                    System.out.println("Electron");
                    
                    //get position and momentum	
                    try{
                        pos_e = p.getEndPoint().v();
                        mom_e = p.getMomentum().v();
                    }
                    catch (RuntimeException ex){
                        System.out.println(ex);
                    }
                    
                    //get energy
                    e_e = p.getEnergy();

                    //if electron hit detector, add to hit count
                    if (pos_e[2]< 12000){
                        hit++;
                    }

                    //scale to face
                    e_scx = faceZ*pos_e[0]/pos_e[2];
                    e_scy = faceZ*pos_e[1]/pos_e[2];

                    //transform momentum 4vector
                    T_e = ScippUtils.transform(mom_e, e_e);
                    //determine radius
                    double angles[] = PolarCoords.CtoP(T_e[0], T_e[1]);
                    rad_e = angles[0];
                    theta_e = Math.atan(rad_e/Math.abs(T_e[2]));
                    
                }

                else if (p.getPDGID()==-11){
                    System.out.println("Positron");

                    //get position and momentum
                    try{
                        pos_p = p.getEndPoint().v();
                        mom_p = p.getMomentum().v();
                    }
                    catch (RuntimeException ex){
                        System.out.println(ex);
                    }
                    //get energy
                    p_p = p.getEnergy();

                    //if positron hit detector, add to hit count
                    if (pos_p[2]> -12000){
                        hit++;
                    }

                    //scale to face
                    p_scx = faceZ*pos_p[0]/pos_p[2];
                    p_scy = faceZ*pos_p[1]/pos_p[2];

                    
                    //transform momentum 4vector
                    T_p = ScippUtils.transform(mom_p, p_p);
                    //determine radius
                    double angles[] = PolarCoords.CtoP(T_p[0], T_p[1]);
                    rad_p = angles[0];
                    theta_p = Math.atan(rad_p/Math.abs(T_p[2]));
                    
                }	
        
	        }//end FINAL_STATE
        }//end for loop through events

        //switch on hit
        switch (hit) {
            //both miss
            case 0: //DONT CARE ABOUT MISS/MISS EVENTS  
                /*try{
                    root.fill("Tmiss", (theta_e - theta_p));
                    //root.fill("Tmmiss", theta_e_mom, theta_p_mom);

                    root.fill("MissXY_e", e_scx, e_scy);
                    root.fill("MissXY_p", p_scx, p_scy);
                }
                catch(java.io.IOException e){
                    System.out.println(e);
                            System.exit(1);
                }*/
                    break;
            //either E or P miss
            case 1:  
            hitmiss++;
                try{
                    //fill energy distribution plots for hit/miss electrons and positrons	
                    root.fill("T_hit_PvE", theta_e, theta_p);
                    root.fill("E_hit_PvE", e_e, p_p);
                    root.fill("EvT_hit_E", theta_e, e_e);
                    root.fill("EvT_hit_P", theta_p, p_p);

                    //fill scaled-to-face hit/miss position plots
                    if(pos_e[2]>12000){
                        root.fill("pos_emiss", e_scx, e_scy);
                    }
                    else{
                        root.fill("pos_pmiss", p_scx, p_scy);
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

                    root.fill("T_2hit_PvE", theta_e, theta_p);
                    //theta positron v electron
                    root.fill("E_2hit_PvE", e_e, p_p);
                   
                    //energy v theta electrons and positrons separately
                    root.fill("EvT_2hit_E", theta_e, e_e);
                    root.fill("EvT_2hit_P", theta_p, p_p);

                    //make a cut on the angle difference between e and p and plot remaining points
                    //these are final state e/p that are no back-to-back
                    //angle chosen: 0.005 rad
                    double tDiff = (theta_e - theta_p);
                    root.fill("T_hitDiff", tDiff);
                    
                    if(Math.abs(tDiff)<cut1){
                        //add to cut count for fraction events counts
                        cut1count++;
                        root.fill("T_hitDiff_c1", tDiff);
                        //fill cut theta + energy plots
                        root.fill("T_2hit_cut_PvE", theta_e, theta_p);
                        root.fill("E_2hit_cut_PvE", e_e, p_p);

                        root.fill("EvT_2hit_cut_E", theta_e, e_e);
                        root.fill("EvT_2hit_cut_P", theta_p, p_p);
                        //fill scaled position of these electrons and positrons
                        root.fill("HitXY_e", e_scx, e_scy);
                        root.fill("HitXY_p", p_scx, p_scy);
                    }
                    else if(Math.abs(tDiff)<cut2){
                        cut2count++;
                        root.fill("T_hitDiff_c2", tDiff);
                    }    
                    else{
                        root.fill("T_2hit_cutc_PvE", theta_e, theta_p);
                        root.fill("E_2hit_cutc_PvE", e_e, p_p);

                        root.fill("EvT_2hit_cutc_E", theta_e, e_e);
                        root.fill("EvT_2hit_cutc_P", theta_p, p_p);
                    }
                    //root.fill("Tmhit", theta_e_mom, theta_p_mom);

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
    private int eventNumber;
    private int cut1count, cut2count;
    private int hitmiss=0;
    private int faceZ = 2950;
    private double cut1 = 0.001;
    private double cut2 = 0.0005;
    private double x_ang = 0.007;

    private String finals = "";
    //xml derived variables
    private String jrootFile = "";
    
    //variables for jroot file construction and background/signal file reading
    private LCIOFileManager lcioFileMNGR = new LCIOFileManager();
    private Jroot root;

}
