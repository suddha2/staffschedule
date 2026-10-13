package com.midco.rota.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Skills in use, so the employee and shift-template screens offer the same spellings the
 * solver matches on (it compares skills exactly, ignoring case and outer spaces). Taken from
 * active employees, active templates and shift types; most-used first.
 */
@RestController
@RequestMapping("/api/skills")
public class SkillController {

	private final JdbcTemplate jdbc;

	public SkillController(JdbcTemplate jdbc) {
		this.jdbc = jdbc;
	}

	@GetMapping
	public ResponseEntity<List<String>> skills() {
		List<String> skills = jdbc.queryForList("""
				SELECT skill FROM (
				  SELECT trim(x) AS skill FROM employee, unnest(string_to_array(skills, ',')) AS x WHERE active
				  UNION ALL
				  SELECT trim(x) FROM shift_templates, unnest(string_to_array(required_skills, ',')) AS x WHERE active
				  UNION ALL
				  SELECT trim(required_skill) FROM shift_type WHERE required_skill IS NOT NULL
				) s
				WHERE skill <> ''
				GROUP BY skill
				ORDER BY count(*) DESC, skill
				""", String.class);
		return ResponseEntity.ok(skills);
	}
}
