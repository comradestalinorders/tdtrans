package net.is_bg.ltf.declarations.d14.buildingtransfer;

import java.io.Serializable;
import java.util.List;
import net.is_bg.ltf.db.common.DBStatement;

public interface ITaxDocTree extends Serializable{
	public void load(long taxdocid);
	List<DBStatement> getSaveList();
}
