package scipp_ilc.drivers;

import java.util.AbstractList;
import java.util.ArrayList;
import java.io.Writer;
import java.io.PrintWriter;
import java.lang.String;
import java.lang.StringBuilder;
import java.io.FileWriter;
import java.io.PrintWriter;

import java.lang.Math;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.lcsim.conditions.ConditionsManager;
import org.lcsim.conditions.ConditionsSet;
import org.lcsim.event.EventHeader;
import org.lcsim.event.SimCalorimeterHit;
import org.lcsim.geometry.Detector;
import org.lcsim.geometry.IDDecoder;
import org.lcsim.util.Driver;
import org.lcsim.util.aida.AIDA;




/**
 *
 * @author Norman A Graf
 * @coauthor George Courcoubetis
 * @version $Id:
 */

//FIXME: Most of this file is very superfluous for what we (scipp_ilc) need. It needs to be stripped down a LOT
//            -cmilke
public class ECalOccupancyAsciiDump extends Driver
{

    private boolean _debug = true;
    private Set<String> collections = new HashSet<String>();
    private Map<String, Map<Long, int[]>> cellCountMaps = new HashMap<String, Map<Long, int[]>>();
    private Map<String, IDDecoder> _idDecoders = new HashMap<String, IDDecoder>();
    private List<Map<Long, int[]> > event_array  = new ArrayList<Map<Long, int[]>>();
    private AIDA aida = AIDA.defaultInstance();

    private ConditionsSet _cond;
    private double _ECalMipCut;

    public void setOutputfile(String s) {
        this.outputFile = s;
    }


    public void startOfData() {


    } 

    @Override
        protected void detectorChanged(Detector detector)
        {
            ConditionsManager mgr = ConditionsManager.defaultInstance();
            try {
                _cond = mgr.getConditions("CalorimeterCalibration");
                System.out.println("found conditions for " + detector.getName());
                _ECalMipCut = _cond.getDouble("ECalMip_Cut");
                System.out.println("_ECalMipCut = "+_ECalMipCut);
            } catch (ConditionsManager.ConditionsSetNotFoundException e) {
                System.out.println("ConditionSet CalorimeterCalibration not found for detector " + mgr.getDetector());
                System.out.println("Please check that this properties file exists for this detector ");
            }
        }



    @Override
        protected void process(EventHeader event)
        {
            // loop over all of the collections
            for (String collectionName : collections) {
                // fetch the SimCalorimeterHits
                System.out.println("bla" + collectionName);
                List<SimCalorimeterHit> hits = event.get(SimCalorimeterHit.class, collectionName);
                log("There are " + hits.size() + " " + collectionName);
                // get the right Map to populate
                //Map<Long, int[]> map = cellCountMaps.get(collectionName);
                Map<Long, int[]> abs  = new HashMap<Long, int[]>();
                event_array.add(abs);
                Map<Long, int[]> map = event_array.get(eventnumber);
                // loop over all of the hits
                for (SimCalorimeterHit hit : hits) {//do things here!!!!!!!!!
                    double[] pos = hit.getPosition();
                    int xpos = (int)pos[0];
                    int ypos = (int)pos[1];
                    int zpos = (int)pos[2];

                    double energy= hit.getCorrectedEnergy();
                    int layer = hit.getLayerNumber();

                    long cellid = hit.getCellID();

                    double radius = Math.hypot(pos[0],pos[1]);
                    if (radius>maxradii){
                        maxradii=(int)radius+1;
                    }
                    if (!_idDecoders.containsKey(collectionName)) {
                        _idDecoders.put(collectionName, hit.getIDDecoder());
                    }
                    double rawEnergy = hit.getRawEnergy();
                    aida.cloud1D(collectionName + " hit Energy").fill(rawEnergy);
                    if (true){//rawEnergy > _ECalMipCut) {//WARNING I messed with this
                        aida.cloud1D(collectionName + " hit Energy after cut").fill(rawEnergy);
                        long cellId = hit.getCellID();
                        // and update the occupancy of this address
                        if (map.containsKey(cellId)) {
                            //        System.out.println("id: "+cellId+" now has "+(map.get(cellId) + 1)+ " hits.");
                            int[] b={map.get(cellId)[0]+1,map.get(cellId)[1], map.get(cellId)[2], map.get(cellId)[3], map.get(cellId)[4] }; 
                            map.put(cellId, b);
                        } else {
                            int[] a = {1,xpos,ypos,zpos,eventnumber};
                            map.put(cellId, a);
                        }
                    }
                }
            }
                System.out.println("**************EVENT NUMBER = " + eventnumber++);
                //event_maps.add(map);
        }




        @Override
            protected void endOfData()
            {
                try {
                    // quick analysis...
                    // loop over all of the calorimeters
                    //PrintWriter writer = new PrintWriter("/export/home/cmilke/output/pairbackgrounds.txt", "UTF-8");
                    FileWriter writer = new FileWriter(outputFile + ".txt", false);
                    //    writer.println("      BUNCH EndCapHits Occupancy Study Results");
                    //writer.println("The second line");
                    //writer.close();
                    for (String collectionName : collections) {
                        // get the right Map to analyze
                        //System.out.println("it is" + collectionName);
                        //Map<Long, int[]> map = cellCountMaps.get(collectionName);
                        for( int i=0; i<eventnumber; i++ ) {

                            Map<Long, int[]> map = event_array.get(i);
                            // get the IDDecoder
                            IDDecoder idDecoder = _idDecoders.get(collectionName);
                            //get its keys
                            Set<Long> keys = map.keySet();

                            // loop over all of the hits
                            for (Long key : keys) {
                                // System.out.println(map.get(key)[1]);
                                idDecoder.setID(key);
                                int layer = idDecoder.getLayer();
                                double[] pos = idDecoder.getPosition();
                                int hitCount = map.get(key)[0]; //It was Integer instead of int
                                int xpos = map.get(key)[1];
                                int ypos = map.get(key)[2];
                                int zpos = map.get(key)[3];
                                int eventnum= map.get(key)[4];
                                // and fill the histogram
                                //if (hitCount > 3) {
                                //   System.out.println(collectionName + " id " + key + " has " + hitCount + " hits.");
                                //}
                                writer.write(eventnum + "; " + key + "; " + hitCount + "; "  + xpos + "; " + ypos + "; " + zpos +"\n");
                                aida.histogram1D(collectionName + "layer " + layer + " occupancy rates", 100, 0., 100.).fill(hitCount);
                                aida.cloud2D(collectionName + "layer " + layer + " occupancy rates vs position").fill(pos[0],pos[1],hitCount);
                            }
                        }
                    }
                    writer.close();
                } catch (java.io.IOException e) {
                    System.out.println(e);
                    System.exit(1);
                } 
            }

            public void setCollectionNames(String[] collectionNames)
            {
                    System.out.println("there are " + collectionNames.length+ " collections to process: ");
                    collections.addAll(Arrays.asList(collectionNames));
                    System.out.println("processing: ");
                    for (String collectionName : collections) {
                        System.out.println(collectionName);
                        cellCountMaps.put(collectionName, new HashMap<Long, int[]>());
                    }
            }

            private void log(String s)
            {
                if (_debug) {
                    System.out.println(s);
                }
            }

            private String outputFile;

            private int numberofbins=5;
            private int maxradii=0;


            private int eventnumber=0;
        }



