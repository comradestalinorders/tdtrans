package net.is_bg.ltf.declarations.d14.buildingtransfer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


import net.is_bg.ltf.AbstractPart;
import net.is_bg.ltf.ApplicationGlobals;
import net.is_bg.ltf.businessmodels.addresses.Address;
import net.is_bg.ltf.businessmodels.building.Building;
import net.is_bg.ltf.businessmodels.building.BuildingBean;
import net.is_bg.ltf.businessmodels.homeobject.HomeObject;
import net.is_bg.ltf.businessmodels.land.Land;
import net.is_bg.ltf.businessmodels.property.Property;
import net.is_bg.ltf.businessmodels.taxdoc.ITaxDocDao;
import net.is_bg.ltf.businessmodels.taxdoc.TaxDoc;
import net.is_bg.ltf.businessmodels.taxobject.TaxObject;
import net.is_bg.ltf.db.dao.common.DecodeDao;
import net.is_bg.ltf.declarations.d14.DeclDao;
import net.is_bg.ltf.services.ServiceLocator;

/**
 * Used to load taxdoc data for selected Taxdoc!!!
 * @author lubo
 */
class TaxDocDataLoaderHelper  {
	
	
	/***
	 * Loads the buildings by taxdocId & returns a List with buildings!!!
	 * @param sericeLocator
	 * @param taxDocId
	 * @return
	 */
	static List<Building> loadBuildingsList(long taxDocId, long propertyId){
		   return loadBuildingsList(taxDocId, propertyId, true);
	}
	
	
	/***
	 *  The same as loadBuildingsList but optionally fill homeobjects in buildings!!!
	 * @param taxDocId
	 * @param propertyId
	 * @param fillHomeObjects
	 * @return
	 */
	static List<Building> loadBuildingsList(long taxDocId, long propertyId, boolean fillHomeObjects){
		List<Building> buildings = new ArrayList<Building>();
		ServiceLocator sericeLocator = ApplicationGlobals.getApplicationGlobals().getLocator();
		   //load taxdoc by taxdocid
	       List<?> result = sericeLocator.getTaxDocDao().getTaxDoc(taxDocId);
	       if(result.size() > 0 && result.get(0)!=null){
	    		DeclDao dao = sericeLocator.getDeclDao();
	    	    
	    	    //load buildings
	    	    buildings = dao.selectBuildings(propertyId);
	    	    
	    	    //load home objects in building....
	    	    if(fillHomeObjects)
	    		for (Building building : buildings) {
	    			 //load home objects for each building
	    			loadHomeObjectsForBuilding(building);
	    		}
	       }
	       return buildings;
	}
	
	private static List<AbstractPart> loadPartHomeObjects(/*HomeObject ho,*/ long homeObjId) {
		ServiceLocator sericeLocator = ApplicationGlobals.getApplicationGlobals().getLocator();
		DeclDao dao = sericeLocator.getDeclDao();
		return dao.selectPartHomeObject(homeObjId);
	}
	
	/***
	 * Loads Taxdoc, Property, Land & buildings for taxdoc Id - Used to load data when transfer declaration is chosen.
	 * @param thisTaxdoc 
	 * @param taxDocId
	 * @return
	 */
	static LoadedTaxDocData loadTaxDocDataByTaxDocId(TaxDoc thisTaxdoc, long taxDocId) {
		LoadedTaxDocData data = new LoadedTaxDocData();
		ServiceLocator sericeLocator = ApplicationGlobals.getApplicationGlobals().getLocator();
		DeclDao dao = sericeLocator.getDeclDao();
		ITaxDocDao tdDao = sericeLocator.getTaxDocDao();
		
		//load taxdoc
		data.taxdoc = (net.is_bg.ltf.businessmodels.taxdoc.TaxDoc)tdDao.getTaxDoc(taxDocId).get(0);
		
		//load taxobject  from original declaration
		TaxObject taxObject = sericeLocator.getTaxObjectDao().getTaxObjectById(thisTaxdoc.getTaxObjectId());
		
		//load property
		data.property = dao.selectProperty(taxDocId);
		long propertyId = data.property.getId();
		if(data.property == null) {
			data.property = new Property();
			data.property.setId(-1);
		}
		
		//fill property additional properties
		Address adr = sericeLocator.getAddressDao().getAddress(taxObject.getAddress().getId());
		data.property.setTaxDocId(thisTaxdoc.getId());
		data.property.setPropDocDate(thisTaxdoc.getEarnDate());
		data.property.setCategory(adr.getCity().getCategory());
		iniUnnecessaryProperties(data.property);
		
		//load land
		data.land = dao.selectLand(propertyId);
		
		//long landId = data.land.getId();
		if(data.land == null) {
			data.land = new Land();
			data.land.setId(-1);
		    data.land.setEdited(true);
		}
		
		//fill land additional properties
		iniUnnecessaryProperties(data.land);
		
		data.buildingsToSelect = loadBuildingsList(taxDocId, propertyId);
		return data;
	}
	
	
	/**
	 * Init not necessary properties of Property!
	 */
	private static void iniUnnecessaryProperties(Property property) {
		if(property.getId() > 0) property.setId(-property.getId());
		property.setSpecifyOtherUsage(null);
		property.setSeqNo(0);
		property.setOneOwner(false);
		property.setOneSender(false);
		property.setOneUser(false);
	}
	
	/***
	 * Init not necessary properties of Land
	 * @param land
	 */
	private static void iniUnnecessaryProperties(Land land) {
		if(land.getId() > 0) land.setId(-land.getId());
		land.setTaxBeginDate(null);
		land.setPropertyId(-1);
		land.setBuildingOwner(false);
		land.setDivident(0);
		land.setDivisor(0);
		land.setEarnDate(null);
		land.setChangeDate(null);
		land.setNewLandTaxBeginDate(null);
		land.setTaxEndDate(null);
		land.setPropertyId(-1);
	}
	
	
	private static  Property loadPropertyByTaxDocId(long taxdocId) {
		ServiceLocator sericeLocator = ApplicationGlobals.getApplicationGlobals().getLocator();
		DeclDao dao = sericeLocator.getDeclDao();
		Property property = dao.selectProperty(taxdocId);
		return property;
	}
	
	
	
	
	/***
	 * Loads a map with homeobjects in all buildings of the property!!!!
	 * @param sericeLocator
	 * @param taxDocId
	 * @return
	 */
	static Map<Long, HomeObject> loadHomeObjectsMap(long taxdocId){
		Map<Long, HomeObject> m = new HashMap<Long, HomeObject>();
		Property p = loadPropertyByTaxDocId(taxdocId);
		List<Building> buildings = loadBuildingsList( taxdocId, p.getId());
		
		for(Building b:buildings){
			for(HomeObject ho:b.getHomeObjects()){
				m.put(ho.getId(), ho);
			}
		}
		return m;
	}
	
	
	
	
	
	/***
	 * Loads the buildings by taxdocId & returns a Map  with buildings where key is the building id!!!
	 * @param sericeLocator
	 * @param taxDocId
	 * @return
	 */
	static Map<Long, Building> loadBuildingsMap(long taxDocId){
		return loadBuildingsMap(taxDocId, true);
	}
	
	/***
	 * The same as loadBuildingsMap but optionally fill homeobjects!!!
	 * @param sericeLocator
	 * @param taxDocId
	 * @param fillHomeObjects
	 * @return
	 */
	static Map<Long, Building> loadBuildingsMap(long taxDocId, boolean fillHomeObjects){
		Map<Long, Building> m = new HashMap<Long, Building>();
		Property p = loadPropertyByTaxDocId(taxDocId);
		for(Building building :loadBuildingsList(taxDocId, p.getId(), fillHomeObjects)){
			m.put(building.getId(), building);
		}
		return m;
	}
	
	/***
	 * Converts building Map to building List!!!
	 * @param buildings
	 * @return
	 */
	static List<Building> mapToList(Map<Long, Building> buildings){
		List<Building> build = new ArrayList<Building>();
		for(Building b: buildings.values()){
			build.add(b);
		}
		return build;
	}
	
	/***
	 * Converts building List to building Map!!!
	 * @param buildings
	 * @return
	 */
	static Map<Long, Building> listToMap(List<Building> buildings){
		Map<Long, Building> m = new HashMap<Long, Building>();
		for(Building building :(buildings)){
			m.put(building.getId(), building);
		}
		return m;
	}
	
	/*private static SelectItem[] getKindConstruction(DecodeDao decodeDao){
		return decodeDao.getItems(DecodeDao.KIND_CONSTRUCTION);
	}*/
	
	/**
	 * Loads home objects for the input building param by buildingId & fills them in the building input param!!!
	 * @param dao
	 * @param decodeDao
	 * @param building
	 */
	private static void loadHomeObjectsForBuilding(Building building){
		DeclDao dao = getDeclDao();
		List<HomeObject> homeObjList = dao.selectHomeObject(building.getId());
		//building.setEarnDate(getEarnDate());
		
		//прехвърляне на дата "облагане от" от декларацията към сградите и от там към обектите
		/*if (building.getTaxBeginDate() == null && getBeginTaxDate() != null) {
			building.setTaxBeginDate(DateUtil.getNextMonthFirstDate(building.getEarnDate()));
			
			if (building.getTaxBeginDate() != null && building.getTaxBeginDate().before(getBeginTaxDate())){
				building.setTaxBeginDate(getBeginTaxDate());
			}
		}*/
		
		building.setHomeObjects(homeObjList);
		building.setNumberHomeObjects(homeObjList.size());
		BuildingBean.setKindHomeObjects(building);
		BuildingBean.setKindConstruction(building);
		
		//запазване на броя обекти в сградата
		building.setNumberHomeObjectsPrev(building.getNumberHomeObjects());
	}


	static TaxObject loadTaxObject(long taxObjectId) {
		ServiceLocator sericeLocator = ApplicationGlobals.getApplicationGlobals().getLocator();
		return sericeLocator.getTaxObjectDao().getTaxObjectById(taxObjectId);
	}
	
	static DeclDao getDeclDao() {
		ServiceLocator sericeLocator = ApplicationGlobals.getApplicationGlobals().getLocator();
		return sericeLocator.getDeclDao();
	}
	
	
	
	/*static Decl1427SaveState negate(Decl1427SaveState st) {
		//negate taxdocId & property Id
		st.taxDoc.setId(-st.taxDoc.getId());
		st.property.setId(-st.property.getId());
		
		for(Building b:st.buildingList) {
			//negate building ids 
			b.setId(-b.getId());
			
			//negate part home objects for homeobj
			for(HomeObject ho : b.getHomeObjects()) {
				//negate home object id
				ho.setId(-ho.getId());
				
				//negate part home object ids
				for(Long hoId: st.homeObjParts.keySet()) {
					List<AbstractPart> ap =  st.homeObjParts.remove(hoId);
					for(AbstractPart a : ap) a.setId(-a.getId());
					st.homeObjParts.put(-hoId, ap);
				}
			}
		}
		return st;
	}*/
	
	
	/***
	 * Load the minimum taxdoc data needed to be saved for new declaration.
	 * @param taxDocId
	 * @return
	 */
	/*static Decl1427SaveState loadDecl14SaveState(long taxDocId) {
		Decl1427SaveState st = new Decl1427SaveState();
		ServiceLocator sericeLocator = ApplicationGlobals.getApplicationGlobals().getLocator();
		DeclDao dao = sericeLocator.getDeclDao();
		ITaxDocDao tdDao = sericeLocator.getTaxDocDao();
		
		//load taxdoc
		st.taxDoc = (TaxDoc)tdDao.getTaxDoc(taxDocId).get(0);
		
		//load property
		st.property = dao.selectProperty(taxDocId);
		long propertyId = st.property.getId();
		
		//load buildings & home objects 
		st.buildingList  = dao.selectBuildings(propertyId);
		
		
		//load part home object
		for(Building b:st.buildingList) {
			//load home objects 
			b.setHomeObjects(dao.selectHomeObject(b.getId()));
			
			//load part home object for homeobject
			for(HomeObject ho : b.getHomeObjects()) {
				List<AbstractPart> pho = loadPartHomeObjects(ho.getId());
				
				//add to part home object map
				st.homeObjParts.put(ho.getId(), pho);
			}
		}
		return st;
	}
 	*/
}
