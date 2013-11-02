package com.example.semantic;

import java.awt.FlowLayout;
import java.awt.event.MouseEvent;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.URL;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.event.MouseInputAdapter;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;

import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.sun.tools.xjc.XJCFacade;
import com.example.semantic.weather.CurrentObservation;

public class WeatherConverter {

	public static void main(String[] args) {
		
		LocationWithURL[] locations =
			{
				new LocationWithURL("Baltimore-Washington International Airport", "http://w1.weather.gov/xml/current_obs/KBWI.xml"),
				new LocationWithURL("Boca Raton Airport", "http://weather.gov/xml/current_obs/KBCT.xml"),
				new LocationWithURL("Bozeman/Gallatin", "http://w1.weather.gov/xml/current_obs/KBZN.xml"),
				new LocationWithURL("Molokai Airport", "http://weather.gov/xml/current_obs/PHMK.xml"),
				new LocationWithURL("Green Bay, Austin Straubel International Airport", "http://weather.gov/xml/current_obs/KGRB.xml"),
				new LocationWithURL("Anchorage International Airport", "http://weather.gov/xml/current_obs/PANC.xml")
			};
		
		// The Velocity template file
		final String vmFile = "Resources/weather-rdf.vm";
		
		// Output file where the model will be saved in Turtle format
		final String outputFile = "output.ttl";
		
		final CurrentObservation[] currentObservations = {null};
		final ByteArrayOutputStream vmOutputStream = new ByteArrayOutputStream();
		final Model rdfModel = ModelFactory.createDefaultModel();
		
		JFrame frame = new JFrame();
		frame.setLayout(new FlowLayout());
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		JButton generateButton = new JButton("Gen Bind Classes");
		generateButton.addMouseListener(new
					MouseInputAdapter(){

						@Override
						public void mouseClicked(MouseEvent arg0) {
							generateBindingClasses();							
						}
					}
				);
		
		final JComboBox<LocationWithURL> jComboLocations = new JComboBox<>(locations);
		
		
		JButton unmarshalButton = new JButton("Unmarshal");
		unmarshalButton.addMouseListener(new
					MouseInputAdapter(){

						@Override
						public void mouseClicked(MouseEvent arg0) {
							currentObservations[0] = unmarshal(((LocationWithURL)jComboLocations.getSelectedItem()).getURL());							
						}
					}
				);
		
		JButton velocityButton = new JButton("Velocity");
		velocityButton.addMouseListener(new
					MouseInputAdapter(){

						@Override
						public void mouseClicked(MouseEvent arg0) {
							executeVelocityTemplate(vmFile, currentObservations[0], vmOutputStream);
						}
					}
				);
		
		JButton jenaModelButton = new JButton("Create Jena Model");
		jenaModelButton.addMouseListener(new
					MouseInputAdapter(){

						@Override
						public void mouseClicked(MouseEvent arg0) {
							buildJenaModel(vmOutputStream, rdfModel);
						}
					}
				);
		
		JButton appendModelButton = new JButton("Append to Model");
		appendModelButton.addMouseListener(new
					MouseInputAdapter(){

						@Override
						public void mouseClicked(MouseEvent arg0) {
							appendToJenaModel(vmOutputStream, rdfModel);
						}
					}
				);
		
		JButton modelToFileButton = new JButton("Model to File");
		modelToFileButton.addMouseListener(new
					MouseInputAdapter(){

						@Override
						public void mouseClicked(MouseEvent arg0) {
							writeModelToFile(outputFile, rdfModel);
						}
					}
				);
		
		frame.add(generateButton);
		frame.add(jComboLocations);
		frame.add(unmarshalButton);
		frame.add(velocityButton);
		frame.add(jenaModelButton);
		frame.add(appendModelButton);
		frame.add(modelToFileButton);
		frame.pack();
		frame.setVisible(true);
		
		
	}

	/**
	 * 
	 */
	private static void generateBindingClasses() {
		// Prepare the strings for sending arguments to XJCFacade
		String targetPackageName = "com.example.semantic.weather";
		String XMLSchema = "Resources/current_observation.xsd";
		String targetDirectoryForGeneratedClasses = "gen";
		final String[] arguments = {"-p", targetPackageName, XMLSchema, "-d", targetDirectoryForGeneratedClasses};
		
		try {
			XJCFacade.main(arguments);
		} catch (Throwable e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		/* This will avoid shutting down after generating binding classes. 
		try {
			Process proc = Runtime.getRuntime().exec("java -cp jaxb-xjc-2.2.7.jar;jaxb-core-2.2.7.jar com.sun.tools.xjc.XJCFacade"
													+ " " + arguments[0]
													+ " " + arguments[1]
													+ " " + arguments[2]
													+ " " + arguments[3]
													+ " " + arguments[4]);
			InputStream err = proc.getErrorStream();
			BufferedReader reader = new BufferedReader(new InputStreamReader(err));
			String s = reader.readLine();
			if (s != null)
				System.err.println(s);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		 */
	}



	/**
	 * 
	 * @param outputFile
	 * @param rdfModel
	 */
	private static void writeModelToFile(String outputFile, Model rdfModel){
		try {
			// create the output file
			FileOutputStream outputStream = new FileOutputStream(outputFile);
			rdfModel.write(outputStream, "TURTLE");
			outputStream.flush();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/**
	 * 
	 * @param vmOutputStream
	 * @param rdfModel
	 */
	private static void buildJenaModel(ByteArrayOutputStream vmOutputStream, Model rdfModel) {
		rdfModel.removeAll();
		// build a jena model so we can serialize to Turtle
		ByteArrayInputStream modelInputStream = new ByteArrayInputStream(
				vmOutputStream.toByteArray());
		
		rdfModel.read(modelInputStream, null, "RDF/XML");
		
		rdfModel.write(System.out);
	}
	
	/**
	 * 
	 * @param vmOutputStream
	 * @param rdfModel
	 */
	private static void appendToJenaModel(ByteArrayOutputStream vmOutputStream, Model rdfModel) {
		// build a jena model so we can serialize to Turtle
		ByteArrayInputStream modelInputStream = new ByteArrayInputStream(
				vmOutputStream.toByteArray());
		
		rdfModel.read(modelInputStream, null, "RDF/XML");
		
		rdfModel.write(System.out);
	}

	/**
	 * 
	 * @param vmFile
	 * @param currentObservation
	 * @param vmOutputStream
	 */
	private static void executeVelocityTemplate(String vmFile,
			CurrentObservation currentObservation, ByteArrayOutputStream vmOutputStream){
		try {
			vmOutputStream.reset();
			// get the vm file input
			FileInputStream vmFileInput = new FileInputStream(vmFile);
			// execute our velocity template
			VelocityEngine engine = new VelocityEngine();
			engine.init();
			VelocityContext velocityContext = new VelocityContext();
			velocityContext.put("observation", currentObservation);
			// set up an output stream that we can redirect to the jena model
			
			Writer resultsWriter = new OutputStreamWriter(vmOutputStream);
			engine.evaluate(velocityContext, resultsWriter, "weatherRdf",
					new InputStreamReader(vmFileInput));
			resultsWriter.close();
			
			System.out.print(vmOutputStream.toString());
			
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			
		}
	}

	/**
	 * 
	 * @param xmlFile
	 * @return
	 */
	private static CurrentObservation unmarshal(String xmlFile){
		try{
			// get the xml file input
			URL xmlFileUrl = new URL(xmlFile);
			InputStream xmlFileInputStream = xmlFileUrl.openStream();
			// unmarshal the information from xml
			JAXBContext jaxbContext = JAXBContext
					.newInstance("com.example.semantic.weather");
			Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
			CurrentObservation currentObservation = (CurrentObservation) unmarshaller
					.unmarshal(xmlFileInputStream);
			return currentObservation;
		}
		catch (Exception e){
			System.out.print(e.toString());
			return null;
		}
	}
}


/**
 * A class for use in a JComboBox. The location will be displayed,
 * and an action listener (or whoever) can get the URL.
 * 
 */
class LocationWithURL{
	public LocationWithURL(String aLocation, String aURL){
		location = aLocation;
		uRL = aURL;
	}
	
	@Override
	public String toString(){
		return location;
	}
	
	public String getURL(){
		return uRL;
	}

	private String location;
	private String uRL;
}