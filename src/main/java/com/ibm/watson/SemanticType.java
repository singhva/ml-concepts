package com.ibm.watson;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class SemanticType {
	private static Log log = LogFactory.getLog(SemanticType.class);
	private List<String> shortTypes;
	private Map<String, String> typeMapping;
	private static SemanticType DEFAULT_TYPE;
	
	private SemanticType(List<String> shortTypes, Map<String, String> typeMapping) {
		this.shortTypes = shortTypes;
		this.typeMapping = typeMapping;
	}
	
	public static void main(String[] args) throws Exception {
		Path path = Paths.get(SemanticType.class.getResource("/SemanticTypes_2013AA.txt").toURI());
		System.out.println(Files.readAllLines(path));
	}
	
	public static void build() {
		try {
			Path path = Paths.get(SemanticType.class.getResource("/SemanticTypes_2013AA.txt").toURI());
			List<String> shortTypes = new ArrayList<>();
			Map<String, String> mapping = new HashMap<>();
			for (String raw : Files.readAllLines(path)) {
				String[] line = raw.split("\\|");
				shortTypes.add(line[0]);
				mapping.put(line[0], line[2]);
			}
			DEFAULT_TYPE = new SemanticType(shortTypes, mapping);
		} catch (Exception e) {
			log.error(e);
			throw new RuntimeException("Unable to load semantic types file");
		}
	}

	public static String get(String shortType) {
		if (DEFAULT_TYPE == null) build();
		return DEFAULT_TYPE.typeMapping.get(shortType);
	}
	
	public static List<String> getAll() {
		if (DEFAULT_TYPE == null) build();
		return DEFAULT_TYPE.shortTypes;
	}

}