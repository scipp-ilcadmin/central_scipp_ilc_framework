/*
 * TransversMomentumAnalysis.java
 *
 * Created on 6 Feb 16
 * @author Spenser Estrada
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

public class TransverseMomentumAnalysis extends Driver {
    
    
   //DEFINE XML FUNCTIONS
   //These functions are specially formatted functions to pull variable data
   //from the xml file
   /**************************************************************************
      XML FUNCTION FORMAT

   public void //setVariablename(variable type) { 
      //the first letter after "set" must be uppercase
      //but can (must?) be lowercase in xml file
      set variable here; 
   }
   **************************************************************************/

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
         // Non-Primary, c_theta < .9
         root.init("TH1D","sec_loCosThet","sec_loCosThet","Transverse Momenta of Secondaries, Low cos#theta ", 350, 0, 10 );
         // Primary, c_theta < .9
         root.init("TH1D","pri_loCosThet","pri_loCosThet","Transverse Momenta of Primaries, Low cos#theta ", 350, 0, 10 );
         // Non-primary, c_theta > .9
         root.init("TH1D","sec_hiCosThet","sec_hiCosThet","Transverse Momenta of Secondaries, High cos#theta ", 350, 0, 10 );
         // Primary, c_theta > .9
         root.init("TH1D","pri_hiCosThet","pri_hiCosThet","Transverse Momenta of Primaries, High cos#theta ", 350, 0, 10 );
         // Priamary Ptot vs. Theta
         root.init("TH2D","pri_ptotVtheta","pri_ptotVtheta","Total Momentum of Primaries vs. #theta", 350, 0, 3.15, 350, 0, 260.0 );
         // Secondary, Ptot vs. Theta
         root.init("TH2D","sec_ptotVtheta","sec_ptotVtheta","Total Momentum of Secondaries vs. #theta", 350, 0, 3.15, 350, 0, 70.0 );
         // Primary Pxy vs theta 
         root.init("TH2D","pri_pxyVtheta","pri_pxyVtheta","Transvers Momentum of Primaries vs. #theta", 350, 0, 3.15, 350, 0, 5.0 );
         // Secondary Pxy vs Theta
         root.init("TH2D","sec_pxyVtheta","sec_pxyVtheta","Transverse Momentum of Secondaries vs. #theta", 350, 0, 3.15, 350, 0, 2.0 );
         // Electron Vs. Positron Theta
         root.init("TH2D","elec_thetaVposi_theta","elec_thetaVposi_theta","Electron #theta (x) vs. Positron #theta (y)", 350, -.05, .05, 350, 3.12, 3.15 );
       } catch(java.io.IOException e) {
          System.out.println(e);
          System.exit(1);
       }
    }

    // This function is called after all file runs have finished,
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

   // PROCESS FUNCTION
   // This is where the vast bulk of the program is run and controlled
   public void process( EventHeader event ) { 
      super.process( event );
      int p_count_limit = 1000;
      boolean use_limit = false;
      double maxPxy= 0;  double maxPz=0;
      int p_count = 0;
      double totPxy_sec_hiCosThet = 0;
      double totPxy_pri_hiCosThet = 0;
      double totPxy_sec_loCosThet = 0;
      double totPxy_pri_loCosThet = 0;
      double elec_theta = 0; double posi_theta = 0;
      try {
         //System.out.println(
         for (MCParticle p : event.getMCParticles() ) {
            int ID = p.getPDGID();

            // block of stuff for comparing to stdhep analysis
            double x, y, z, mag, En;
            x = p.getPX(); y = p.getPY(); z = p.getPZ();
            mag = p.getMomentum().magnitude();
            En = p.getEnergy();
            System.out.println("ID: "+ID);
            System.out.printf("(%.3f, %.3f, %.3f) \n", x, y, z);
            System.out.println("Pmag: "+mag);
            System.out.println("E: "+En);
            // End of stuff

            boolean fin_st =(p.getGeneratorStatus()==MCParticle.FINAL_STATE);
            boolean primary = ( p.getParents().size()==0 ||
                                p.getParents().size()==2 );
            boolean neutrino = (ID==12 || ID==14 || ID==16 || ID==18 );
            //double elec_theta; double posiTheta;
            if( fin_st && !neutrino ){
               double ptot =0; double theta = 0;
               boolean loCos = cosTheta( p, min_theta );
               //System.out.println("loCos: "+loCos);
               double pxy = magPxPy(p);
               System.out.println("Pxy: "+pxy);
               ptot = magPtot(p);
               theta = getTheta(p);
               //System.out.println("Ptot: "+ptot+" Theta: "+theta);
               if( primary ){
                  if ( loCos ) totPxy_pri_loCosThet += pxy;
                  else totPxy_pri_hiCosThet += pxy;
                  root.fill("pri_ptotVtheta", theta, ptot);
                  root.fill("pri_pxyVtheta", theta, pxy);
                  boolean isElectron = ( ID==11 );
                  boolean isPositron = ( ID==-11); 
                  if ( isElectron ) elec_theta = theta;
                  if ( isPositron ) posi_theta = theta;
                  System.out.println("theta: "+theta+" ptot: "+ptot);
                  //System.out.println("elec_theta: "+theta+" ptot: "+ptot);
               }
               else {
                  if (loCos ) totPxy_sec_loCosThet += pxy;
                  else totPxy_sec_hiCosThet += pxy;
                  root.fill("sec_ptotVtheta", theta, ptot);
                  root.fill("sec_pxyVtheta", theta, pxy);
               }
            }
            p_count++;
            if ( use_limit && (p_count++ > p_count_limit) ) break;    
         }
         System.out.println("e theta: "+elec_theta+", p theta: "+posi_theta);
         root.fill("pri_loCosThet", totPxy_pri_loCosThet );
         root.fill("pri_hiCosThet", totPxy_pri_hiCosThet );
         root.fill("sec_loCosThet", totPxy_sec_loCosThet );
         root.fill("sec_hiCosThet", totPxy_sec_hiCosThet );
         root.fill("elec_thetaVposi_theta", elec_theta, posi_theta);

      } catch(java.io.IOException e) {
         System.out.println(e);
         System.exit(1);
      } 
      System.out.println("finished event "+ ++eventNumber);        

   }//End Process

   public static double magPxPy(MCParticle p){
      double px = p.getPX(); double py = p.getPY();
      double sum = Math.pow(px, 2) + Math.pow(py, 2);
      return Math.pow(sum, .5);
   }

   public static double magPtot(MCParticle p){
      double px = p.getPX(); double py = p.getPY(); double pz = p.getPZ();
      double sum = Math.pow(px, 2) + Math.pow(py, 2) + Math.pow(pz, 2); 
      return Math.pow(sum, .5);
   }
   
   public static double getTheta(MCParticle p){
      double x = p.getPX(); double y = p.getPY(); double z = p.getPZ();
      double r = Math.sqrt( Math.pow(x,2)+Math.pow(y,2)+Math.pow(z,2) );
      return Math.abs( Math.acos(z/r) ) ;
   }


   // tests if particle final position is more than a certain cos(theta)
   // away from the center line (z axis)
   public static boolean cosTheta(MCParticle p, double cos_t ){
      double x = p.getPX(); double y = p.getPY(); double z = p.getPZ();
      double r = Math.sqrt( Math.pow(x,2)+Math.pow(y,2)+Math.pow(z,2) );
      System.out.println("z: "+z+" r: "+r+" z/r: "+(z/r) );
      return ( Math.abs(z/r) < cos_t );
   }
    

   /*here all the classwide variables are declared*/
   private int eventNumber;
   private double min_theta = .9;

   //xml derived variables
   private String jrootFile = "";

   //variables for jroot file construction and background/signal file reading
   private LCIOFileManager lcioFileMNGR = new LCIOFileManager();
   private Jroot root;
   private boolean aligned;
}
