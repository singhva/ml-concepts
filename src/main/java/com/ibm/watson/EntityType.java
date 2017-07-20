package com.ibm.watson;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

public class EntityType {
	
	private List<String> entityTypes;
	private Map<String, String[]> entityToSemTypeMapping;
	private static EntityType DEFAULT_TYPE;
	
	private EntityType(List<String> entityTypes, Map<String, String[]> entityToSemTypeMapping) {
		this.entityTypes = entityTypes;
		this.entityToSemTypeMapping = entityToSemTypeMapping;
	}
	
	public static void main(String[] args) {
		EntityType.build();
		String[] types = EntityType.getSemanticTypes("Anatomy_Specific_Entity");
		System.out.println(String.join("\n", types));
	}
	
	public static void build() {
		List<String> entityTypes = new ArrayList<>();
		Map<String, String[]> entityToSemTypeMapping = new HashMap<>();
		DEFAULT_TYPE = new EntityType(entityTypes, entityToSemTypeMapping);
		try (Workbook workbook = new XSSFWorkbook(SemanticType.class.getResourceAsStream("/Semantic_Types_to_Entity_Types_Mapping_for_CDS.xlsx"))) {
			Sheet sheet = workbook.getSheetAt(0);
			Iterator<Row> it = sheet.rowIterator();
			it.next();
			while(it.hasNext()) {
				Row row = it.next();
				Iterator<Cell> it1 = row.cellIterator();
				List<String> temp = new ArrayList<>();
				String entityType = it1.next().getStringCellValue(); 
				entityTypes.add(entityType);
				while (it1.hasNext()) {
					String value = it1.next().getStringCellValue().trim();
					if ( ! (value.equals("") || value.equals("unknown")) )
						temp.add(value);
				}
				entityToSemTypeMapping.put(entityType, temp.toArray(new String[0]));
			}
		} catch (IOException e) {
			throw new RuntimeException("Unable to load mapping file");
		}
	}
	
	public static String[] getSemanticTypes(String entityType) {
		if (DEFAULT_TYPE == null) build();
		return DEFAULT_TYPE.entityToSemTypeMapping.get(entityType);
	}
	
	public static List<String> getAllEntityTypes() {
		if (DEFAULT_TYPE == null) build();
		return DEFAULT_TYPE.entityTypes;
	}

}
