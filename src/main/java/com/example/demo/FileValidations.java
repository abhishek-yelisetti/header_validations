package com.example.demo;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

public class FileValidations {

	final static String PLATES = "plates/";
	final static String TUBES = "tubes/";

	public static boolean areZipFileHeadersValid(String fileZipInput) {

		try {

			ZipInputStream zipStream = new ZipInputStream(new FileInputStream(fileZipInput));

			if (!checkFolderValidityInZipFile(zipStream)) {
				return false; // Invalid zip file folder structure
			}

			UnzipFile.unzipFile(fileZipInput);

			if (!haveValidContainerAliquotCounts()) {
				return false;
			}

			ZipFile zipFile = new ZipFile(fileZipInput);

			Enumeration<? extends ZipEntry> entries = zipFile.entries();

			while (entries.hasMoreElements()) {
				ZipEntry zipEntry = entries.nextElement();
				if (zipEntry.isDirectory()) {
					if (zipEntry.getName().equals(PLATES)) {
						zipEntry = entries.nextElement();
						while (zipEntry != null && zipEntry.getName().startsWith(PLATES) && !zipEntry.isDirectory()) {
							InputStream stream = zipFile.getInputStream(zipEntry);
							BufferedReader reader = new BufferedReader(new InputStreamReader(stream));

							String headers[] = reader.readLine().split(",");
							if (!evaluateHeaders(headers)) {
								return false; // All headers are not present in the csv file
							}

							zipEntry = entries.nextElement();
						}
					} else if (zipEntry.getName().equals(TUBES)) {
						zipEntry = entries.nextElement();
						while (zipEntry != null && zipEntry.getName().startsWith(TUBES) && !zipEntry.isDirectory()) {
							InputStream stream = zipFile.getInputStream(zipEntry);
							BufferedReader reader = new BufferedReader(new InputStreamReader(stream));

							String headers[] = reader.readLine().split(",");
							if (!evaluateHeaders(headers)) {
								return false; // All headers are not present in the csv file
							}

							zipEntry = entries.nextElement();
						}
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

	private static boolean haveValidContainerAliquotCounts() {

		final String PLATE_FILE_PATH = "src/main/resources/container_data/plates";
		final String TUBE_FILE_PATH = "src/main/resources/container_data/tubes";

		final int TUBES_FILE_LIMIT = 1000;

		try {
			File plates = new File(PLATE_FILE_PATH);
			File tubes = new File(TUBE_FILE_PATH);

			if (plates.exists() && plates.isDirectory()) {
				for (File plate_data : plates.listFiles()) {
					FileInputStream fstream = new FileInputStream(plate_data);
					BufferedReader br = new BufferedReader(new InputStreamReader(new DataInputStream(fstream)));

					int line_count = 0;

					String line = "";
					while ((line = br.readLine()) != null) {
						line_count++;
					}

					line_count--; // Removing header line

					int container_aliquot_count = 10; // Get container aliquot count from Data base

					if (line_count > container_aliquot_count) {
						return false; // No. of rows in the csv is greater than the container aliquot count
					}
				}
			}

			if (tubes.exists() && tubes.isDirectory()) {
				for (File tube_data : tubes.listFiles()) {
					FileInputStream fstream = new FileInputStream(tube_data);
					BufferedReader br = new BufferedReader(new InputStreamReader(new DataInputStream(fstream)));

					int line_count = 0;

					String line = "";
					while ((line = br.readLine()) != null) {
						line_count++;
					}

					line_count--; // Removing header line

					if (line_count > TUBES_FILE_LIMIT) {
						return false; // No. of rows in the csv is greater than the max tubes per file limit
					}
				}
			}

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
							if (!zipEntry.getName().endsWith(".csv") || zipEntry.getName().startsWith("../")) {
								return false; // Not a csv file
							}
							plateFileCount++;
							zipEntry = zipStream.getNextEntry();
						}
					} else if (folderName.equals(TUBES)) {
						zipEntry = zipStream.getNextEntry();
						while (zipEntry != null && zipEntry.getName().startsWith(TUBES) && !zipEntry.isDirectory()) {
							if (!zipEntry.getName().endsWith(".csv") || zipEntry.getName().startsWith("../")) {
								return false; // Not a csv file
							}
							tubeFileCount++;
							zipEntry = zipStream.getNextEntry();
						}
					} else {
						return false; // Folder other than plates and tubes present in zip file
					}
				} else if (!zipEntry.isDirectory()) {
					return false; // Invalid zip folder structure
				}
			}

			if (plateFileCount > 100 || tubeFileCount > 10) {
				return false; // File max upload limit of plates or tubes exceeded
			}
		} catch (Exception e) {
			System.out.println(e);
		}
		return true;
	}

	private static boolean evaluateHeaders(String headers[]) {

		ArrayList<String> foundHeadersList = new ArrayList<>();
		ArrayList<String> remainingHeadersList = getMandatoryHeaders();

		HashMap<String, Integer> compoundHeaders = getCompoundHeaders();
		HashSet<String> indexNumberSet = new HashSet<>();

		for (int i = 0; i < headers.length; i++) {

			String header = headers[i];

			if (header.startsWith("ct_") || header.startsWith("aq_")) {
				if (header.endsWith("_o") || header.endsWith("_m")) {
					if (remainingHeadersList.contains(header) && !foundHeadersList.contains(header)) {
						remainingHeadersList.remove(header);
						foundHeadersList.add(header);
					} else if (foundHeadersList.contains(header)) {
						return false; // Duplicate header found in csv
					} else if (!remainingHeadersList.contains(header) && !foundHeadersList.contains(header)) {
						return false; // Invalid header (Header not present in template)
					}
				}
			} else if (header.startsWith("cp_") && header.endsWith("_o")) {
				String indexNumber = header.split("_")[1];
				String compoundHeader = header.split("_", 3)[2];

				if (compoundHeaders.containsKey(compoundHeader)) {
					compoundHeaders.put(compoundHeader, compoundHeaders.get(compoundHeader) + 1);
				}
				indexNumberSet.add(indexNumber);
			}
		}

		if (remainingHeadersList.size() != 0) {
			return false; // All headers in template not present in csv file
		}

		for (Map.Entry<String, Integer> header : compoundHeaders.entrySet()) {
			if (header.getValue() != indexNumberSet.size()) {
				return false; // All compound headers not present (varying indexes)
			}
		}

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
