/*
 * BeamParameterDetermination.java
 *
 * This driver looks to measure observables
 * in the BeamCalorimeter, and will later
 * reconstruct beam parameters from these.
 * 
 * OBSERVABLES to be measured: L-R , T-D,
 * & diagonal asymmetries, Thrust axis, total
 * deposition, ...
 *
 * Works in progress: LR & TD assymetry
 * The next thing to start: thrust axis?
 *
 * Basis driver:
 * --> BeamcalEnergyDep.java
 *
 * Last edited on Mar 31, 2016, 3:54 PM
 * @Author: Luc d'Hauthuille 
 *
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
import hep.physics.vec.BasicHep3Vector;
import hep.physics.vec.VecOp;
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
    
    //This function is called when program is started,
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



    //******************************Power Analysis******************************************//

    // Computes the radiation dosage of a pixel over x years based on the energyPerCrossing.
    public double radDosage(double energyPerCrossing){
	int x_years = 3; //number of years
	double n_y = 125*Math.pow(10,9); //Crossings in 1 year
	double EDepYear = energyPerCrossing*n_y*x_years;
	double pixelArea = Math.pow(10,-2); //Pixel Area in cm^2
	double Eparticle = 120*Math.pow(10,-6); // Eparticle(120 keV -> 0.000120 GeV) 
	double numberPrtcles = (EDepYear/pixelArea)/(Eparticle); //Flux (# of prtcles through pixelArea)
	double RAD = 4*Math.pow(10,7); //# particles through a cm^2 = 1 Rad
	double radDosage = numberPrtcles/(RAD);
	return radDosage;
    }

    // Computes leakage current at 600V as a function of Temperature
    // in Celsius based on Wyatt's data(PF sensor, 300 MRads) 
    public double getCurrent(double T){
	double I = 5.79338*Math.pow(10,-6) + (7.39127*Math.pow(10,-7))*T + 
	    4.15927*Math.pow(10,-8)*Math.pow(T,2) + 8.9582*Math.pow(10,-10)*Math.pow(T,3);
	return I;
    }
    // Computes power drawn, in [W], per cm^2 from data based on A_sensor = 0.025cm^2 
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

    //*************************************************************************************//


    //In testing
    public double maxPixelE(double currMax,double energy){
	double newMax = currMax;
	if(energy> currMax) newMax = energy;
	return newMax;
    }
    //Computes the LR assymetries based on hit coordinates
    //*Need to be weighted by energy
    public void assym_LR(double[] pos, double hitEnergy){
	//System.out.println(sum_LR);
	sum_LR += pos[0]*hitEnergy;//weight hit position by energy 
	return;
    }
    public void assym_LR_postTransform(double[] pos, double hitEnergy){
        //System.out.println(sum_LR_post);
        sum_LR_post += pos[0]*hitEnergy; //weight hit position by energy
        return;
    }
    public void assym_TD(double[] pos, double hitEnergy){
        //System.out.println(sum_TD);                                                                                                   
        sum_TD += pos[1]*hitEnergy;//weight hit position by energy
        return;
    }
    public void assym_TD_postTransform(double[] pos, double hitEnergy){
        //System.out.println(sum_LR_post);
        sum_TD_post += pos[1]*hitEnergy; //weight hit position by energy
        return;
    }
    //Compute mean shower depth
    public void compute_meanDepth(List<SimCalorimeterHit> hits){
	double energyDepth_productSum = 0; // sum of the products of energy*depth
	double energyDepthL_productSum = 0; // sum of the products of energy*depth
	double sumOfEnergy = 0;
	for(SimCalorimeterHit hit: hits){	    
	    double[] vec = hit.getPosition();
	    if(vec[2] < 0){ /*pass negative beamcal*/}
	    else{
	    double[] transformed_Vec = PolarCoords.ZtoBeamOut(vec[0],vec[1],vec[2]);
	    double energy = hit.getRawEnergy();
	    sumOfEnergy += energy;
	    energyDepth_productSum += energy*transformed_Vec[2];
	    energyDepthL_productSum += energy*hit.getLayerNumber();
	    }
	}
	meanDepth_Z = energyDepth_productSum/sumOfEnergy;
	meanDepth_Layer = energyDepthL_productSum/sumOfEnergy;
	System.out.println("Mean depth is " + meanDepth_Z);
	System.out.println("Mean Layer is " + meanDepth_Layer);
	
    }
    //Compute thrust axis and value
    public void compute_Thrust(List<SimCalorimeterHit> hits, EventHeader event){
	   EventShape es = new EventShape();
	   /*
	     List<BasicHep3Vector> vecs = new ArrayList<BasicHep3Vector>();
	     for(SimCalorimeterHit hit: hits){
	     BasicHep3Vector a = new BasicHep3Vector(hit.getPosition());
	     vecs.add(a);
	     }
	     es.setEvent(vecs);
	   */
	   List<MCParticle> final_Particles = new ArrayList<MCParticle>();
	   int counter = 0;
	   for(MCParticle p: event.getMCParticles()){
	       if(p.getGeneratorStatus() == MCParticle.FINAL_STATE){
		   final_Particles.add(p);
	       }
	       if(counter > 990) break;
	       counter++;
	   }
	   List<Hep3Vector> momentaList = new ArrayList<Hep3Vector>();
	   for(MCParticle p: final_Particles){ momentaList.add(p.getMomentum());}
	   es.setEvent(momentaList);
	   Hep3Vector thrust = es.thrustAxis();
	   System.out.println("The thrust is " + thrust.magnitude());
    }
    

    //Observables//
    public double meanDepth_Z = 0;
    public double meanDepth_Layer = 0;
    public double sum_LR = 0;
    public double sum_LR_post = 0;
    public double sum_TD = 0;
    public double sum_TD_post = 0;

    //PROCESS FUNCTION
    //This is where the vast bulk of the program is run and controlled
    public void process( EventHeader event ) {
	//set assymetries to 0 again
	sum_LR = 0;
	sum_LR_post = 0;
        sum_TD = 0;
	sum_TD_post = 0;

	super.process( event );
        List<SimCalorimeterHit> hits = event.get(SimCalorimeterHit.class, "BeamCalHits");
	//initialize event variables
	int check_layer = 0;int hit_count_limit = 100; boolean use_limit = false;
        boolean reject_negative = false; // reject negative beamcal?
        int hit_count = 0;
      
	double powerDrawn = 0;
	double sumOfEnergy = 0;
	double sumOfPowerDrawn = 0;
        double maxPixelEnergy = 0;
	int layerOfMaxE = 0;
	double[] eDep = new double[25];

	try {
	    
	    compute_meanDepth(hits);
	    compute_Thrust(hits, event);
	    // loop through the List of <SimCalHits..>
            for (SimCalorimeterHit hit : hits) {
                double[] vec = hit.getPosition();
		int layer = hit.getLayerNumber();
		if ( reject_negative && (vec[2]<0) ){ layersHit2.add(layer);} //pass over event
                else {
		    
	            double energy = hit.getRawEnergy();
		    assym_LR(vec, energy);
		    assym_TD(vec, energy);
		    double[] transformed_Vec = PolarCoords.ZtoBeamOut(vec[0],vec[1],vec[2]);
		    assym_LR_postTransform(transformed_Vec,energy);
		    assym_TD_postTransform(transformed_Vec,energy);
		    
		    //********Power lies beneath
		    /*
		    sumOfEnergy += energy;
		    //REPLACE with maxPixelE(03/31/16)
		    if(maxPixelEnergy < energy){maxPixelEnergy = energy; layerOfMaxE = layer; }
		    */
		    //layersHit.add(layer);
		    //if( layer< 25) eDep[layer] += energy; 
		    
		    //All Layers. Set wholeBcal to true at bottom
		    //if(wholeBcal == true){/*Select Layers*/}
		    if(layersBcal && layer <= 15){
			//power[layer] += powerDraw(radDosage(energy/numberOfEvents),runTemp);//Divide by Event# for avg/event.
			root.fill("heatmap"+layer,vec[0],vec[1],powerDraw(radDosage(energy/numberOfEvents),runTemp));//mW
			root.fill("histE"+layer,powerDraw(radDosage(energy/numberOfEvents),runTemp)); //in mW(each pixel)
			//c. 03/31/16
			/**/
			/*Plot radDosage
			  root.fill("heatmap"+layer,vec[0],vec[1],radDosage(energy/numberOfEvents));//mW/cm^2
			  root.fill("histE"+layer,radDosage(energy/numberOfEvents)); //in mW(each pixel's)*/
			/* Plots (Energy Deposited)*#ofCrossingsPerYear*3			
			   root.fill("heatmap"+layer,vec[0],vec[1],energy*125*Math.pow(10,9)*3);
			   root.fill("histE"+layer,energy*125*Math.pow(10,9)*3); */
		    }
		    sumOfPowerDrawn += powerDrawn; 
		    //********************
		    sumOfEnergy += energy;
		}
                if ( use_limit && (hit_count++ > hit_count_limit) ) break;    
            }
	} catch(java.io.IOException e) {
            System.out.println(e);
            System.exit(1);
        }
	// Post-processing print statements:
        System.out.println("finished event "+ ++eventNumber);        
	System.out.println("Total energy deposited across BeamCal: " + sumOfEnergy);
	energyDepOverEvents += sumOfEnergy;
	System.out.println("Highest energy deposited on a pixel was " + maxPixelEnergy 
			   + "on layer " + layerOfMaxE);
	System.out.println("L_R assym number for this event, weighted by E:" + (sum_LR/sumOfEnergy));
	System.out.println("L_R assym number for this event, weighted by E, w/ transform :" +
			   (sum_LR_post/sumOfEnergy));
	System.out.println("T_D assym number for this event, weighted by E:" + (sum_TD/sumOfEnergy));
	System.out.println("T_D assym number for this event, weighted by E:" + 
			   (sum_TD_post/sumOfEnergy));
	System.out.println("************************");
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
