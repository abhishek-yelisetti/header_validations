package com.example.demo;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.Scanner;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

public class FileValidations {
	
	final static String PLATES = "plates/";
	final static String TUBES = "tubes/";

	public static boolean areZipFileHeadersValid(String fileZipInput) {

		try {
			
			ZipFile zipFile = new ZipFile(fileZipInput);

		    Enumeration<? extends ZipEntry> entries = zipFile.entries();

		    while(entries.hasMoreElements()){
		        ZipEntry zipEntry = entries.nextElement();
		        if(zipEntry.isDirectory()) {
		        	if(zipEntry.getName().equals(PLATES)) {
		        		zipEntry = entries.nextElement();
						while(zipEntry != null && zipEntry.getName().startsWith(PLATES) && !zipEntry.isDirectory()) {
							InputStream stream = zipFile.getInputStream(zipEntry);
							evaluateHeaders(stream);
							
							zipEntry = entries.nextElement();
						}
		        	}
		        }
		    }

			File destDir = new File("src/main/resources/unZipFile");
			byte[] buffer = new byte[1024];
			ZipInputStream zipStream = new ZipInputStream(new FileInputStream(fileZipInput));
		
			if(!checkFolderValidityInZipFile(zipStream)) {
				return false;
			}
			
			zipStream = new ZipInputStream(new FileInputStream(fileZipInput));
	
			ZipEntry zipEntry = zipStream.getNextEntry();
			
			while (zipEntry != null) {
				
				String folderName = zipEntry.getName();
				
				if(folderName.equals(PLATES)) {
					zipEntry = zipStream.getNextEntry();
					while(zipEntry != null && zipEntry.getName().startsWith(PLATES) && !zipEntry.isDirectory()) {
						if(!zipEntry.getName().endsWith(".csv")) {
							return false;
						}
						zipEntry = zipStream.getNextEntry();
					}
				}
				else if(folderName.equals(TUBES)) {
					zipEntry = zipStream.getNextEntry();
					while(zipEntry != null && zipEntry.getName().startsWith(TUBES) && !zipEntry.isDirectory()) {
						if(!zipEntry.getName().endsWith(".csv")) {
							return false;
						}
						zipEntry = zipStream.getNextEntry();
					}
				}
			}

			zipStream.closeEntry();
			zipStream.close();
		} catch (Exception e) {
			System.out.println(e);
		}
		return true;
	}
	
	private static void evaluateHeaders(InputStream stream) {
        
        Scanner scanner = new Scanner(stream).useDelimiter(",");
        
//        private final String mandatoryHeaders[] = new String[] {
//        		
//        }
        
        
        while(scanner.hasNext()) {
        	String header = scanner.next();
        	if(!isHeader(header)) {
        		break;
        	}
        }
        
	}

	private static boolean isHeader(String header) {
		return header.startsWith("ct") || header.startsWith("aq") || header.startsWith("cp");
	}

	private static boolean checkFolderValidityInZipFile(ZipInputStream zipStream) {
		try {
			
			int plateFileCount = 0;
			int tubeFileCount = 0;
			ZipEntry zipEntry = zipStream.getNextEntry();
			
			while(zipEntry != null) {
				
				String folderName = zipEntry.getName();
				if(zipEntry.isDirectory()) {
					if(folderName.equals(PLATES)) {
						zipEntry = zipStream.getNextEntry();
						while(zipEntry != null && zipEntry.getName().startsWith(PLATES) && !zipEntry.isDirectory()) {
							if(!zipEntry.getName().endsWith(".csv")) {
								return false;
							}
							plateFileCount++;
							zipEntry = zipStream.getNextEntry();
						}
					}
					else if(folderName.equals(TUBES)) {
						zipEntry = zipStream.getNextEntry();
						while(zipEntry != null && zipEntry.getName().startsWith(TUBES) && !zipEntry.isDirectory()) {
							if(!zipEntry.getName().endsWith(".csv")) {
								return false;
							}
							tubeFileCount++;
							zipEntry = zipStream.getNextEntry();
						}
					}
					else {
						return false;
					}
				}
				else if(!zipEntry.isDirectory()) {
					return false;
				}	
			}
			
			if(plateFileCount > 100 || tubeFileCount > 10) {
				return false;
			}
		}catch(Exception e) {
			System.out.println(e);
		}
		return true;
	}

	public static void main(String[] args) {
		System.out.println(areZipFileHeadersValid("src/main/resources/container_data.zip"));
	}
}
