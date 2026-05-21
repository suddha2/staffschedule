package com.midco.rota.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.midco.rota.repository.EmployeeDeviceRepository;
import com.midco.rota.repository.MobileLoginCodeRepository;

/**
 * Revokes a (former) employee's mobile access. Called when an employee is
 * deactivated or deleted: drops their FCM device registrations and any
 * outstanding login codes.
 *
 * <p>The PASETO session token is stateless, so there's nothing to delete for
 * it here — {@code PasetoAuthenticationFilter}'s active-employee check rejects
 * an inactive employee's token on its next request.
 */
@Service
public class EmployeeAccessService {

	private final EmployeeDeviceRepository deviceRepository;
	private final MobileLoginCodeRepository loginCodeRepository;

	public EmployeeAccessService(EmployeeDeviceRepository deviceRepository,
			MobileLoginCodeRepository loginCodeRepository) {
		this.deviceRepository = deviceRepository;
		this.loginCodeRepository = loginCodeRepository;
	}

	@Transactional
	public void revokeMobileAccess(Integer employeeId, String email) {
		if (employeeId != null) {
			deviceRepository.deleteByEmployeeId(employeeId);
		}
		if (email != null && !email.isBlank()) {
			loginCodeRepository.deleteByEmailIgnoreCase(email.trim());
		}
	}
}
