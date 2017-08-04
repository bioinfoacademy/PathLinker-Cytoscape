package com.dpgil.pathlinker.path_linker.internal;

import com.dpgil.pathlinker.path_linker.internal.Algorithms.Path;

import java.awt.Component;
import java.awt.Container;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;

import org.cytoscape.application.swing.CytoPanelComponent;
import org.cytoscape.application.swing.CytoPanelName;

/**
 * // -------------------------------------------------------------------------
 * /** Panel that displays the results for the PathLinker CytoScape plugin
 *
 * @author Daniel Gil
 * @version Nov 23, 2015
 */
@SuppressWarnings("serial")
public class PathLinkerResultPanel extends JPanel implements CytoPanelComponent {

	private ArrayList<Path> _results;
	private String _title;
	private JButton _discardBtn;
	private JButton _downloadBtn;
	private JTable _resultTable;

	/**
	 * Constructor for the result frame class
	 * @param title the title of the result panel
	 * @param results the results from pathlinker
	 */
	public PathLinkerResultPanel(String title, ArrayList<Path> results)
	{
		this._title = title;
		this._results = results;
		initializePanel();
	}

	/** Listener for the Download button */
	class DownloadButtonListener implements ActionListener {
		// calls downloadResultTable method if no error
		@Override
		public void actionPerformed(ActionEvent e) {
			try {
				downloadResultTable();
			} catch (IOException e1) {
				JOptionPane.showMessageDialog(null, "Unable to download result due to unexpected error");
			}
		}
	}

	/** Listener for the discard button */
	class DiscardButtonListener implements ActionListener {

		// Discard the entire currently selected result panel tab if user chooses yes
		@Override
		public void actionPerformed(ActionEvent e) {

			int choice = JOptionPane.showConfirmDialog(null, "Discarded result will be remove permanently. Continue?");
			if (choice != 0) return; // quit if they say no or cancel

			Container btnParent = _discardBtn.getParent();
			Container panelParent = btnParent.getParent();
			panelParent.remove(btnParent);
		}
	}

	/**
	 * Sets up all the components in the panel 
	 */
	private void initializePanel() {
		this.setLayout(new GridBagLayout());

		setUpDownloadBtn();
		setUpDiscardBtn();
		setupTable();
	}

	/**
	 * Sets up the download button
	 */
	private void setUpDownloadBtn()
	{
		_downloadBtn = new JButton("Download");
		_downloadBtn.addActionListener(new DownloadButtonListener());

		GridBagConstraints constraint = new GridBagConstraints();
		constraint.fill = GridBagConstraints.NONE;
		constraint.anchor = GridBagConstraints.LINE_START;
		constraint.weightx = 0;
		constraint.gridx = 0;
		constraint.gridy = 0;
		constraint.gridwidth = 1;

		this.add(_downloadBtn, constraint);
	}

	/**
	 * Sets up the delete button
	 */
	private void setUpDiscardBtn()
	{
		_discardBtn = new JButton("Discard");
		_discardBtn.addActionListener(new DiscardButtonListener());

		GridBagConstraints constraint = new GridBagConstraints();
		constraint.fill = GridBagConstraints.NONE;
		constraint.anchor = GridBagConstraints.LINE_START;
		constraint.weightx = 0;
		constraint.gridx = 1;
		constraint.gridy = 0;
		constraint.gridwidth = 1;

		this.add(_discardBtn, constraint);
	}

	/**
	 * Initializes a JPanel given the results from the plugin
	 * @param results the results from the plugin
	 */
	private void setupTable()
	{
		Object[] columnNames = new Object[] { "Path index", "Path score", "Path" };
		Object[][] rowData = new Object[_results.size()][columnNames.length];

		for (int i = 0; i < _results.size(); i++)
		{
			rowData[i][0] = i + 1;
			rowData[i][1] = _results.get(i).weight;
			rowData[i][2] = pathAsString(_results.get(i));
		}

		// overrides the default table model to make all cells non editable
		class NonEditableModel
		extends DefaultTableModel
		{

			NonEditableModel(Object[][] data, Object[] colNames)
			{
				super(data, colNames);
			}


			@Override
			public boolean isCellEditable(int row, int column)
			{
				return false;
			}
		}

		// populates the table with the path data
		_resultTable =
				new JTable(new NonEditableModel(rowData, columnNames));

		// fixes column widths
		TableColumn index = _resultTable.getColumnModel().getColumn(0);
		index.setMaxWidth(100);

		TableColumn score = _resultTable.getColumnModel().getColumn(1);
		score.setMinWidth(150);
		score.setMaxWidth(200);

		TableColumn path = _resultTable.getColumnModel().getColumn(2);
		path.setMinWidth(200);

		// table automatically resizes to fit path column
		_resultTable.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);

		// scrollable panel
		JScrollPane scrollPane = new JScrollPane(_resultTable, 
				JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		_resultTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
		scrollPane.setMinimumSize(scrollPane.getPreferredSize());

		GridBagConstraints constraint = new GridBagConstraints();
		constraint.fill = GridBagConstraints.BOTH;
		constraint.anchor = GridBagConstraints.LINE_START;
		constraint.weightx = 1;
		constraint.gridx = 0;
		constraint.gridy = 1;
		constraint.gridwidth = 2;

		this.add(scrollPane, constraint);
	}

	/**
	 * The method is triggered by DownloadButtonListener
	 * Creates a dialogue for user to save the result tables
	 * @throws IOException
	 */
	private void downloadResultTable() throws IOException {

		// override approveSelection method to warn if user overwrites a existing file when saving
		JFileChooser fc = new JFileChooser() {
			@Override
			public void approveSelection(){
				File f = getSelectedFile();
				if (f.exists() && getDialogType() == SAVE_DIALOG) {
					int result = JOptionPane.showConfirmDialog(this,
							"The file exists, overwrite?","Existing file",JOptionPane.YES_NO_CANCEL_OPTION);
					switch(result) {
					case JOptionPane.YES_OPTION:
						super.approveSelection();
						return;
					case JOptionPane.NO_OPTION:
						return;
					case JOptionPane.CLOSED_OPTION:
						return;
					case JOptionPane.CANCEL_OPTION:
						cancelSelection();
						return;
					}
				}
				super.approveSelection();
			}
		};
		
		fc.setDialogTitle("Sepcify the file to save"); // title of the dialogue
		int userSelection = fc.showSaveDialog(null);

		if (userSelection == JFileChooser.APPROVE_OPTION) {
			File fileToSave = fc.getSelectedFile();
			BufferedWriter writer = new BufferedWriter(new FileWriter(fileToSave.getAbsolutePath()));

			for(int i = 0; i < _resultTable.getColumnCount(); i++) {
				writer.write(_resultTable.getColumnName(i));
				writer.write("\t");
			}

			for (int i = 0; i < _resultTable.getRowCount(); i++) {
				writer.newLine();
				for(int j = 0; j < _resultTable.getColumnCount(); j++) {
					writer.write((_resultTable.getValueAt(i, j).toString()));
					writer.write("\t");;
				}
			}
			
			writer.close();
			JOptionPane.showMessageDialog(null, "Download successful");
		
		} else {
			JOptionPane.showMessageDialog(null, "Download canceled");
		}
	}

	/**
	 * Converts a path to a string concatenating the node names A path in the
	 * network involving A -> B -> C would return A|B|C
	 *
	 * @param p
	 *            the path to convert to a string
	 * @return the concatenation of the node names
	 */
	private String pathAsString(Path p)
	{
		// builds the path string without supersource/supertarget [1,len-1]
		StringBuilder currPath = new StringBuilder();
		for (int i = 1; i < p.size() - 1; i++)
			currPath.append(p.nodeIdMap.get(p.get(i)) + "|");

		currPath.setLength(currPath.length() - 1);

		return currPath.toString();
	}

	@Override
	public Component getComponent() {
		return this;
	}

	@Override
	public CytoPanelName getCytoPanelName() {
		return CytoPanelName.EAST;
	}

	@Override
	public Icon getIcon() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getTitle() {
		return "Result " + _title;
	}
}