package net.is_bg.ltf.declarations.d14.buildingtransfer;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

import net.is_bg.ltf.AbstractManagedBean;
import net.is_bg.ltf.AppConstants;
import net.is_bg.ltf.businessmodels.building.Building;
import net.is_bg.ltf.businessmodels.building.BuildingBean;
import net.is_bg.ltf.businessmodels.firmbj14.FirmObj14;
import net.is_bg.ltf.businessmodels.homeobject.HomeObject;
import net.is_bg.ltf.businessmodels.land.Land;
import net.is_bg.ltf.businessmodels.property.Property;
import net.is_bg.ltf.businessmodels.taxdoc.TaxDoc;
import net.is_bg.ltf.businessmodels.taxdoc.TaxDocBean;
import net.is_bg.ltf.businessmodels.taxobject.TaxObject;
import net.is_bg.ltf.businessmodels.taxsubjects.TaxSubject;
import net.is_bg.ltf.declarations.d14.Decl14Bean;

/**
 * Used to transfer data from one  declaration to other!!!!
 * This is the only public part of the transfer functionality!!!
 * @author lubo
 *
 */
public class TaxDocTransferBean extends AbstractManagedBean {
	/**
	 * 
	 */
	private static final long serialVersionUID = 8769254392980971905L;
	private TaxDocBean taxdocBean;   //search bean used to chose declaration to load buildings !!!	
	private LoadedTaxDocData loadedData = new LoadedTaxDocData();             //the data loaded from other declaration
	private BuildingBean  buildingBean;
	private SelectedBuildingsHomeObjects selectedObjects; //data helper structure used to map selected objects & buildings to Sets & maps!!!
	private int comboSelectedItem = 9; //search by partida default
	private Map<Long, FirmObj14> firmObjectForHomeObject = new HashMap<Long, FirmObj14>();
	private boolean choseDeclVisible = false;
	private static SimpleDateFormat df = new SimpleDateFormat("dd.mm.yyyy");
	private static final ResourceBundle MSG_DECL_14 = ResourceBundle.getBundle(AppConstants.MSG_DECL_14);
	private String declInfo = "";
	private boolean checkAll;            //boolean indicating if all building & home objects are checked!!!
	//private TaxDoc thisTaxDoc;           //the current taxDoc in selected declaration.....
	private static final  String LAND = "'1'";
	private static final  String BUILD = "'2', '3'";
	private IPropertyLandProperties propLandProperties;
	private boolean hasLand;
	private boolean hasBuilding;
	private TaxObject taxObject;
	
	public  class BuildingTransferTaxDocBean extends TaxDocBean{
		private static final long serialVersionUID = 2201654893689958683L;
		public BuildingTransferTaxDocBean() {
			super("14", TaxDoc.STATUS_PROCESSED_AND_CLSED, 0);
		}

		@Override
		public String select() {
			String ret =  super.select();
			loadDataForTaxdoc(propLandProperties.getTaxDoc(), getRtnObject().getId());   //on select - load all data to be transferred!
			createDeclInfo();
			return ret;
		}
	}
	
	
	
	public static TaxDocTransferBean getInstance(IPropertyLandProperties propLandProperties, TaxDoc thisTaxDoc, BuildingBean  buildingBean) {
		return new TaxDocTransferBean(propLandProperties, thisTaxDoc, buildingBean);
	}
	
	
	private TaxDocTransferBean(IPropertyLandProperties propLandProperties, TaxDoc thisTaxDoc, BuildingBean  buildingBean){   //building bean in declaration passed to constructor!!!
		this.propLandProperties = propLandProperties;
		this.taxObject = null;
		this.taxdocBean = new BuildingTransferTaxDocBean();
		this.buildingBean = buildingBean;
		taxdocBean.setRndrSelectItemEMessage(false);
		taxdocBean.setRndrSelectItemOperation(false);
		taxdocBean.setRndrSelectItemsKindDecl(false);
		taxdocBean.setGetMode(true);
		taxdocBean.setRndrSelectItemsKindDecl(false);
		taxdocBean.setRndrSelectItemsKind(false);
		taxdocBean.setComboSelectedItem(String.valueOf(comboSelectedItem));
		taxdocBean.setMunicipalityId(visit.getCurUser().getMunicipality().getId());
		taxdocBean.setCancelOutcome(AppConstants.TO_CALLING_FORM);
		taxdocBean.setSelectOutcome(AppConstants.TO_CALLING_FORM);
	}
	
	
	private void initKindlanBeforeSearch() {
		
		String inClause = "";
		taxObject = propLandProperties.getTaxDoc().getTaxObject();
		if(taxObject == null) {
			taxObject = TaxDocDataLoaderHelper.loadTaxObject(propLandProperties.getTaxDoc().getTaxObjectId());
		}
		hasLand = Decl14Bean.getHasLand(taxObject);
		hasBuilding = Decl14Bean.getHasBuilding(taxObject);
		if(hasLand) {
			inClause = LAND; if(hasBuilding) inClause += (", " + BUILD);
		}else if(hasBuilding) inClause = BUILD;
		taxdocBean.setKindPropertyExpression(" in ("  +inClause + ") ");    
	}
	
	/***
	 * Information about chosen declaration from which we transfer objects!!!
	 */
	private void createDeclInfo(){
		TaxDoc taxdoc = taxdocBean.getRtnObject();
		if(taxdocBean.getRtnObject()== null) declInfo = "";
		declInfo = "";
		String docno = MSG_DECL_14.getString("vhNo") + taxdoc.getDocNo()+ " ";
		String docDate = taxdoc.getDocDate() == null ? " " :df.format(taxdoc.getDocDate()) + " ";
		declInfo+=docno + "/" + docDate;
		TaxSubject ts = taxdoc.getTaxSubject();
		if(ts == null) return ;
		declInfo+= MSG_DECL_14.getString("ein") + ts.getIdn() + " " + ts.getName();
	}
	
	
	/**
	 * Check if this home object has been transfered from other declaration!!!
	 * @param homeObjId
	 * @return
	 */
	public boolean isTransferedHomeObj(long homeObjId){
		if(selectedObjects != null && selectedObjects.homeObjectsTobeTransferred.containsKey(homeObjId)) return true;
		return false;
	}
	
	/***
	 * Check if this building is transfered from other declaration!!!
	 * @param buildingId
	 * @return
	 */
	public boolean isTransferedBuilding(long buildingId){
		if(selectedObjects != null && selectedObjects.buildingsTobeSelected.containsKey(buildingId)) return true;
		return false;
	}
	
	
	/**
	 * Used to load data for chosen declaration
	 * @param taxdocId
	 */
	private void loadDataForTaxdoc(TaxDoc thisTaxdoc, long taxdocId){
		setCheckAll(false);
		if(selectedObjects !=null)  selectedObjects.removeSelecetedBuildingsFromDeclaration();   //just clean any previous transfered objects from other declaration!!!
		loadedData = TaxDocDataLoaderHelper.loadTaxDocDataByTaxDocId(thisTaxdoc, taxdocId);
		choseDeclVisible = true;
		
		
		if(hasBuilding) {
			selectedObjects = SelectedBuildingsHomeObjects.getSelectedHomeObjects(propLandProperties.getTaxDoc(), this,  TaxDocDataLoaderHelper.loadBuildingsMap(taxdocId,  true), TaxDocDataLoaderHelper.listToMap(loadedData.buildingsToSelect));
		}
		
		boolean noLand = (propLandProperties.getLand() == null || propLandProperties.getLand().getId() <= 0);
		boolean noProperty	= (propLandProperties.getProperty() == null || propLandProperties.getProperty().getId() <= 0);
		
		//load property if no property
		if(noProperty && loadedData.property.getId() <=-1) {
			//transfer property
			propLandProperties.setProperty(new Property());   //clear all first
			propLandProperties.setProperty(loadedData.property);
		}
		
		//load land
		if(noLand && loadedData.land.getId() <=-1) {
			//transfer land
			propLandProperties.setLand(new Land());   //clear all first
			propLandProperties.setLand(loadedData.land);
		}
	}
	
	
	/**
	 * Prepare transfered buildings to be saved in the current declaration!!!
	 */
	public void prepareTransferedBuildingsForSave(){
		if(selectedObjects != null) selectedObjects.setTrasnferredBuildingsHomeObjcetsIdsToMinusOne();
	}
	
	/**
	 * selects deselects all buildings at once!!!
	 */
	public void selectAll(){
		for(Building b: loadedData.buildingsToSelect){
			b.setCheck(checkAll);
			selectedObjects.selectBuilding(b, checkAll);
		}
	}
	
	/**
	 * Select / deselect building!
	 * @param id
	 * @param type
	 */
	public void selectBuilding(Building building){
		selectedObjects.selectBuilding(building);
	}
	
	
	/**
	 * Select / deselect homeobject!
	 * @param id
	 */
	public void selectHomeObject(HomeObject homeObject){
		selectedObjects.selectHomeObject(homeObject);
	}

	public List<Building> getBuildingsToSelect() {
		return loadedData.buildingsToSelect;
	}
	
	
	public String searchTaxDoc(String nav) {
		initKindlanBeforeSearch();
		int cnt =  taxdocBean.searchFirstPage();
		String cancelSelectOutcome =  "land".equals(nav) ? "toCallingForm1" : AppConstants.TO_CALLING_FORM;
		//if only one declaration load it 
		if(cnt == 1){
			loadDataForTaxdoc(propLandProperties.getTaxDoc(), ((TaxDoc)taxdocBean.getTableValue().get(0)).getId());   //on select - load buildings to be transferred 
			createDeclInfo();
			return null;
		}
		taxdocBean.modalDialogClear();
		taxdocBean.setSelectOutcome(cancelSelectOutcome);
		taxdocBean.setCancelOutcome(cancelSelectOutcome);
		return AppConstants.TO_SEARCH_FORM;
	}

	public TaxDocBean getTaxdocBean() {
		return taxdocBean;
	}
	
	
	

	/***
	 * initilaize buildings to be selected list - call after saving declaration  tree
	 */
	public void initBuildingsToSelect(){
		loadedData.buildingsToSelect = new ArrayList<Building>(); 
	}

	public String getBuildingKindFunction(String kindFunctionCode){
		return selectedObjects.getBuildingKindFunction(kindFunctionCode);
	}
	
	
	public static void showBuildings(List<Building> blist){
		for(Building b: blist) showBuilding(b);
	}
	
	public static void showBuilding(Building b){
		System.out.println("Building Id " + b.getId() + ", Building Number" + b.getNumber());
		System.out.println("Floor Number " + b.getFloorNumber());
		
		showHomeObjects(b.getHomeObjects(), "	");
	}
	
	public static void showHomeObjects(List<HomeObject> homeObjects, String indent){
		for(HomeObject ho: homeObjects) showHomeObject(ho, indent);
	}
	

	
	public static void showHomeObject(HomeObject homeObject, String indent){
		System.out.println(indent + "Home Object Id " + homeObject.getId() + " HomeObj seqNo " + homeObject.getSeqNo());
		System.out.println(indent + "Build Year " + homeObject.getBuildYear());
		System.out.println(indent + "KindhomeObjectByWord " + homeObject.getKindHomeObjByWord());
		System.out.println(indent + "HomeObjNo " + homeObject.getHomeObjNo());
	}

	/***
	 * The firm objects loaded when declaration is chosen for processing!!!
	 * @param tmpList
	 */
	public void loadFirmObjects(List<FirmObj14> tmpList) {
		firmObjectForHomeObject.clear();
		if(tmpList!=null){
			for(FirmObj14 firm : tmpList){
				firmObjectForHomeObject.put(firm.getHomeObjId(), firm);
			}
		}
	}
	
	public static void reIndexBuildings(BuildingBean buildingBean){
		SelectedBuildingsHomeObjects.reIndexBuildings(buildingBean);
	}
	
	/***
	 * Returns a firm object for home object id if any is present in firmObjectForHomeObject Map!!!
	 * @return
	 */
	public FirmObj14 getFirmObjectForHomeObjectId(long homeobjectId){
		return firmObjectForHomeObject.get(homeobjectId);
	}
	
	/**
	 * Close the panel for choosing declaration!!!
	 */
	public void choseDeclClose(){
		choseDeclVisible = false;
	}
	
	
	/**
	 * Open the panel for choosing declaration!!!
	 */
	public void choseDeclOpen(){
		choseDeclVisible = true;
	}
	
	public boolean isChoseDeclVisible() {
		return choseDeclVisible;
	}
	
	public String getDeclInfo() {
		return declInfo;
	}
	
	public boolean isCheckAll() {
		return checkAll;
	}
	
	public void setCheckAll(boolean checkAll) {
		this.checkAll = checkAll;
	}
	
	public void invertCheckAll(){
		setCheckAll(!checkAll);
		selectAll();
	}
	
	public void invertSelectBuilding(Building building){
		building.setCheck(!building.isCheck());
		selectBuilding(building);
	}
	
	
	public void invertSelectHomeObject(HomeObject homeObject){
		homeObject.setCheck(!homeObject.isCheck());
		selectHomeObject(homeObject);
	}
	
	public BuildingBean getBuildingBean() {
		return buildingBean;
	}
	
	public boolean isNewDecl() {
		Property p = propLandProperties.getProperty();
		return (p== null || p.getId() <= 0);
	}
}
