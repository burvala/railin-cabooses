package CTC;

import java.awt.EventQueue;
import java.util.ArrayList;

import javax.swing.JFrame;
import javax.swing.JPanel;

import TrackModel.Block;
import javax.swing.JButton;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

public class CTCTester {

	private JFrame frame;
	private static CTCModule ctc;
	public static ArrayList<Block> myBlocks;
	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		ctc = new CTCModule(null);
		myBlocks = new ArrayList<Block>();
		for (int blockCount = 0; blockCount < 5; blockCount++) {
			Block blk = new Block(blockCount);
			//blk.setBlockNumber(blockCount);
			if(blockCount == 1)
				blk.setType(1);
			if(blockCount == 2)
				blk.setType(2);
			myBlocks.add(blk);
		}
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					CTCTester window = new CTCTester();
					window.frame.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	/**
	 * Create the application.
	 */
	public CTCTester() {
		initialize();
	}

	/**
	 * Initialize the contents of the frame.
	 */
	private void initialize() {
		frame = new JFrame();
		frame.setBounds(100, 100, 360, 545);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.getContentPane().setLayout(null);
		
		JPanel panel = ctc.getPanel();
		panel.setBounds(10, 11, 300, 378);
		frame.getContentPane().add(panel);
		
		JButton btnGetTrack = new JButton("Get Track");
		btnGetTrack.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				ctc.setGLine(myBlocks);
			}
		});
		btnGetTrack.setBounds(10, 473, 89, 23);
		frame.getContentPane().add(btnGetTrack);
	}
}
