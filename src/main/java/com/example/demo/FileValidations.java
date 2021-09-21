package com.example.demo;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
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

			while (entries.hasMoreElements()) {
				ZipEntry zipEntry = entries.nextElement();
				if (zipEntry.isDirectory()) {
					if (zipEntry.getName().equals(PLATES)) {
						zipEntry = entries.nextElement();
						while (zipEntry != null && zipEntry.getName().startsWith(PLATES) && !zipEntry.isDirectory()) {
							InputStream stream = zipFile.getInputStream(zipEntry);
							evaluateHeaders(stream);

							zipEntry = entries.nextElement();
						}
					} else if (zipEntry.getName().equals(TUBES)) {
						zipEntry = entries.nextElement();
						while (zipEntry != null && zipEntry.getName().startsWith(TUBES) && !zipEntry.isDirectory()) {
							InputStream stream = zipFile.getInputStream(zipEntry);
							evaluateHeaders(stream);

							zipEntry = entries.nextElement();
						}
					}
				}
			}
			new UnzipFile(fileZipInput);
			ZipInputStream zipStream = new ZipInputStream(new FileInputStream(fileZipInput));

			if (!checkFolderValidityInZipFile(zipStream)) {
				return false;
			}

			zipStream = new ZipInputStream(new FileInputStream(fileZipInput));

			ZipEntry zipEntry = zipStream.getNextEntry();

			while (zipEntry != null) {

				String folderName = zipEntry.getName();

				if (folderName.equals(PLATES)) {
					zipEntry = zipStream.getNextEntry();
					while (zipEntry != null && zipEntry.getName().startsWith(PLATES) && !zipEntry.isDirectory()) {
						if (!zipEntry.getName().endsWith(".csv")) {
							return false;
						}
						zipEntry = zipStream.getNextEntry();
					}
				} else if (folderName.equals(TUBES)) {
					zipEntry = zipStream.getNextEntry();
					while (zipEntry != null && zipEntry.getName().startsWith(TUBES) && !zipEntry.isDirectory()) {
						if (!zipEntry.getName().endsWith(".csv")) {
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

	private static boolean checkFolderValidityInZipFile(ZipInputStream zipStream) {
		try {

			int plateFileCount = 0;
			int tubeFileCount = 0;
			ZipEntry zipEntry = zipStream.getNextEntry();

			while (zipEntry != null) {

				String folderName = zipEntry.getName();
				if (zipEntry.isDirectory()) {
					if (folderName.equals(PLATES)) {
						zipEntry = zipStream.getNextEntry();
						while (zipEntry != null && zipEntry.getName().startsWith(PLATES) && !zipEntry.isDirectory()) {
							if (!zipEntry.getName().endsWith(".csv")) {
								return false;
							}
							plateFileCount++;
							zipEntry = zipStream.getNextEntry();
						}
					} else if (folderName.equals(TUBES)) {
						zipEntry = zipStream.getNextEntry();
						while (zipEntry != null && zipEntry.getName().startsWith(TUBES) && !zipEntry.isDirectory()) {
							if (!zipEntry.getName().endsWith(".csv")) {
								return false;
							}
							tubeFileCount++;
							zipEntry = zipStream.getNextEntry();
						}
					} else {
						return false;
					}
				} else if (!zipEntry.isDirectory()) {
					return false;
				}
			}

			if (plateFileCount > 100 || tubeFileCount > 10) {
				return false;
			}
		} catch (Exception e) {
			System.out.println(e);
		}
		return true;
	}

	private static boolean evaluateHeaders(InputStream stream) {

		Scanner scanner = new Scanner(stream).useDelimiter(",");

		ArrayList<String> foundHeadersList = new ArrayList<>();
		ArrayList<String> remainingHeadersList = getMandatoryHeaders();

		HashMap<String, Integer> compoundHeaders = getCompoundHeaders();
		HashSet<String> indexNumberSet = new HashSet<>();
		
		int count = 0;

		while (scanner.hasNext()) {
			
			String header = scanner.next();
			if(header.contains("\n")) {
				header = header.split("\n")[0];
				count++;
			}
			if (!isHeader(header)) {
//				break;
			} 
			else {
				if (header.startsWith("ct") || header.startsWith("aq")) {
					if (header.endsWith("_o") || header.endsWith("_m")) {
						if (remainingHeadersList.contains(header) && !foundHeadersList.contains(header)) {
							remainingHeadersList.remove(header);
							foundHeadersList.add(header);
						} 
						else if (foundHeadersList.contains(header)) {
							return false;
						} 
						else if (!remainingHeadersList.contains(header) && !foundHeadersList.contains(header)) {
							return false;
						}
					}
				} 
				else if (header.startsWith("cp") && header.endsWith("_o")) {
					String indexNumber = header.split("_")[1];
					String compoundHeader = header.split("_", 3)[2];

					if (compoundHeaders.containsKey(compoundHeader)) {
						compoundHeaders.put(compoundHeader, compoundHeaders.get(compoundHeader) + 1);
					}
					indexNumberSet.add(indexNumber);
				}
			}
		}

		if (remainingHeadersList.size() != 0) {
			return false;
		}

		for (Map.Entry<String, Integer> header : compoundHeaders.entrySet()) {
			if (header.getValue() != indexNumberSet.size()) {
				return false;
			}
		}
		
		System.out.println("Count :" + count);

		return true;
	}

	private static ArrayList<String> getMandatoryHeaders() {
		return new ArrayList<String>() {
			{
				add("ct_type_m");
				add("ct_label_m");
				add("ct_empty_mass_mg_o");
				add("ct_suggested_barcode_o");
				add("aq_well_index_m");
				add("aq_well_label_o");
				add("aq_vol_ul_o");
				add("aq_mass_mg_o");
			}
		};
	}

	private static HashMap<String, Integer> getCompoundHeaders() {
		return new HashMap<String, Integer>() {
			{
				put("smiles_o", 0);
				put("concentration_o", 0);
				put("pub_chem_id_o", 0);
				put("cas_number_o", 0);
				put("mfcd_number_o", 0);
				put("solubility_flag_o", 0);
				put("hazard_flags_o", 0);
			}
		};
	}

	private static boolean isHeader(String header) {
		return header.startsWith("ct_") || header.startsWith("aq_") || header.startsWith("cp_");
	}

	public static void main(String[] args) {
		System.out.println(areZipFileHeadersValid("src/main/resources/container_data.zip"));
	}
}
