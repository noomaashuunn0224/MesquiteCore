/* Mesquite source code.  Copyright 1997-2011 W. Maddison and D. Maddison.
Version 2.75, September 2011.
Disclaimer:  The Mesquite source code is lengthy and we are few.  There are no doubt inefficiencies and goofs in this code. 
The commenting leaves much to be desired. Please approach this source code with the spirit of helping out.
Perhaps with your help we can be more than a few, and make Mesquite better.

Mesquite is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY.
Mesquite's web site is http://mesquiteproject.org

This source code and its compiled class files are free and modifiable under the terms of 
GNU Lesser General Public License.  (http://www.gnu.org/copyleft/lesser.html)
 */
package mesquite.lib.duties;

import java.awt.*;
import java.util.*;
import java.io.*;
import mesquite.lib.*;
import mesquite.lib.characters.CharacterData;


/* ======================================================================== */
/**Superclass of file interpreting modules (e.g., NEXUS file reader/writer).  Different subclasses are expected
to read different data file formats.  Example module: "Interpret NEXUS files" (class InterpretNexus).  Example of use:
see BasicFileCoordinator.*/

public abstract class FileInterpreter extends MesquiteModule  {
	public static final int CURRENTDELIMITER=0;
	public static final int MACOSDELIMITER=1;
	public static final int WINDOWSDELIMITER=2;
	public static final int UNIXDELIMITER=3;
	public int lineDelimiter = CURRENTDELIMITER;
	public boolean writeOnlySelectedData = false;
	public boolean writeOnlyIncludedData = true;
	public boolean writeOnlySelectedTaxa = false;
	public boolean writeTaxaWithAllMissing = true;  //default changed to true as true  after 2. 75
	public boolean writeExcludedCharacters = true;
	
	protected static int REPLACEDATA = 0;
	protected static int REPLACEIFEMPTY = 1;
	protected static int ADDASNEW = 2;

//	protected static boolean defaultReplaceDataOfTaxonWithSameName = false;
	protected static int defaultReplaceDataOfTaxonWithSameNameInt = ADDASNEW;
	protected static boolean defaultHasQueriedAboutSameNameTaxa = false;
//	protected boolean replaceDataOfTaxonWithSameName = defaultReplaceDataOfTaxonWithSameName;
	protected int replaceDataOfTaxonWithSameNameInt = defaultReplaceDataOfTaxonWithSameNameInt;
	protected boolean hasQueriedAboutSameNameTaxa = defaultHasQueriedAboutSameNameTaxa;
	protected int totalFilesToImport = 1;
	protected int importFileNumber = 0;
	protected boolean multiFileImport = false;
	protected int lastNewTaxonFilled = -1;
	protected int maximumTaxonFilled =-1;

	public Class getDutyClass() {
		return FileInterpreter.class;
	}
	public String getFunctionIconPath(){
		return getRootImageDirectoryPath() + "functionIcons/fileInterpret.gif";
	}

	/*.................................................................................................................*/
	/** returns whether this module is requesting to appear as a primary choice */
	public boolean requestPrimaryChoice(){
		return true;  
	}

	/*.................................................................................................................*/
	/** returns whether this module is requesting to appear as a primary choice */
	public void reset(){
//		replaceDataOfTaxonWithSameName = defaultReplaceDataOfTaxonWithSameName;
		replaceDataOfTaxonWithSameNameInt = defaultReplaceDataOfTaxonWithSameNameInt;
		hasQueriedAboutSameNameTaxa = defaultHasQueriedAboutSameNameTaxa;
	}

	/** This is deprecated and should not be overridden.  The subsequent methods should be used instead.*/
	public boolean canExport(){
		return false;
	}
	/** returns whether module is able ever to export.*/
	public boolean canExportEver(){
		return canExport();
	}
	/** returns whether module has something it can export in the project.  Should be overridden*/
	public boolean canExportProject(MesquiteProject project){
		return canExport();
	}

	/** returns whether module can export a character data matrix of the given type.  Should be overridden*/
	public boolean canExportData(Class dataClass){
		return canExport();
	}


	/** Returns whether the module can read (import) files with data of class dataClass*/
	public boolean canImport(Class dataClass){
		return canImport();
	}
	/** Returns whether the module can read (import) files */
	public boolean canImport(){
		return false;
	}
	/** Returns whether the module can read (import) files considering the passed argument string (e.g., fuse) */
	public boolean canImport(String arguments, Class dataClass){
		boolean fuse = parser.hasFileReadingArgument(arguments, "fuseTaxaCharBlocks");
		if (fuse)
			return false;
		return canImport(dataClass);
	}
	/** Returns whether the module can read (import) files considering the passed argument string (e.g., fuse) */
	public boolean canImport(String arguments){
		return canImport(arguments,null);
	}

	/** reads a file using the methods of MesquiteFile and places its data into the given MesquiteProject
	which will already have been instantiated.  Recall that a MesquiteProject is not the external file on disk or server,
	but is rather the collection of taxa, data, trees, etc. that together typically make up the information in
	a NEXUS file.  The external file is referred to by the MesquiteFile.*/
	public abstract void readFile(MesquiteProject mf, MesquiteFile mNF, String arguments);

	//	public abstract void readFile();

	public String getDutyName() {
		return "File Interpreter";
	}

	/*.................................................................................................................*/
	public String getLineEnding() {
		if (lineDelimiter == CURRENTDELIMITER) 
			return StringUtil.lineEnding();
		else if (lineDelimiter == MACOSDELIMITER) 
			return "\r";
		else if (lineDelimiter == WINDOWSDELIMITER) 
			return "\r\n";
		else if (lineDelimiter == UNIXDELIMITER) 
			return "\n";
		return StringUtil.lineEnding();
	}

	/*.................................................................................................................*/
	public String preferredDataFileExtension() {
		return "";
	}

	/*.................................................................................................................*/
	protected String stripNex(String name) {
		if (name == null)
			return null;
		int length = name.length();
		if (length<4)
			return name;
		String last4 = name.substring(length-4, length);
		if (last4.equalsIgnoreCase(".nex")) 
			return name.substring(0, length-4);
		else
			return name;
	}

	/*.................................................................................................................*/
	public void setLineDelimiter(int newDelimiter) {
		lineDelimiter = newDelimiter;
	}

	public boolean isSubstantive(){
		return false;  
	}
	/*.................................................................................................................*/
	public void finishImport(ProgressIndicator progIndicator, MesquiteFile file, boolean abort){
		if (progIndicator!=null)
			progIndicator.goAway();
		if (file!=null) 
			file.closeReading();
		if (abort){ //���		
			if (file!=null)
				file.close();
			resetAllMenuBars();
			//decrementMenuResetSuppression();
			return;
		}
		/*
		 * 	FileCoordinator fc = getFileCoordinator();
		if (fc!=null && getProject().getNumberCharMatrices()>0) {
			MesquiteModule dwc = (MesquiteModule)fc.doCommand("getEmployee", "'Data Window Coordinator'",CommandChecker.defaultChecker);
			if (dwc!=null) {
				for (int i=0; i< getProject().getNumberCharMatrices(); i++)
					dwc.doCommand("showDataWindow", ""+i,CommandChecker.defaultChecker);
			}
		}
		 */
	}
	/*.................................................................................................................*/
	public void saveExportedFileWithExtension(StringBuffer outputBuffer, String arguments, String suggestedFileEnding) {
		if (outputBuffer == null)
			return;
		String output = outputBuffer.toString();

		String name = getProject().getHomeFileName();
		if (name==null)
			name = "untitled." + suggestedFileEnding;
		else if (suggestedFileEnding.equalsIgnoreCase("NEX"))
			name = stripNex(name)+"2." + suggestedFileEnding;
		else 
			name = stripNex(name) + "." + suggestedFileEnding;
		saveExportedFile(output, arguments, name);

	}
	/*.................................................................................................................*/
	public String getPathForExport(String arguments, String suggestedFileName, MesquiteString dir, MesquiteString fn) {
		//check arguments for filename and directory
		String tempFileName=null;
		String tempDirectoryName=null;
		String token = parser.getFirstToken(arguments);
		while (token !=null){
			if (token.equalsIgnoreCase("file")){
				parser.getNextToken(); // =
				tempFileName = parser.getNextToken();
			}
			else if (token.equalsIgnoreCase("directory")){
				parser.getNextToken(); // =
				tempDirectoryName = parser.getNextToken();
				if (tempDirectoryName != null && !tempDirectoryName.endsWith(MesquiteFile.fileSeparator)) //(tempDirectoryName.length()-1) != MesquiteFile.fileSeparator.charAt(0))
					tempDirectoryName += MesquiteFile.fileSeparator;
			}
			token = parser.getNextToken();
		}
		if (StringUtil.blank(tempFileName)){
			String queryMessage = "Export to file";
			MainThread.incrementSuppressWaitWindow();
			MesquiteFileDialog fdlg= new MesquiteFileDialog(containerOfModule(), queryMessage, FileDialog.SAVE);   // Save File dialog box
			if (suggestedFileName == null)
				suggestedFileName = "untitled";
			fdlg.setFile(suggestedFileName);

			fdlg.setBackground(ColorTheme.getInterfaceBackground());
			fdlg.setVisible(true);
			tempFileName=fdlg.getFile();
			tempDirectoryName=fdlg.getDirectory();
			//	fdlg.dispose();
			MainThread.decrementSuppressWaitWindow();
		}
		if (!StringUtil.blank(tempFileName)) {
			if (StringUtil.blank(tempDirectoryName))
				tempDirectoryName = "";
			if (dir!=null)
				dir.setValue(tempDirectoryName);
			if (fn != null){
				fn.setValue(tempFileName);
			}
			return tempDirectoryName+tempFileName;
		}
		return null;

	}
	/*.................................................................................................................*/
	public void saveExportedFile(String output, String arguments, String suggestedFileName) {

		String path = getPathForExport(arguments, suggestedFileName, null, null);
		if (path!=null) {
			logln("Exporting file to " + path);
			MesquiteFile.putFileContents(path, output, true);
			logln("Export complete.");
		}
	}
	/** Returns the Character data as a StringBuffer in the Interperter's format.  This method should be overridden for those Interpreters that can provide this service. */
	/*.................................................................................................................*/
	public  StringBuffer getDataAsFileText(MesquiteFile file, CharacterData data) {
		return null;
	}
	public int getTotalFilesToImport() {
		return totalFilesToImport;
	}
	public void setTotalFilesToImport(int totalFilesToImport) {
		this.totalFilesToImport = totalFilesToImport;
	}
	public int getImportFileNumber() {
		return importFileNumber;
	}
	public void setImportFileNumber(int importFileNumber) {
		this.importFileNumber = importFileNumber;
	}
	public boolean getMultiFileImport() {
		return multiFileImport;
	}
	public void setMultiFileImport(boolean multiFileImport) {
		this.multiFileImport = multiFileImport;
	}
	public int getLastNewTaxonFilled() {
		return lastNewTaxonFilled;
	}
	public void setLastNewTaxonFilled(int lastNewTaxonFilled) {
		this.lastNewTaxonFilled = lastNewTaxonFilled;
		maximumTaxonFilled = lastNewTaxonFilled;
	}
	public int getMaximumTaxonFilled() {
		return maximumTaxonFilled;
	}
	public void setMaximumTaxonFilled(int maximumTaxonFilled) {
		this.maximumTaxonFilled = maximumTaxonFilled;
	}
	public void checkMaximumTaxonFilled(int taxonFilled) {
		if (taxonFilled> this.maximumTaxonFilled)
			 maximumTaxonFilled = taxonFilled;
	}


}



