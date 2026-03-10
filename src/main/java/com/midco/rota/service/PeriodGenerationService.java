package com.midco.rota.service;

import com.midco.rota.model.Period;
import com.midco.rota.repository.PeriodRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class PeriodGenerationService {

	private static final Logger logger = LoggerFactory.getLogger(PeriodGenerationService.class);

	private static final long PERIOD_DURATION_DAYS = 28;
	private static final long DAYS_TO_COVER = 365;

	private final PeriodRepository periodRepository;
	private final PeriodService periodService;

	public PeriodGenerationService(PeriodRepository periodRepository, PeriodService periodService) {
		this.periodRepository = periodRepository;
		this.periodService = periodService;
	}

	/**
	 * Scheduled job: Runs every day at 2 AM Checks if we're in the last period →
	 * generates next year → archives old
	 */
	@Scheduled(cron = "0 0 2 * * *")
	@Transactional
	public void autoManagePeriods() {
		logger.info("Running scheduled period management...");

		LocalDate today = LocalDate.now();

		// Find which period we're currently in
		Optional<Period> currentPeriodOpt = periodRepository.findByDate(today);

		if (currentPeriodOpt.isEmpty()) {
			logger.warn("No current period found for today: {}", today);
			return;
		}

		Period currentPeriod = currentPeriodOpt.get();
		logger.info("Currently in: {} ({} to {})", currentPeriod.getName(), currentPeriod.getStartDate(),
				currentPeriod.getEndDate());

		// Check if this is the last period
		if (isLastPeriodOfCycle(currentPeriod)) {
			logger.info("🎯 We're in the LAST period! Generating next 365 days...");

			generateNext365Days();
			archiveOldPeriods();

			logger.info("✅ Period management complete");
		} else {
			logger.info("Not in last period yet. No action needed.");
		}
	}

	/**
	 * Check if the given period is the last active period
	 */
	private boolean isLastPeriodOfCycle(Period period) {
		Optional<LocalDate> latestEndDate = periodRepository.findLatestEndDate();

		if (latestEndDate.isEmpty()) {
			return false;
		}

		return period.getEndDate().equals(latestEndDate.get());
	}

	/**
	 * Generate periods to cover next 365 days Start from last period end + 1 day
	 * Generate 28-day periods until we've covered 365 days
	 */
	@Transactional
	public void generateNext365Days() {
		// Get last period's end date
		Optional<LocalDate> latestEndDate = periodRepository.findLatestEndDate();

		if (latestEndDate.isEmpty()) {
			throw new RuntimeException("No existing periods found. Cannot generate.");
		}

		// Start from last end date + 1 day
		LocalDate startDate = latestEndDate.get().plusDays(1);

		// Target: cover 365 days from start
		LocalDate targetEndDate = startDate.plusDays(DAYS_TO_COVER - 1);

		logger.info("Generating periods from {} to cover until {} (365 days)", startDate, targetEndDate);

		List<Period> newPeriods = new ArrayList<>();
		LocalDate currentStart = startDate;
		int periodNumber = 1;

		// Generate 28-day periods
		while (currentStart.isBefore(targetEndDate) || currentStart.equals(targetEndDate)) {
			LocalDate currentEnd = currentStart.plusDays(PERIOD_DURATION_DAYS - 1);

			// ✅ FIX: Don't create partial periods at the end
			// If this period would exceed target, stop at previous complete period
			if (currentEnd.isAfter(targetEndDate)) {
				logger.info("Next period would exceed 365-day target. Stopping at {} complete periods.",
						periodNumber - 1);
				break;
			}

			Period period = new Period();
			period.setName(String.format("Period - %d", periodNumber));
			period.setStartDate(currentStart);
			period.setEndDate(currentEnd);
			period.setIsActive(true);

			newPeriods.add(period);

			long days = java.time.temporal.ChronoUnit.DAYS.between(currentStart, currentEnd) + 1;
			logger.debug("Period {}: {} to {} ({} days)", periodNumber, currentStart, currentEnd, days);

			// Move to next period
			currentStart = currentEnd.plusDays(1);
			periodNumber++;

			// Safety: should be 13 periods (13 × 28 = 364 days)
			if (periodNumber > 14) {
				logger.warn("Generated more than 14 periods, stopping");
				break;
			}
		}

		// Save all at once
		periodRepository.saveAll(newPeriods);

		long totalDays = java.time.temporal.ChronoUnit.DAYS.between(newPeriods.get(0).getStartDate(),
				newPeriods.get(newPeriods.size() - 1).getEndDate()) + 1;

		logger.info("✅ Generated {} periods: {} to {} ({} days total)", newPeriods.size(),
				newPeriods.get(0).getStartDate(), newPeriods.get(newPeriods.size() - 1).getEndDate(), totalDays);

		// Refresh cache
		periodService.clearCache();
		periodService.preloadCache();
	}

	/**
	 * Archive periods from previous years (set is_active = false)
	 */
	@Transactional
	public void archiveOldPeriods() {
		LocalDate today = LocalDate.now();
		int currentYear = today.getYear();

		List<Period> activePeriods = periodRepository.findAllActive();
		int archivedCount = 0;

		for (Period period : activePeriods) {
			// Archive if period ends before current year
			if (period.getEndDate().getYear() < currentYear) {
				period.setIsActive(false);
				periodRepository.save(period);
				archivedCount++;
				logger.debug("Archived: {} ({} to {})", period.getName(), period.getStartDate(), period.getEndDate());
			}
		}

		if (archivedCount > 0) {
			logger.info("✅ Archived {} old periods", archivedCount);
			periodService.clearCache();
			periodService.preloadCache();
		} else {
			logger.info("No old periods to archive");
		}
	}

	/**
	 * Manual trigger to generate next 365 days
	 */
	@Transactional
	public String manualGenerate() {
		int beforeCount = periodRepository.findAllActive().size();
		generateNext365Days();
		int afterCount = periodRepository.findAllActive().size();
		int generated = afterCount - beforeCount;

		return String.format("✅ Generated %d periods covering 365 days. Total active: %d", generated, afterCount);
	}

	/**
	 * Manual trigger to archive old periods
	 */
	@Transactional
	public String manualArchive() {
		int beforeCount = periodRepository.findAllActive().size();
		archiveOldPeriods();
		int afterCount = periodRepository.findAllActive().size();
		int archived = beforeCount - afterCount;

		return String.format("✅ Archived %d periods. Active remaining: %d", archived, afterCount);
	}

	/**
	 * Get current status
	 */
	public String getStatus() {
		LocalDate today = LocalDate.now();
		List<Period> activePeriods = periodRepository.findAllActive();

		Optional<Period> currentPeriodOpt = periodRepository.findByDate(today);
		Optional<LocalDate> latestEndDate = periodRepository.findLatestEndDate();

		if (currentPeriodOpt.isEmpty()) {
			return "No current period found for today";
		}

		Period currentPeriod = currentPeriodOpt.get();
		boolean isLast = isLastPeriodOfCycle(currentPeriod);

		return String.format(
				"Active Periods: %d\n" + "Current Period: %s (%s to %s)\n" + "Latest Period Ends: %s\n"
						+ "Is Last Period: %s\n" + "Will Generate Next: %s",
				activePeriods.size(), currentPeriod.getName(), currentPeriod.getStartDate(), currentPeriod.getEndDate(),
				latestEndDate.map(LocalDate::toString).orElse("N/A"), isLast ? "YES" : "NO",
				isLast ? "YES (in last period)" : "NO");
	}
}