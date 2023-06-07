package current;

import java.awt.BorderLayout;
import java.awt.Desktop;
import java.awt.LayoutManager;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

import utils.StringUtilities;

/**
 * Description: skims pdf bank statements and extracts entries
 * 
 * @author Michael Frake
 * @version Mar 23, 2022
 */

public class Extract {

	public static ArrayList<String> paymentTypes = new ArrayList<String>(
			Arrays.asList("ACH", "Debit", "Card", "Bill", "Payment", "POS"));

	public static ArrayList<String> ignore = new ArrayList<String>(Arrays.asList("BP-Amazon", "Bank of America",
			"M Life", "BP-Dillard's", "COMENITY", "BARCLAY", "Spirit", "BP-Sam's Club", "PAYPAL", "Transfer", "ATM"));

	private static float totalDeposits = 0;
	private static float totalChecks = 0;

	private static JFrame frame;

	public static void main(String[] args) {

		frame = new JFrame("Statements Extractor");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		JPanel panel = new JPanel();
		LayoutManager layout = new BorderLayout(10, 10);
		panel.setLayout(layout);

		JLabel instructions = new JLabel("Select the statements folder.");
		instructions.setHorizontalAlignment(JLabel.CENTER);
		instructions.setVerticalAlignment(JLabel.CENTER);
		JButton button = new JButton("Extract");

		button.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				String defaultFilePath = "G:\\My Drive\\Finance\\Taxes\\";
				File defaultFile = new File(defaultFilePath);
				JFileChooser fileChooser = new JFileChooser(
						defaultFile.exists() ? defaultFilePath : System.getProperty("user.dir"));
				fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
				int option = fileChooser.showOpenDialog(frame);
				if (option == JFileChooser.APPROVE_OPTION) {
					File file = fileChooser.getSelectedFile();
					try {
						extract(file, file.getCanonicalPath());
						System.out.println("Extraction completed");
						System.exit(0);
					} catch (IOException e1) {
						JOptionPane.showMessageDialog(frame, "Problem opening file.");
					}
				} else {
					JOptionPane.showMessageDialog(frame, "No file selected.");
				}
			}
		});

		panel.add(instructions, BorderLayout.NORTH);
		panel.add(new JPanel(), BorderLayout.WEST);
		panel.add(new JPanel(), BorderLayout.EAST);
		panel.add(button, BorderLayout.CENTER);
		panel.add(new JPanel(), BorderLayout.SOUTH);

		frame.getContentPane().add(panel);
		frame.pack();
		frame.setSize(frame.getWidth() + 150, frame.getHeight() + 10);
		frame.setLocationRelativeTo(null);
		frame.setVisible(true);

		/*File file = new File("G:\\My Drive\\Finance\\Taxes\\2021\\Bank Statements");
		try {
			extract(file, file.getCanonicalPath());
		} catch (IOException e) {
			e.printStackTrace();
		}*/

		/*extract(new File("G:\\My Drive\\Finance\\Taxes\\2019\\Statements"),
				"G:\\My Drive\\Finance\\Taxes\\2019\\Statements");
		*/
	}

	static String getText(File pdfFile) throws IOException {
		PDDocument doc = Loader.loadPDF(pdfFile);
		return new PDFTextStripper().getText(doc);
	}

	static void extract(File folder, String outputDirectory) {
		new File(folder.getAbsolutePath() + "\\masterOutput.csv").delete();
		String masterOutput = "";
		for (final File fileEntry : folder.listFiles()) {
			if (fileEntry.isDirectory()) {
				//
			} else {
				try {
					System.out.println("processing " + fileEntry.getName());
					masterOutput += outputTaxCSV(fileEntry);
				} catch (Exception e) {
					e.printStackTrace();
					JOptionPane.showMessageDialog(frame, "Invalid file in directory.");
				}
			}
		}
		masterOutput += "Total deposits:," + totalDeposits + "\n";
		masterOutput += "Total checks:," + totalChecks + "\n";

		FileWriter fw = null;
		String outputFile = outputDirectory + "\\masterOutput.csv";
		try {
			fw = new FileWriter(outputFile);
		} catch (IOException e) {
			e.printStackTrace();
			JOptionPane.showMessageDialog(frame, "Couldn't open file.");
		}
		BufferedWriter bw = new BufferedWriter(fw);
		PrintWriter pw = new PrintWriter(bw);

		pw.print(masterOutput);
		try {
			bw.close();
			pw.close();
			fw.close();
		} catch (IOException e) {
			e.printStackTrace();
			JOptionPane.showMessageDialog(frame, "Couldn't close read/write file operations.");
		}

		try {
			Desktop.getDesktop().open(new File(outputFile));
		} catch (Exception e) {
			JOptionPane.showMessageDialog(frame, "Couldn't open output file.");
			e.printStackTrace();
		}

	}

	static String outputTaxCSV(File inputFile) {
		String text = null;
		try {
			text = getText(inputFile);
		} catch (IOException e) {
			// header exception
			e.printStackTrace();
		}

		String outputFileName = inputFile.getName();
		outputFileName = outputFileName.substring(0, outputFileName.indexOf(".")) + ".csv";

		String output = "";

		float amount = 0;
		int currentLine = 0;

		if (text == null)
			return "";
		String[] lines = text.split("\n");
		linesLoop: for (String line : lines) {
			currentLine++;
			if (line.indexOf("Deposits or Other Credits for") > -1) {
				line = line.substring(line.indexOf("$") + 1).replaceAll(",", "");
				totalDeposits += Float.parseFloat(line);
			} else if (line.indexOf("Drafts Cleared for") > -1) {
				line = line.substring(line.indexOf("$") + 1).replaceAll(",", "");
				totalChecks += Float.parseFloat(line);
			}
			if (line.indexOf("Withdrawal") == -1 || line.indexOf("/") == -1) {
				continue;
			}
			for (String s : ignore) {
				if (line.indexOf(s) > -1)
					continue linesLoop;
			}
			String[] tokens = line.split("[ \t]");
			int i = 0;
			String vendor = "", date = "", paymentType = "";
			tokenScan: for (String token : tokens) {
				i++;
				if (i == 1) {
					continue tokenScan;
				}
				if (i == 2) {
					if (token.indexOf("/") == -1)
						break tokenScan;
					date = token;
					continue tokenScan;
				}
				if (token.equals("Withdrawal"))
					continue tokenScan;
				if (paymentTypes.contains(token)) {
					paymentType += token;
				} else if (i == tokens.length - 1) {
					if (token.startsWith("-") && token.length() > 1) {
						try {
							amount = Float.parseFloat(token.substring(1).replaceAll(",", ""));
						} catch (NumberFormatException e) {
							System.out.println("parse problem on " + token);
						}
						continue tokenScan;
					} else {
						vendor += " " + token;
					}
					String amountLine = lines[currentLine + 1];
					int amountLocation = amountLine.lastIndexOf("-") + 1;
					if (amountLocation == -1)
						continue tokenScan;
					int amountOffset = amountLine.substring(amountLocation).indexOf(".");
					String amountString = amountLine.substring(amountLocation, amountLocation + amountOffset + 3);
					amountString = amountString.replaceAll(",", "");
					try {
						amount = Float.parseFloat(amountString);
					} catch (NumberFormatException e) {
						continue linesLoop;
					}
				} else {
					vendor += " " + token.trim();
				}
			}

			/* data cleaning */

			if (date.isEmpty())
				continue;
			// System.out.print(vendor);
			vendor = vendor.replaceAll("FVID", "");
			if (vendor.contains("BP")) {
				vendor += " " + lines[currentLine].trim();
				vendor = vendor.replaceAll("[^A-z\\s]", "").replaceAll("\n", "").replaceAll("\r", "");
			} else {
				vendor = vendor.replaceAll("[^A-z\\s]", "");
			}
			vendor = vendor.replaceAll("  ", " ").trim();
			// System.out.println(vendor);

			if (paymentType.contains("Debit"))
				paymentType = "Debit";
			else if (paymentType.contains("Bill"))
				paymentType = "Bank Bill Payment";
			else if (paymentType.contains("ACH"))
				paymentType = "ACH";
			else if (paymentType.contains("POS"))
				paymentType = "POS";
			else
				paymentType = "ACH";

			String category = "";
			if (vendor.contains("FAMILY DOLLAR") || vendor.contains("WALMART") || vendor.contains("WalMart")
					|| vendor.contains("WALGREENS") || vendor.contains("KROGER") || vendor.contains("Club")
					|| vendor.contains("CLUB") || vendor.contains("DYNAREX") || vendor.contains("GLOVE")) {
				category = "Supplies";
			} else if (vendor.contains("DEPOT") || vendor.contains("LOWE") || vendor.contains("THE HOME DE")
					|| vendor.contains("HARBOR FREI")) {
				category = "Maintenance";
			} else if (vendor.contains("WAITR") || vendor.contains("BJ's") || vendor.contains("CAESARS")
					|| vendor.contains("CHICK") || vendor.contains("SOMBRERO") || vendor.contains("FOOD")
					|| vendor.contains("JUCYS") || vendor.contains("BUCEES") || vendor.contains("CAFE")
					|| vendor.contains("PIZZA") || vendor.contains("STARBUCKS") || vendor.contains("TACO")
					|| vendor.contains("ROADHOUSE") || vendor.contains("BUTCHER") || vendor.contains("THE CATCH")
					|| vendor.contains("BURGER") || vendor.contains("YANG") || vendor.contains("SONIC")
					|| vendor.contains("ROTOLOS") || vendor.contains("MCDONALDS") || vendor.contains("CASA OLE")
					|| vendor.contains("BAR") || vendor.contains("GRILL") || vendor.contains("CANES")
					|| vendor.contains("OUTBACK") || vendor.contains("PANDA") || vendor.contains("DONUT")
					|| vendor.contains("BROOKSHIR") || vendor.contains("SUPER F")) {
				category = "Meals";
			} else if (vendor.contains("METAL MAFIA") || vendor.contains("NEILMED") || vendor.contains("NEOMETAL")
					|| vendor.contains("BODY JEWELRY") || vendor.contains("HOLLYWOOD BODY") || vendor.contains("GEMS")
					|| vendor.contains("KINGPIN") || vendor.contains("JUNIPURR") || vendor.contains("ANATOMETAL")
					|| vendor.contains("HENRY SCHEIN") || vendor.contains("INDUSTRIAL STR")
					|| vendor.contains("BODY VISION")) {
				category = "Cost of goods sold";
			} else if (vendor.contains("DILLARD") || vendor.contains("Dillards") || vendor.contains("OIL BOWL")
					|| vendor.contains("UNIMAX") || vendor.contains("WASH MASTER") || vendor.contains("PETSMART")
					|| vendor.contains("BOUTIQUE") || vendor.contains("BED BATH")) {
				category = "Employee benefits programs";
			} else if (vendor.contains("HOBBYLOBBY") || vendor.contains("MICHAELS")) {
				category = "Office expenses";
			} else if (vendor.contains("CHRISTUS") || vendor.contains("Aid")) {
				category = "Insurance and medical";
			} else if (vendor.contains("HOTWIRE") || vendor.contains("RENTACAR") || vendor.contains("RENT A CAR")
					|| vendor.contains("Ticket") || vendor.contains("AIRLINES") || vendor.contains("SHELL")
					|| vendor.contains("VACATION") || vendor.contains("WINGATE") || vendor.contains("ZIPPY JS")
					|| vendor.contains("FUEL") || vendor.contains("EXXON") || vendor.contains("EXPRESS")
					|| vendor.contains("INN") || vendor.contains("SUITE") || vendor.contains("HOTEL")
					|| vendor.contains("CHEVRON") || vendor.contains("DFW") || vendor.contains("EZ MART")
					|| vendor.contains("PRICELINE") || vendor.contains("SAMS TOWN") || vendor.contains("KYLES")
					|| vendor.contains("TIGER MART") || vendor.contains("LYFT") || vendor.contains("RIDE")
					|| vendor.contains("AIRBNB")) {
				category = "Travel";
			} else if (vendor.contains("TAX") || vendor.contains("Charge")
					|| vendor.contains("ASSOCIATION OF PROFESS")) {
				category = "Taxes and licenses";
			} else if (vendor.contains("STORAGE") || vendor.contains("SAFEBOX")) {
				category = "Rent or lease";
			} else if (vendor.contains("BP")) {
				if (vendor.contains("802") || vendor.contains("Regency")) {
					if (vendor.contains("Mort")) {
						vendor = "802 Regency Mortage";
						category = "Mortgage";
					} else if (vendor.contains("Cit")) {
						vendor = "802 Regency City of Longview Water";
						category = "Utilities";
					} else if (vendor.contains("Cen")) {
						vendor = "802 Regency Center Point Energy";
						category = "Utilities";
					} else if (vendor.contains("Lon")) {
						vendor = "802 Regency Longview Cable";
						category = "Utilities";
					} else if (vendor.contains("SWE")) {
						vendor = "802 Regency SWEPCO";
						category = "Utilities";
					}
				} else if (vendor.contains("Fugis")) {
					if (vendor.contains("City")) {
						vendor = "Fugi's City of Longview Water";
						category = "Utilities";
					} else if (vendor.contains("ATT")) {
						vendor = "Fugi's AT&T";
						category = "Utilities";
					} else if (vendor.contains("SWEPCO")) {
						vendor = "Fugi's SWEPCO";
						category = "Utilities";
					}
				} else if (vendor.contains("Amazon")) {
					vendor = "Amazon";
					category = "Supplies";
				}
			} else if (vendor.contains("CABLE") || vendor.contains("UTILITY") || vendor.contains("ELECT")) {
				category = "Utilities";
			} else if (vendor.contains("STRAIGHTTALK") || vendor.contains("VONAGE")) {
				category = "Communication";
			} else {
				category = "Other expenses";
			}
			vendor = StringUtilities.toCamelCase(vendor);
			output += (date + "," + vendor + "," + category + "," + paymentType + "," + amount + ",Imported from "
					+ outputFileName.substring(0, outputFileName.indexOf(".")) + " statement") + "\n";
		}
		return output;
	}

}
