package com.mogak.npec.board.repository;


import com.mogak.npec.board.domain.Board;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface BoardRepository extends JpaRepository<Board, Long> {
    @Query(value = "select b from Board b join fetch b.member where b.isDeleted = false",
            countQuery = "select count(b) from Board b where b.isDeleted = false")
    Page<Board> findAllByIsDeletedFalse(Pageable pageable);

    @Query("select b from Board b where b.content like %:query% or b.title like %:query%")
    Page<Board> searchBoard(@Param("query") String query, Pageable pageable);

    @Query("select b from Board b join fetch b.member m where b.id in (:boardIds) ")
    List<Board> findAllByIdIn(@Param("boardIds") List<Long> boardIds);
}
