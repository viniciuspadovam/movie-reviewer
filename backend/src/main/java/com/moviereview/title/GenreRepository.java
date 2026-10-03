package com.moviereview.title;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface GenreRepository extends JpaRepository<Genre, Integer> {

	@Query("""
			select distinct g from Title t join t.genres g
			where exists (select 1 from Review r where r.title = t
				and r.status = com.moviereview.review.ReviewStatus.PUBLISHED)
			order by g.name""")
	List<Genre> findAllInUse();

}
