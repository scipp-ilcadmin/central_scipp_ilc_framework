/*
 * EcalEnergyAnalysis.java
 *
 * Created on July 11, 2013, 10:45 AM
 * Edited on August 26, 2015, 02:21 AM
 * @author Christopher Milke
 *
 */
package scipp_ilc.drivers;

import scipp_ilc.base.util.Jroot;
import scipp_ilc.base.util.PolarCoords;

import org.lcsim.event.EventHeader;
import org.lcsim.event.MCParticle;
import org.lcsim.event.SimCalorimeterHit;

import org.lcsim.util.Driver;

import java.util.List;
import java.lang.Math;


public class EcalEnergyAnalysis extends Driver {
    
    
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
    
    //END DEFINE XML FUNCTIONS




    //This function is called when the program is first started
    //and initializes all persistent data
    public void startOfData() {
        eventNumber = 0;
        String root_mode = "NEW";
        
        try {
            root = new Jroot(jrootFile,root_mode);
            
            String title = "Average Energy Distribution of Two Photon Events as a Function of ";
            root.init("TProfile","rad","rad", title + "Radius", EcalMax/Binsize, 0, EcalMax, 0, 500);
            root.init("TProfile","lay","lay", title + "Layer", Ecal_num_layers, 0, Ecal_num_layers-1, 0, 500);
            //root.init("TH1D","rad_hist","rad_hist", title + "Radius", 700, 0, 1400);
            //root.init("TH1D","lay_hist","lay_hist", title + "Layer", 31, 0, 30);
            
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
        super.process( event );
        List<SimCalorimeterHit> hits = event.get(SimCalorimeterHit.class, "EcalEndcapHits");
        boolean reject_negative = true;

        double[] rad_energy = new double[EcalMax/Binsize];
        double[] lay_energy = new double[Ecal_num_layers];
        for(int r = 0; r < (EcalMax/Binsize); r++) rad_energy[r] = 0.0;
        for(int l = 0; l <  Ecal_num_layers; l++) lay_energy[l] = 0.0;
        
        for (SimCalorimeterHit hit : hits) {
            double[] vec = hit.getPosition();
            if ( reject_negative && (vec[2]<0) ) continue; //pass over event

            int layer = hit.getLayerNumber();
            double radius = Math.hypot(vec[0],vec[1]);
            int radbin = (int) radius / Binsize;

            double energy = hit.getCorrectedEnergy();
            rad_energy[radbin] += energy;
            lay_energy[layer] += energy;
        }

        try {
            for(int r = 0; r < (EcalMax/Binsize); r++) {
                if (rad_energy[r] != 0.0) root.fill("rad",r*Binsize,rad_energy[r]);
            }
            for(int l = 0; l <  Ecal_num_layers; l++) {
                if (lay_energy[l] != 0.0) root.fill("lay",l,lay_energy[l]);
            }
        } catch(java.io.IOException e) {System.out.println(e); System.exit(1);}
        

        System.out.println("finished event "+ ++eventNumber);        

    }//End Process


    /*here all the classwide variables are declared*/
    private int eventNumber;
    private int EcalMax = 1400;
    private int Binsize = 8;
    private int Ecal_num_layers = 31;

    //xml derived variables
    private String jrootFile = "";

    //variables for jroot file construction and background/signal file reading
    private Jroot root;
}
