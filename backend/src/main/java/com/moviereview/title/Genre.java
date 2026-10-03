package com.moviereview.title;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity
public class Genre {

	@Id
	private Integer id;

	private String name;

	protected Genre() {
	}

	public Genre(Integer id, String name) {
		this.id = id;
		this.name = name;
	}

	public Integer getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public void rename(String name) {
		this.name = name;
	}

}
