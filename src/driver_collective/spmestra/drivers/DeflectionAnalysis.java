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

public class DeflectionAnalysis extends Driver {
    
    
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
         // Priamary Ptot vs. Theta
         root.init("TH2D","hit_pri_ptotVtheta","hit_pri_ptotVtheta","Total Momentum of Primaries vs. #theta", 350, 0, 3.15, 350, 0, 260.0 );
         // Secondary, Ptot vs. Theta
         root.init("TH2D","hit_sec_ptotVtheta","hit_sec_ptotVtheta","Total Momentum of Secondaries vs. #theta", 350, 0, 3.15, 350, 0, 70.0 );
         // Primary Pxy vs theta 
         root.init("TH2D","hit_pri_pxyVtheta","hit_pri_pxyVtheta","Transvers Momentum of Primaries vs. #theta", 350, 0, 3.15, 350, 0, 5.0 );
         // Secondary Pxy vs Theta
         root.init("TH2D","hit_sec_pxyVtheta","hit_sec_pxyVtheta","Transverse Momentum of Secondaries vs. #theta", 350, 0, 3.15, 350, 0, 2.0 );
         // Electron Vs. Positron Theta
         root.init("TH2D","hit_elec_thetaVposi_theta","hit_elec_thetaVposi_theta","Electron #theta (x) vs. Positron #theta (y)", 350, -.05, .05, 350, 3.12, 3.15 );
       
         // Priamary Ptot vs. Theta
         root.init("TH2D","miss_pri_ptotVtheta","miss_pri_ptotVtheta","Total Momentum of Primaries vs. #theta", 350, 0, 3.15, 350, 0, 260.0 );
         //Secondary, Ptot vs. Theta
         root.init("TH2D","miss_sec_ptotVtheta","miss_sec_ptotVtheta","Total Momentum of Secondaries vs. #theta", 350, 0, 3.15, 350, 0, 70.0 );
         // Primary Pxy vs theta 
         root.init("TH2D","miss_pri_pxyVtheta","miss_pri_pxyVtheta","Transvers Momentum of Primaries vs. #theta", 350, 0, 3.15, 350, 0, 5.0 );
         //Secondary Pxy vs Theta
         root.init("TH2D","miss_sec_pxyVtheta","miss_sec_pxyVtheta","Transverse Momentum of Secondaries vs. #theta", 350, 0, 3.15, 350, 0, 2.0 );
         // Electron Vs. Positron Theta
         root.init("TH2D","miss_elec_thetaVposi_theta","miss_elec_thetaVposi_theta","Electron #theta (x) vs. Positron #theta (y)", 350, -.05, .05, 350, 3.12, 3.15 );


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
         boolean hit = false;
         for (MCParticle p : event.getMCParticles() ) {
            boolean fin_st =(p.getGeneratorStatus()==MCParticle.FINAL_STATE);
            boolean primary = ( p.getParents().size()==0 ||
                                p.getParents().size()==2 );
            if ( primary && fin_st ){
               hit = primaryHit( p );
            }
         }
         for (MCParticle p : event.getMCParticles() ) {
            int ID = p.getPDGID();
            boolean fin_st =(p.getGeneratorStatus()==MCParticle.FINAL_STATE);
            boolean primary = ( p.getParents().size()==0 ||
                                p.getParents().size()==2 );
            boolean neutrino = (ID==12 || ID==14 || ID==16 || ID==18 );
            if( fin_st && !neutrino ){
               double ptot =0; double theta = 0;
               double pxy = magPxPy(p);
               ptot = magPtot(p);
               theta = getTheta(p);
               if( primary ){
                  if (hit){
                     root.fill("hit_pri_ptotVtheta", theta, ptot);
                     root.fill("hit_pri_pxyVtheta", theta, pxy);
                  }
                  if (!hit){
                     root.fill("miss_pri_ptotVtheta", theta, ptot);
                     root.fill("miss_pri_pxyVtheta", theta, pxy);
                  }
                  boolean isElectron = ( ID==11 );
                  boolean isPositron = ( ID==-11); 
                  if ( isElectron ) elec_theta = theta;
                  if ( isPositron ) posi_theta = theta;
               }
               else {
                  if (hit){
                     root.fill("hit_sec_ptotVtheta", theta, ptot);
                     root.fill("hit_sec_pxyVtheta", theta, pxy);
                  } 
                  if (!hit){
                     root.fill("miss_hit_sec_ptotVtheta", theta, ptot);
                     root.fill("miss_hit_sec_pxyVtheta", theta, pxy);
                  }
               }
            }
            p_count++;
            if ( use_limit && (p_count++ > p_count_limit) ) break;    
         }
         System.out.println("e theta: "+elec_theta+", p theta: "+posi_theta);
         if (hit)
            root.fill("hit_elec_thetaVposi_theta", elec_theta, posi_theta);
         if (!hit)
            root.fill("miss_elec_thetaVposi_theta", elec_theta, posi_theta);             
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

   public static boolean primaryHit( MCParticle p ){
      double[] vec = p.getEndPoint().v();
      vec = PolarCoords.ZtoBeamOut(vec[0],vec[1],vec[2]);
      if ( Math.abs( Math.atan(vec[1]/vec[0]) )> .007 ) return true;
      return false;
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
