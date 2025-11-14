package com.midco.rota.controller;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DataFormat;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.midco.rota.RateTableProvider;
import com.midco.rota.model.EmployeeShiftStatDTO;
import com.midco.rota.model.PaycycleStatsDTO;
import com.midco.rota.model.ServiceStatsDTO;
import com.midco.rota.model.ShiftSummaryDTO;
import com.midco.rota.model.ShiftTypeStatsDTO;
import com.midco.rota.model.WeekStatsDTO;
import com.midco.rota.model.WeeklyShiftStatDTO;
import com.midco.rota.service.PaycycleStatsService;
import com.midco.rota.util.RateCode;
import com.midco.rota.util.ShiftType;

import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletResponse;

@RestController
@RequestMapping("/api/stats")
public class StatsController {

	private final RateTableProvider rateTableProvider;

	@Autowired
	private final PaycycleStatsService statsService;

	private DateTimeFormatter dateFormat = DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT).withLocale(Locale.UK);

	public StatsController(PaycycleStatsService statsService, RateTableProvider rateTableProvider) {
		this.statsService = statsService;
		this.rateTableProvider = rateTableProvider;
	}

	@GetMapping("/serviceStats")
	public ResponseEntity<List<PaycycleStatsDTO>> serviceStats(@RequestParam Long id) {

		List<PaycycleStatsDTO> summary = statsService.generateServiceSummary(id);

		return ResponseEntity.ok(summary);
	}

	@GetMapping("/empStats")
	public ResponseEntity<List<EmployeeShiftStatDTO>> empStats(@RequestParam Long id) {

		List<EmployeeShiftStatDTO> summary = statsService.generateEmpSummary(id);

		return ResponseEntity.ok(summary);
	}

	@GetMapping("/exportStats")
	public void exportStatsStream(@RequestParam Long id, HttpServletResponse response) throws IOException {
		response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
		String filename = "stats.xlsx";
		String encoded = URLEncoder.encode(filename, StandardCharsets.UTF_8.name());
		response.setHeader("Content-Disposition",
				"attachment; filename=\"" + filename + "\"; filename*=UTF-8''" + encoded);

		// Create workbook first
		SXSSFWorkbook workbook = new SXSSFWorkbook(200);
		workbook.setCompressTempFiles(true);

		try {
			// populate workbook
			List<PaycycleStatsDTO> serviceSummary = statsService.generateServiceSummary(id);
			List<EmployeeShiftStatDTO> empSummary = statsService.generateEmpSummary(id);
			createSheetServiceStats(workbook, "ServiceStats", serviceSummary);
			createSheetEmpStats(workbook, "EmpStats", empSummary);
			createSheetProjectedPay(workbook, "ProjectedPay", empSummary);
			// Write while servlet output stream is still open
			ServletOutputStream out = response.getOutputStream();
			workbook.write(out);
			out.flush();

			// IMPORTANT: dispose temp files while out is still open
			workbook.dispose();
			// Do NOT call workbook.close() here; it will try to dispose/close again
		} catch (IOException e) {
			// try best-effort to clean up temp files
			try {
				workbook.dispose();
			} catch (Exception ignored) {
			}
			throw e;
		}
		// do not explicitly close the servlet output stream; container will handle it
	}

	private void createSheetServiceStats(Workbook workbook, String sheetName, List<PaycycleStatsDTO> data) {
		Sheet sheet = workbook.createSheet(sheetName);
		int rowIdx = 0;
		if (data == null || data.isEmpty()) {

			return;
		}

		String[] headers = { "Region", "Period", "PeriodId", "Location", "WeekNumber", "WeekStart", "WeekEnd",
				"ShiftType", "TotalHours", "AllocatedHours", "UnallocatedHours", "ShiftCount", "AllocationCount" };
		// Header Row
		Row headerRow = sheet.createRow(rowIdx++);
		CellStyle headerStyle = createHeaderStyle(workbook);
		for (int i = 0; i < headers.length; i++) {
			Cell cell = headerRow.createCell(i);
			cell.setCellValue(headers[i]);
			cell.setCellStyle(headerStyle);
		}

		// --- Flatten nested data ---
		for (PaycycleStatsDTO paycycle : data) {
			String region = paycycle.region;
			String period = paycycle.period;
			String periodId = paycycle.periodId;

			if (paycycle.services == null)
				continue;

			for (ServiceStatsDTO service : paycycle.services) {
				String location = service.location;

				if (service.weeks == null)
					continue;

				for (WeekStatsDTO week : service.weeks) {
					int weekNumber = week.weekNumber;
					String start = week.start.format(dateFormat);
					String end = week.end.format(dateFormat);

					if (week.shiftStats == null)
						continue;

					for (ShiftTypeStatsDTO stat : week.shiftStats) {
						Row row = sheet.createRow(rowIdx++);
						int col = 0;

						row.createCell(col++).setCellValue(region);
						row.createCell(col++).setCellValue(period);
						row.createCell(col++).setCellValue(periodId);
						row.createCell(col++).setCellValue(location);
						row.createCell(col++).setCellValue(weekNumber);
						row.createCell(col++).setCellValue(start);
						row.createCell(col++).setCellValue(end);
						row.createCell(col++).setCellValue(stat.shiftType.toString());
						row.createCell(col++).setCellValue(stat.totalHours.toString());
						row.createCell(col++).setCellValue(stat.allocatedHours.toString());
						row.createCell(col++).setCellValue(stat.unallocatedHours.toString());
						row.createCell(col++).setCellValue(stat.shiftCount);
						row.createCell(col++).setCellValue(stat.allocationCount);
					}
				}
			}
		}

//		// Auto-size columns for better readability (optional)
//		for (int i = 0; i < headers.length; i++) {
//			sheet.autoSizeColumn(i);
//		}

		// Freeze the header row
		sheet.createFreezePane(0, 1);

	}

	private void createSheetEmpStats(Workbook workbook, String sheetName, List<EmployeeShiftStatDTO> data) {
		// sanitize sheet name
		String safeName = sheetName == null ? "EmpStats" : sheetName;
		if (safeName.length() > 31)
			safeName = safeName.substring(0, 31);
		Sheet sheet = workbook.createSheet(safeName);

		if (data == null || data.isEmpty())
			return;

		// Define columns. Adjust order/labels to match your ServiceStats sheet style.
		String[] headers = { "First Name", "Last Name", "ContractType", "WeekNumber", "WeekStart", "WeekEnd",
				"ShiftType", "ShiftCount", "ShiftHours" };
		int numCols = headers.length;

		// Track max chars per column so we can set column widths safely with SXSSF
		int[] maxChars = new int[numCols];
		for (int i = 0; i < numCols; i++)
			maxChars[i] = headers[i] == null ? 0 : headers[i].length();

		// Styles and formats (reuse)
		CellStyle headerStyle = createHeaderStyle(workbook);
		DataFormat df = workbook.createDataFormat();
		CellStyle intStyle = workbook.createCellStyle();
		intStyle.setDataFormat(df.getFormat("0"));
		CellStyle decimalStyle = workbook.createCellStyle();
		decimalStyle.setDataFormat(df.getFormat("0.00"));
		CellStyle dateStyle = workbook.createCellStyle();
		dateStyle.setDataFormat(df.getFormat("dd-mm-yyyy"));

		int rowIdx = 0;
		Row headerRow = sheet.createRow(rowIdx++);
		for (int i = 0; i < numCols; i++) {
			Cell c = headerRow.createCell(i);
			c.setCellValue(headers[i]);
			c.setCellStyle(headerStyle);
		}

		int dataRowCount = 0;

		// Decide an order for ShiftType columns; keeps exported rows predictable
		List<ShiftType> shiftTypeOrder = Arrays.asList(ShiftType.values()); // or custom order

		for (EmployeeShiftStatDTO emp : data) {
			if (emp == null || emp.weeklyStats == null)
				continue;
			//String empName = emp.name == null ? "" : emp.name;
			String contract = emp.contractType == null ? "" : emp.contractType.toString();

			for (WeeklyShiftStatDTO week : emp.weeklyStats) {
				if (week == null || week.shiftSummary == null)
					continue;
				Integer weekNumber = week.weekNumber;
				LocalDate start = week.weekStart;
				LocalDate end = week.weekEnd;

				// For each shift type present in the map (or all types in ordered list)
				for (ShiftType st : shiftTypeOrder) {
					ShiftSummaryDTO summary = week.shiftSummary.get(st);
					// Skip empty summaries if you prefer fewer rows:
					if (summary == null || (summary.count == 0
							&& (summary.hours == null || summary.hours.compareTo(BigDecimal.ZERO) == 0))) {
						continue;
					}

					Row row = sheet.createRow(rowIdx++);
					int col = 0;
					// Split name into first/last
					String fullName = emp.name == null ? "" : emp.name.trim();
					String firstName = "";
					String lastName = "";
					if (!fullName.isEmpty()) {
						String[] parts = fullName.split("\\s+", 2);
						firstName = parts[0];
						if (parts.length > 1)
							lastName = parts[1];
					}
					
					// EmployeeName
					row.createCell(col++).setCellValue(firstName);
					row.createCell(col++).setCellValue(lastName);

					// ContractType
					row.createCell(col++).setCellValue(contract);
					// WeekNumber
					if (weekNumber != null) {
						Cell cWeek = row.createCell(col++);
						cWeek.setCellValue(weekNumber);
						cWeek.setCellStyle(intStyle);
						maxChars[col - 1] = Math.max(maxChars[col - 1], String.valueOf(weekNumber).length());
					} else {
						row.createCell(col++).setCellValue("");
					}

					// WeekStart (as Excel date)
					Cell cStart = row.createCell(col++);
					if (start != null) {
						cStart.setCellValue(start);
						cStart.setCellStyle(dateStyle);
						maxChars[col - 1] = Math.max(maxChars[col - 1], start.toString().length());
					} else {
						cStart.setCellValue("");
					}

					// WeekEnd
					Cell cEnd = row.createCell(col++);
					if (end != null) {
						cEnd.setCellValue(end);
						cEnd.setCellStyle(dateStyle);
						maxChars[col - 1] = Math.max(maxChars[col - 1], end.toString().length());
					} else {
						cEnd.setCellValue("");
					}

					// ShiftType
					String stName = st == null ? "" : st.toString();
					Cell shiftType = row.createCell(col++);
					shiftType.setCellValue(stName);
					shiftType.setCellStyle(intStyle);
					// ShiftCount (int)
					Cell cCount = row.createCell(col++);
					cCount.setCellValue(summary == null ? 0 : summary.count);
					cCount.setCellStyle(intStyle);
					// maxChars[col - 1] = Math.max(maxChars[col - 1], String.valueOf(summary ==
					// null ? 0 : summary.count).length());

					// ShiftHours (decimal)
					Cell cHours = row.createCell(col++);
					double hoursVal = summary == null ? 0d : summary.hours == null ? 0d : summary.hours.doubleValue();
					cHours.setCellValue(hoursVal);
					cHours.setCellStyle(decimalStyle);
					// maxChars[col - 1] = Math.max(maxChars[col - 1], String.format(Locale.ROOT,
					// "%.2f", hoursVal).length());

					dataRowCount++;
				}
			}
		}
		// Freeze header
		sheet.createFreezePane(0, 1);
	}

	private CellStyle createHeaderStyle(Workbook workbook) {
		CellStyle style = workbook.createCellStyle();

		// Create a bold font
		Font headerFont = workbook.createFont();
		headerFont.setBold(true);
		headerFont.setFontHeightInPoints((short) 11);
		headerFont.setColor(IndexedColors.WHITE.getIndex());

		// Apply font and background color
		style.setFont(headerFont);
		style.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
		style.setFillPattern(FillPatternType.SOLID_FOREGROUND);

		// Center text
		style.setAlignment(HorizontalAlignment.CENTER);
		style.setVerticalAlignment(VerticalAlignment.CENTER);

		// Add thin borders around cells
		style.setBorderTop(BorderStyle.THIN);
		style.setBorderBottom(BorderStyle.THIN);
		style.setBorderLeft(BorderStyle.THIN);
		style.setBorderRight(BorderStyle.THIN);

		return style;
	}

	private void createSheetProjectedPay(Workbook workbook, String sheetName, List<EmployeeShiftStatDTO> data) {
		Sheet sheet = workbook.createSheet(sheetName);
		if (data == null || data.isEmpty())
			return;

		List<Integer> sortedWeeks = data.stream().filter(Objects::nonNull)
				.flatMap(e -> e.weeklyStats == null ? Stream.<WeeklyShiftStatDTO>empty() : e.weeklyStats.stream())
				.filter(Objects::nonNull).map(ws -> ws.weekNumber).distinct().sorted().collect(Collectors.toList());
		if (sortedWeeks.isEmpty())
			sortedWeeks = Arrays.asList(1, 2, 3, 4);

		List<ShiftType> shiftTypes = Arrays.asList(ShiftType.values());

		CellStyle headerStyle = createHeaderStyle(workbook);
		DataFormat df = workbook.createDataFormat();
		CellStyle intStyle = workbook.createCellStyle();
		intStyle.setDataFormat(df.getFormat("0"));
		CellStyle decStyle = workbook.createCellStyle();
		decStyle.setDataFormat(df.getFormat("0.00"));

		// --- Top header row ---
		Row topHeader = sheet.createRow(0);
		int col = 0;

		// First Name
		sheet.addMergedRegion(new CellRangeAddress(0, 1, col, col));
		Cell fnHeader = topHeader.createCell(col++);
		fnHeader.setCellValue("First Name");
		fnHeader.setCellStyle(headerStyle);

		// Last Name
		sheet.addMergedRegion(new CellRangeAddress(0, 1, col, col));
		Cell lnHeader = topHeader.createCell(col++);
		lnHeader.setCellValue("Last Name");
		lnHeader.setCellStyle(headerStyle);

		// Contract Type
		sheet.addMergedRegion(new CellRangeAddress(0, 1, col, col));
		Cell ctHeader = topHeader.createCell(col++);
		ctHeader.setCellValue("Contract Type");
		ctHeader.setCellStyle(headerStyle);

		// Weeks
		for (Integer w : sortedWeeks) {
			sheet.addMergedRegion(new CellRangeAddress(0, 0, col, col + 1));
			Cell weekCell = topHeader.createCell(col);
			weekCell.setCellValue("Week " + w);
			weekCell.setCellStyle(headerStyle);
			col += 2;
		}

		// Totals
		sheet.addMergedRegion(new CellRangeAddress(0, 0, col, col + 1));
		Cell totalCell = topHeader.createCell(col);
		totalCell.setCellValue("Total");
		totalCell.setCellStyle(headerStyle);
		col += 2;

		// Projected Pay spanning dynamic columns
		int payCols = shiftTypes.stream().mapToInt(st -> 2).sum() + 1; // 2 per type + TOTAL PAY
		sheet.addMergedRegion(new CellRangeAddress(0, 0, col, col + payCols - 1));
		Cell payHeader = topHeader.createCell(col);
		payHeader.setCellValue("PROJECTED PAY");
		payHeader.setCellStyle(headerStyle);

		// --- Subheader row ---
		Row subHeader = sheet.createRow(1);
		col = 3; // start after FirstName, LastName, ContractType

		for (int i = 0; i < sortedWeeks.size(); i++) {
			Cell hCell = subHeader.createCell(col++);
			hCell.setCellValue("Hours");
			hCell.setCellStyle(headerStyle);

			Cell sCell = subHeader.createCell(col++);
			sCell.setCellValue("Shifts");
			sCell.setCellStyle(headerStyle);
		}

		Cell thCell = subHeader.createCell(col++);
		thCell.setCellValue("Hours");
		thCell.setCellStyle(headerStyle);

		Cell tsCell = subHeader.createCell(col++);
		tsCell.setCellValue("Shifts");
		tsCell.setCellStyle(headerStyle);

		for (ShiftType st : shiftTypes) {
			String rateType = getRateTypeForShiftType(st); // "Hourly" or "Daily"
			if ("HOURLY".equals(rateType)) {
				Cell h = subHeader.createCell(col++);
				h.setCellValue(st + " Hours");
				h.setCellStyle(headerStyle);

				Cell p = subHeader.createCell(col++);
				p.setCellValue(st + " Pay");
				p.setCellStyle(headerStyle);
			} else {
				Cell c = subHeader.createCell(col++);
				c.setCellValue(st + " Count");
				c.setCellStyle(headerStyle);

				Cell p = subHeader.createCell(col++);
				p.setCellValue(st + " Pay");
				p.setCellStyle(headerStyle);
			}
		}

		Cell tpCell = subHeader.createCell(col++);
		tpCell.setCellValue("TOTAL PAY");
		tpCell.setCellStyle(headerStyle);

		// --- Data rows ---
		int rowIdx = 2;
		for (EmployeeShiftStatDTO emp : data) {
			if (emp == null)
				continue;
			Map<Integer, WeeklyShiftStatDTO> weekMap = emp.weeklyStats == null ? Collections.emptyMap()
					: emp.weeklyStats.stream().filter(Objects::nonNull)
							.collect(Collectors.toMap(ws -> ws.weekNumber, Function.identity(), (a, b) -> a));

			Row row = sheet.createRow(rowIdx++);
			col = 0;

			// Split name into first/last
			String fullName = emp.name == null ? "" : emp.name.trim();
			String firstName = "";
			String lastName = "";
			if (!fullName.isEmpty()) {
				String[] parts = fullName.split("\\s+", 2);
				firstName = parts[0];
				if (parts.length > 1)
					lastName = parts[1];
			}

			row.createCell(col++).setCellValue(firstName);
			row.createCell(col++).setCellValue(lastName);
			row.createCell(col++).setCellValue(emp.contractType == null ? "" : emp.contractType.toString());

			double totalHours = 0;
			int totalShifts = 0;
			for (Integer w : sortedWeeks) {
				WeeklyShiftStatDTO ws = weekMap.get(w);
				double hrs = 0;
				int shifts = 0;
				if (ws != null && ws.shiftSummary != null) {
					for (ShiftSummaryDTO s : ws.shiftSummary.values()) {
						if (s != null) {
							shifts += s.count;
							if (s.hours != null)
								hrs += s.hours.doubleValue();
						}
					}
				}
				row.createCell(col++).setCellValue(hrs);
				row.createCell(col++).setCellValue(shifts);
				totalHours += hrs;
				totalShifts += shifts;
			}
			row.createCell(col++).setCellValue(totalHours);
			row.createCell(col++).setCellValue(totalShifts);

			double totalPay = 0;
			for (ShiftType st : shiftTypes) {
				ShiftSummaryDTO summary = emp.weeklyStats == null ? null
						: emp.weeklyStats.stream().map(ws -> ws.shiftSummary == null ? null : ws.shiftSummary.get(st))
								.filter(Objects::nonNull).reduce(new ShiftSummaryDTO(), (a, b) -> {
									a.count += b.count;
									a.hours = a.hours.add(b.hours);
									return a;
								});

				String rateType = getRateTypeForShiftType(st);
				BigDecimal rate = getRateForShiftType(emp.region, rateType, emp.rateCode);
				double pay = 0;
				if ("HOURLY".equals(rateType)) {
					double hrs = summary == null ? 0 : summary.hours.doubleValue();
					row.createCell(col++).setCellValue(hrs);
					pay = hrs * (rate == null ? 0 : rate.doubleValue());
					row.createCell(col++).setCellValue(pay);
				} else {
					int cnt = summary == null ? 0 : summary.count;
					row.createCell(col++).setCellValue(cnt);
					pay = cnt * (rate == null ? 0 : rate.doubleValue());
					row.createCell(col++).setCellValue(pay);
				}
				totalPay += pay;
			}
			row.createCell(col++).setCellValue(totalPay);
		}

		sheet.createFreezePane(0, 2);
	}

	// Decide whether a shift type is paid hourly or daily
	private String getRateTypeForShiftType(ShiftType st) {
		switch (st) {
		case LONG_DAY:
			return "DAILY";
		case FLOATING:
			return "HOURLY";
		case DAY:
			return "HOURLY";
		case WAKING_NIGHT:
			return "HOURLY";
		case CARE_CALL:
			return "HOURLY";
		case SLEEP_IN:
			return "DAILY";
		default:
			return "DAILY"; // fallback
		}
	}

	// Return the numeric rate for a shift type + contract type
	private BigDecimal getRateForShiftType(String region, String rateType, RateCode rateCode) {
		if ("DAILY".equals(rateType)) {
			// if rate Type is DAILY, override ratecode and pick daily rate 
			return RateTableProvider.getAmount(region, rateType, RateCode.L1.name());
		}

		return RateTableProvider.getAmount(region, rateType, rateCode.name());
	}

}
