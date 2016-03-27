/*
 * MomentumAnalysis.java
 *
 * Created on 24 Jan 16
 * @author Spenser Estrada
 *
 */
package scipp_ilc.drivers;

import scipp_ilc.base.util.LCIOFileManager;
import scipp_ilc.base.util.PolarCoords;
import scipp_ilc.base.util.Jroot;

import org.lcsim.event.EventHeader;
import org.lcsim.event.MCParticle;
import org.lcsim.event.SimCalorimeterHit;

import org.lcsim.util.Driver;
import org.lcsim.util.Driver.NextEventException;

import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.ListIterator;
import java.util.Collections;

import java.lang.String;

public class MomentumAnalysis extends Driver {
    
    
    //DEFINE XML FUNCTIONS
    //These functions are specially formatted functions to pull variable data from the xml file
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
 
    public void setBeamOutAligned(boolean algn) {
        aligned = algn;
    }
     
    //END DEFINE XML FUNCTIONS

    //This function is called when the program is first started
    //and initializes all persistent data
    public void startOfData() {
        eventNumber = 0;
        String root_mode = "NEW";
        
        try {
            root = new Jroot(jrootFile,root_mode);

            root.init("TH2D","theta_rad_pos","theta_rad_pos","Theta of Radius (pi)", 350, 0, 150, 350, .99, 1.0);        
            root.init("TH2D","theta_rad_neg","theta_rad_neg","Theta of Radius (pi)", 350, 0, 150, 350, 0, .01);

            root.init("TH2D","PxPy_Pz","PxPy_Pz","XY Momenta of Z Momentum", 350, 0, 260, 350, 0, 4);
            root.init("TH2D","theta_phi_pos","theta_phi_pos","Theta of Phi (pi)", 350, -1, 1, 350, .99, 1.0);
            root.init("TH2D","theta_phi_neg","theta_phi_neg","Theta of Phi (pi)", 350, -1, 1, 350, 0, .01);

        } catch(java.io.IOException e) {
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

    //PROCESS FUNCTION
    //This is where the vast bulk of the program is run and controlled
    public void process( EventHeader event ) { 
    // care not about these hits, only particles
        super.process( event );
        
        //int check_layer = 0;
        int p_count_limit = 1000;
        boolean use_limit = false;
        boolean reject_negative = false;
      
        // find some sort of cap on these
        double maxPxy= 0;  double maxPz=0;
        double maxTheta = 0; double maxPhi=0;
        int p_count = 0;
        int final_particle_count = 0;
        try {
            for (MCParticle p : event.getMCParticles() ) {
                int ID = p.getPDGID();
                int state = p.getGeneratorStatus() ;
                if(state==MCParticle.FINAL_STATE) final_particle_count++;
                if( (ID==11 || ID==-11)&& state==MCParticle.FINAL_STATE ){
                   double[] vec = p.getEndPoint().v();
                   vec = PolarCoords.ZtoBeamOut(vec[0],vec[1],vec[2]); 
                   vec = toRadial(vec[0],vec[1],vec[2]);
                   double pxy = magPxPy(p);
                   double pz  = p.getPZ();
                   if (vec[1] >.5 ){
                      root.fill("theta_rad_pos",vec[2],vec[1]);            
                      root.fill("theta_phi_pos",vec[0],vec[1]);
                   }else {
                      root.fill("theta_rad_neg",vec[2],vec[1]);
                      root.fill("theta_phi_neg",vec[0],vec[1]);

                   }
                   root.fill("PxPy_Pz",pz,pxy);
                   printParents( p );
                   p_count++;
                }
                if ( use_limit && (p_count++ > p_count_limit) ) break;    
            }
        } catch(java.io.IOException e) {
            System.out.println(e);
            System.exit(1);
        } 
        System.out.println("Tot Final State Particles: "+final_particle_count);
        System.out.println("======finished event "+ ++eventNumber +"======");        

    }//End Process

    public static double magPxPy(MCParticle p){
       double px = p.getPX(); double py = p.getPY();
       double sum = Math.pow(px, 2) + Math.pow(py, 2);
       return Math.pow(sum, .5);
    }

    public static void printParents(MCParticle p){
       System.out.println(" Daughter is: "+p.getType().getName() );
       //System.out.println( p );
       int i = 1;
       for ( MCParticle q : p.getParents() ){
       // System.out.println("   Its parent "+i+" is: "+q.getType().getName() );
       //   System.out.println("   "+ q );
       //   System.out.println("   State: "+q.getGeneratorStatus() );
          i++;
       }
       System.out.println("Has "+(i-1)+" parents");
       System.out.println();
    }

    // cartesian to polar/spherical. Use phi, r, z for cylindrical.
    // Use phi, theta, r for spherical
    // Angles are in multiple of PI
    public static double[] toRadial(double x, double y, double z){
        //double [] vec;
        double r = Math.sqrt( Math.pow(x,2)+Math.pow(y,2) );
        double phi = Math.atan2(y, x)/Math.PI ;
        double theta = Math.atan2(r, z)/Math.PI;
        double[] vec = {phi, theta, r, z};
        return vec;
    }
    

    /*here all the classwide variables are declared*/
    private int eventNumber;


    //xml derived variables
    private String jrootFile = "";

    //variables for jroot file construction and background/signal file reading
    private LCIOFileManager lcioFileMNGR = new LCIOFileManager();
    private Jroot root;
    private boolean aligned;
}
