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

//import hep.physics.jet.EventShape;
import scipp_ilc.drivers.EventShape; 
//Import the EventShape class, customized to allow for larger list
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
    //************END OF POWER**************************************************************//


    //In testing
    public double maxPixelE(double currMax,double energy){
	double newMax = currMax;
	if(energy> currMax) newMax = energy;
	return newMax;
    }
    
    /*
    public void assym_LR(double[] pos, double hitEnergy){
	//System.out.println(sum_LR);
	sum_LR += pos[0]*hitEnergy;//weight hit position by energy 
	return;
	}
    public void assym_TD(double[] pos, double hitEnergy){
        //System.out.println(sum_TD);
	sum_TD += pos[1]*hitEnergy;//weight hit position by energy
        return;
	}
*/

    //Computes the LR assymetries based on hit coordinates
    //*Need to be weighted by energy
    
    public void assym_LR_postTransform(double[] pos, double hitEnergy){
        //System.out.println(sum_LR_post);
        sum_LR_E += pos[0]*hitEnergy; //weight hit position by energy
	sum_LR_Hits += pos[0];
        return;
    }

    public void assym_TD_postTransform(double[] pos, double hitEnergy){
        //System.out.println(sum_LR_post);
        sum_TD_E += pos[1]*hitEnergy; //weight hit position by energy
	sum_TD_Hits += pos[1];
        return;
    }
    //Compute mean shower depth (NOTE: Layers start at 1, instead of the usual 0 here)
    public void compute_meanDepth(List<SimCalorimeterHit> hits){
	double[] eDep = new double[51];
	int[] hitLayerArray = new int[51];
	double energyDepth_productSum = 0; // sum of the products of energy*depth
	double energyDepthL_productSum = 0; // sum of the products of energy*depth
	double hitDepthL = 0;
	double sumOfEnergy = 0;
	int hitnum = 0;
	
	for(SimCalorimeterHit hit: hits){	    
	    double[] vec = hit.getPosition();
	    if(vec[2] > 0){
		double[] transformed_Vec = PolarCoords.ZtoBeamOut(vec[0],vec[1],vec[2]);
		double energy = hit.getRawEnergy();
		sumOfEnergy += energy;
		hitnum++;
		eDep[hit.getLayerNumber()+1] += hit.getRawEnergy();
		hitLayerArray[hit.getLayerNumber()+1]++;
		energyDepth_productSum += energy*transformed_Vec[2];
		energyDepthL_productSum += energy*(hit.getLayerNumber()+1);
		hitDepthL += hit.getLayerNumber()+1;
	    }
	    else{/*dosomethingwith negative hits?*/}
	}
	
	hitDepthL = hitDepthL/hitnum;
	System.out.println("E total deposited" + sumOfEnergy);
	System.out.println("E deposited" + Arrays.toString(eDep));
	System.out.println("Hits by layer" + Arrays.toString(hitLayerArray));
	System.out.println("Hit number based:" + hitDepthL);
	
	meanDepth_Z = energyDepth_productSum/sumOfEnergy;
	meanDepth_Layer = energyDepthL_productSum/sumOfEnergy;
	System.out.println("Energy based:");
	System.out.println("Mean depth is " + meanDepth_Z);
	System.out.println("Mean Layer is " + meanDepth_Layer);	
    }

    //Compute thrust axis and value
    public void compute_Thrust(List<SimCalorimeterHit> hits, EventHeader event){	
	//Thrust quantities computed from Calorimeter hits (POSITION ONLY!)
	EventShape es_hits = new EventShape();
	List<BasicHep3Vector> vecs2 = new ArrayList<BasicHep3Vector>();
	double eSum = 0;
	for(SimCalorimeterHit hit: hits){
	    if(hit.getPosition()[2] > 0){//keep only positive
		BasicHep3Vector a = new BasicHep3Vector(hit.getPosition());
		a.setV(a.x(),a.y(),0);
		//double energy = hit.getRawEnergy();
		//a.setV(energy*a.x(), energy*a.y(),0);
		vecs2.add(a);	    
		//eSum += energy;
	    }
	}
	double x_avg = 0;
	double y_avg = 0;
	double c =0;
	for(BasicHep3Vector hitPos_Eweight: vecs2){
	    x_avg+= hitPos_Eweight.x();
	    y_avg+= hitPos_Eweight.y();
	    c++;
	}
	x_avg = x_avg/c;
	y_avg = y_avg/c;
	System.out.println("X_avg is" + x_avg);
	System.out.println("Y_avg is" + y_avg);

	List<BasicHep3Vector> vecs = new ArrayList<BasicHep3Vector>();
        for(SimCalorimeterHit hit: hits){
            if(hit.getPosition()[2] > 0){//keep only positive                                                           
		BasicHep3Vector a = new BasicHep3Vector(hit.getPosition());
		
		//a.setV(a.x(),a.y(),0);
		//double energy =hit.getRawEnergy();
		//eSum += energy;
		a.setV(a.x() - x_avg, a.y() - y_avg,0);
		vecs.add(a);
	    }
        }

	es_hits.setEvent(vecs);
	Hep3Vector thrustAxis_hits = es_hits.thrustAxis();
	System.out.println("The thrust fromt hits is " + thrustAxis_hits.magnitude());
	System.out.println("The thrustAxis from hits is " + thrustAxis_hits.toString());
	
	//Thrust quantities computed from final state particles(traditional jets?)
	EventShape es_fParticles = new EventShape();
	List<MCParticle> final_Particles = new ArrayList<MCParticle>();
	int counter = 0;
	for(MCParticle p: event.getMCParticles()){
	    if(p.getGeneratorStatus() == MCParticle.FINAL_STATE){
		final_Particles.add(p);
	    }
	    if(counter > 100000) break; //Don't go over maximum
	    counter++;
	}
	
	List<Hep3Vector> momentaList = new ArrayList<Hep3Vector>();
	for(MCParticle p: final_Particles){ momentaList.add(p.getMomentum());}
	es_fParticles.setEvent(momentaList);
	Hep3Vector thrust = es_fParticles.thrustAxis();
	System.out.println("The thrust from final state particles is " + thrust.magnitude());
	//System.out.println("The number of hits in this event is " + countHits);
	//System.out.println("The number of f_Particles in this event is " + countfinalParticles);
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
	try {	    
	    compute_meanDepth(hits);
	    compute_Thrust(hits,event);
	    
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
