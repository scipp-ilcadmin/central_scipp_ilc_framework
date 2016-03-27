/*
 * BeamParameterDetermination.java
 *
 * Using a given number of crossings
 * to average over, makes heatmaps of radiation dosage
 * from Edeposition, computed over a whole year.   
 *
 * Previous function/ Basis:
 * Determines max energy deposited on a 1mm x 1mm pixel
 * then makes heat maps and frequency distributions of
 * energy deposited on first 15 Layers.
 *
 * 
 * Last edited on Mar 08, 2016, 9:21 AM
 * @Author: Luc d'Hauthuille 
 * ~ Based on EventAnalysis written by Christopher Milke et al. ~
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

public class BeamParameterDetermination extends Driver {
    
    //DEFINE XML FUNCTIONS
    //These functions are specially formatted functions to pull variable data from the xml file
    /*****************************************************************************************
        XML FUNCTION FORMAT
    public void //setVariablename(variable type) { 
    //the first letter after "set" must be uppercase but can (must?) be lowercase in xml file
        set variable here; 
    }
    *******************************************************************************************/

    public void setOutputfile(String s) {
	this.jrootFile = s;
    }
    //END DEFINE XML FUNCTIONS
    
    //This function is called when program is first started,
    //initializes all persistent data
    public void startOfData() {
        eventNumber = 0;
        String root_mode = "NEW";
	try {
            root = new Jroot(jrootFile,root_mode);
	    /*root.init("TH2D","scatter1","posxy","X Y Hit Occupancy"
	      +  "Over All Layers",350, -175, 175, 350, -175, 175);*/	
	    if(layersBcal == true){
		int layers = 15;
		for(int i = 0; i<= layers; i++){
		    String plotName = "heatmap" +i;
		    String histName = "histE" +i;
		    String plotName2 = "X Y of Layer " +i;
		    String histName2 = "Energy Dep on Layer " +i;
		    root.init("TH2D",plotName, plotName,plotName2, 
			      350, -175, 175, 350, -175, 175);
		    root.init("TH1D",histName,histName,histName2,100,0,3);
		}
	    }if(wholeBcal ==true){
		// Makes a heatmap over all layers of Bcal
		/***************/
		String plotName = "heatmap";
		String plotName2 = "X Y Power Drawn over ALL Layers";
		root.init("TH2D",plotName, plotName,plotName2, 350, -175, 175, 350, -175, 175);
		String histName = "histE_whole";
		String histName2 = "Energy Dep, by layer";
		root.init("TH1D",histName,histName,histName2,50,0,50);
	    }
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
	    System.out.println("Over all events, total energy deposited = " + 
			       energyDepOverEvents +" GeV");
	    System.out.println("Average per event = " +energyDepOverEvents/numberOfEvents);
	    //for(Integer i: layersHit){System.out.println(i);}	    
	}
        catch (java.io.IOException e) {
           System.out.println(e);
           System.exit(1);
        }
    }

    // Computes the radiation dosage of a pixel over x years based on the energyPerCrossing.
    public double radDosage(double energyPerCrossing){
	int x_years = 3; //number of years
	double n_y = 125*Math.pow(10,9); //Crossings in 1 year
	double EDepYear = energyPerCrossing*n_y*x_years;
	double pixelArea = Math.pow(10,-2); //Pixel Area in cm^2
	double Eparticle = 120*Math.pow(10,-6); // Eparticle(120 keV -> 0.000120 GeV) 
	double numberPrtcles = (EDepYear/pixelArea)/(Eparticle); //Number of particles through this area
	double RAD = 4*Math.pow(10,7); //# particles through a cm^2 = 1 Rad
	double radDosage = numberPrtcles/(RAD);
	return radDosage;
    }
    // Computes leakage current at 600V as a function of Temperature
    // in Celsius based on data obtained by Wyatt for a 0.025cm^2 sensor
    // circa 12/16 
    public double getCurrent(double T){
	double I = 5.79338*Math.pow(10,-6) + (7.39127*Math.pow(10,-7))*T + 
	    4.15927*Math.pow(10,-8)*Math.pow(T,2) + 8.9582*Math.pow(10,-10)*Math.pow(T,3);
	return I;
    }
    // Computes power drawn, in [W], per cm^2 from data
    // for A_sensor = 0.025cm^2 
    public double getRawPower(double T){
	double I = getCurrent(T);
	double biasVoltage = 600;
	double s = 40;// scale factor to convert to pwr/cm^2 
	return s*biasVoltage*I;
    }
    // Returns power draw in mW for a pixel, given 
    // radiation dosage and temperature.
    public double powerDraw(double radDosage, double T){
	double pixelArea = Math.pow(10,-2);
	//double slope_PwrDraw = (80)/(3*Math.pow(10,8)); //80mW/cm^2 / 300 MRads //[01/29/16]
	double slope_PwrDraw = (1)/(2.7*Math.pow(10,8)); //Linear scaling factor(3->2.7 *10^8 Rads) 
	//slope_PwrDraw *= (1/10^2); //Since our pixels are (10^-2) cm^2 
	double pwrPerSquarecm = (radDosage*slope_PwrDraw)*getRawPower(T); 
	//obtain power draw per cm^2, for such a radiation dosage
	double pwrDraw = pwrPerSquarecm* pixelArea;//Obtain pwr draw for this pixel
	return pwrDraw; // in Watts, [01/29/16]
    }

    //PROCESS FUNCTION
    //This is where the vast bulk of the program is run and controlled
    public void process( EventHeader event ) {
        super.process( event );
        List<SimCalorimeterHit> hits = event.get(SimCalorimeterHit.class, "BeamCalHits");
	int check_layer = 0;int hit_count_limit = 100; boolean use_limit = false;
        boolean reject_negative = true;
        int hit_count = 0;
	double powerDrawn = 0;
	double sumOfEnergy = 0;
	double sumOfPowerDrawn = 0;
        double maxPixelEnergy = 0;
	int layerOfMaxE = 0;
	double[] power = new double[16];
	double[] eDep = new double[25];
	try {
            for (SimCalorimeterHit hit : hits) {
                double[] vec = hit.getPosition();
		int layer = hit.getLayerNumber();
		if ( reject_negative && (vec[2]<0) ){ layersHit2.add(layer);} //pass over event
                else {
		    double energy = hit.getRawEnergy();		   
		    sumOfEnergy += energy;
		    if(maxPixelEnergy < energy){
			maxPixelEnergy = energy;
			layerOfMaxE = layer;
		    }
		    //layersHit.add(layer);
		    if( layer< 25) eDep[layer] += energy; 
		    //All Layers. Set wholeBcal to true at bottom
		    if(wholeBcal == true){   
			powerDrawn = powerDraw(radDosage(energy/numberOfEvents),runTemp);
			root.fill("heatmap",vec[0],vec[1],powerDraw(radDosage(energy/numberOfEvents),runTemp));
			//root.fill("heatmap",vec[0],vec[1],energy); //Used for P129 project
			//Select Layers
		    }if(layersBcal && layer <= 15){
			power[layer] += powerDraw(radDosage(energy/numberOfEvents),runTemp);//Divide by Event# for avg/event.
			// Multiply powerDraw by 100 was removed from the fill function
			root.fill("heatmap"+layer,vec[0],vec[1],powerDraw(radDosage(energy/numberOfEvents),runTemp));//mW
			root.fill("histE"+layer,powerDraw(radDosage(energy/numberOfEvents),runTemp)); //in mW(each pixel)
			/*Plot radDosage
			  root.fill("heatmap"+layer,vec[0],vec[1],radDosage(energy/numberOfEvents));//mW/cm^2
			  root.fill("histE"+layer,radDosage(energy/numberOfEvents)); //in mW(each pixel's)*/
			/* Plots (Energy Deposited)*#ofCrossingsPerYear*3			
			   root.fill("heatmap"+layer,vec[0],vec[1],energy*125*Math.pow(10,9)*3);
			   root.fill("histE"+layer,energy*125*Math.pow(10,9)*3); */
		    }
		    //sumOfEnergy += energy;
		    sumOfPowerDrawn += powerDrawn; 
		}
                if ( use_limit && (hit_count++ > hit_count_limit) ) break;    
            }
        } catch(java.io.IOException e) {
            System.out.println(e);
            System.exit(1);
        }
	//Redundant likely
	try{
	for(int l = 0; l< 25; l++){
	    root.fill("histE_whole",l,eDep[l]);
	}
	}catch(java.io.IOException e) {
            System.out.println(e);
            System.exit(1);
        }
	// Post-processing print statements:
        System.out.println("finished event "+ ++eventNumber);        
	System.out.println("Energy deposited over layers:" + Arrays.toString(eDep));
	System.out.println("Total energy deposited across BeamCal: " + sumOfEnergy);
	System.out.println("Total power drawn across BeamCal: " + sumOfPowerDrawn);
	System.out.println("Highest power drawn on a pixel was " + 
			   powerDraw(radDosage(maxPixelEnergy/numberOfEvents),runTemp) 
			   + "on layer " + layerOfMaxE);
	energyDepOverEvents += sumOfEnergy;
	System.out.println("Highest energy deposited on a pixel was " + maxPixelEnergy 
			   + "on layer " + layerOfMaxE);
	System.out.println("Radiation dosage for 3 years on this pixel is thus "
			   + radDosage(maxPixelEnergy));
	
	System.out.println("Power Drawn over this event, per layer :" + Arrays.toString(power));
    }//End Process


    /*here all the classwide variables are declared*/
    private int numberOfEvents = 10; //This is used to get averages per event.
    private int eventNumber;
    private double runTemp = 15;
    private boolean wholeBcal = true;
    private boolean layersBcal = true;
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
