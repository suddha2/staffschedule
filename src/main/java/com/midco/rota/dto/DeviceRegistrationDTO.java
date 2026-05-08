package com.midco.rota.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class DeviceRegistrationDTO {

	@NotBlank
	@Size(max = 512)
	private String fcmToken;

	@Pattern(regexp = "ANDROID|IOS|WEB", message = "platform must be ANDROID, IOS or WEB")
	private String platform = "ANDROID";

	public DeviceRegistrationDTO() {
	}

	public String getFcmToken() {
		return fcmToken;
	}

	public void setFcmToken(String fcmToken) {
		this.fcmToken = fcmToken;
	}

	public String getPlatform() {
		return platform;
	}

	public void setPlatform(String platform) {
		this.platform = platform;
	}
}
