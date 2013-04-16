package CTC;

import java.util.ArrayList;

import Log.Log;
import Simulator.Simulator;
import TrackModel.Block;
import TrackModel.Line;

public class CTCModule {
	//// Fields
	private CTCPanel gui;
	private Simulator sim = null;
	private ArrayList<Integer> gTrainIDs = null;
	private ArrayList<String> lineNames = null;
	private ArrayList<Line> lines = null;
	private Log log = Log.Instance();
	
	public CTCModule() {
		gui = new CTCPanel(this);
		lineNames = new ArrayList<String>();
		lineNames.add("Lines");
		gTrainIDs = new ArrayList<Integer>();
		lines = new ArrayList<Line>();
	}
		
	public CTCPanel getPanel() {
		return this.gui;
	}
	
	public void setSimulator(Simulator s) {
		sim = s;
	}
	
	protected void scheduleTrain(String line) {
		if(sim != null) {
			sim.scheduleTrain(line);
			log.append(0, "Train added to " + line + " Line\n");
		}
	}
	
	protected ArrayList<String> getLines() {
		return lineNames;
	}
	
	public void addLine(Line line) {
		lineNames.add(line.getName());
		lines.add(line);
		gui.update();
	}
	
	protected ArrayList<Block> getLine(String lName) {
		for(Line l : lines) {
			if(l.getName().equals(lName))
				return l.getBlocks();
		}
		return null;
	}
	
	public void setGLTrains(ArrayList<Integer> tids) {
		gTrainIDs = tids;
		gui.update();
	}
	
	public ArrayList<Integer> getGLTrains() {
		return gTrainIDs;
	}
	
	public void closeBLock(String lName, int bNum) {
		sim.closeBLock(bNum, lName);
		log.append(2, "BLock: " + bNum + " on Line: " + lName + "\n");
	}
}
