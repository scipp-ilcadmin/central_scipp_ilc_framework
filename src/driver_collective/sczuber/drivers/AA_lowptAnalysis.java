/*
 * AA_lowptAnalysis.java
 *
 * Created on July 11, 2013, 10:45 AM
 * Edited on August 26, 2015, 02:21 AM
 * @author Christopher Milke
 *
 */
package scipp_ilc.drivers;

import scipp_ilc.base.util.Jroot;
import scipp_ilc.base.util.PolarCoords;

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

import hep.io.stdhep.*;
import hep.io.xdr.XDRInputStream;

public class AA_lowptAnalysis extends Driver {


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
        logCount1 = 0;
        logCount2 = 0;
        String root_mode = "UPDATE";

        try {
            root = new Jroot(jrootFile,root_mode);
            root.init("TH1D", "detectable_scalar", "detectable_scalar", "detectable_scalar", 10000, 0, 100);
            root.init("TH1D", "detectable_vector", "detectable_vector", "detectable_vector", 10000, 0, 100);
            root.init("TH1D", "detectable_mass", "detectable_mass", "detectable_mass", 10000, 0, 1000);
            root.init("TH1D", "detected_scalar", "detected_scalar", "detected_scalar", 10000, 0, 100);
            root.init("TH1D", "detected_vector", "detected_vector", "detected_vector", 10000, 0, 100);
            root.init("TH1D", "detected_mass", "detected_mass", "detected_mass", 10000, 0, 1000);
            root.init("TH1D", "true_scalar", "true_scalar", "true_scalar", 10000, 0, 100);
            root.init("TH1D", "true_vector", "true_vector", "true_vector", 10000, 0, 100);
            root.init("TH1D", "true_mass", "true_mass", "true_mass", 10000, 0, 1000);

            //file process loop
            int total = 0;
            //int limit = 10000;
            for(String filename: stdhepfilelist) {
                System.out.println("FILENAME = " + filename);
                StdhepReader reader = new StdhepReader(filename);
                for (int i=0;i<reader.getNumberOfEvents();i++) {
                    StdhepRecord record = reader.nextRecord();
                    if (record instanceof StdhepEvent) {
                        StdhepEvent event = (StdhepEvent) record;
                        if (++total % 1000 == 0) {
                            System.out.println("    TOTAL = " + total);
                            root.proc("f.Write()");
                        }
                        //do stuff with even
                        analyze(event);
                    }
                    //if (total > limit) break;
                }
                //if (total > limit) break;
            } 
            System.out.println("\n\nFINISHED " + total);
            root.end();

        } catch (java.io.IOException e) {
            System.out.println(e);
            System.exit(1);
        }
    }

    private void analyze(StdhepEvent event){
        int number_particles = event.getNHEP();

        int electronI = -1;
        int positronI = -1;
        double electronEnergy = 0;
        double positronEnergy = 0;

        for (int particleI = 0; particleI < number_particles; particleI++) {
            if ( event.getISTHEP(particleI) != FINAL_STATE ) continue;

            int pdgid = event.getIDHEP( particleI ) ;
            double energy = event.getPHEP(particleI, 3);

            if ( pdgid == Electron_ID && energy > electronEnergy ) {
                electronI = particleI;
                electronEnergy = energy;
            }
            if ( pdgid == Positron_ID && energy > positronEnergy ) {
                positronI = particleI;
                positronEnergy = energy;
            }
        }

        //these two sets of values are witheld from the total
        //until it is determined that the particle they derived
        //their values from was not the initial electron or positron
        //i.e. that particle's energy is not the highest


        //not-detectable  , detectable ,  not detectED ,  detectED : px, py, pz, E, scalar
        double[][] totals = { {0,0,0,0,0}, {0,0,0,0,0}, {0,0,0,0,0}, {0,0,0,0,0} };

        //find initial electron and positron
        for (int particleI = 0; particleI < number_particles; particleI++) {
            if ( event.getISTHEP(particleI) != FINAL_STATE ) continue;
            if ( particleI == electronI ) continue;
            if ( particleI == positronI ) continue;
            int pdgid = event.getIDHEP( particleI ) ;

            double mom_x  = event.getPHEP(particleI, 0); 
            double mom_y  = event.getPHEP(particleI, 1); 
            double mom_z  = event.getPHEP(particleI, 2);
            double energy = event.getPHEP(particleI, 3);

            int is_detectable = check_if_detectable(pdgid,mom_x,mom_y,mom_z);
            int is_detected = check_if_detected(pdgid, mom_x,mom_y,mom_z);
            update_totals(totals,mom_x,mom_y,mom_z,energy,is_detectable,is_detected);
        }

        double square_detectable_px = Math.pow( totals[1][0], 2 );
        double square_detectable_py = Math.pow( totals[1][1], 2 );
        double square_detectable_pz = Math.pow( totals[1][2], 2 );
        double square_detectable_pE = Math.pow( totals[1][3], 2 );

        double total_detectable_scalar = totals[1][4];
        double total_detectable_vector = Math.sqrt( square_detectable_px + square_detectable_py );
        double total_detectable_mass_squared = square_detectable_pE - square_detectable_px - square_detectable_py - square_detectable_pz;
        if ( (-1e-9) < total_detectable_mass_squared && total_detectable_mass_squared < 0 ) total_detectable_mass_squared = 0.0;
        double total_detectable_mass = Math.sqrt(total_detectable_mass_squared);

    
        double square_detected_px = Math.pow( totals[3][0], 2);
        double square_detected_py = Math.pow( totals[3][1], 2);
        double square_detected_pz = Math.pow( totals[3][2], 2);
        double square_detected_pE = Math.pow( totals[3][3], 2);
        
        
        double total_detected_scalar = totals[3][4];
        double total_detected_vector = Math.sqrt( square_detectable_px + square_detectable_py );
        double total_detected_mass_squared = square_detectable_pE - square_detectable_px - square_detectable_py - square_detectable_pz;
        if ( (-1e-9) < total_detected_mass_squared && total_detected_mass_squared < 0 ) total_detected_mass_squared = 0.0;
        double total_detected_mass = Math.sqrt(total_detected_mass_squared);  
        

        double square_true_px = Math.pow( totals[0][0] + totals[1][0], 2 );
        double square_true_py = Math.pow( totals[0][1] + totals[1][1], 2 );
        double square_true_pz = Math.pow( totals[0][2] + totals[1][2], 2 );
        double square_true_pE = Math.pow( totals[0][3] + totals[1][3], 2 );

        double total_true_scalar = totals[0][4] + totals[1][4];
        double total_true_vector = Math.sqrt(square_true_px + square_true_py);
        double total_true_mass_squared = square_true_pE - square_true_px - square_true_py - square_true_pz;
        if ( (-1e-9) < total_true_mass_squared && total_true_mass_squared < 0 ) total_true_mass_squared = 0.0;
        double total_true_mass = Math.sqrt(total_true_mass_squared);

        try {
            root.fill("detectable_scalar", total_detectable_scalar);
            root.fill("detected_scalar", total_detected_scalar);
            root.fill("detectable_vector", total_detectable_vector);
            root.fill("detected_vector", total_detected_vector);
            root.fill("detectable_mass", total_detectable_mass);
            root.fill("detected_mass", total_detected_mass);
            root.fill("true_scalar", total_true_scalar);
            root.fill("true_vector", total_true_vector);
            root.fill("true_mass", total_true_mass);

        } catch(java.io.IOException e) {
            System.out.println(e);
            System.exit(1);
        }

    }


    private void update_totals( double[][] totals, double mom_x, double mom_y, double mom_z, double energy, int is_detectable, int is_detected ) {
        totals[is_detectable][0] += mom_x;
        totals[is_detected][0] += mom_x;
        totals[is_detectable][1] += mom_y;
        totals[is_detected][1] += mom_y;
        totals[is_detectable][2] += mom_z;
        totals[is_detected][2] += mom_z;
        totals[is_detectable][3] += energy;
        totals[is_detected][3] += energy;
        totals[is_detectable][4] += Math.sqrt( mom_x*mom_x + mom_y*mom_y );
        totals[is_detected][4] += Math.sqrt( mom_x*mom_x + mom_y*mom_y );

    }


    private int check_if_detectable( int id, double mom_x, double mom_y, double mom_z ) {
        boolean is_neutrino = ( id == 12 || id == -12 || 
                                id == 14 || id == -14 ||
                                id == 16 || id == -16 ||
                                id == 18 || id == -18 );

        double mom_total = Math.pow(mom_x*mom_x + mom_y*mom_y + mom_z*mom_z,0.5);
        double cos_theta = mom_z / mom_total;
        boolean is_forward = ( Math.abs(cos_theta) > 0.9 );

        return ( is_neutrino ) ? 0 : 1 ; // 0 of not detected 1 if detected 
        //if (is_neutrino)  return 0; //not detectable
        //if (!is_neutrino) return 1; //detectable
    }
    private int check_if_detected( int id, double mom_x, double mom_y, double mom_z ){
        boolean is_neutrino = ( id == 12 || id == -12 ||
                                id == 14 || id == -14 ||
                                id == 16 || id == -16 ||
                                id == 18 || id == -18 );
        double mom_total = Math.pow(mom_x*mom_x + mom_y*mom_y + mom_z*mom_z,0.5);
        double cos_theta = mom_z / mom_total; 
        boolean is_forward = ( Math.abs(cos_theta) > 0.9 );
        return ( is_neutrino || is_forward) ? 2 : 3;
        //if ( is_neutrino || is_forward ) return 2; //not detected
        //if ( !is_neutrino && !is_forward ) return 3; //detected
    }
    // Generator Statuses
    private final int DOCUMENTATION = 3;
    private final int FINAL_STATE = 1;
    private final int INTERMEDIATE = 2;

    // particle IDs
    private final int Electron_ID = +11;
    private final int Positron_ID = -11;

    /*here all the classwide variables are declared*/
    private int eventNumber;
    private int logCount1;
    private int logCount2;

    //xml derived variables
    private String jrootFile = "";
    private ArrayList<String> stdhepfilelist = new ArrayList();

    //variables for jroot file construction and background/signal file reading
    private Jroot root;
}
