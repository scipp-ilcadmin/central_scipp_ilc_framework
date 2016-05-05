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
         fw_loM.close();
         efw_loM.close();
         pfw_loM.close();
         fw_hiM.close();
         efw_hiM.close();
         pfw_hiM.close();
         System.out.println("MaxQ: "+maxQ+", MaxR: "+maxR);

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
      double scal_Ptot = {0,0,0};
      double[] vec_Ptot = {0,0,0};
      double Etot = 0;
      double M = 0;

      // Low angle particles where cos(theta)<.9
      double scal_Ptot_loAngle = {0,0,0};
      double[] vec_Ptot_loAngle = {0,0,0};
      double Etot_loAngle = 0;
      double M_loAngle = 0;      

      try{
         //System.out.println("\n=======================================\n");
         //System.out.println("\n"+n+" particle event\n");
         for ( int p = 0; p<n; p++) {
            int ID = event.getIDHEP( p );                           
            boolean finState = ( event.getISTHEP( p ) == FINAL_STATE);
            
            // Momentum and energy values
            double x = event.getPHEP(p, 0); 
            double y = event.getPHEP(p, 1); 
            double z = event.getPHEP(p, 2);
            double En = event.getPHEP(p, 3);
            double cosAngle = getCosAngle(x,y,z);
            // Running Energy sum of partcles not HE e-||e+
 
            boolean isNeutrino = (ID==12 || ID==14 || ID==16 || ID==18 );
            boolean isElec = (ID==11);          
            boolean isPosi = (ID==-11);
 
            if( finState ){
               if( !isElec && !isPosi ){
                  if(!isNeutrino && (cosAngle <.9) ){
                     addScal(scal_Ptot_loAngle, x, y, z);
                     addVec(vec_Ptot_loAngle, x, y, z);
                     En_tot_loAngle += En;
                  } 
                  addScal(scal_Ptot, x, y, z);
                  addVec(vec_Ptot, x, y, z);
                  En_tot += En;
                  }
               }
            }
         }
         
         // Relativity-invariant mass of particles not HE e-||e+
         M = getM(vec_Ptot, En){
         M_loAngle = getM(vec_Ptot_loAngle, En_loAngle);
         
                  

         if( M>= 2.0){
            fw_hiM.write(P+" "+mag_pr+";\n");
            efw_hiM.write(Q+" "+mag_pr+";\n");
            pfw_hiM.write(R+" "+mag_pr+";\n");
         } else {
            fw_loM.write(P+" "+mag_pr+";\n");
            efw_loM.write(Q+" "+mag_pr+";\n");
            pfw_loM.write(R+" "+mag_pr+";\n");
         }         
      } catch(java.io.IOException e) {
         System.out.println(e);
         System.exit(1);
      }
   }

   public static double getM(double[] P, double En){
       double x = P[0]; double y=P[1]; double z=P[2];
       return Math.sqrt( En*En - (x*x+y*y+z*z) );
   }

   public static double getMag( double[] P ){
       double x = P[0]; double y=P[1]; double z=P[2];
       return Math.sqrt(x*x+y*y+z*z);
   }

   public static double getCosTheta(double x, double y, double z){
       r = Math.sqrt(x*x+y*y+z*z);
       return z/r;
   }
 
   public static void addScal(double s, double x, double y, double z){
      s+= Math.sqrt( x*x+y*y+z*z );
   }

   public static void addVec(double[] v, double x, double y, double z){
      v[0]+=x; v[1]+=y; v[2]+=z;
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
   private FileWriter fw_vec_Ptot_Angle; 
   private FileWriter fw_M_loAngle;

}
