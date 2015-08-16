/* Mesquite source code.  Copyright 1997 and onward, W. Maddison and D. Maddison. 


Disclaimer:  The Mesquite source code is lengthy and we are few.  There are no doubt inefficiencies and goofs in this code. 
The commenting leaves much to be desired. Please approach this source code with the spirit of helping out.
Perhaps with your help we can be more than a few, and make Mesquite better.

Mesquite is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY.
Mesquite's web site is http://mesquiteproject.org

This source code and its compiled class files are free and modifiable under the terms of 
GNU Lesser General Public License.  (http://www.gnu.org/copyleft/lesser.html)
 */
package mesquite.molec.TaxaListHasData;
/*~~  */


import mesquite.categ.lib.MolecularData;
import mesquite.categ.lib.MolecularState;
import mesquite.lists.lib.*;

import java.awt.Color;
import java.awt.Container;
import java.awt.Graphics;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.util.*;

import mesquite.lib.*;
import mesquite.lib.characters.*;
import mesquite.lib.duties.*;
import mesquite.lib.table.*;

/* ======================================================================== */
public class TaxaListHasData extends TaxonListAssistant  {
	
	Taxa taxa=null;
	MesquiteTable table = null;
	MatrixSourceCoord matrixSourceTask;
	//Taxa currentTaxa = null;
	MCharactersDistribution observedStates =null;
	CharacterData data = null;
	Associable tInfo = null;
	static String localCopyDataClipboard = "";
	static String localCopyDataTaxon = "";
	static CharacterData localCopyData = null;

	/*.................................................................................................................*/
	public String getName() {
		return "Has Data in Matrix";
	}
	public String getExplanation() {
		return "Indicates whether taxon has non-missing non-gap data in a given matrix." ;
	}
	public void getEmployeeNeeds(){  //This gets called on startup to harvest information; override this and inside, call registerEmployeeNeed
		EmployeeNeed e = registerEmployeeNeed(MatrixSourceCoord.class, getName() + "  needs a source of characters.",
				"The source of characters is arranged initially");
	}
	/*.................................................................................................................*/
	public boolean startJob(String arguments, Object condition, boolean hiredByName) {
		setSuppressEmployeeAutoRehiring(true);
		matrixSourceTask = (MatrixSourceCoord)hireEmployee(MatrixSourceCoord.class, "Source of character matrix (for " + getName() + ")"); 
		if (matrixSourceTask==null)
			return sorry(getName() + " couldn't start because no source of character matrices was obtained.");
		//	addMenuItem("Delete Prepended Length", makeCommand("deletePrepended", this));  // for Wayne!!!!!!
		//	addMenuItem("Delete *", makeCommand("deleteStar", this));  // for Wayne!!!!!!
		addMenuItem("Delete Data For Selected Taxa", makeCommand("deleteData", this));
		addMenuItem("Prepend Sequence Length", makeCommand("prependLength", this));
		addMenuItem("Prepend Number of Non-missing Sites", makeCommand("prependNumSites", this));
		addMenuItem("Delete Stored Annotation", makeCommand("deleteAnnotation", this));
		return true;
	}

	/*.................................................................................................................*/
	public int getVersionOfFirstRelease(){
		return 250;  
	}
	/** Returns whether or not it's appropriate for an employer to hire more than one instance of this module.  
 	If false then is hired only once; second attempt fails.*/
	public boolean canHireMoreThanOnce(){
		return true;
	}
	/*.................................................................................................................*/
	/** Generated by an employee who quit.  The MesquiteModule should act accordingly. */
	public void employeeQuit(MesquiteModule employee) {
		if (employee == matrixSourceTask)  // character source quit and none rehired automatically
			iQuit();
	}
	/*.................................................................................................................*/
	/** endJob is called as a module is quitting; modules should put their clean up code here.*/
	public void endJob() {
		if (data != null)
			data.removeListener(this);
	}
	/*.................................................................................................................*/
	public Snapshot getSnapshot(MesquiteFile file) { 
		Snapshot temp = new Snapshot();
		temp.addLine("getMatrixSource", matrixSourceTask);
		return temp;
	}

	void captureCharacterDataFromObservedStates(){
		if (observedStates ==null){
			if (data != null)
				data.removeListener(this);
			data = null;
		}
		else {
			CharacterData temp = observedStates.getParentData();
			if (temp != data){
				if (data != null)
					data.removeListener(this);
				if (temp != null)
					temp.addListenerHighPriority(this);
				data = temp;
			}
		}
	}
	public CharacterData getCharacterData(){
		return data;
	}


	/* ................................................................................................................. */

	public boolean clipBoardHasString() {

		Clipboard clip = Toolkit.getDefaultToolkit().getSystemClipboard();
		Transferable t = clip.getContents(this);
		try {
			String s = (String) t.getTransferData(DataFlavor.stringFlavor);
			if (s != null) {
				return true;
			}
		} catch (Exception e) {
			MesquiteMessage.printStackTrace(e);
		}
		return false;
	}


	/*.................................................................................................................*/
	public boolean arrowTouchInRow(Graphics g, int ic, int x, int y, boolean doubleClick, int modifiers){
		if (MesquiteEvent.rightClick(modifiers)) {
			MesquitePopup popup = new MesquitePopup(table.getMatrixPanel());

			String copyMenuText = "Copy ";
			if (observedStates != null) {
				CharacterData data = observedStates.getParentData();
				if (data != null) {
					copyMenuText += data.getName() + " Data";
					copyMenuText += " [from " + data.getTaxa().getTaxonName(ic) + "]" ;
					
				}
			}
			MesquiteCommand mcCopy = makeCommand("copyData", this);
			mcCopy.setDefaultArguments(""+ic);
			MesquiteCheckMenuItem mCopyItem = new MesquiteCheckMenuItem(copyMenuText, this, mcCopy, null, null);
			popup.add(mCopyItem);

			String pasteMenuText = "Paste ";
			if (StringUtil.notEmpty(localCopyDataClipboard) && localCopyData != null) {
					pasteMenuText += localCopyData.getName() + " Data";
					if (StringUtil.notEmpty(localCopyDataTaxon)) {
						pasteMenuText += " [from " + localCopyDataTaxon + "] " ;
					}
			}
			MesquiteCommand mcPaste = makeCommand("pasteData", this);  //only if something in clipboard
			mcPaste.setDefaultArguments(""+ic);
			MesquiteCheckMenuItem mPasteItem = new MesquiteCheckMenuItem(pasteMenuText, this, mcPaste, null, null);
			mPasteItem.setEnabled(StringUtil.notEmpty(localCopyDataClipboard));
			popup.add(mPasteItem);

			MesquiteCommand mcDelete = makeCommand("deleteDataTouched", this);
			mcDelete.setDefaultArguments(""+ic);
			MesquiteCheckMenuItem mDeleteItem = new MesquiteCheckMenuItem("Delete Data", this, mcDelete, null, null);
			popup.add(mDeleteItem);

			popup.showPopup(x,y+18);

			return true;
		}
		return false;
	}



	MesquiteInteger pos = new MesquiteInteger();

	/*.................................................................................................................*/
	public Object doCommand(String commandName, String arguments, CommandChecker checker) {
		if (checker.compare(this.getClass(), "Returns the matrix source", null, commandName, "getMatrixSource")) {
			return matrixSourceTask;
		}
		else if (checker.compare(this.getClass(), "Copies the data for selected taxon", null, commandName, "copyData")) {
			if (observedStates == null)
				return null;
			CharacterData data = observedStates.getParentData();
			if (data == null)
				return null;
			int it = MesquiteInteger.fromString(parser.getFirstToken(arguments));
			if (MesquiteInteger.isCombinable(it)) {
				StringBuffer sb = new StringBuffer();
				data.copyDataFromRowIntoBuffer(it, sb);
				if (StringUtil.notEmpty(sb.toString())) {
					localCopyDataClipboard = sb.toString();
					localCopyData = data;
					localCopyDataTaxon= data.getTaxa().getTaxonName(it);
				}
				else {
					localCopyDataClipboard = null;
					localCopyData = null;
					localCopyDataTaxon = null;
				}
			}
			return null;
		}
		else if (checker.compare(this.getClass(), "Pastes the data for selected taxon", null, commandName, "pasteData")) {
			if (observedStates == null)
				return null;
			CharacterData data = observedStates.getParentData();
			if (data == null)
				return null;
			int it = MesquiteInteger.fromString(parser.getFirstToken(arguments));
			if (MesquiteInteger.isCombinable(it) && StringUtil.notEmpty(localCopyDataClipboard)) {
				data.pasteDataFromStringIntoTaxon(it, localCopyDataClipboard);
			}
			return null;
		}
		else if (checker.compare(this.getClass(), "Pastes the data for selected taxon", null, commandName, "deleteDataTouched")) {
			if (observedStates == null)
				return null;
			CharacterData data = observedStates.getParentData();
			if (data == null)
				return null;
			int it = MesquiteInteger.fromString(parser.getFirstToken(arguments));
			Debugg.println("prepare to delete row: "+it);
			if (MesquiteInteger.isCombinable(it)) {
				if (!AlertDialog.query(containerOfModule(), "Delete Data?", "Are you sure you want to delete the data for taxon " +data.getTaxa().getTaxonName(it) + " in the matrix \"" + data.getName() + "\"", "No", "Yes")) {
					zapData(data,it);
				}
			}
			return null;
		}
		else if (checker.compare(this.getClass(), "Deletes the data for selected taxa", null, commandName, "deleteData")) {
			if (observedStates == null)
				return null;
			captureCharacterDataFromObservedStates();
			if (data == null)
				return null;
			if (!AlertDialog.query(containerOfModule(), "Delete Data?", "Are you sure you want to delete the data for these taxa in the matrix \"" + data.getName() + "\"", "No", "Yes"))
				zapData(data);
			return null;
		}
		else if (checker.compare(this.getClass(), "deleteds () and anything between", null, commandName, "deletePrepended")) {
			if (observedStates == null || taxa == null)
				return null;
			boolean anySelected = taxa.anySelected();
			int myColumn = -1;
			if (getEmployer() instanceof ListModule){

				myColumn = ((ListModule)getEmployer()).getMyColumn(this);
				if (table != null)
					anySelected = anySelected || table.anyCellSelectedInColumnAnyWay(myColumn);
			}

			for (int it = 0; it<taxa.getNumTaxa(); it++){
				if ((!anySelected || selected(taxa, it, myColumn))){
					String note = getNote(it);
					while (!StringUtil.blank(note) && note.indexOf("(")>=0){
						int start = note.indexOf("(");
						int end = note.indexOf(")");
						String firstBit = "";
						if (start>0)
							firstBit = note.substring(0, start);
						note = firstBit + note.substring(end+1, note.length());
					}
					setNote(it, note);

				}
			}
			outputInvalid();
			parametersChanged();
			return null;
		}
		else if (checker.compare(this.getClass(), "deletes *", null, commandName, "deleteStar")) {
			if (observedStates == null || taxa == null)
				return null;
			boolean anySelected = taxa.anySelected();
			int myColumn = -1;
			if (getEmployer() instanceof ListModule){

				myColumn = ((ListModule)getEmployer()).getMyColumn(this);
				if (table != null)
					anySelected = anySelected || table.anyCellSelectedInColumnAnyWay(myColumn);
			}
			for (int it = 0; it<taxa.getNumTaxa(); it++){
				if ((!anySelected || selected(taxa, it, myColumn))){
					String note = getNote(it);
					while (!StringUtil.blank(note) && note.indexOf("*")>=0){
						int start = note.indexOf("*");
						String firstBit = "";
						if (start>0)
							firstBit = note.substring(0, start);
						note = firstBit + note.substring(start+1, note.length());
					}
					setNote(it, note);

				}
			}
			outputInvalid();
			parametersChanged();
			return null;
		}
		else if (checker.compare(this.getClass(), "Prepends to the note the sequence length (including N\'s and ?\'s) for the selected taxa", null, commandName, "prependLength")) {
			if (observedStates == null || taxa == null)
				return null;
			boolean anySelected = taxa.anySelected();
			int myColumn = -1;
			if (getEmployer() instanceof ListModule){

				myColumn = ((ListModule)getEmployer()).getMyColumn(this);
				if (table != null)
					anySelected = anySelected || table.anyCellSelectedInColumnAnyWay(myColumn);
			}
			for (int it = 0; it<taxa.getNumTaxa(); it++){
				if (hasData(it) && (!anySelected || selected(taxa, it, myColumn))){
					String note = getNote(it);
					if (StringUtil.blank(note))
						note = "(" + sequenceLength(it) + ")";
					else
						note = "(" + sequenceLength(it) + ") " + note;
					setNote(it, note);
				}
			}
			outputInvalid();
			parametersChanged();
			return null;
		}
		else if (checker.compare(this.getClass(), "Prepends to the note the number of non-missing sites (not including N\'s and ?\'s) for the selected taxa", null, commandName, "prependNumSites")) {
			if (observedStates == null || taxa == null)
				return null;
			boolean anySelected = taxa.anySelected();
			int myColumn = -1;
			if (getEmployer() instanceof ListModule){

				myColumn = ((ListModule)getEmployer()).getMyColumn(this);
				if (table != null)
					anySelected = anySelected || table.anyCellSelectedInColumnAnyWay(myColumn);
			}
			for (int it = 0; it<taxa.getNumTaxa(); it++){
				if (hasData(it) && (!anySelected || selected(taxa, it, myColumn))){
					String note = getNote(it);
					if (StringUtil.blank(note))
						note = "(" + numSites(it) + ")";
					else
						note = "(" + numSites(it) + ") " + note;
					setNote(it, note);
				}
			}
			outputInvalid();
			parametersChanged();
			return null;
		}
		else if (checker.compare(this.getClass(), "Deletes the notes for the selected taxa", null, commandName, "deleteAnnotation")) {
			if (observedStates == null || taxa == null)
				return null;
			boolean anySelected = taxa.anySelected();
			int myColumn = -1;
			if (getEmployer() instanceof ListModule){

				myColumn = ((ListModule)getEmployer()).getMyColumn(this);
				if (table != null)
					anySelected = anySelected || table.anyCellSelectedInColumnAnyWay(myColumn);
			}
			for (int it = 0; it<taxa.getNumTaxa(); it++){
				if (hasData(it) && (!anySelected || selected(taxa, it, myColumn))){
					setNote(it, null);
				}
			}
			outputInvalid();
			parametersChanged();
			return null;
		}
		else return  super.doCommand(commandName, arguments, checker);
	}

	/*...............................................................................................................*/
	/** returns whether or not a cell of table is editable.*/
	public boolean isCellEditable(int row){
		return true;
	}
	/*...............................................................................................................*/
	/** for those permitting editing, indicates user has edited to incoming string.*/
	public void setString(int row, String s){

		if (StringUtil.blank(s))
			setNote(row, null);
		else if (s.equalsIgnoreCase("Yes") || s.equalsIgnoreCase("No Data"))
			return;
		else 
			setNote(row, s);
	}

	void setNote(int row, String s){
		if (tInfo == null)
			return;
		tInfo.setAssociatedObject(MolecularData.genBankNumberRef, row, s);
	}
	String getNote(int row){
		if (tInfo == null)
			return null;
		Object obj = tInfo.getAssociatedObject(MolecularData.genBankNumberRef, row);
		if (obj == null || !(obj instanceof String))
			return null;
		return (String)obj;
	}
	/*...............................................................................................................*/
	public void setTableAndTaxa(MesquiteTable table, Taxa taxa){
		//if (this.data !=null)
		//	this.data.removeListener(this);
		if (taxa != this.taxa)
			observedStates =  null;
		this.taxa = taxa;
		matrixSourceTask.initialize(taxa);

		this.table = table;

		doCalcs();
	}
	/*...............................................................................................................*/
	Bits bits;
	public void doCalcs(){
		if (bits != null)
			bits.clearAllBits();
		if (taxa == null)
			return;
		if (bits == null)
			bits = new Bits(taxa.getNumTaxa());
		else
			bits.resetSize(taxa.getNumTaxa());
		if (observedStates == null ) {
			tInfo = null;
			observedStates = matrixSourceTask.getCurrentMatrix(taxa);
			if (observedStates != null) {
				captureCharacterDataFromObservedStates();

				if (data != null)
					tInfo = data.getTaxaInfo(true);
			}
		}
		if (observedStates==null)
			return;
		for (int it = 0; it<taxa.getNumTaxa(); it++){
			if (hasData(it))
				bits.setBit(it);
		}
	}
	/*...............................................................................................................*/
	int sequenceLength(int it){
		CharacterState cs = null;
		if (observedStates == null)
			return 0;
		int count = 0;
		for (int ic=0; ic<observedStates.getNumChars(); ic++) {
			cs = observedStates.getCharacterState(cs, ic, it);
			if (cs == null)
				return 0;
			if (cs instanceof MolecularState){
				if (!cs.isInapplicable())  //if Molecular, then count missing & with state
					count++;
			}
			else
				if (!cs.isInapplicable() && !cs.isUnassigned())  //if Molecular, then count missing & with state
					count++;


		}

		return count;
	}
	/*...............................................................................................................*/
	int numSites(int it){
		CharacterState cs = null;
		if (observedStates == null)
			return 0;
		int count = 0;
		for (int ic=0; ic<observedStates.getNumChars(); ic++) {
			cs = observedStates.getCharacterState(cs, ic, it);
			if (cs == null)
				return 0;
			if (!cs.isInapplicable() && !cs.isUnassigned())
				count++;

		}

		return count;
	}	/*...............................................................................................................*/
	boolean hasData(int it){
		CharacterState cs = null;
		try {
			for (int ic=0; ic<observedStates.getNumChars(); ic++) {
				cs = observedStates.getCharacterState(cs, ic, it);
				if (cs == null)
					return false;
				if (!cs.isInapplicable() && !cs.isUnassigned()) 
					return true;

			}
		}
		catch (NullPointerException e){
		}
		return false;
	}
	/*...............................................................................................................*/

	public String getExplanationForRow(int ic){
		if (observedStates != null && observedStates.getParentData() != null){
			captureCharacterDataFromObservedStates();

			Associable tInfo = data.getTaxaInfo(false);
			if (tInfo == null)
				return null;
			return "Notes: " + tInfo.toString(ic);
		}
		return null;
	}
	/*...............................................................................................................*/
	boolean selected(Taxa taxa, int it, int myColumn){
		if (taxa.getSelected(it)){
			return true;
		}
		if (table != null && myColumn >=0){
			if (table.isCellSelectedAnyWay(myColumn, it))
				return true;
		}
		return false;
	}

	void zapData(CharacterData data){
		Taxa taxa = data.getTaxa();
		Associable tInfo = data.getTaxaInfo(false);
		int myColumn = -1;
		if (getEmployer() instanceof ListModule){

			myColumn = ((ListModule)getEmployer()).getMyColumn(this);
		}
		for (int it = 0; it<taxa.getNumTaxa(); it++){
			if (selected(taxa, it, myColumn)){
				if (tInfo != null)
					tInfo.deassignAssociated(it);
				for (int ic=0; ic<data.getNumChars(); ic++)
					data.deassign(ic, it);

			}
		}
		data.notifyListeners(this, new Notification(MesquiteListener.DATA_CHANGED));
		outputInvalid();
		parametersChanged();
	}
	/*.................................................................................................................*/
	void zapData(CharacterData data, int it){
		Taxa taxa = data.getTaxa();
		if (it<0 || it>=taxa.getNumTaxa())
			return;
		Associable tInfo = data.getTaxaInfo(false);
		int myColumn = -1;
		if (getEmployer() instanceof ListModule){

			myColumn = ((ListModule)getEmployer()).getMyColumn(this);
		}
		if (tInfo != null)
			tInfo.deassignAssociated(it);
		for (int ic=0; ic<data.getNumChars(); ic++)
			data.deassign(ic, it);

		data.notifyListeners(this, new Notification(MesquiteListener.DATA_CHANGED));
		outputInvalid();
		parametersChanged();
	}
	
	

	/*.................................................................................................................*/
	public void employeeParametersChanged(MesquiteModule employee, MesquiteModule source, Notification notification) {
		observedStates = null;
		super.employeeParametersChanged(employee, source, notification);
	}

	/*.................................................................................................................*/
	/** passes which object was disposed*/
	public void disposing(Object obj){
		if (obj != null && obj  == data)
			iQuit();
	}
	/*.................................................................................................................*/
	
	/** Gets background color for cell for row ic.  Override it if you want to change the color from the default. */
	public Color getBackgroundColorOfCell(int it, boolean selected){
		if (observedStates == null){
			doCalcs();
			if (observedStates==null)
				return null;
		}
		if (observedStates.getParentData() != null){
			captureCharacterDataFromObservedStates();

			Associable tInfo = data.getTaxaInfo(false);
			NameReference genBankColor = NameReference.getNameReference("genbankcolor");
			Object obj = tInfo.getAssociatedObject(genBankColor,  it);  //not saved to file
			if (obj instanceof Color)
				return (Color)obj;
		}
		if (bits ==null || it <0 || it > bits.getSize())
			return null;
		String note = getNote(it);
		if (selected){
			if (bits.isBitOn(it))
				return ColorDistribution.darkGreen;
			else
				return ColorDistribution.darkRed;		}
		else if (bits.isBitOn(it)){
			if (StringUtil.blank(note))
				return ColorDistribution.veryLightGreen;
			if ( !(note.equalsIgnoreCase("x")))
				return ColorDistribution.lightGreenYellowish;
			return ColorDistribution.lightGreenYellow;
		}
		else {
			if (StringUtil.blank(note))
				return ColorDistribution.brown;
			if ( !(note.equalsIgnoreCase("x"))) {
				return Color.red;
			}
			return ColorDistribution.lightRed;
		}
	}
	public String getStringForTaxon(int it){
		String note = getNote(it);
		if (note != null)
			return note;
		if (observedStates == null)
			doCalcs();
		if (bits ==null || it <0 || it > bits.getSize())
			return "?";
		if (bits.isBitOn(it))
			return "Yes";
		else
			return "No Data";
	}
	public String getWidestString(){
		return "88888888888";
	}
	/*.................................................................................................................*/
	public String getTitle() {
		if (observedStates == null)
			doCalcs();
		if (observedStates != null && getProject().getNumberCharMatricesVisible()>1){
			CharacterData d = observedStates.getParentData();
			if (d != null && d.getName()!= null) {
				String n =  d.getName();
				if (n.length()>12)
					n = n.substring(0, 12); 
				return "Has Data (" + n + ")";
			}
		}
		return "Has Data in Matrix";
	}
	/*.................................................................................................................*/
	/** returns whether this module is requesting to appear as a primary choice */
	public boolean requestPrimaryChoice(){
		return true;  
	}
	/*.................................................................................................................*/
	public boolean isPrerelease(){
		return false;  
	}

}

