/*
 * BeamCalEnergy.java
 *
 * Determines max energy deposited on a 1mm x 1mm pixel
 * then makes heat maps and frequency distributions of
 * energy deposited on first 15 Layers.
 * Additional function: Using a given number of crossings
 * to average over, makes heatmaps of radiation dosage
 * from Edeposition, computed over a whole year.   
 * 
 * Last edited on Mar 08, 2016, 9:21 AM
 * @Authors: Luc d'Hauthuille 
 * ~ Based on EventAnalysis written by Christopher Milke et al.
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

import hep.physics.jet.EventShape;
import hep.physics.particle.Particle;
import hep.physics.vec.Hep3Vector;

import java.util.Map;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.ListIterator;
import java.util.Collections;
import java.util.Arrays;

import java.lang.String;

public class EnergyLayerAnalysis extends Driver {
    
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

	    String plot_key_1 = "histHits";
	    String plot_key_2 = "histE";
	    String plot_key_3 = "realHits";
	    String plot_title_1 = "Pixels hit per layer";
	    String plot_title_2 = "Energy Dep per Layer";
	    String plot_title_3 = "Number of MC contributions per layer using .getMCParticleCount() on each pixel hit";
	    root.init("TH1D",plot_key_1, plot_key_1, plot_title_1, 50, 0, 50);
	    root.init("TH1D",plot_key_2, plot_key_2, plot_title_2, 50, 0, 50);
	    root.init("TH1D",plot_key_3, plot_key_3, plot_title_3, 50, 0, 50);
	}catch(java.io.IOException e) {
            System.out.println(e);
            System.exit(1);
        }
    }
    
    // This function is called after all file runs have finished,
    // and closes any necessary data. Prints energy deposited over
    // all layers and the layers hit on + and - Beamcal.
    public void endOfData(){
        try {
            root.end();
	    //	    System.out.println("Over all events, total energy deposited = " + 
	    //		       energyDepOverEvents +" GeV");
	    System.out.println("end");
	}
        catch (java.io.IOException e) {
           System.out.println(e);
           System.exit(1);
        }
    }


    //This is where the vast bulk of the program is run and controlled
    public void process( EventHeader event ) {    //PROCESS FUNCTION
        super.process( event );
        List<SimCalorimeterHit> hits = event.get(SimCalorimeterHit.class, "BeamCalHits");
	//Event-wide variables
	int check_layer = 0; int hit_count_limit = 100; boolean use_limit = false;
        boolean reject_negative = true;
        int hit_count = 0;
	double sumOfEnergy = 0;
	double maxPixelEnergy = 0;
		
	try {
            for (SimCalorimeterHit hit : hits) {
                double[] vec = hit.getPosition();
		int layer = hit.getLayerNumber();
		//if ( reject_negative && (vec[2]<0){/*Pass over negative Bcal hits*/}
                if(vec[2]>0) {
		    double energy = hit.getRawEnergy();		   
		    sumOfEnergy += energy;
		    root.fill("histHits", layer);
		    root.fill("histE", layer, energy);
		    root.fill("realHits", layer, hit.getMCParticleCount());
		    int contributions = hit.getMCParticleCount();
		    if(hit_count < 10){
			for(int i=0; i<contributions; i++){
			    System.out.println(hit.getMCParticle(i).getType().getName());
			}
		    }
		    hit_count++;
		}
		//                if ( use_limit && (hit_count++ > hit_count_limit) ) break;    
            }
	    System.out.println("Total energy deposited in (+) beamcal: " + sumOfEnergy + "GeV");
        } catch(java.io.IOException e) {
            System.out.println(e);
            System.exit(1);
        }
	//Post-Event print statements
	System.out.println("finished event "+ ++eventNumber);        
    }//End Process


    /*here all the classwide variables are declared*/
    private int numberOfEvents = 10; //This is used to get averages per event.
    private int eventNumber;
    private double runTemp = 15;

    private double energyDepOverEvents = 0;
    private HashSet<Integer> layersHit = new HashSet<Integer>();
    private HashSet<Integer> layersHit2 = new HashSet<Integer>();
    //xml derived variables
    private String jrootFile = "";

    //variables for jroot file construction and background/signal file reading
    private LCIOFileManager lcioFileMNGR = new LCIOFileManager();
    private Jroot root;
    private boolean aligned;
}



