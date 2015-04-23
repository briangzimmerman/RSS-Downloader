package rssDownloader;

import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JSpinner;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SpinnerModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.table.DefaultTableModel;

public class DownloaderPanel extends JPanel{
	JPanel panel = new JPanel();
	JTextField urlBox, directoryBox;
	JButton downloadButton, directoryButton, closeButton;
	JComboBox fileTypes;
	JFileChooser directorySelector;
	JSpinner numDownloads;
	//JTable downloadsTable;
	Listener listener = new Listener();
	UpdateTableModel downloads;
	String[] acceptedTypes = {"mp3", "zip", "mp4", "wav", "wma"};
	String destination = null;

	public DownloaderPanel(){
		JTabbedPane tabs = new JTabbedPane();
		
		JComponent mainTab = makeMainPanel();
		tabs.addTab("RSS tab", null, mainTab, "Set url and destination folder");
		
		JComponent downloadsTab = makeDownloadsPanel();
		tabs.addTab("Downloads", null, downloadsTab, "Current and pending downloads");
		
		panel.add(tabs);
	}

	public JPanel getPanel(){
		return panel;
	}

	private JComponent makeMainPanel(){
		JPanel mainPanel = new JPanel(false);
		mainPanel.setLayout(new GridBagLayout());

		//url option
		addItem(mainPanel, new JLabel("Enter url of RSS Feed: "), 0, 0, 1, 1,
				GridBagConstraints.EAST);
		urlBox = new JTextField(25);
		addItem(mainPanel, urlBox, 1, 0, 2, 1, GridBagConstraints.WEST);

		//download path
		addItem(mainPanel, new JLabel("Enter destination directory: "), 0, 2,
				1, 1, GridBagConstraints.EAST);
		directoryBox = new JTextField(25);
		addItem(mainPanel, directoryBox, 1, 2, 1, 1, GridBagConstraints.WEST);
		directoryButton = new JButton("Select Path");
		directoryButton.addActionListener(listener);
		addItem(mainPanel, directoryButton, 2, 2, 1, 1, GridBagConstraints.WEST);

		//filetype dropdown
		addItem(mainPanel, new JLabel("Choose file type: "), 0, 4, 1, 1,
				GridBagConstraints.EAST);
		fileTypes = new JComboBox(acceptedTypes);
		addItem(mainPanel, fileTypes, 1, 4, 1, 1, GridBagConstraints.WEST);
		
		//number of parallel downloads
		addItem(mainPanel, new JLabel("Number of parallel downloads: "), 0, 5, 1, 1,
				GridBagConstraints.EAST);
		SpinnerModel restraints = new SpinnerNumberModel(5, 1, 10, 1);
		numDownloads = new JSpinner(restraints);
		addItem(mainPanel, numDownloads, 1, 5, 1, 1, GridBagConstraints.WEST);
		
		//download and close buttons
		Box buttonBox = Box.createHorizontalBox();
		closeButton = new JButton("Close");
		closeButton.addActionListener(listener);
		downloadButton = new JButton("Download");
		downloadButton.addActionListener(listener);
		buttonBox.add(downloadButton);
		buttonBox.add(buttonBox.createHorizontalStrut(20));
		buttonBox.add(closeButton);
		addItem(mainPanel, buttonBox, 1, 6, 1, 1, GridBagConstraints.NORTH);
		
		return mainPanel;
	}
	
	private JComponent makeDownloadsPanel(){
		JPanel downloadsPanel = new JPanel(false);
		downloads = new UpdateTableModel();
	/*	downloads.addColumn("File Name");
		downloads.addColumn("Status");
		downloads.addColumn("Progress"); */
		JTable table = new JTable(downloads);
		table.getColumn("Progress").setCellRenderer(new ProgressCellRenderer());
//		table.getColumn("Status").setCellRenderer(new ProgressCellRenderer());
		addItem(downloadsPanel, table, 0, 0, 1, 1, GridBagConstraints.EAST);
		return downloadsPanel;
	}

	private void addItem(JPanel p, JComponent c, int x, int y, int width, int height, int align){
		GridBagConstraints gc = new GridBagConstraints();
		gc.gridx = x;
		gc.gridy = y;
		gc.gridheight = height;
		gc.weightx = 100.0;
		gc.weighty = 100.0;
		gc.insets = new Insets(5, 5, 5, 5);
		gc.anchor = align;
		gc.fill = GridBagConstraints.NONE;
		p.add(c, gc);
	}

	private class Listener extends JFrame implements ActionListener{
		private Validator validator = new Validator();

		public void actionPerformed(ActionEvent action){
			if(action.getSource() == downloadButton)
				download();
			else if(action.getSource() == directoryButton)
				selectDirectory();
			else if(action.getSource() == closeButton)
				close();
		}

		private void download(){
			String url = urlBox.getText();
			int numParrallelDownloads = (int) numDownloads.getValue();
			if(validator.isValidURL(url) && validator.isValidDirectory(destination)
					&& validator.isValidDownloadNum(numParrallelDownloads)){
				String fileType = (String) fileTypes.getSelectedItem();
				HashMap<String, String> filesSources = parseHTML(url, fileType);
				Downloader downloader = new Downloader(destination, numParrallelDownloads, downloads);
				downloader.download(filesSources);
			} else {
				if(!validator.isValidURL(url))
					JOptionPane.showMessageDialog(new JFrame(), "Please enter a valid url.");
				else if(!validator.isValidDirectory(destination))
					JOptionPane.showMessageDialog(new JFrame(), "Please select a valid directory.");
				else if(!validator.isValidDownloadNum(numParrallelDownloads))
					JOptionPane.showMessageDialog(new JFrame(), "Number of parrallel downloads must be greater than 0.");
			}
		}

		private HashMap<String, String> parseHTML(String url, String format){
			HashMap<String, String> filesAndSources = new HashMap<String, String>();
			try {
				URL feed = new URL(url);
				BufferedReader html = new BufferedReader(new InputStreamReader(feed.openStream()));
				String fileRegex = 
						"(?:http:\\/\\/|https:\\/\\/)?(?:www)?[-a-zA-Z0-9@:%_\\+.~%?\\/=]+\\/((?:[-a-zA-Z0-9_.]+)\\."+format+")";
				Pattern filePattern = Pattern.compile(fileRegex);
				String htmlLine;
				while((htmlLine = html.readLine()) != null){
					Matcher foundFile = filePattern.matcher(htmlLine);
					if(foundFile.find())
						filesAndSources.put(foundFile.group(1), foundFile.group(0));
				}
			} catch (MalformedURLException e) {
				System.out.println("Malformed URL");
			} catch (IOException e) {
				System.out.println("IOException");
			} finally {
				return filesAndSources;
			}
		}

		private void selectDirectory(){
			directorySelector = new JFileChooser();
			directorySelector.setFileHidingEnabled(true);
			directorySelector.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
			int result = directorySelector.showOpenDialog(null);
			if(result == JFileChooser.APPROVE_OPTION){
				destination = directorySelector.getSelectedFile().getAbsolutePath() + "/";
				directoryBox.setText(destination);
			}
		}

		private void close(){
			System.exit(0);
		}
	}
}
