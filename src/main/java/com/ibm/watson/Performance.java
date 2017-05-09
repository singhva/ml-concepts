package com.ibm.watson;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.Row.MissingCellPolicy;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

public class Performance {

	public static void main(String[] args) throws FileNotFoundException, IOException {
		Workbook workbook = new XSSFWorkbook(new FileInputStream("/Users/singhv/Desktop/Concept_Detection/corpus-02.xlsx"));
		Sheet sheet = workbook.getSheet("Mentions");
		Row first = sheet.getRow(0);
		
		Font font = workbook.createFont();
		font.setFontHeightInPoints((short) 11);
		font.setFontName("Calibri");
		font.setBold(true);
		CellStyle style = workbook.createCellStyle();
		style.setFont(font);
		
		Cell nciCodeCell = first.createCell(2, CellType.STRING);
		nciCodeCell.setCellValue("NCI Code");
		nciCodeCell.setCellStyle(style);
		Cell preferredNameCell = first.createCell(3, CellType.STRING);
		preferredNameCell.setCellValue("NCI Preferred Name");
		preferredNameCell.setCellStyle(style);
		Cell semanticType = first.createCell(4, CellType.STRING);
		semanticType.setCellValue("Semantic Type");
		semanticType.setCellStyle(style);
		
		QueryLucene queryLucene = new QueryLucene("/Users/singhv/Documents/Data/lucene_nci");
		Iterator<Row> rowIt = sheet.rowIterator();
		rowIt.next();
		while (rowIt.hasNext()) {
			Row row = rowIt.next();
			String mention = row.getCell(1, MissingCellPolicy.CREATE_NULL_AS_BLANK).getStringCellValue();
			System.out.println(mention);
			if (!StandardAnalyzer.ENGLISH_STOP_WORDS_SET.contains(mention)) {
				List<DocumentAndScore> docs = queryLucene.doQuery(mention);
				if (docs.size() == 1) {
					Document doc = docs.get(0).getDocument();
					row.getCell(2).setCellValue(doc.get("code"));
					row.getCell(3).setCellValue(doc.get("name"));
					row.getCell(4).setCellValue(doc.get("type"));
				}
				else if (docs.size() > 1) {
					List<String> names = new ArrayList<>();
					List<String> codes = new ArrayList<>();
					List<String> types = new ArrayList<>();
					docs.subList(0, Math.min(3, docs.size())).forEach( docAndScore -> {
						Document doc = docAndScore.getDocument();
						names.add(doc.get("name"));
						codes.add(doc.get("code"));
						types.add(doc.get("type"));
					});
					row.getCell(2).setCellValue(String.join(", ", codes));
					row.getCell(3).setCellValue(String.join(", ", names));
					row.getCell(4).setCellValue(String.join(", ", types));
				}
				else {
					row.getCell(2).setCellValue(" ");
					row.getCell(3).setCellValue(" ");
					row.getCell(4).setCellValue(" ");
				}	
			}
			else {
				row.getCell(2).setCellValue(" ");
				row.getCell(3).setCellValue(" ");
				row.getCell(4).setCellValue(" ");
			}
			
		}
		workbook.write(new FileOutputStream(new File("/Users/singhv/Desktop/Concept_Detection/corpus-02_lucene_v2.xlsx")));
		queryLucene.finish();
		workbook.close();
	}

}