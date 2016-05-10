/*
 * Created on 2 May 16
 * by Spenser Estrada
 *
 */
package scipp_ilc.drivers;

import scipp_ilc.base.util.Jroot;

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
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;

import hep.io.stdhep.*;
import hep.io.xdr.XDRInputStream;

public class StdhepMomenta extends Driver {
    
    
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

   public void setStdhepfilelist(String filename) {
      try {
         BufferedReader br = new BufferedReader(new FileReader(filename));
         String line;
         while ((line = br.readLine()) != null) {
            this.stdhepfilelist.add(line);
         }
      }
      catch (java.io.IOException e) {
         System.out.println(e);
         System.exit(1);
      }
   }

    
   //END DEFINE XML FUNCTIONS




   //This function is called when the program is first started
   //and initializes all persistent data
   public void startOfData() {
      eventNumber = 0;
      //String root_mode = "NEW";
      System.out.println("\n\nHELLOOOOO START\n\n");
        
      try {
         /* Differentiate output by 
          * 1) which HE particle measuring Q of 
          *    both: "prVQ", e-: "prVeQ", e+: "prVpQ
          * 2) Total invariant mass (M)
          *    >= 2GeV: "hiM", <2GeV: "loM"
          */
         String path = jrootFile;        
 
         fw_scal_Ptot = new FileWriter(path+"_scal_Ptot"); 
         fw_vec_Ptot = new FileWriter(path+"_vec_Ptot");    
         fw_M = new FileWriter(path+"_M");    
        
         fw_scal_Ptot_loAngle = new FileWriter(path+"_scal_Ptot_loAngle");
         fw_vec_Ptot_loAngle = new FileWriter(path+"_vec_Ptot_loAngle");
         fw_M_loAngle = new FileWriter(path+"_M_loAngle");

         //file process loop
         int total = 0;
         int limit = 2000000;
         for(String filename: stdhepfilelist) {
            StdhepReader reader = new StdhepReader(filename);
            for (int i=0;i<reader.getNumberOfEvents();i++) {
               StdhepRecord record = reader.nextRecord();
               if (record instanceof StdhepEvent) {
                  StdhepEvent event = (StdhepEvent) record;
                  //do stuff with event
                  analyze(event);
               }
               if (total++ > limit) break;
            }
            if (total > limit) break;
         } 
         //root.end();
         fw_scal_Ptot.close();
         fw_vec_Ptot.close();
         fw_M.close();
         fw_scal_Ptot_loAngle.close();
         fw_vec_Ptot_loAngle.close();
         fw_M_loAngle.close();

      } catch (java.io.IOException e) {
         System.out.println(e);
         System.exit(1);
      }
   }

   public void analyze(StdhepEvent event){
      // gets number of particles in event
      int n = event.getNHEP();
      
     /* Values of interest for this driver are
      * scal_Ptot: Sum of P as |P_1|+|P_2|+...+|P_n|
      * vec_Ptot: Sum of P as |P_1+P_2+...+P_n|
      * M: Invariant mass as 
      * (E_1^2+...+E_n^2 - (Px_1^2+...+Px_n^2 + same for Py, Pz) )
      */ 
      double scal_Ptot = 0;
      double[] vec_Ptot = {0, 0};
      double Etot = 0;
      double M = 0;

      // Low angle particles where cos(theta)<.9
      double scal_Ptot_loAngle = 0;
      double[] vec_Ptot_loAngle = {0, 0};
      double Etot_loAngle = 0;
      double M_loAngle = 0;      

      try{
         int HE_elec_ind = -1; int HE_posi_ind =-1;
         double HE_elec_En = 0; double HE_posi_En = 0;
         // Find highest-energy final-state electron and positron
         // These are the initial electron/positron
         for ( int p = 0; p<n; p++){
            double En = event.getPHEP(p, 3);
            int ID = event.getIDHEP( p );
            boolean finState = ( event.getISTHEP( p ) == FINAL_STATE);
            if( ID==11 && En>HE_elec_En && finState ){
               HE_elec_En = En;
               HE_elec_ind = p;
            }
            if( ID==-11 && En>HE_posi_En && finState ){
               HE_posi_En = En;
               HE_posi_ind = p;
            }
         }
         for ( int p = 0; p<n; p++){
            int ID = event.getIDHEP( p );                           
            boolean finState = ( event.getISTHEP( p ) == FINAL_STATE);
            
            // Momentum and energy values
            double x = event.getPHEP(p, 0); 
            double y = event.getPHEP(p, 1); 
            double z = event.getPHEP(p, 2);
            double[] P = {x,y,z};
            double En = event.getPHEP(p, 3);
            double cosAngle = getCosAngle(x,y,z);
            
            boolean isNeutrino = (ID==12 || ID==14 || ID==16 || ID==18 );
            if( finState ){
               if( p!=HE_elec_ind && p!=HE_posi_ind ){
                  double[] Pt = {x,y};
                  if(!isNeutrino && (cosAngle <.9) ){
                     //System.out.println( getMag( Pt ) );
                     scal_Ptot_loAngle+= getMag( Pt );
                     vec_Ptot_loAngle = addVec(vec_Ptot_loAngle, Pt );
                     M_loAngle += getM(x,y,z,En);
                  }  
                  scal_Ptot += getMag( Pt );
                  vec_Ptot = addVec(vec_Ptot, Pt );
                  M += getM(x,y,z,En);
               }
            }
         }
         
         // Relativity-invariant mass of particles not HE e-||e+
         double vec_Ptot_mag = getMag( vec_Ptot );
         double vec_Ptot_mag_loAngle = getMag( vec_Ptot_loAngle );
         
         // writing results
         fw_scal_Ptot.write( scal_Ptot+";\n" );
         fw_vec_Ptot.write( vec_Ptot_mag+";\n" );
         fw_M.write( M+";\n" );
            
         fw_scal_Ptot_loAngle.write( scal_Ptot_loAngle+";\n" );
         fw_vec_Ptot_loAngle.write( vec_Ptot_mag_loAngle+";\n" );
         fw_M_loAngle.write( M_loAngle+";\n" );
 
      } catch(java.io.IOException e) {
         System.out.println(e);
         System.exit(1);
      }
   }

   public static double getM(double x, double y, double z, double En){
       return Math.sqrt( En*En - (x*x+y*y+z*z) );
   }

   public static double getMag( double[] P ){
       double sum = 0;
       for( int i=0; i<P.length; i++){
          sum+= (P[i]*P[i]);
       }
       return Math.sqrt( sum );
   }

   public static double getCosAngle(double x, double y, double z){
       double r = Math.sqrt(x*x+y*y+z*z);
       return z/r;
   }
 
   public static double[] addVec(double[] u, double[] v){
      double[] s = new double[ u.length]; 
      for( int i=0; i< u.length; i++){
         s[i] = u[i] + v[i];
      }
      return s;
   }

   // Generator Statuses
   public static final int DOCUMENTATION = 3;
   public static final int FINAL_STATE = 1;
   public static final int INTERMEDIATE = 2;
 
   /*here all the classwide variables are declared*/
   private int eventNumber;

   //xml derived variables
   private String jrootFile = "";
   private ArrayList<String> stdhepfilelist = new ArrayList();

   //variables for jroot file construction and background/signal file reading
   private Jroot root;
   
   // output text files
   private FileWriter fw_scal_Ptot; 
   private FileWriter fw_vec_Ptot; 
   private FileWriter fw_M;
   
   private FileWriter fw_scal_Ptot_loAngle; 
   private FileWriter fw_vec_Ptot_loAngle; 
   private FileWriter fw_M_loAngle;

}
