package net.is_bg.ltf.declarations.d14.buildingtransfer;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import net.is_bg.ltf.businessmodels.building.Building;
import net.is_bg.ltf.businessmodels.land.Land;
import net.is_bg.ltf.businessmodels.property.Property;
import net.is_bg.ltf.businessmodels.taxdoc.TaxDoc;

/**
 * The data loaded when taxdoc is being transferred from one declaration to other declaration.
 * @author Lubo
 *
 */
class LoadedTaxDocData implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 5933630948836754130L;
	TaxDoc taxdoc;
	Property property;
	Land land;
	List<Building> buildingsToSelect = new ArrayList<Building>();
	
	TaxDoc getTaxdoc() {
		return taxdoc;
	}
	
	Property getProperty() {
		return property;
	}
	
	Land getLand() {
		return land;
	}
	
	List<Building> getBuildingsToSelect() {
		return buildingsToSelect;
	}
}
