package TrackModel;

import javax.swing.JFileChooser;
import javax.swing.JPanel;
import javax.swing.JTextPane;
import javax.swing.JButton;
import javax.swing.JTabbedPane;

import TrackDisplay.GraphPanel;

import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

public class TrackModelPanel extends JPanel {
	private File file;
	private TrackModelModule tm;
	private JTabbedPane tabbedPane;
	JTextPane txtpnTheTextPane = new JTextPane();
	/**
	 * Create the panel.
	 */
	public TrackModelPanel(final TrackModelModule Tm) {
		setLayout(null);
		tm = Tm;
		
		//txtpnTheTextPane.setEnabled(false);
		txtpnTheTextPane.setEditable(false); // use this to change the pane. set it editable to edit, then uneditable for the display 
		txtpnTheTextPane.setText("Selected Block Information");
		txtpnTheTextPane.setBounds(10, 259, 630, 46);
		add(txtpnTheTextPane);
		
		final JFileChooser jfc = new JFileChooser();
		
		JButton btnNewButton = new JButton("Load File...");
		btnNewButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent x) {
				int result = jfc.showOpenDialog(null);
				if (result==JFileChooser.APPROVE_OPTION) {
                    file = jfc.getSelectedFile();
                    tm.getLineFile(file);
				}
			
			}
		});
		btnNewButton.setBounds(10, 316, 200, 23);
		add(btnNewButton);
		
		tabbedPane = null;

	}
	
	public void displayBlockInfo(Block b)
	{
		txtpnTheTextPane.setEditable(true);
		// format: ID | Type | Length | Grade | Speed Limit | Section | aboveGround | Station Name 
		// bID nextB bType bLength bGrade bSpLim bSection belowGround stationName
		//txtpnTheTextPane.setText(/*set the text of the panel*/);
		txtpnTheTextPane.setEditable(false);
	}
	
	protected void addLine(Line l) {
		if(tabbedPane == null) {
			tabbedPane = new JTabbedPane(JTabbedPane.TOP);
			tabbedPane.setBounds(10, 11, 630, 237);
			add(tabbedPane);
		}
		tabbedPane.addTab(l.getName(), null, l.getPanel(), null);
		
	}
	
	public void setPanel(GraphPanel gp, String name)
	{
		int temp = tabbedPane.getSelectedIndex();
		
		tabbedPane.remove(temp);
		tabbedPane.insertTab(name, null, gp, null, temp);
		tabbedPane.setSelectedIndex(temp);
	}
}
