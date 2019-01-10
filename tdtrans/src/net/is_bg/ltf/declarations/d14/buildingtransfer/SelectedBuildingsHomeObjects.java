package net.is_bg.ltf.declarations.d14.buildingtransfer;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;



import net.is_bg.ltf.businessmodels.building.Building;
import net.is_bg.ltf.businessmodels.building.BuildingBean;
import net.is_bg.ltf.businessmodels.homeobject.HomeObject;
import net.is_bg.ltf.businessmodels.taxdoc.TaxDoc;
import net.is_bg.ltf.util.DateUtil;

/***
 * Selected home Objects  & buildings helper data structures !!!
 * All methods & variables in this class are either private or package private!!!
 * @author lubo
 */
class SelectedBuildingsHomeObjects {
	private SelectedBuildingsHomeObjects(){}
	private Map<Long, Set<Long>> buildingIdHomeObjectIds = new HashMap<Long, Set<Long>>();  // a map representation of selected  building & selected corresponding home object ids in that building!!!
	private Map<Long, Building> selectedBuildings = new HashMap<Long, Building>();  // a map with the buildings that user has selected!!! 
	private Map<Long, HomeObject> selectedHomeobjects =  new HashMap<Long, HomeObject>();  //a map with home objects that user has selected!!!
	Map<Long, Building> buildingsTobeSelected = new HashMap<Long, Building>(); //buildings from other declaration that are to be selected for transfer!!!
	private Map<Long, Building> buildingsTobeTransferred = new HashMap<Long, Building>(); //buildings to be added transfered to this declaration
	Map<Long, HomeObject> homeObjectsTobeTransferred = new HashMap<Long, HomeObject>();// the home objects to be added to this declaration
	private List<Building> buildingList;	  //list with buildings in the current declaration that is to be manipulated with buildings from other declaration!!!
	private BuildingBean buildingBean;
	private TaxDocTransferBean buildingTransferBean;
	private Map<String, String> buildingFunctions;  //map s prednazna4eniqta na sgradite!!!
	private TaxDoc thisTaxDoc;// the current taxdoc that we transfer objects into!!!
	
	
	private  IRemoveBuildingCallBack removeBuildingCallBack = new IRemoveBuildingCallBack() {
		@Override
		public void removeBuilding(Building removedBuilding) {
			Building b = buildingsTobeSelected.get(removedBuilding.getId());
			if(buildingBean.getSelectedBuildingNo() > buildingBean.getBuildings().size()){
				buildingBean.setSelectedBuildingNo(buildingBean.getBuildings().size());
			}
			buildingBean.getSelectedBuilding().setCurrent(true);
			if(b == null) return;   //we are removing building that is not in transfer list!!!
			selectBuilding(b, false);
			b.setCheck(false);
		}
	};
	
	private class RemoveBuildingCallBack implements IRemoveBuildingCallBack {
		List<IRemoveBuildingCallBack> callBacklist = new ArrayList<IRemoveBuildingCallBack>();
		
		@Override
		public void removeBuilding(Building removedBuilding) {
			for(IRemoveBuildingCallBack c : callBacklist) if(c!=null) c.removeBuilding(removedBuilding);
		}
	};
	
	private RemoveBuildingCallBack callbackList = new RemoveBuildingCallBack();
	
	
	/***
	 * Select (deselects) building and all the home objects in the building!!!
	 * @param b  boolean variable indicating select (deselect)
	 * @param select
	 */
	void selectBuilding(Building b, boolean check){
		Set<Long> homeObjectIds = getBuildingObjectSet(b.getId());
		for(HomeObject ho:b.getHomeObjects()){
			selectHomeObject(homeObjectIds, ho, check);
		}
		if(!check){
			//deselect all objects in building 
			buildingIdHomeObjectIds.remove(b.getId());    //remove whole  building
			selectedBuildings.remove(b.getId());
			removeBuildingFromDeclaration(b);
		}else{
			selectedBuildings.put(b.getId(), buildingsTobeSelected.get(b.getId()));
			addBuildingToDeclaration(b);                   //add building to declaration if not added
		}
		setCheckAll(check);
	}
	
	
	private void setCheckAll(boolean check){
		if(check) buildingTransferBean.setCheckAll(check);
		else if(buildingIdHomeObjectIds.isEmpty()) buildingTransferBean.setCheckAll(check);
		//else buildingTransferBean.setCheckAll(check);
	}
	
	/**
	 * Sets the id's of home objects that will be transfered to -1 so they will be inserted into this declaration 
	 * rather then updated in the old declaration!!!
	 */
	void setTrasnferredBuildingsHomeObjcetsIdsToMinusOne(){
		for(Long buildingId: buildingIdHomeObjectIds.keySet()){
		  Building b = 	buildingsTobeTransferred.get(buildingId);
		  if(b == null) continue;
		  b.setId(-1);
		  for(Long hoid: buildingIdHomeObjectIds.get(buildingId)){
			 HomeObject ho = homeObjectsTobeTransferred.get(hoid);
			 if(ho == null) continue;
			 ho.setId(-1);
		  }
		}
	}
	
	
	/**
	 * Selects or deselects home object! If none of the objects is selected after this object is deselected - deselects the whole building!!!
	 * @param ho
	 * @param select
	 */
	void selectHomeObject(HomeObject ho, boolean check){
		Set<Long> homeObjectIds = getBuildingObjectSet(ho.getBuildingId());
		selectHomeObject(homeObjectIds, ho, check);
		addHomeObjectToBuilding(ho.getId(), check);
		if(!check){
			//if homeobjects set is empty after removing the homeobject deletes whole building
			if(homeObjectIds.isEmpty()){
				//buildingList.remove();
				buildingIdHomeObjectIds.remove(ho.getBuildingId());
				Building b = selectedBuildings.get(ho.getBuildingId());
				if(b!=null){ 
					b.setCheck(check);
					removeBuildingFromDeclaration(b);
				}
			}
		}else{
			//add the building if building is not in selected buildings map
			Building b = selectedBuildings.get(ho.getBuildingId());
			if(b == null)  {
				selectedBuildings.put(ho.getBuildingId(), buildingsTobeSelected.get(ho.getBuildingId()));
				b = selectedBuildings.get(ho.getBuildingId());
			}
			//add building to declaration
			addBuildingToDeclaration(b);
			b.setCheck(check);
		}
		reIndexObjectsInBuilding(ho.getBuildingId());
		setCheckAll(check);
	}
	
	/***
	 * Invoked when building is being selected / deselected
	 * @param b The selected (deslected) building!
	 */
	void selectBuilding(Building b){
		this.selectBuilding(b, b.isCheck());
	}
	
	/**
	 * Invoked when home object is being selected / deselected 
	 * @param ho The selected (deselected) homeobject!
	 */
	void selectHomeObject(HomeObject ho){
		this.selectHomeObject(ho, ho.isCheck());
	}
	
	
	private void addBuildingToDeclaration(Building b){
		if(b == null) return;
		Building binMap = buildingsTobeTransferred.get(b.getId());
		
		if(binMap == null) return;
		if(buildingList.contains(binMap))  return;   //building is in declaration list
		
		//add the selected home objects to building
		binMap.setHomeObjects(getSelectedHomeObjectsForBuilding(b.getId()));
		
		//remove the single building with id -1!!!!
		removeSingelBuildingInListIfIdMuniOne();
		
		//add building to declaration if not been added
		buildingList.add(binMap);
		
		binMap.setCurrent(false);
		//System.out.println("Adding building with id = " + b.getId() + " to declaration...");
		
		reIndexBuildings();
	}
	
	
	/***
	 * If there is a single building with id = -1  Before selecting a building for transfer remove that building!!! 
	 */
	private void removeSingelBuildingInListIfIdMuniOne(){
		if (buildingList.size() == 1 && buildingList.get(0).getId() == -1)buildingList.clear();
	}
	
	/**
	 * Renumerate home objects in the building!!!
	 * @param buildingId
	 */
	private void reIndexObjectsInBuilding(long buildingId){
		Building binMap = buildingsTobeTransferred.get(buildingId);
		if(binMap == null) return;
		int seqNo = 1;
		for(HomeObject h : binMap.getHomeObjects()){
			h.setSeqNo(seqNo++);
		}
	}
	
	
	
	/**
	 * Renumerate buildings in declaration!!!
	 */
	private void reIndexBuildings(){
		reIndexBuildings(buildingBean);
	}
	
	/**
	 * Renumerates buildings in building bean!!!
	 * @param buildingBean
	 */
	static void reIndexBuildings(BuildingBean buildingBean ){
		int number = 1;
		List<Building> buildingList  =buildingBean.getBuildings();
		for(Building b :buildingBean.getBuildings()){
			b.setNumber(number++);
			b.setCurrent(false);
		}
		if(buildingBean.getSelectedBuildingNo() > buildingList.size()){
			buildingBean.setSelectedBuildingNo(buildingList.size());
		}
	}
	
	/***
	 * Removes selected buildings from current declaration!!! Used when loading new taxdoc buildings to be transfered!!!
	 */
	void removeSelecetedBuildingsFromDeclaration(){
		for(Building b:selectedBuildings.values()){
			removeBuildingFromDeclaration(b);
		}
	}
	
	/**
	 * Called when building is being removed from current declaration buildings list!
	 * @param b Building to be removed!
	 */
	private void removeBuildingFromDeclaration(Building b){
		for(Building bb :buildingBean.getBuildings()){
			bb.setCurrent(false);
		}
		//buildingBean.setNumBuildings(buildingBean.getBuildings().size());
		buildingBean.getSelectedBuilding().setCurrent(true);
		if(b == null) return;
		Building binMap = buildingsTobeTransferred.get(b.getId());
		if(binMap != null) binMap.setHomeObjects(new ArrayList<HomeObject>());  //clear home objects & remove building from declaration
		buildingList.remove(binMap);
		//if list is empty after removal add at least one building!!!
		if(buildingList.size() == 0 ) {
			buildingBean.setNumBuildings(1);
			buildingBean.createBuildings();
		}
		reIndexBuildings();
	}
	
	/***
	 * Add /remove the selected home object to building if object is not in building list!!!
	 * @param ho
	 * @param add
	 */
	private void addHomeObjectToBuilding(long homeObjId, boolean add){
		HomeObject object =   homeObjectsTobeTransferred.get(homeObjId);
		if(object == null) return;
		Building binMap = buildingsTobeTransferred.get(object.getBuildingId());
		if(binMap == null) return;
		if(add){
			if(!binMap.getHomeObjects().contains(object)){
				binMap.getHomeObjects().add(object);     //add home object to building  !!!
				//System.out.println("Addding home object with id " + homeObjId + " to  building with id " + binMap.getId());
			}
		}else{
			if(binMap.getHomeObjects().contains(object)){
				binMap.getHomeObjects().remove(object);    //remove home object from building!!!
				//System.out.println("Removing home object with id " + homeObjId + " from  building with id " + binMap.getId());
			}
		}
		
	}
	/***
	 * Invoked when we select / de select home object
	 * @param homeObjectIds 
	 * @param homeObject
	 * @param select
	 */
	private void selectHomeObject(Set<Long> homeObjectIds, HomeObject homeObject, boolean select){
		if(select) {
			homeObjectIds.add(homeObject.getId());
			selectedHomeobjects.put(homeObject.getId(), homeObject);
		}
		else{
			homeObjectIds.remove(homeObject.getId());
			selectedHomeobjects.remove(homeObject.getId());
		}
		homeObject.setCheck(select);   //check uncheck 
	}
	
	/***
	 * Returns a set of home selected home objects for a building!!!
	 * @param buildingId
	 * @return
	 */
	private Set<Long> getBuildingObjectSet(long buildingId){
		Set<Long> homeObjectIds = buildingIdHomeObjectIds.get(buildingId);
		if(homeObjectIds == null) {homeObjectIds = new TreeSet<Long>(); buildingIdHomeObjectIds.put(buildingId,homeObjectIds);}
		return homeObjectIds;
	}
	
	
	/**
	 * Returns a home objects data structure used to manipulate the buildingList adding or removing buildings & homeObjects!!!
	 * This method must be called when declaration to chose buildings from is loaded on face!!!
	 * @param buildingList
	 * @return
	 */
	static SelectedBuildingsHomeObjects getSelectedHomeObjects(TaxDoc thisTaxDoc, TaxDocTransferBean buildingTransferBean, Map<Long, Building> buildingsTobeTransferred,   Map<Long, Building> buildingsTobeSelected){
		SelectedBuildingsHomeObjects ho =  new SelectedBuildingsHomeObjects();
		BuildingBean buildingBean = buildingTransferBean.getBuildingBean();
		ho.buildingTransferBean = buildingTransferBean;
		ho.thisTaxDoc = thisTaxDoc;
		
		ho.buildingList = buildingBean.getBuildings();
		ho.buildingsTobeSelected = buildingsTobeSelected;
		ho.buildingsTobeTransferred = buildingsTobeTransferred;
		for(Building b : ho.buildingsTobeTransferred.values()){
			ho.clearDatesInTransferedHomeObjcets(b);
			b.setDisableAddDeleteHomeObject(true); //don't allow transfered buildings to be removed or edited!!!
		}  
		ho.buildingBean = buildingBean;
		ho.buildingFunctions = BuildingBean.toBuildingFunctionsMap(buildingBean);
		ho.callbackList.callBacklist.add(buildingBean.getRemoveBuildingCallBack());
		ho.callbackList.callBacklist.add(ho.removeBuildingCallBack);
		buildingBean.setRemoveBuildingCallBack(ho.callbackList);
		ho.separateHomeObjectsFromBuildings();
		return ho;
	}
	
	/**
	 * Clears the begin, end tax dates  in the home objects in building to be transfered!!
	 */
	private  void clearDatesInTransferedHomeObjcets(Building b){
		Date earnDateChangeDate =  (thisTaxDoc.getChangeDate() == null) ? thisTaxDoc.getEarnDate() : thisTaxDoc.getChangeDate();
		for(HomeObject ho:b.getHomeObjects()){
			DateUtil.DateFields df = DateUtil.getDateFields(DateUtil.dateAddSubstract(earnDateChangeDate, "+", "1", "m"));
			df.day = 1;
			ho.setTaxBeginDate(DateUtil.getDateByDateFields(df));         //the first day of the next month! 
			ho.setEarnDate(earnDateChangeDate);
			ho.setTaxEndDate(null);
			ho.setChangeDate(null);
			ho.setDebtPartCount(0);
		}
	}
	
	
	/*private static Map<String, String> toBuildingFunctionsMap(SelectItem [] buildingFunctions){
		Map<String, String>  buildingFunctionsMap = new HashMap<String, String>();
		for(SelectItem sitem : buildingFunctions){
			buildingFunctionsMap.put(sitem.getValue().toString(), sitem.getLabel());
		}
		return buildingFunctionsMap;
	}*/
	
	/**
	 * Extract home objects from buildings & put them in home objects map!!!
	 * After this  method call the  buildingsTobeTransferred contains no homeObjects!!!
	 */
	private void separateHomeObjectsFromBuildings(){
		for(Building b : buildingsTobeTransferred.values()){
			for(HomeObject ho :b.getHomeObjects()){
				homeObjectsTobeTransferred.put(ho.getId(), ho);
			}
			b.setHomeObjects(new ArrayList<HomeObject>());   //clear home objects list
		}
	}
	
	/***
	 * Returns the list with selected home objects for building id!!!
	 * @param buildingId
	 * @return
	 */
	private List<HomeObject>  getSelectedHomeObjectsForBuilding(long buildingId){
		List<HomeObject> homeL = new ArrayList<HomeObject>();
		Set<Long> hoids =  buildingIdHomeObjectIds.get(buildingId);
		for(Long hoid :hoids){ homeL.add(homeObjectsTobeTransferred.get(hoid));}
		return homeL;
	}
	
	/**
	 * Vryshta Prednazna4enieto na sgradata po koda !
	 * @param kindFunctionCode
	 * @return
	 */
	String getBuildingKindFunction(String kindFunctionCode){
		return buildingFunctions.get(kindFunctionCode);
	}
}
