package net.is_bg.ltf.declarations.d14.buildingtransfer;

import net.is_bg.ltf.businessmodels.land.Land;
import net.is_bg.ltf.businessmodels.property.Property;
import net.is_bg.ltf.businessmodels.taxdoc.TaxDoc;

public interface IPropertyLandProperties {
	public void setProperty(Property property);
	public Property getProperty();
	public TaxDoc getTaxDoc();
	public void setLand(Land property);
	public Land getLand();
}
