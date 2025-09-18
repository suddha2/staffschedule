package com.midco.rota.service;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import com.midco.rota.model.DeferredSolveRequest;
import com.midco.rota.model.Rota;
import com.midco.rota.repository.DeferredSolveRequestRepository;
import com.midco.rota.repository.RotaRepository;
import com.midco.rota.util.PayCycleRow;
import com.midco.rota.util.ShiftType;

@Service
public class PayCycleDataService {

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private DeferredSolveRequestRepository deferredSolveRequestRepository;

	@Autowired
	private RotaRepository rotaRepository;

	public record PeriodKey(LocalDate start, LocalDate end) {
		@Override
		public boolean equals(Object o) {
			if (this == o)
				return true;
			if (!(o instanceof PeriodKey))
				return false;
			PeriodKey that = (PeriodKey) o;
			return Objects.equals(start, that.start) && Objects.equals(end, that.end);
		}

		@Override
		public int hashCode() {
			return Objects.hash(start, end);
		}

		@Override
		public String toString() {
			return start.toString() + "-" + end.toString();
		}

	}
	
	public List<PayCycleRow> fetchRows(DeferredSolveRequest  deferredSolveRequest){
		String sql = "SELECT id, name, start_date, end_date FROM pay_cycle_periods where active is true and start_date=? and end_date=? ";

		List<PayCycleRow> result = jdbcTemplate
				.query(sql,new Object[] {deferredSolveRequest.getStartDate(),deferredSolveRequest.getEndDate()},
						(rs, rowNum) -> PayCycleRow.Builder.builder().withCore(rs.getLong("id"), rs.getString("name"),
								rs.getDate("start_date").toLocalDate(), rs.getDate("end_date").toLocalDate(), deferredSolveRequest.getRegion())
								.build());

		return enrichPayCycleRow(result, deferredSolveRequest.getRegion());
	}

	public List<PayCycleRow> fetchRows(String location) {
		String sql = "SELECT id, name, start_date, end_date FROM pay_cycle_periods where active is true";

		List<PayCycleRow> result = jdbcTemplate
				.query(sql,
						(rs, rowNum) -> PayCycleRow.Builder.builder().withCore(rs.getLong("id"), rs.getString("name"),
								rs.getDate("start_date").toLocalDate(), rs.getDate("end_date").toLocalDate(), location)
								.build());

		return enrichPayCycleRow(result, location);

	}

	private List<PayCycleRow> enrichPayCycleRow(List<PayCycleRow> pcr, String location) {

		List<DeferredSolveRequest> dsr = deferredSolveRequestRepository.findByRegion(location);

		Map<PeriodKey, List<DeferredSolveRequest>> requestsByPeriod = dsr.stream()
				.collect(Collectors.groupingBy(r -> new PeriodKey(r.getStartDate(), r.getEndDate())));

		List<PayCycleRow> enriched = pcr.stream().map(row -> {

			PeriodKey key = new PeriodKey(row.getStartDate(), row.getEndDate());

			List<DeferredSolveRequest> matches = requestsByPeriod.getOrDefault(key, List.of());

			boolean hasRequest = !matches.isEmpty();

			int empCount = 0;
			int locCount = 0;
			int shiftCount = 0;
			Long rotaId = null;
			String reqStatus = "PENDING";

			Map<ShiftType, Integer> shiftStats = new HashMap<>();
			Map<String, Integer> shiftAssignmentStats = new HashMap<>();

			for (DeferredSolveRequest req : matches) {
				Long rotaIdLong = req.getRotaId();
				if (rotaIdLong != null) {
					Optional<Rota> rotaOpt = rotaRepository.findById(req.getRotaId().intValue());

					if (rotaOpt.isPresent()) {
						Rota rota = rotaOpt.get();
						empCount = rota.empCount();
						locCount = rota.loctionCount();
						shiftCount = rota.shiftCount();
						shiftStats = rota.shiftTypeSummary();
						shiftAssignmentStats = rota.shiftAssignmentStats();
						rotaId = req.getRotaId();
						reqStatus = (req.getCompletedAt() == null) ? "PENDING" : "COMPLETED";
					}
				}
			}
			return PayCycleRow.builder()
					.withCore(row.getId(), row.getName(), row.getStartDate(), row.getEndDate(), row.getLocation())
					.withSolveRequest(hasRequest).withSolevReqStatus(reqStatus)
					.withStats(empCount, locCount, shiftCount, rotaId, shiftStats)
					.withShiftAssignmentStats(shiftAssignmentStats).build();

		}).toList();
		return enriched;
	}
}
