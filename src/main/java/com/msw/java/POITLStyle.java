package com.msw.java;

import com.deepoove.poi.data.style.Style;
import com.deepoove.poi.data.style.TableStyle;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STJc.Enum;


public class POITLStyle {

	public static Style getHeaderStyle(){
		Style style = new Style();
		style.setBold(true);
		style.setFontSize(11);
		style.setColor("000000");
		style.setFontFamily("宋体");
		style.setVertAlign("baseline");
		return style;
	}
	
	public static TableStyle getHeaderTableStyle(){
		TableStyle style = new TableStyle();
		style.setAlign(Enum.forInt(2));
//		style.setBackgroundColor("B7B7B7");
		return style;
	}
	
	public static Style getBodyStyle(){
		Style style = new Style();
		style.setBold(false);
		style.setFontSize(11);
		style.setColor("000000");
		style.setFontFamily("宋体");
		return style;
	}
	
	public static TableStyle getBodyTableStyle(){
		TableStyle style = new TableStyle();
		style.setAlign(Enum.forInt(2));
		return style;
	}
}
