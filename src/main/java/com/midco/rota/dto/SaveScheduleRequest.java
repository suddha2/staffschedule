package com.midco.rota.dto;

import java.util.List;

public class SaveScheduleRequest {
	private Long rotaId;
	private String versionLabel;
	private String comment;
	private String username;
	private List<ShiftAssignmentChangeDTO> changes;
	private boolean pinAllChanges = false;

	public SaveScheduleRequest() {
	}

	public SaveScheduleRequest(Long rotaId, String versionLabel, String comment, String username,
			List<ShiftAssignmentChangeDTO> changes, boolean pinAllChanges) {
		this.rotaId = rotaId;
		this.versionLabel = versionLabel;
		this.comment = comment;
		this.username = username;
		this.changes = changes;
		this.pinAllChanges = pinAllChanges;
	}

	public Long getRotaId() {
		return rotaId;
	}

	public void setRotaId(Long rotaId) {
		this.rotaId = rotaId;
	}

	public String getVersionLabel() {
		return versionLabel;
	}

	public void setVersionLabel(String versionLabel) {
		this.versionLabel = versionLabel;
	}

	public String getComment() {
		return comment;
	}

	public void setComment(String comment) {
		this.comment = comment;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public List<ShiftAssignmentChangeDTO> getChanges() {
		return changes;
	}

	public void setChanges(List<ShiftAssignmentChangeDTO> changes) {
		this.changes = changes;
	}

	public boolean isPinAllChanges() {
		return pinAllChanges;
	}

	public void setPinAllChanges(boolean pinAllChanges) {
		this.pinAllChanges = pinAllChanges;
	}

}
